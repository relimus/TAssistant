package re.limus.timas.hook.items.qzone

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object HideQzoneAD : SwitchHook(
    R.string.hook_hide_qzone_ad_name,
    R.string.hook_hide_qzone_ad_description
) {

    override val category = UiCategory.QZONE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.qzone.proxy.feedcomponent.model.gdt.QZoneAdFeedDataExtKt".toClass()
            methodName = "isShowingRecommendAd"
            returnType = (Boolean::class.java)
        }.hookBefore {
            result = true
        }
    }
}
