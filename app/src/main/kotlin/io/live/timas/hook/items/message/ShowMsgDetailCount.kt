package io.live.timas.hook.items.message

import android.content.Context
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import io.live.timas.hook.utils.cast
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.setFieldValue
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object ShowMsgDetailCount : SwitchHook() {

    override val name = "显示消息具体数量"

    override val description = "消息数量不再显示99+"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        //群消息
        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.quibadge.QUIBadge".toClass()
            methodName = "updateNum"
            returnType = Void.TYPE
            parameters = arrayOf(Int::class.java)
            paramCount = 1
        }.hookBefore {
            val num = args[0].cast<Int>()
            thisObject.apply {
                setFieldValue("mNum", num)
                setFieldValue("mText", num.toString())
            }
            result = null
        }
        //总消息
        DexFinder.findMethod {
            searchPackages = arrayOf("com.tencent.widget")
            returnType = Void.TYPE
            parameters = arrayOf(
                "com.tencent.mobileqq.quibadge.QUIBadge".toClass(),
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            )
            paramCount = 5
        }.hookBefore {
            args[3] = Int.MAX_VALUE
        }
    }
}