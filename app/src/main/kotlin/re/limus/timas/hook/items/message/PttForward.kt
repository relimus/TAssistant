package re.limus.timas.hook.items.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.ContactUtils
import re.limus.timas.api.CreateElement
import re.limus.timas.api.TIMCustomMenu
import re.limus.timas.api.TIMSendMsgTool
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.items.message.core.OnMenuBuilder
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.callMethod
import top.sacz.xphelper.ext.getFieldValue
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.util.ActivityTools
import java.io.File

@RegisterToUI
object PttForward : SwitchHook(), OnMenuBuilder {

    override val name = "语音转发"

    override val description = "长按语音消息显示转发按钮，可以转发给 其他好友 或 群"

    override val category = UiCategory.MESSAGE

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.forward.ForwardBaseOption".toClass()
            methodName = "buildConfirmDialog"
            returnType = Void.TYPE
        }.hookBefore {
            val extraData = thisObject.getFieldValue<Bundle>("mExtraData")

            // 检查这是否是我们发起的语音转发请求
            if (!extraData.containsKey("ptt_forward_path")) return@hookBefore

            val pttFilePath = extraData.getString("ptt_forward_path")
            if (pttFilePath == null || !File(pttFilePath).exists()) {
                Toast.makeText(ctx, "语音文件不存在", Toast.LENGTH_SHORT).show()
                return@hookBefore
            }
            // 发送语音
            sendPttFile(pttFilePath, extraData)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun sendPttFile(pttFilePath: String, extraData: Bundle) {
        // 只创建一次消息元素，用于复用
        val msgElement = CreateElement.createPttElement(pttFilePath)

        // 优先处理多选列表
        if (extraData.containsKey("forward_multi_target")) {
            val multiTargetList =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extraData.getParcelableArrayList("forward_multi_target", Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                extraData.getParcelableArrayList<Parcelable>("forward_multi_target")
            }

            multiTargetList?.forEach { target ->
                // 使用反射获取 uin 和 uinType
                val targetUin = target.getFieldValue<String>("uin")
                var targetUinType = target.getFieldValue<Int>("uinType")

                // 模拟 ResultRecord.getUinType() 的逻辑
                // 根据反编译代码，当 uinType 为 -1 且 type 为 4 (联系人) 时，uinType 实际为 1006
                if (targetUinType == -1) {
                    val type = target.getFieldValue<Int>("type")
                    if (type == 4) {
                        targetUinType = 1006
                    }
                }

                // 确保 uin 和 uinType 都有效再发送
                if (targetUinType != -1) {
                    // TIM中 uinType 需要 +1 以匹配正确的聊天类型
                    val targetContact = ContactUtils.getContact(targetUinType + 1, targetUin)
                    TIMSendMsgTool.sendMsg(targetContact, arrayListOf(msgElement))
                }
            }
        } else {
            // 如果没有多选列表，则处理单选情况
            val uin = extraData.getString("uin")
            val uinType = extraData.getInt("uintype", -1)

            if (uin != null && uinType != -1) {
                // TIM中 uinType 需要 +1
                val contact = ContactUtils.getContact(uinType + 1, uin)
                TIMSendMsgTool.sendMsg(contact, arrayListOf(msgElement))
            }
        }
    }

    private fun startForwardIntent(context: Context, filePath: String) {
        val forwardActivityClass = ClassUtils.findClass("com.tencent.mobileqq.activity.ForwardRecentActivity")
        context.startActivity(
            Intent(context, forwardActivityClass).apply {
                // selection_mode 为 2 表示可以选择多个目标
                putExtra("selection_mode", 2)
                putExtra("direct_send_if_dataline_forward", false)
                putExtra("forward_text", filePath)
                putExtra("ptt_forward_path", filePath) // 我们自定义的关键key
                putExtra("forward_type", -1) // -1 代表语音
                putExtra("caller_name", "ChatActivity")
                putExtra("k_smartdevice", false)
                putExtra("k_dataline", false)
                putExtra("is_need_show_toast", true)
                putExtra("k_forward_title", "语音转发")
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }

    override val targetTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent")

    override fun onGetMenu(aioMsgItem: Any, targetType: String, param: XC_MethodHook.MethodHookParam) {
        val item = TIMCustomMenu.createMenuItem(aioMsgItem, "转发", R.id.item_ptt_forward, R.drawable.ic_forward) {
            val msgRecord = aioMsgItem.callMethod<Any>("getMsgRecord")
            val elements = msgRecord.callMethod<ArrayList<*>>("getElements")
            elements.firstNotNullOfOrNull { element ->
                // 找到 语音消息 并获取路径
                element?.callMethod<Any?>("getPttElement")
                    ?.callMethod<String?>("getFilePath")
            }?.let { pttFilePath ->
                startForwardIntent(ActivityTools.getTopActivity(), pttFilePath)
            }
        }
        // 将我们的菜单项添加到列表前面
        param.result = listOfNotNull(item) + (param.result as? List<*> ?: emptyList())
    }
}
