package re.limus.timas.hook

import android.content.Context
import android.content.ContextWrapper
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import re.limus.timas.hook.manager.ApiItemsManager
import re.limus.timas.hook.manager.HookManager
import java.lang.reflect.Method

class HookSteps {
    /**
     * 获取初始的Hook方法
     * 使用 Kotlin 的错误处理机制简化嵌套 try-catch
     *
     * @param loadPackageParam 加载包参数
     * @return 找到的方法，如果找不到则返回 null
     */
    fun getApplicationCreateMethod(loadPackageParam: LoadPackageParam): Method? {
        // 尝试从 Application 类获取方法
        return runCatching {
            val applicationName = loadPackageParam.appInfo.name
            val clz = loadPackageParam.classLoader.loadClass(applicationName)
            
            // 按优先级尝试获取方法：attachBaseContext -> onCreate -> 父类的 attachBaseContext -> 父类的 onCreate
            runCatching { clz.getDeclaredMethod("attachBaseContext", Context::class.java) }.getOrNull()
                ?: runCatching { clz.getDeclaredMethod("onCreate") }.getOrNull()
                ?: runCatching {
                    clz.superclass?.getDeclaredMethod("attachBaseContext", Context::class.java)
                }.getOrNull()
                ?: runCatching {
                    clz.superclass?.getDeclaredMethod("onCreate")
                }.getOrNull()
        }.getOrNull()
            // 如果从 Application 类获取失败，尝试从 ContextWrapper 获取
            ?: runCatching {
                ContextWrapper::class.java.getDeclaredMethod(
                    "attachBaseContext",
                    Context::class.java
                )
            }.getOrNull()
    }
    fun initHook() {
        ApiItemsManager.loadAllApiItems()
        HookManager.loadEnabledHooks()
    }
}