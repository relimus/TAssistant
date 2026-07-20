package re.limus.timas.hook.items.function

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object CheckBannedUserCard : SwitchHook() {

    override val name = "查看封号用户资料"

    override val description = "去除 查看封禁账号资料卡 的限制"

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