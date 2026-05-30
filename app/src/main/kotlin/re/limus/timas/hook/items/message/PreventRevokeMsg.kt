package re.limus.timas.hook.items.message

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import com.alibaba.fastjson2.TypeReference
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.TIMMessageViewListener
import re.limus.timas.api.TIMMsgViewAdapter
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.items.message.core.PreventRevokeMsgCore
import re.limus.timas.hook.utils.cast
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.ConstructorUtils
import top.sacz.xphelper.reflect.FieldUtils
import top.sacz.xphelper.reflect.MethodUtils
import top.sacz.xphelper.util.ConfigUtils
import kotlin.collections.get

@RegisterToUI
object PreventRevokeMsg : SwitchHook() {

    override val name = "防撤回"

    override val description = "防止他人消息被撤回, 并带有灰字提示"

    override val category = UiCategory.MESSAGE

    private const val viewId = 0x298382
    private var retractMessageMap: MutableMap<String, MutableList<Int>> = HashMap()
    override fun onHook(ctx: Context, loader: ClassLoader) {
        readData()

        MethodUtils.create($$"com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession$CppProxy")
            .methodName("onMsfPush")
            .params(
                String::class.java,
                ByteArray::class.java,
                "com.tencent.qqnt.kernel.nativeinterface.PushExtraInfo".toClass()
            ).first().hookBefore {
                val cmd = args[0].cast<String>()
                val protoBuf = args[1].cast<ByteArray>()
                if (cmd == "trpc.msg.register_proxy.RegisterProxy.InfoSyncPush") {
                    PreventRevokeMsgCore.handleInfoSyncPush(protoBuf, this)
                } else if (cmd == "trpc.msg.olpush.OlPushService.MsgPush") {
                    PreventRevokeMsgCore.handleMsgPush(protoBuf, this)
                }
            }
        hookAIOMsgUpdate()
    }

    private fun getConfigUtils(): ConfigUtils {
        return ConfigUtils("RevokeMsgDataBase")
    }

    private fun hookAIOMsgUpdate() {
        TIMMessageViewListener.addMessageViewUpdateListener(
            this,
            object : TIMMessageViewListener.OnChatViewUpdateListener {
                override fun onViewUpdateAfter(msgItemView: View, msgRecord: Any) {
                    //约束布局
                    val rootView = msgItemView.cast<ViewGroup>()

                    //防止有撤回 进群等消息类型
                    if (!TIMMsgViewAdapter.hasContentMessage(rootView)) return

                    val peerUid: String = FieldUtils.create(msgRecord)
                        .fieldName("peerUid")
                        .fieldType(String::class.java)
                        .firstValue(msgRecord)

                    val msgSeq: Long = FieldUtils.create(msgRecord)
                        .fieldName("msgSeq")
                        .fieldType(Long::class.javaPrimitiveType).firstValue(msgRecord)

                    //防止错误添加提示没有删除
                    val recallPromptTextView = rootView.findViewById<View>(viewId)
                    if (recallPromptTextView != null) rootView.removeView(recallPromptTextView)
                    //这个msg是秒级的 不是毫秒
                    var msgTime: Long = FieldUtils.create(msgRecord).fieldName("msgTime")
                        .fieldType(Long::class.javaPrimitiveType).firstValue(msgRecord)
                    //变成毫秒级
                    msgTime *= 1000
                    //计算时间差 发送时间低于1秒不判断
                    if ((System.currentTimeMillis() - msgTime) < 1000) {
                        return
                    }
                    //如果有那就是已经撤回的消息
                    if (isRetractMessage(peerUid, msgSeq.toInt())) {
                        addViewToQQMessageView(rootView)
                    }
                }

            })
    }

    private fun addViewToQQMessageView(rootView: ViewGroup) {
        rootView.context
        val parentLayoutId = rootView.id
        val contentId: Int = TIMMsgViewAdapter.getContentViewId()
        //制定约束布局参数 用反射做 不然androidx引用的是模块的而不是QQ自身的
        val newLayoutParams: LayoutParams = ConstructorUtils.newInstance(
            ClassUtils.findClass($$"androidx.constraintlayout.widget.ConstraintLayout$LayoutParams"),
            arrayOf<Class<*>?>(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ),
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).cast<LayoutParams>()
        FieldUtils.create(newLayoutParams)
            .fieldName("startToStart")
            .setFirst(newLayoutParams, parentLayoutId)

        FieldUtils.create(newLayoutParams)
            .fieldName("endToEnd")
            .setFirst(newLayoutParams, parentLayoutId)

        FieldUtils.create(newLayoutParams)
            .fieldName("topToTop")
            .setFirst(newLayoutParams, contentId)
    }

    /**
     * 是否撤回的消息
     */
    private fun isRetractMessage(peerUid: String?, msgSeq: Int): Boolean {
        val seqList = retractMessageMap[peerUid] ?: return false
        return seqList.contains(msgSeq)
    }

    /**
     * 写入本地撤回记录
     */
    fun writeAndRefresh(peerUid: String, msgSeq: Int) {
        var seqList: MutableList<Int>? = retractMessageMap[peerUid]
        if (seqList == null) {
            seqList = ArrayList()
        }
        //往该set添加seq
        seqList.add(msgSeq)
        //刷新map
        retractMessageMap[peerUid] = seqList
        getConfigUtils().put("retractMessageMap", retractMessageMap)
    }

    /**
     * 从本地读取撤回记录数据
     */
    private fun readData() {
        val type = object : TypeReference<MutableMap<String, MutableList<Int>>>() {}
        var localRetractMessageMap = getConfigUtils().getObject("retractMessageMap", type)
        if (localRetractMessageMap == null) {
            localRetractMessageMap = HashMap()
        }
        this.retractMessageMap = localRetractMessageMap
    }
}