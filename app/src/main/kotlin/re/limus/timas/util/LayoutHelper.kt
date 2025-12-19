package re.limus.timas.util

import android.content.Context

object LayoutHelper {

    fun dp2px(ctx: Context, value: Float): Int =
        (ctx.resources.displayMetrics.density * value + 0.5f).toInt()
}