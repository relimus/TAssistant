package re.limus.timas.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import re.limus.timas.annotations.UiCategory
import re.limus.timas.ui.fragment.AboutFragment
import re.limus.timas.ui.fragment.HookListFragment

class SettingsPagerAdapter(
    activity: FragmentActivity,
    private val categories: List<UiCategory>
) : FragmentStateAdapter(activity) {

    // 总页面数 = 分类数 + 1 (关于页面)
    override fun getItemCount(): Int = categories.size + 1

    override fun createFragment(position: Int): Fragment {
        return if (position < categories.size) {
            // 如果是分类页面，创建 HookListFragment
            HookListFragment.newInstance(categories[position])
        } else {
            // 最后一个页面，创建 AboutFragment
            AboutFragment.newInstance()
        }
    }
}
