package re.limus.timas.api

import android.content.Context
import android.os.Bundle
import android.view.View
import re.limus.timas.hook.base.XBridge
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.FieldUtils
import top.sacz.xphelper.reflect.Ignore
import top.sacz.xphelper.reflect.MethodUtils

class TIMMessageViewListener : XBridge() {

    companion object {

        private val ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP: HashMap<XBridge, OnChatViewUpdateListener> =
            HashMap()

        /**
         * 添加消息监听器 责任链模式
         */
        @JvmStatic
        fun addMessageViewUpdateListener(
            hookItem: XBridge,
            onMsgViewUpdateListener: OnChatViewUpdateListener
        ) {
            ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP[hookItem] = onMsgViewUpdateListener
        }
    }

    override fun onHook(ctx: Context, loader: ClassLoader) {

        MethodUtils.create("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB")
            .returnType(Void.TYPE)
            .params(Int::class.java, Ignore::class.java, List::class.java, Bundle::class.java)
            .first()
            .hookAfter {
                val msgView = FieldUtils.create(thisObject)
                    .fieldType(View::class.java)
                    .firstValue<View>(thisObject)

                val aioMsgItem = FieldUtils.create(thisObject)
                    .fieldType(ClassUtils.findClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"))
                    .firstValue<Any>(thisObject)

                onViewUpdate(aioMsgItem, msgView)
            }
    }

    private fun onViewUpdate(aioMsgItem: Any, msgView: View) {
        val msgRecord: Any = MethodUtils.create(aioMsgItem.javaClass)
            .methodName("getMsgRecord")
            .invokeFirst(aioMsgItem)

        for ((_, listener) in ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP.entries) {
            listener.onViewUpdateAfter(msgView, msgRecord)
        }
    }


    interface OnChatViewUpdateListener {
        fun onViewUpdateAfter(msgItemView: View, msgRecord: Any)
    }
}