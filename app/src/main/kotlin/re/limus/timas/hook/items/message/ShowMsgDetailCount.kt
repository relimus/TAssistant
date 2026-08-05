package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.cast
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.setFieldValue
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object ShowMsgDetailCount : SwitchHook(
    R.string.hook_show_msg_detail_count_name,
    R.string.hook_show_msg_detail_count_description
) {

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
