package re.limus.timas.hook.items.message.core

import re.limus.timas.hook.base.XBridge
import re.limus.timas.hook.manager.HookManager
import re.limus.timas.hook.items.message.PreventRevokeMsg
import re.limus.timas.hook.utils.XLog

@Suppress("UNCHECKED_CAST")
object Factory {
    /**
     * 获取Hook项实例
     * 优先从HookManager获取，如果没有则创建新实例
     */
    fun <T : XBridge> getItem(clazz: Class<T>): T? {
        return when (clazz) {
            PreventRevokeMsg::class.java -> {
                // 特殊处理 PreventRevokeMsg：直接返回单例实例
                PreventRevokeMsg as T
            }
            else -> {
                // 尝试从HookManager获取已注册的Hook实例
                try {
                    val hookItem = HookManager.getAllHooks().find { it.hook::class.java == clazz }
                    if (hookItem != null) {
                        // 找到匹配的Hook项，直接返回其hook实例
                        hookItem.hook as T
                    } else {
                        // 如果Hook未在HookManager中注册，尝试直接创建实例
                        val newInstance = clazz.getDeclaredConstructor().newInstance()
                        newInstance as T
                    }
                } catch (e: Exception) {
                    XLog.e("Factory.getItem失败: ${clazz.name}\n $e")
                    null
                }
            }
        }
    }
}