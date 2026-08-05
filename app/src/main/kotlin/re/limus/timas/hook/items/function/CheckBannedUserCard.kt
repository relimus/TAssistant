package re.limus.timas.hook.items.function

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object CheckBannedUserCard : SwitchHook(
    R.string.hook_check_banned_user_card_name,
    R.string.hook_check_banned_user_card_description
) {

    override val category = UiCategory.FUNCTION

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = loader.loadClass("com.tencent.mobileqq.profilecard.activity.TimFriendProfileCardActivity")
            methodName = "onCardUpdate"
        }.hookBefore {
            val card = args[0]
            FieldUtils.setField(card, "forbidCode", 0)
            FieldUtils.setField(card, "isForbidAccount", false)
        }
    }
}
