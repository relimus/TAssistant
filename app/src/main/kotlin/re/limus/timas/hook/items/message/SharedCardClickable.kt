package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object SharedCardClickable : SwitchHook(
    R.string.hook_shared_card_clickable_name,
    R.string.hook_shared_card_clickable_description
) {

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.aio.msglist.holder.component.ark.d".toClass()
            methodName = "a"
            parameters = arrayOf(String::class.java, String::class.java)
            returnType = Boolean::class.java
        }.hookAfter {
            result = true
        }
    }
}
