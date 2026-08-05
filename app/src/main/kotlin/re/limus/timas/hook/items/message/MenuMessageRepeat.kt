package re.limus.timas.hook.items.message

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.ContactUtils
import re.limus.timas.api.TIMCustomMenu
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.items.message.core.OnMenuBuilder
import re.limus.timas.hook.items.message.core.RepeatMessageClickListener
import top.sacz.xphelper.ext.callMethod

@RegisterToUI
object MenuMessageRepeat : SwitchHook(
    R.string.hook_menu_message_repeat_name,
    R.string.hook_menu_message_repeat_description
), OnMenuBuilder {

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        //什么都不用写
    }

    override fun onGetMenu(aioMsgItem: Any, targetType: String, param: XC_MethodHook.MethodHookParam) {
        val item = TIMCustomMenu.createMenuItem(
            aioMsgItem,
            "复读",
            R.id.item_msg_repeat,
            R.drawable.ic_repeat
        ) {
            val msgRecord = aioMsgItem.callMethod<Any>("getMsgRecord")
            val listener =
                RepeatMessageClickListener(msgRecord, ContactUtils.getCurrentContact())
            listener.rereading()
        }
        param.result = listOf(item) + param.result as List<*>
    }
}
