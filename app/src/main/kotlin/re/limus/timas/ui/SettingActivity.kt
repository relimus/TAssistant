package re.limus.timas.ui

import android.content.res.Configuration
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import re.limus.timas.R
import re.limus.timas.databinding.ActivitySettingBinding
import re.limus.timas.hook.manager.HookManager
import re.limus.timas.ui.adapter.SettingsPagerAdapter
import re.limus.timas.ui.base.InjectedActivity
import re.limus.timas.ui.utils.getLabel

class SettingActivity : InjectedActivity<ActivitySettingBinding>(ActivitySettingBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TAssistant)
        super.onCreate(savedInstanceState)

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
                usedCategories[position].getLabel(this)
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
