package re.limus.timas.hook.base

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodHook.Unhook
import de.robv.android.xposed.XposedBridge
import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.dexkit.MethodFinder
import java.lang.reflect.Member
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 定义一个带接收者的函数类型别名。
 * 这使得 lambda 表达式的 `this` 关键字指向一个 MethodHookParam 实例，
 * 从而可以直接访问其属性如 args, result, thisObject 等。
 */
typealias HookAction = MethodHookParam.() -> Unit

/**
 * 所有hook功能的基础类,都应该要继承这个类
 */
abstract class XBridge {

    /**
     * 是否加载
     */
    var isLoad: Boolean = false

    private val unhookRefs: MutableList<Unhook> = CopyOnWriteArrayList()

    fun startLoad() {
        if (isLoad) {
            return
        }
        try {
            isLoad = true
            // 修复：只调用一次initOnce()
            if (initOnce()) {
                onHook(XpHelper.context, XpHelper.classLoader)
            }
        } catch (e: Throwable) {
            XLog.e(e)
        }
    }

    fun initOnce(): Boolean {
        return true
    }
    open fun onHook(ctx: Context, loader: ClassLoader) {}

    // --- Hook 扩展函数 ---
    
    /**
     * 创建 Hook 回调的通用方法
     * 提取公共逻辑，减少重复代码
     * @param priority Hook 优先级，null 表示使用默认优先级
     * @param action Hook 触发时执行的动作
     * @param isBefore true 表示 hookBefore，false 表示 hookAfter
     * @return 创建的 XC_MethodHook 实例
     */
    private fun createHookCallback(
        priority: Int? = null,
        action: HookAction,
        isBefore: Boolean
    ): XC_MethodHook {
        val hook = if (priority != null) {
            object : XC_MethodHook(priority) {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isBefore) tryExecute(param, action)
                }
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isBefore) tryExecute(param, action)
                }
            }
        } else {
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isBefore) tryExecute(param, action)
                }
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isBefore) tryExecute(param, action)
                }
            }
        }
        return hook
    }
    
    /**
     * 注册 Unhook 并返回
     * 统一处理 unhook 的注册逻辑
     */
    private fun registerUnhook(unhook: Unhook?): Unhook? {
        unhook?.let { unhookRefs.add(it) }
        return unhook
    }

    /**
     * [hookBefore] 的扩展函数版本，实现 `findedMethod.hookBefore()` 的调用风格。
     * @receiver 要被Hook的 Method 或 Constructor。
     * @param action Hook 触发时执行的动作。
     * @return 一个 Unhook 对象，可用于取消Hook。
     */
    protected fun Member.hookBefore(action: HookAction): Unhook? {
        return registerUnhook(
            XposedBridge.hookMethod(this, createHookCallback(null, action, isBefore = true))
        )
    }

    protected fun MethodFinder.hookBefore(action: HookAction): Unhook? {
        val member = first()
        return registerUnhook(
            XposedBridge.hookMethod(member, createHookCallback(null, action, isBefore = true))
        )
    }

    protected fun MethodFinder.hookConstructorBefore(action: HookAction): Unhook? {
        val member = firstConstructor()
        return registerUnhook(
            XposedBridge.hookMethod(member, createHookCallback(null, action, isBefore = true))
        )
    }

    /**
     * [hookBefore] 带优先级的扩展函数版本。
     * @receiver 要被Hook的 Method 或 Constructor。
     * @param priority Hook 优先级。
     * @param action Hook 触发时执行的动作。
     * @return 一个 Unhook 对象，可用于取消Hook。
     */
    protected fun Member.hookBefore(priority: Int, action: HookAction): Unhook? {
        return registerUnhook(
            XposedBridge.hookMethod(this, createHookCallback(priority, action, isBefore = true))
        )
    }

    /**
     * [hookAfter] 的扩展函数版本，实现 `findedMethod.hookAfter()` 的调用风格。
     * @receiver 要被Hook的 Method 或 Constructor。
     * @param action Hook 触发时执行的动作。
     * @return 一个 Unhook 对象，可用于取消Hook。
     */
    protected fun Member.hookAfter(action: HookAction): Unhook? {
        return registerUnhook(
            XposedBridge.hookMethod(this, createHookCallback(null, action, isBefore = false))
        )
    }

    protected fun MethodFinder.hookAfter(action: HookAction): Unhook? {
        val member = first()
        return registerUnhook(
            XposedBridge.hookMethod(member, createHookCallback(null, action, isBefore = false))
        )
    }

    protected fun MethodFinder.hookConstructorAfter(action: HookAction): Unhook? {
        val member = firstConstructor()
        return registerUnhook(
            XposedBridge.hookMethod(member, createHookCallback(null, action, isBefore = false))
        )
    }

    /**
     * [hookAfter] 带优先级的扩展函数版本。
     * @receiver 要被Hook的 Method 或 Constructor。
     * @param priority Hook 优先级。
     * @param action Hook 触发时执行的动作。
     * @return 一个 Unhook 对象，可用于取消Hook。
     */
    protected fun Member.hookAfter(priority: Int, action: HookAction): Unhook? {
        return registerUnhook(
            XposedBridge.hookMethod(this, createHookCallback(priority, action, isBefore = false))
        )
    }

    // --- 扩展函数结束 ---

    private fun tryExecute(param: MethodHookParam, hookAction: HookAction) {
        try {
            // 调用带接收者的 lambda
            param.hookAction()
        } catch (throwable: Throwable) {
            XLog.e(throwable)
        }
    }

    fun unload() {
        try {
            unhookRefs.forEach {
                try {
                    it.unhook()
                } catch (_: Throwable) {
                }
            }
            unhookRefs.clear()
            isLoad = false
        } catch (e: Throwable) {
            XLog.e(e)
        }
    }
}
