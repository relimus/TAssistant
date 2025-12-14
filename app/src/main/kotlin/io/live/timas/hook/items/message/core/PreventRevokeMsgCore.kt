package io.live.timas.hook.items.message.core

import android.annotation.SuppressLint
import com.google.protobuf.ByteString
import de.robv.android.xposed.XC_MethodHook
import io.live.timas.api.TIMEnvTool
import io.live.timas.hook.items.message.PreventRevokeMsg
import io.live.timas.hook.items.message.helper.GroupNickResolver
import io.live.timas.hook.items.message.helper.LocalGrayTips
import io.live.timas.hook.utils.XLog
import io.live.trpc.msg.InfoSyncPushOuterClass
import io.live.trpc.msg.MsgPushOuterClass
import io.live.trpc.msg.QQMessageOuterClass
import top.sacz.xphelper.XpHelper.classLoader
import java.lang.reflect.Proxy

/**
 * 防撤回核心解析
 */
object PreventRevokeMsgCore {
    fun handleInfoSyncPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val infoSyncPush = InfoSyncPushOuterClass.InfoSyncPush.parseFrom(buffer)
        val recallMsgSeqList = mutableListOf<Pair<String, Int>>()
        //新代码 构建新的InfoSyncPush
        val newInfoSyncPush = infoSyncPush.toBuilder().apply {
            syncRecallContent = syncRecallContent.toBuilder().apply {
                syncInfoBodyList.forEachIndexed { index, syncInfoBody ->
                    val newMsgList = syncInfoBody.msgList.filter { qqMessage ->
                        val msgType = qqMessage.messageContentInfo.msgType
                        val msgSubType = qqMessage.messageContentInfo.msgSubType
                        val isRecall =
                            (msgType == 732 && msgSubType == 17) || (msgType == 528 && msgSubType == 138)
                        //是私聊消息
                        if (msgType == 528 && msgSubType == 138) {
                            val opInfo = qqMessage.messageBody.operationInfo
                            val c2cRecall = QQMessageOuterClass.QQMessage.MessageBody.C2CRecallOperationInfo
                                .parseFrom(opInfo)
                            val msgSeq = c2cRecall.info.msgSeq
                            val senderUid = qqMessage.messageHead.senderUid
                            recallMsgSeqList.add(senderUid to msgSeq)
                        } else if (msgType == 732 && msgSubType == 17) {
                            //群聊消息
                            val opInfo = qqMessage.messageBody.operationInfo
                            val groupRecall =
                                QQMessageOuterClass.QQMessage.MessageBody.GroupRecallOperationInfo
                                    .parseFrom(opInfo)
                            //groupUin
                            val groupPeerId = groupRecall.peerId.toString()
                            //msg seq
                            val recallMsgSeq = groupRecall.info.msgInfo.msgSeq
                            recallMsgSeqList.add(groupPeerId to recallMsgSeq)
                        }
                        !isRecall
                    }
                    setSyncInfoBody(
                        index,
                        syncInfoBody.toBuilder().clearMsg().addAllMsg(newMsgList).build()
                    )
                }
            }.build()
        }.build()
        param.args[1] = newInfoSyncPush.toByteArray()
        val retracting = Factory.getItem(PreventRevokeMsg::class.java)
        recallMsgSeqList.forEach { (peerId, msgSeq) ->
            retracting?.writeAndRefresh(peerId, msgSeq)
        }
    }

    fun handleMsgPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val msgPush = MsgPushOuterClass.MsgPush.parseFrom(buffer)
        val msg = msgPush.qqMessage
        // 目前用不着
        // val msgTargetUid = msg.messageHead.receiverUid   // 接收人的uid
//        if (msgTargetUid != EnvHelper.getQQAppRuntime().currentUid) return  // 不是当前用户接受就返回
        val msgType = msg.messageContentInfo.msgType
        val msgSubType = msg.messageContentInfo.msgSubType

        val operationInfoByteArray = msg.messageBody.operationInfo.toByteArray()

        when (msgType) {
            732 -> when (msgSubType) {
                17 -> onGroupRecallByMsgPush(operationInfoByteArray, msgPush, param)
            }

            528 -> when (msgSubType) {
                138 -> onC2CRecallByMsgPush(operationInfoByteArray, msgPush, param)
            }
        }
    }


    private fun onC2CRecallByMsgPush(
        operationInfoByteArray: ByteArray,
        msgPush: MsgPushOuterClass.MsgPush,
        param: XC_MethodHook.MethodHookParam
    ) {
        val operationInfo = QQMessageOuterClass.QQMessage.MessageBody.C2CRecallOperationInfo.parseFrom(operationInfoByteArray)
        //msg seq
        val recallMsgSeq = operationInfo.info.msgSeq
        //peerUid
        val operatorUid = operationInfo.info.operatorUid

        //本地消息key 用这个判断是不是已经撤回的消息

        val retracting = Factory.getItem(PreventRevokeMsg::class.java)
        retracting?.writeAndRefresh(operatorUid, recallMsgSeq)


        val newOperationInfoByteArray = operationInfo.toBuilder().apply {
            info = info.toBuilder().apply {
                msgSeq = 1
            }.build()
        }.build().toByteArray()

        val newMsgPush = msgPush.toBuilder().apply {
            qqMessage = qqMessage.toBuilder().apply {
                messageBody = messageBody.toBuilder().apply {
                    setOperationInfo(
                        ByteString.copyFrom(newOperationInfoByteArray)
                    )
                }.build()
            }.build()
        }.build()
        param.args[1] = newMsgPush.toByteArray()

        // 添加本地灰字提示（单聊）
        addLocalGrayTip(
            param,
            "KCHATTYPEC2C",
            operatorUid,
            prefix = "对方尝试撤回",
            seq = recallMsgSeq.toLong()
        )
    }

    @SuppressLint("SuspiciousIndentation")
    private fun onGroupRecallByMsgPush(
        operationInfoByteArray: ByteArray,
        msgPush: MsgPushOuterClass.MsgPush,
        param: XC_MethodHook.MethodHookParam
    ) {
        val firstPart = operationInfoByteArray.copyOfRange(0, 7)
        val secondPart = operationInfoByteArray.copyOfRange(7, operationInfoByteArray.size)

        val operationInfo =
            QQMessageOuterClass.QQMessage.MessageBody.GroupRecallOperationInfo.parseFrom(secondPart)
        //msg seq
        val recallMsgSeq = operationInfo.info.msgInfo.msgSeq
        //group uin
        val groupPeerId = operationInfo.peerId.toString()
        // operator/target uid
        val operatorUid = operationInfo.info.operatorUid
        val targetUid = operationInfo.info.msgInfo.senderUid

        val retracting = Factory.getItem(PreventRevokeMsg::class.java)
        retracting?.writeAndRefresh(groupPeerId, recallMsgSeq)

        val newOperationInfoByteArray = firstPart + (operationInfo.toBuilder().apply {
            msgSeq = 1
            info = info.toBuilder().apply {
                msgInfo = msgInfo.toBuilder().setMsgSeq(1).build()
            }.build()
        }.build().toByteArray())

        val newMsgPush = msgPush.toBuilder().apply {
            qqMessage = qqMessage.toBuilder().apply {
                messageBody = messageBody.toBuilder().apply {
                    setOperationInfo(
                        ByteString.copyFrom(newOperationInfoByteArray)
                    )
                }.build()
            }.build()
        }.build()
        param.args[1] = newMsgPush.toByteArray()

            // 如果是“我自己在群内撤回自己的消息”，不触发灰字提示
            runCatching {
                val myUin = TIMEnvTool.getCurrentUin()
                val myUid = if (!myUin.isNullOrBlank()) TIMEnvTool.getUidFromUin(myUin) else null
                if (!myUid.isNullOrBlank() && operatorUid == myUid && targetUid == myUid) {
                    return
                }
            }

            val operatorUin = TIMEnvTool.getUinFromUid(operatorUid) ?: "0"
            val targetUin = TIMEnvTool.getUinFromUid(targetUid) ?: "0"

            val operatorNick =
                GroupNickResolver.getNickOrNull(groupPeerId, operatorUin)?.text ?: operatorUin

            if (operatorUid == targetUid) {
                LocalGrayTips.send(
                    wrapperSession = param.thisObject,
                    chatTypeField = "KCHATTYPEGROUP",
                    id = groupPeerId,
                    items = listOf(
                        LocalGrayTips.Node.Member(
                            uid = operatorUid,
                            uin = operatorUin,
                            nick = operatorNick
                        ),
                        LocalGrayTips.Node.Text(" 尝试撤回 "),
                        LocalGrayTips.Node.MsgRef("一条消息", recallMsgSeq.toLong()),
                    ),
                    recentAbstract = "$operatorNick 尝试撤回 一条消息"
                )
            } else {
                // 撤回他人：操作者 想撤回 目标 的 消息，已拦截
                val targetNick =
                    GroupNickResolver.getNickOrNull(groupPeerId, targetUin)?.text ?: targetUin
                LocalGrayTips.send(
                    wrapperSession = param.thisObject,
                    chatTypeField = "KCHATTYPEGROUP",
                    id = groupPeerId,
                    items = listOf(
                        LocalGrayTips.Node.Member(
                            uid = operatorUid,
                            uin = operatorUin,
                            nick = operatorNick
                        ),
                        LocalGrayTips.Node.Text(" 尝试撤回 "),
                        LocalGrayTips.Node.Member(
                            uid = targetUid,
                            uin = targetUin,
                            nick = targetNick
                        ),
                        LocalGrayTips.Node.Text(" 的 "),
                        LocalGrayTips.Node.MsgRef("消息", recallMsgSeq.toLong()),
                    ),
                    recentAbstract = "$operatorNick 尝试撤回 $targetNick 的 消息"
                )
            }
    }


    /**
     * 通过 WrapperSession 的 msgService 发送一条本地 Json 灰字系统消息
     */
    private fun addLocalGrayTip(
        param: XC_MethodHook.MethodHookParam,
        chatTypeField: String,
        id: String,
        prefix: String,
        seq: Long,
        suffix: String = ""
    ) {
        runCatching {
            val wrapperSession = param.thisObject ?: return

            // get msgService
            val msgService =
                wrapperSession.javaClass.getMethod("getMsgService").invoke(wrapperSession)
                    ?: return

            // chatType
            val msgConstCls =
                classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.MsgConstant")
            val chatType = msgConstCls.getField(chatTypeField).getInt(null)

            // Contact (prefer nativeinterface.Contact, fallback to kernelpublic) with safe load
            var contactClassName = "com.tencent.qqnt.kernel.nativeinterface.Contact"
            var jsonGrayElementClassName = "com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement"
            var contactCls = runCatching { classLoader.loadClass(contactClassName) }.getOrNull()
            if (contactCls == null) {
                contactClassName = "com.tencent.qqnt.kernelpublic.nativeinterface.Contact"
                jsonGrayElementClassName =
                    "com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement"
                contactCls = classLoader.loadClass(contactClassName)
            }
            val ctor3 = contactCls.constructors.firstOrNull {
                it.parameterTypes.size == 3 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[1] == String::class.java &&
                        it.parameterTypes[2] == String::class.java
            }
            val ctor2 = contactCls.constructors.firstOrNull {
                it.parameterTypes.size == 2 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[1] == String::class.java
            }
            val contact = when {
                ctor3 != null -> ctor3.newInstance(chatType, id, "")
                ctor2 != null -> ctor2.newInstance(chatType, id)
                else -> contactCls.getConstructor(
                    Int::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java
                )
                    .newInstance(chatType, id, "")
            }

            // busiId
            val busiCls = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.JsonGrayBusiId")
            val busiField = when (chatTypeField) {
                "KCHATTYPEGROUP" -> runCatching { busiCls.getField("AIO_AV_GROUP_NOTICE") }.getOrNull()
                else -> runCatching { busiCls.getField("AIO_AV_C2C_NOTICE") }.getOrNull()
            } ?: runCatching { busiCls.getField("AIO_ROBOT_SAFETY_TIP") }.getOrNull()
            val busiId = (busiField?.getInt(null)) ?: 0

            // json body: prefix + 可跳转的消息引用 + suffix

            val safePrefix = prefix.replace("\"", "\\\"")
            val safeSuffix = suffix.replace("\"", "\\\"")
            val jsonStr = """
                {"align":"center","items":[
                  {"_type":"_text","type":"nor","txt":"$safePrefix","col":"1"},
                  {"_type":"_url","type":"url","txt":"一条消息","local_jp":58,"param":{"seq":$seq},"col":"3"},
                  {"_type":"_text","type":"nor","txt":"$safeSuffix","col":"1"}
                ]}
            """.trimIndent()

            // JsonGrayElement
            val elementCls = classLoader.loadClass(jsonGrayElementClassName)
            val xmlParamCls = runCatching {
                val xmlParamClassName = jsonGrayElementClassName.replace("JsonGrayElement", "XmlToJsonParam")
                classLoader.loadClass(xmlParamClassName)
            }.getOrNull()
            val element = elementCls.getConstructor(
                Long::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                xmlParamCls
            ).newInstance(busiId.toLong(), jsonStr, "${prefix}消息${suffix}", false, null)

            // find addLocalJsonGrayTipMsg(Contact, JsonGrayElement, boolean, boolean, IAddJsonGrayTipMsgCallback)
            val addMethod = msgService.javaClass.methods.firstOrNull { m ->
                m.name == "addLocalJsonGrayTipMsg" && m.parameterTypes.size == 5 &&
                        m.parameterTypes[0].name == contactClassName
            } ?: return

            runCatching {
                addMethod.invoke(msgService, contact, element, true, true, null)
            }.onFailure {
                runCatching {
                    val cbType = addMethod.parameterTypes[4]
                    val proxy = Proxy.newProxyInstance(
                        classLoader,
                        arrayOf(cbType)
                    ) { _, _, _ -> null }
                    addMethod.invoke(msgService, contact, element, true, true, proxy)
                }.onFailure { e ->
                    XLog.e(e)
                }
            }
        }.onFailure { e ->
            XLog.e(e)
        }
    }

    /**
     * 通过 wrapperSession 的 uixConvertService 将 uid 转换为 uin；
     * 先用 TIMEnvTool.getUinFromUid，失败再回退到 uixConvertService。
     */

}