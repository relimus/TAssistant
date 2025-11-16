package io.live.timas.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.google.android.material.tabs.TabLayoutMediator
import io.live.timas.R
import io.live.timas.databinding.ActivitySettingBinding
import io.live.timas.hook.HookManager
import io.live.timas.ui.adapter.SettingsPagerAdapter
import top.sacz.xphelper.activity.BaseActivity

class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TAssistant)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPagerAndTabs()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.collapsingToolbar.title = getString(R.string.app_name)
    }

    private fun setupViewPagerAndTabs() {

        val usedCategories = HookManager.getAllHooks()
            .map { it.category }
            .distinct()
            .sortedBy { it.ordinal }

        val pagerAdapter = SettingsPagerAdapter(this, usedCategories)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.offscreenPageLimit = pagerAdapter.itemCount

        TabLayoutMediator(binding.tabLayoutCategory, binding.viewPager) { tab, position ->
            tab.text = if (position < usedCategories.size) {
                usedCategories[position].displayName
            } else {
                getString(R.string.about)
            }
        }.attach()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }
}
