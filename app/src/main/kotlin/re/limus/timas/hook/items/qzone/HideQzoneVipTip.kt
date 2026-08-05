package re.limus.timas.hook.items.qzone

import android.content.Context
import android.view.View
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object HideQzoneVipTip : SwitchHook(
    R.string.hook_hide_qzone_vip_tip_name,
    R.string.hook_hide_qzone_vip_tip_description
) {

    override val category = UiCategory.QZONE

    override fun onHook(ctx: Context, loader: ClassLoader) {

        val targetClass1 =
            "com.qzone.reborn.feedx.widget.header.QZoneFeedxHeaderVipElement".toClass()
        val targetClass2 = "com.qzone.reborn.feedx.widget.header.ax".toClass()

        DexFinder.findMethod {
            declaredClass = targetClass1
            parameters = arrayOf(View::class.java)
        }.hookConstructorAfter {
            FieldUtils.create(thisObject)
                .fieldName("h")
                .setFirst(thisObject, null)
        }

        DexFinder.findMethod {
            declaredClass = targetClass2
            parameters = arrayOf(View::class.java, Boolean::class.java)
        }.hookConstructorAfter {
            FieldUtils.create(thisObject)
                .fieldName("h")
                .setFirst(thisObject, null)
        }
    }
}
