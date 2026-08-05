package re.limus.timas.ui.utils

import android.content.Context
import re.limus.timas.annotations.UiCategory
import re.limus.timas.R

fun UiCategory.getLabel(context: Context): String {
    return when (this) {
        UiCategory.MESSAGE -> context.getString(R.string.tab_message)
        UiCategory.FUNCTION -> context.getString(R.string.tab_function)
        UiCategory.STYLE -> context.getString(R.string.tab_style)
        UiCategory.FILE -> context.getString(R.string.tab_file)
        UiCategory.QZONE -> context.getString(R.string.tab_qzone)
        UiCategory.OTHER -> context.getString(R.string.tab_other)
    }
}