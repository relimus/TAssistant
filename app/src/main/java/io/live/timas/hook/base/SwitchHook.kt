package io.live.timas.hook.base

import io.live.timas.annotations.UiCategory

abstract class SwitchHook : XBridge() {
    abstract val name: String
    open val description: CharSequence? = null
    open val category: UiCategory = UiCategory.OTHER
    open val needRestart: Boolean = false
}