package re.limus.timas.ui.model

import androidx.annotation.StringRes
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook

/**
 * Hook 项数据模型，用于 UI 显示
 */
data class HookItem(
    val hook: SwitchHook,
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int?,
    val category: UiCategory,
    val needRestart: Boolean,
    var isEnabled: Boolean = false
)


