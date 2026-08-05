package re.limus.timas.hook.items.message

import android.content.Context
import android.content.Intent
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.ContactUtils
import re.limus.timas.api.TIMContactUpdateListener
import re.limus.timas.api.TIMTroopSettingItem
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object TroopSettingEssence : SwitchHook(
    R.string.hook_troop_setting_essence_name,
    R.string.hook_troop_setting_essence_description
) {

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        TIMTroopSettingItem.addItem(
            key = "TAssistantEssence",
            title = "精华消息",
            rightText = "立即查看",
            index = 3
        ) {
            startEssence(it.context)
        }
    }

    private fun startEssence(context: Context) {
        val troopUin = getCurrentGroupUin()
        runCatching {
            val browser = ("com.tencent.mobileqq.activity.QQBrowserDelegationActivity").toClass()
            context.startActivity(
                Intent(context, browser).apply {
                    putExtra("fling_action_key", 2)
                    putExtra("fling_code_key", context.hashCode())
                    putExtra("useDefBackText", true)
                    putExtra("param_force_internal_browser", true)
                    putExtra("url", "https://qun.qq.com/essence/index?gc=$troopUin")
                }
            )
        }.onFailure {
            XLog.e(it)
        }
    }

    private fun getCurrentGroupUin(): String? {
        val aio = try {
            TIMContactUpdateListener.getCurrentAIOContact()
        } catch (e: Throwable) {
            XLog.e(e)
        }
        return aio.let { ContactUtils.getGroupUinFromAIOContact(it) }
    }
}
