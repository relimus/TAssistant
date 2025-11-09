package io.live.timas.hook.items.qzone

import android.content.Context
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object HideQzoneAD : SwitchHook() {

    override val name = "隐藏QQ空间广告"

    override val description: CharSequence = "去除空间 烦人的广告"

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