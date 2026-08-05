package re.limus.timas.hook.items.function

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.ext.getStaticFieldValue
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object ForcePadMode : SwitchHook(
    R.string.hook_force_pad_mode_name,
    R.string.hook_force_pad_mode_description
) {

    override val category = UiCategory.FUNCTION

    override fun onHook(ctx: Context, loader: ClassLoader) {
        val appSettingClass = "com.tencent.common.config.AppSetting".toClass()
        val method = appSettingClass.getDeclaredMethod("f")
        method.hookBefore {
            val pad = appSettingClass.getStaticFieldValue<Int>("g")
            result = pad
        }
    }
}
