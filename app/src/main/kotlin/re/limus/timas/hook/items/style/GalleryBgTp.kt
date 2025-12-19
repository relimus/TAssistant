package re.limus.timas.hook.items.style

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object GalleryBgTp : SwitchHook() {

    override val name = "图片背景透明"

    override val description: CharSequence = "查看图片时背景透明"

    override val category = UiCategory.STYLE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            val kRFWLayerAnimPart =
                "com.tencent.richframework.gallery.part.RFWLayerAnimPart".toClass()

            DexFinder.findMethod {
                declaredClass = kRFWLayerAnimPart
                methodName = "updateBackgroundAlpha"
                parameters = arrayOf(Int::class.java)
            }.hookBefore {
                args[0] = 0
            }
        }
    }
}