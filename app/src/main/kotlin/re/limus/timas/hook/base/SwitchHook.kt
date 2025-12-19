package re.limus.timas.hook.base

import re.limus.timas.annotations.UiCategory

abstract class SwitchHook : XBridge() {
    abstract val name: String
    open val description: CharSequence? = null
    open val category: UiCategory = UiCategory.OTHER
    open val needRestart: Boolean = false
}