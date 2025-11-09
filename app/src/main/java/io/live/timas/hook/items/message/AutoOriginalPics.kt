package io.live.timas.hook.items.message

import android.content.Context
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import top.sacz.xphelper.ext.toMethod

@RegisterToUI
object AutoOriginalPics : SwitchHook() {

    override val name = "自动发送原图"

    override val description = "自动勾选发送原图按钮"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        //半屏相册
        val photoPanelVB = loader.loadClass("com.tencent.mobileqq.aio.panel.photo.PhotoPanelVB")
        val bindViewAndDataMethod = photoPanelVB.getDeclaredMethod("Q0")
        val setCheckedMethod = photoPanelVB.getDeclaredMethod("s", Boolean::class.java)
        bindViewAndDataMethod.hookAfter {
            setCheckedMethod.invoke(thisObject, true)
        }
        val photoFullscreen = "Lcom/tencent/qqnt/qbasealbum/model/Config;->z()Z".toMethod()
        //全屏相册
        photoFullscreen.hookAfter {
            result = true
        }
    }
}