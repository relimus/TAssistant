package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object AllLeftSlip : SwitchHook() {

    override val name = "去除左滑消息限制"

    override val description = "开启后也许所有消息均可左滑 (?)"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        val targetClass = "com.tencent.mobileqq.ark.api.impl.ArkHelperImpl".toClass()

        DexFinder.findMethod {
            declaredClass = targetClass
            methodName = "isSupportReply"
            parameters = arrayOf(String::class.java, String::class.java, String::class.java)
        }.hookAfter{
            result = true
        }
    }
}