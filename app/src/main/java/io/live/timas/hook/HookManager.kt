package io.live.timas.hook

import io.live.timas.hook.base.SwitchHook
import io.live.timas.hook.generated.HookRegistry
import io.live.timas.ui.model.HookItem
import top.sacz.xphelper.util.ConfigUtils

/**
 * Hook 管理器
 * 负责 Hook 的加载、卸载和状态管理
 */
object HookManager {
    
    private const val PREF_NAME = "TA_Cache"
    private val configUtils = ConfigUtils(PREF_NAME)
    
    // 性能优化：缓存 Hook 列表，避免重复加载
    @Volatile
    private var cachedHooks: List<HookItem>? = null
    
    // 性能优化：缓存启用状态，减少存储访问
    private val enabledStateCache = mutableMapOf<String, Boolean>()
    
    /**
     * 获取所有 Hook 实例
     * 直接使用 KSP 生成的实例列表，完全无需反射
     */
    fun getAllHooks(): List<HookItem> {
        return cachedHooks ?: synchronized(this) {
            cachedHooks ?: run {
                val hooks = HookRegistry.hookInstances.mapNotNull { hookInstance ->
                    try {
                        loadHookItem(hookInstance)
                    } catch (_: Exception) {
                        null
                    }
                }
                cachedHooks = hooks
                hooks
            }
        }
    }
    
    /**
     * 加载单个 Hook 项
     * 直接使用传入的 Hook 实例，无需反射
     */
    private fun loadHookItem(hookInstance: SwitchHook): HookItem {
        val key = getStorageKey(hookInstance)
        val isEnabled = getEnabledState(key)
        
        return HookItem(
            hook = hookInstance,
            name = hookInstance.name,
            description = hookInstance.description,
            category = hookInstance.category,
            needRestart = hookInstance.needRestart,
            isEnabled = isEnabled
        )
    }
    
    /**
     * 检查 Hook 是否启用
     * 使用缓存优化性能，减少存储访问
     */
    fun isEnabled(hook: SwitchHook): Boolean {
        val key = getStorageKey(hook)
        return getEnabledState(key)
    }
    
    /**
     * 设置 Hook 启用状态
     * @param hook Hook 实例
     * @param enabled 是否启用
     */
    fun setEnabled(hook: SwitchHook, enabled: Boolean) {
        val key = getStorageKey(hook)
        
        // 更新存储
        configUtils.put(key, enabled)
        
        // 更新缓存
        enabledStateCache[key] = enabled
        
        // 如果不需要重启，立即加载或卸载
        if (!hook.needRestart) {
            if (enabled) {
                hook.startLoad()
            } else {
                hook.unload()
            }
        }
        
        // 清除 Hook 列表缓存，强制下次重新加载以更新状态
        invalidateHooksCache()
    }
    
    /**
     * 初始化时加载所有已启用的 Hooks
     */
    fun loadEnabledHooks() {
        getAllHooks().forEach { item ->
            if (item.isEnabled && !item.hook.isLoad) {
                item.hook.startLoad()
            }
        }
    }
    
    /**
     * 获取存储键
     * 从 HookRegistry 中直接获取类名，避免反射
     * 格式：{simpleName}_state
     */
    private fun getStorageKey(hook: SwitchHook): String {
        val simpleName = HookRegistry.hookClassNames[hook]
            ?: error("$hook not found in HookRegistry")
        return simpleName
    }
    
    /**
     * 获取启用状态（带缓存）
     */
    private fun getEnabledState(key: String): Boolean {
        return enabledStateCache[key] ?: run {
            val state = configUtils.getBoolean(key, false)
            enabledStateCache[key] = state
            state
        }
    }
    
    /**
     * 清除 Hook 列表缓存
     * 在状态变更时调用，确保下次获取时使用最新状态
     */
    private fun invalidateHooksCache() {
        cachedHooks = null
    }
}



