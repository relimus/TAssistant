package re.limus.timas.hook.items.message.helper

import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.XpHelper.classLoader
import java.lang.reflect.Proxy

/**
 * 本地灰字提示封装：支持 _text/_member/_url 节点，反射调用 TIM 的 addLocalJsonGrayTipMsg
 */
object LocalGrayTips {

    sealed interface Node {
        data class Text(val text: String, val col: String = "1") : Node
        data class Member(
            val uid: String,
            val uin: String,
            val nick: String,
            val col: String = "3"
        ) : Node
        data class MsgRef(
            val text: String,
            val seq: Long,
            val col: String = "3"
        ) : Node
    }

    /**
     * 发送本地灰字（通过 wrapperSession -> msgService -> addLocalJsonGrayTipMsg）
     * @param wrapperSession IQQNTWrapperSession 实例（onMsfPush 的 thisObject）
     * @param chatTypeField  "KCHATTYPEGROUP" 或 "KCHATTYPEC2C"
     * @param id             群ID（群聊）或 uid（单聊）
     * @param items          节点列表：Text/Member/MsgRef
     * @param recentAbstract 最近会话摘要文本
     */
    fun send(
        wrapperSession: Any,
        chatTypeField: String,
        id: String,
        items: List<Node>,
        recentAbstract: String
    ) {
        runCatching {
            // msgService
            val msgService = wrapperSession.javaClass.getMethod("getMsgService").invoke(wrapperSession)
                ?: return

            // chatType（保持使用 kernel.nativeinterface.MsgConstant）
            val msgConstCls = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.MsgConstant")
            val chatType = msgConstCls.getField(chatTypeField).getInt(null)

            // Contact/JsonGrayElement：固定使用 kernelpublic 命名空间
            val contactClassName = "com.tencent.qqnt.kernelpublic.nativeinterface.Contact"
            val jsonGrayElementClassName = "com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement"
            val contactCls = classLoader.loadClass(contactClassName)
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
                ).newInstance(chatType, id, "")
            }

            // 选择 busiId（群聊/单聊），失败回退 ROBOT_SAFETY_TIP
            val busiCls = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.JsonGrayBusiId")
            val busiField = when (chatTypeField) {
                "KCHATTYPEGROUP" -> runCatching { busiCls.getField("AIO_AV_GROUP_NOTICE") }.getOrNull()
                else -> runCatching { busiCls.getField("AIO_AV_C2C_NOTICE") }.getOrNull()
            } ?: runCatching { busiCls.getField("AIO_ROBOT_SAFETY_TIP") }.getOrNull()
            val busiId = (busiField?.getInt(null)) ?: 0

            // 构造 JSON items
            val jsonItems = buildString {
                append("[")
                items.forEachIndexed { index, node ->
                    if (index > 0) append(",")
                    when (node) {
                        is Node.Text -> append("{" + "\"_type\":\"_text\",\"type\":\"nor\",\"txt\":\"" + node.text.replace("\"","\\\"") + "\",\"col\":\"" + node.col + "\"}")
                        is Node.Member -> append("{" + "\"_type\":\"_member\",\"type\":\"qq\",\"uid\":\"" + node.uid + "\",\"jp\":\"" + node.uid + "\",\"uin\":\"" + node.uin + "\",\"tp\":\"0\",\"nm\":\"" + node.nick.replace("\"","\\\"") + "\",\"col\":\"" + node.col + "\"}")
                        is Node.MsgRef -> append("{" + "\"_type\":\"_url\",\"type\":\"url\",\"txt\":\"" + node.text.replace("\"","\\\"") + "\",\"local_jp\":58,\"param\":{\"seq\":" + node.seq + "},\"col\":\"" + node.col + "\"}")
                    }
                }
                append("]")
            }
            val json = "{\"align\":\"center\",\"items\":$jsonItems}"

            // JsonGrayElement
            val elementCls = classLoader.loadClass(jsonGrayElementClassName)
            val xmlParamCls = runCatching { classLoader.loadClass(jsonGrayElementClassName.replace("JsonGrayElement","XmlToJsonParam")) }.getOrNull()
            val element = elementCls.getConstructor(
                Long::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                xmlParamCls
            ).newInstance(busiId.toLong(), json, recentAbstract, false, null)

            // 调用 addLocalJsonGrayTipMsg
            val addMethod = msgService.javaClass.methods.firstOrNull { m ->
                m.name == "addLocalJsonGrayTipMsg" && m.parameterTypes.size == 5 &&
                        m.parameterTypes[0].name == contactClassName
            } ?: return

            runCatching {
                addMethod.invoke(msgService, contact, element, true, true, null)
            }.onFailure {
                val cbType = addMethod.parameterTypes[4]
                val proxy = Proxy.newProxyInstance(classLoader, arrayOf(cbType)) { _, _, _ -> null }
                addMethod.invoke(msgService, contact, element, true, true, proxy)
            }
        }.onFailure { e ->
            XLog.e(e)
        }
    }
}