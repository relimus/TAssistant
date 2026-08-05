package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.ext.toMethod

@RegisterToUI
object AutoOriginalPics : SwitchHook(
    R.string.hook_auto_original_pics_name,
    R.string.hook_auto_original_pics_description
) {

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
