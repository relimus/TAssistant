package io.live.timas.ui.model

import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook

/**
 * Hook 项数据模型，用于 UI 显示
 */
data class HookItem(
    val hook: SwitchHook,
    val name: String,
    val description: CharSequence?,
    val category: UiCategory,
    val needRestart: Boolean,
    var isEnabled: Boolean = false
)




