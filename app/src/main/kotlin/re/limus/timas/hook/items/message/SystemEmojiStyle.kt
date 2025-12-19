package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.reflect.MethodUtils

@RegisterToUI
object SystemEmojiStyle : SwitchHook() {

    override val name = "使用系统 Emoji 样式"

    override val description = "如题所示"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        val emojiClass = "com.tencent.mobileqq.text.EmotcationConstants"

        MethodUtils.create(emojiClass)
            .params(Int::class.java)
            .returnType(Int::class.java)
            .first()
            .hookAfter {
                result = -1
            }

        MethodUtils.create(emojiClass)
            .params(Int::class.java, Int::class.java)
            .returnType(Int::class.java)
            .first()
            .hookAfter {
                result = -1
            }
    }
}