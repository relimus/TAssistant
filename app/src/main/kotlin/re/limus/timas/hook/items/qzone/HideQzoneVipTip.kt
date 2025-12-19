package re.limus.timas.hook.items.qzone

import android.content.Context
import android.view.View
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object HideQzoneVipTip : SwitchHook() {

    override val name = "隐藏QQ空间VIP标识"

    override val description =
        "隐藏空间中头像下边的的\"开通VIP\", 会导致右侧小眼睛动画消失 (介意勿用)"

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