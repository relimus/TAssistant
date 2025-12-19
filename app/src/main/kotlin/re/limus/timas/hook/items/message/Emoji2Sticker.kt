package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object Emoji2Sticker : SwitchHook() {

    override val name = "不要超级表情"

    override val description = "将发送的 超级占位置的表情 转为 消息气泡内的小表情"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.aio.utils.t".toClass()
            methodName = "a"
        }.hookBefore {
            result = false
        }
    }
}