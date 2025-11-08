package io.live.timas.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.live.timas.R
import io.live.timas.annotations.UiCategory
import io.live.timas.databinding.ActivitySettingBinding
import io.live.timas.hook.HookManager
import io.live.timas.ui.adapter.HookAdapter
import io.live.timas.ui.model.HookItem
import top.sacz.xphelper.activity.BaseActivity

class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var adapter: HookAdapter
    private var allHooks: List<HookItem> = emptyList()
    private var currentCategory: UiCategory? = null
    private val categoryMap: MutableMap<Int, UiCategory> = mutableMapOf()
    
    // 动画相关
    private var currentAnimator: ObjectAnimator? = null
    private val animationDuration = 250L // Material Design 标准快速动画时长

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TAssistant)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadHooks()
        setupCategoryTabs()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // 设置 CollapsingToolbarLayout 标题
        binding.collapsingToolbar.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView() {
        adapter = HookAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupCategoryTabs() {
        // 清空现有tabs和映射
        binding.tabLayoutCategory.removeAllTabs()
        categoryMap.clear()
        
        // 获取所有使用的category（去重并按枚举顺序排序）
        val usedCategories = allHooks
            .map { it.category }
            .distinct()
            .sortedBy { it.ordinal }
        
        // 为每个category创建tab
        usedCategories.forEachIndexed { index, category ->
            binding.tabLayoutCategory.addTab(
                binding.tabLayoutCategory.newTab().setText(category.displayName)
            )
            categoryMap[index] = category
        }
        
        // 默认选中第一个tab并显示对应内容
        if (usedCategories.isNotEmpty()) {
            currentCategory = usedCategories.first()
            updateHooksWithoutAnimation()
        }

        // Tab 选中监听
        binding.tabLayoutCategory.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentCategory = categoryMap[tab.position]
                updateHooksWithAnimation()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadHooks() {
        allHooks = HookManager.getAllHooks()
    }

    /**
     * 无动画更新（初始化时使用）
     */
    private fun updateHooksWithoutAnimation() {
        val filteredHooks = getFilteredHooks()
        updateAdapterData(filteredHooks)
    }

    /**
     * 带淡入淡出动画的更新（Tab切换时使用）
     */
    private fun updateHooksWithAnimation() {
        // 取消正在进行的动画
        currentAnimator?.cancel()
        currentAnimator = null
        
        val filteredHooks = getFilteredHooks()
        
        // 在动画开始前，强制刷新背景并确保使用当前主题颜色
        forceRefreshBackground()
        
        // 淡出动画
        val fadeOut = ObjectAnimator.ofFloat(binding.recyclerView, "alpha", 1f, 0f)
        fadeOut.duration = animationDuration / 2
        currentAnimator = fadeOut
        
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 淡出完成后，更新内容
                updateAdapterData(filteredHooks)
                
                // 强制刷新背景，确保新内容使用正确的主题颜色
                forceRefreshBackground()
                
                // 淡入动画
                val fadeIn = ObjectAnimator.ofFloat(binding.recyclerView, "alpha", 0f, 1f)
                fadeIn.duration = animationDuration / 2
                currentAnimator = fadeIn
                fadeIn.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        currentAnimator = null
                    }
                })
                fadeIn.start()
            }

            override fun onAnimationCancel(animation: Animator) {
                // 动画被取消时，直接更新数据，不执行淡入
                updateAdapterData(filteredHooks)
                binding.recyclerView.alpha = 1f
                forceRefreshBackground()
                currentAnimator = null
            }
        })
        
        fadeOut.start()
    }
    
    /**
     * 强制刷新背景（同步方式，立即生效）
     */
    private fun forceRefreshBackground() {
        // 获取当前主题的 colorSurface 颜色值并立即设置
        val typedArray = obtainStyledAttributes(intArrayOf(
            com.google.android.material.R.attr.colorSurface
        ))
        try {
            val colorSurface = typedArray.getColor(0, 0)
            if (colorSurface != 0) {
                binding.recyclerView.setBackgroundColor(colorSurface)
                // 立即刷新，确保在动画过程中也能看到正确的颜色
                binding.recyclerView.invalidate()
            }
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * 获取过滤后的Hook列表
     */
    private fun getFilteredHooks(): List<HookItem> {
        return if (currentCategory == null) {
            allHooks
        } else {
            allHooks.filter { it.category == currentCategory }
        }
    }

    /**
     * 更新 Adapter 数据
     */
    private fun updateAdapterData(hooks: List<HookItem>) {
        adapter = HookAdapter(hooks)
        binding.recyclerView.adapter = adapter
    }

    /**
     * 处理配置变更（如深色模式切换）
     * 当系统深色模式切换时，重新应用主题并刷新UI
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleConfigurationChange()
    }
    
    /**
     * 处理配置变更的核心逻辑
     */
    private fun handleConfigurationChange() {
        // 保存当前状态
        val savedState = ConfigurationState(
            category = currentCategory,
            hooks = allHooks.toList()
        )
        
        // 取消可能正在进行的动画
        cancelCurrentAnimation()
        
        // 重新应用主题和重建UI
        applyThemeAndRebuildUI(savedState)
        
        // 恢复之前的状态
        restoreSavedState(savedState)
    }
    
    /**
     * 配置状态数据类
     */
    private data class ConfigurationState(
        val category: UiCategory?,
        val hooks: List<HookItem>
    )
    
    /**
     * 取消当前正在进行的动画
     */
    private fun cancelCurrentAnimation() {
        currentAnimator?.cancel()
        currentAnimator = null
    }
    
    /**
     * 应用主题并重建UI
     */
    private fun applyThemeAndRebuildUI(savedState: ConfigurationState) {
        // 重新应用主题以确保深色模式正确应用
        setTheme(R.style.Theme_TAssistant)
        
        // 重新初始化视图以应用新主题
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        
        // 恢复状态
        allHooks = savedState.hooks
        currentCategory = savedState.category
        
        // 重新设置UI
        setupToolbar()
        setupRecyclerView()
        forceRefreshBackground()
        setupCategoryTabs()
    }
    
    /**
     * 恢复保存的状态
     */
    private fun restoreSavedState(savedState: ConfigurationState) {
        // 恢复之前的选中tab
        val savedCategory = savedState.category
        if (savedCategory != null) {
            val tabIndex = categoryMap.entries.find { it.value == savedCategory }?.key
            if (tabIndex != null && tabIndex < binding.tabLayoutCategory.tabCount) {
                binding.tabLayoutCategory.getTabAt(tabIndex)?.select()
            }
        }
    }
}