package io.live.timas.hook

import android.content.Context
import android.content.ContextWrapper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import top.sacz.xphelper.XpHelper

class HookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        var loadPackageParam: LoadPackageParam? = null
    }

    private val hookSteps: HookSteps = HookSteps()

    override fun initZygote(startupParam: StartupParam) {
        // 初始化 Zygote
        XpHelper.initZygote(startupParam)
    }

    override fun handleLoadPackage(loadParam: LoadPackageParam) {
        if (loadParam.packageName != "com.tencent.tim") return

        loadPackageParam = loadParam
        val applicationCreateMethod = hookSteps.getApplicationCreateMethod(loadParam)

        XposedBridge.hookMethod(applicationCreateMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.thisObject as ContextWrapper
                entryHook(context.baseContext)
            }
        })
    }

    private fun entryHook(context: Context) {
        XpHelper.initContext(context)
        XpHelper.injectResourcesToContext(context)
        hookSteps.initHook()
    }
}
