package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.cast
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object ShowAccurateGaggedTime: SwitchHook() {

    override val name = "显示更详细的禁言时间"

    override val description = "禁言时间精确到秒"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.qqnt.troop.impl.TroopGagUtils".toClass()
            parameters = arrayOf(Long::class.java)
            returnType = String::class.java
        }.hookBefore {

            val time = args[0].cast<Long>()
            result = if (time <= 0) {
                "0秒"
            } else {
                formatDuration(time)
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}时")
            if (minutes > 0) append("${minutes}分")
            if (secs > 0 || isEmpty()) append("${secs}秒")
        }
    }
}