package re.limus.timas.hook.items.message.core

import android.annotation.SuppressLint
import com.google.protobuf.ByteString
import de.robv.android.xposed.XC_MethodHook
import re.limus.timas.api.TIMEnvTool
import re.limus.timas.hook.items.message.PreventRevokeMsg
import re.limus.timas.hook.items.message.helper.GroupNickResolver
import re.limus.timas.hook.items.message.helper.LocalGrayTips
import re.limus.trpc.msg.InfoSyncPushOuterClass
import re.limus.trpc.msg.MsgPushOuterClass
import re.limus.trpc.msg.QQMessageOuterClass

/**
 * 防撤回核心解析
 */
object PreventRevokeMsgCore {

    private const val TYPE_GROUP = 732
    private const val SUB_TYPE_GROUP_RECALL = 17
    private const val TYPE_C2C = 528
    private const val SUB_TYPE_C2C_RECALL = 138

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
                        val isRecall = isRecallMsg(msgType, msgSubType)

                        if (isRecall) {
                            runCatching {
                                //是私聊消息
                                if (msgType == TYPE_C2C && msgSubType == SUB_TYPE_C2C_RECALL) {
                                    val c2cRecall = QQMessageOuterClass.QQMessage.MessageBody.C2CRecallOperationInfo.parseFrom(qqMessage.messageBody.operationInfo)
                                    recallMsgSeqList.add(qqMessage.messageHead.senderUid to c2cRecall.info.msgSeq)
                                } else
                                //群聊消息
                                    if (msgType == TYPE_GROUP && msgSubType == SUB_TYPE_GROUP_RECALL) {
                                    val groupRecall = QQMessageOuterClass.QQMessage.MessageBody.GroupRecallOperationInfo.parseFrom(qqMessage.messageBody.operationInfo)
                                    recallMsgSeqList.add(groupRecall.peerId.toString() to groupRecall.info.msgInfo.msgSeq)
                                }
                            }
                        }
                        !isRecall
                    }
                    setSyncInfoBody(index, syncInfoBody.toBuilder().clearMsg().addAllMsg(newMsgList).build())
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
        val msgType = msg.messageContentInfo.msgType
        val msgSubType = msg.messageContentInfo.msgSubType
        val opBytes = msg.messageBody.operationInfo.toByteArray()

        when (msgType) {
            TYPE_GROUP if msgSubType == SUB_TYPE_GROUP_RECALL -> onGroupRecallByMsgPush(opBytes, msgPush, param)
            TYPE_C2C if msgSubType == SUB_TYPE_C2C_RECALL -> onC2CRecallByMsgPush(opBytes, msgPush, param)
        }
    }

    private fun onC2CRecallByMsgPush(
        opBytes: ByteArray,
        msgPush: MsgPushOuterClass.MsgPush,
        param: XC_MethodHook.MethodHookParam
    ) {
        val opInfo = QQMessageOuterClass.QQMessage.MessageBody.C2CRecallOperationInfo.parseFrom(opBytes)
        val recallMsgSeq = opInfo.info.msgSeq
        val operatorUid = opInfo.info.operatorUid

        Factory.getItem(PreventRevokeMsg::class.java)?.writeAndRefresh(operatorUid, recallMsgSeq)

        // 修改 Seq 拦截 UI 撤回
        val newOpBytes = opInfo.toBuilder().apply {
            info = info.toBuilder().setMsgSeq(1).build()
        }.build().toByteArray()

        param.args[1] = modifyMsgPushOpInfo(msgPush, newOpBytes)

        // 使用统一的 LocalGrayTips 发送提示
        LocalGrayTips.send(
            wrapperSession = param.thisObject,
            chatTypeField = "KCHATTYPEC2C",
            id = operatorUid,
            items = listOf(
                LocalGrayTips.Node.Text("对方尝试撤回 "),
                LocalGrayTips.Node.MsgRef("一条消息", recallMsgSeq.toLong())
            ),
            recentAbstract = "对方尝试撤回一条消息"
        )
    }

    @SuppressLint("SuspiciousIndentation")
    private fun onGroupRecallByMsgPush(
        opBytes: ByteArray,
        msgPush: MsgPushOuterClass.MsgPush,
        param: XC_MethodHook.MethodHookParam
    ) {
        // 保持原有的 7字节前缀逻辑
        val firstPart = opBytes.copyOfRange(0, 7)
        val secondPart = opBytes.copyOfRange(7, opBytes.size)

        val opInfo = QQMessageOuterClass.QQMessage.MessageBody.GroupRecallOperationInfo.parseFrom(secondPart)
        //msg seq
        val recallMsgSeq = opInfo.info.msgInfo.msgSeq
        //group uin
        val groupPeerId = opInfo.peerId.toString()
        // operator/target uid
        val operatorUid = opInfo.info.operatorUid
        val targetUid = opInfo.info.msgInfo.senderUid

        Factory.getItem(PreventRevokeMsg::class.java)?.writeAndRefresh(groupPeerId, recallMsgSeq)

        val newOpBytes = firstPart + opInfo.toBuilder().apply {
            msgSeq = 1
            info = info.toBuilder().apply {
                msgInfo = msgInfo.toBuilder().setMsgSeq(1).build()
            }.build()
        }.build().toByteArray()

        param.args[1] = modifyMsgPushOpInfo(msgPush, newOpBytes)

        // 过滤“我撤回我自己”
        runCatching {
            val myUid = TIMEnvTool.getCurrentUin()?.let { TIMEnvTool.getUidFromUin(it) }
            if (!myUid.isNullOrBlank() && operatorUid == myUid && targetUid == myUid) return
        }

        val operatorUin = TIMEnvTool.getUinFromUid(operatorUid) ?: "0"
        val targetUin = TIMEnvTool.getUinFromUid(targetUid) ?: "0"
        val operatorNick = GroupNickResolver.getNickOrNull(groupPeerId, operatorUin)?.text ?: operatorUin

        val nodes = mutableListOf<LocalGrayTips.Node>()
        nodes.add(LocalGrayTips.Node.Member(operatorUid, operatorUin, operatorNick))
        nodes.add(LocalGrayTips.Node.Text(" 尝试撤回 "))

        val recentAbstract: String
        if (operatorUid == targetUid) {
            nodes.add(LocalGrayTips.Node.MsgRef("一条消息", recallMsgSeq.toLong()))
            recentAbstract = "$operatorNick 尝试撤回 一条消息"
        } else {
            val targetNick = GroupNickResolver.getNickOrNull(groupPeerId, targetUin)?.text ?: targetUin
            nodes.add(LocalGrayTips.Node.Member(targetUid, targetUin, targetNick))
            nodes.add(LocalGrayTips.Node.Text(" 的 "))
            nodes.add(LocalGrayTips.Node.MsgRef("消息", recallMsgSeq.toLong()))
            recentAbstract = "$operatorNick 尝试撤回 $targetNick 的 消息"
        }

        LocalGrayTips.send(
            wrapperSession = param.thisObject,
            chatTypeField = "KCHATTYPEGROUP",
            id = groupPeerId,
            items = nodes,
            recentAbstract = recentAbstract
        )
    }

    private fun isRecallMsg(type: Int, subType: Int) =
        (type == TYPE_GROUP && subType == SUB_TYPE_GROUP_RECALL) || (type == TYPE_C2C && subType == SUB_TYPE_C2C_RECALL)

    private fun modifyMsgPushOpInfo(msgPush: MsgPushOuterClass.MsgPush, newOp: ByteArray): ByteArray {
        return msgPush.toBuilder().apply {
            qqMessage = qqMessage.toBuilder().apply {
                messageBody = messageBody.toBuilder().apply {
                    setOperationInfo(ByteString.copyFrom(newOp))
                }.build()
            }.build()
        }.build().toByteArray()
    }
}
