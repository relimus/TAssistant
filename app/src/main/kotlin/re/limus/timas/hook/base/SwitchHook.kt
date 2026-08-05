package re.limus.timas.hook.base

import android.content.Context
import androidx.annotation.StringRes
import re.limus.timas.annotations.UiCategory

abstract class SwitchHook(
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int? = null
) : XBridge() {
    open val category: UiCategory = UiCategory.OTHER
    open val needRestart: Boolean = false
    open fun onclick(context: Context) {}
}
