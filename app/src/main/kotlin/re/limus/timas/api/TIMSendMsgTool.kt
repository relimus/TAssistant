package re.limus.timas.api

import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.MethodUtils
import java.lang.reflect.Method
import java.lang.reflect.Proxy


object TIMSendMsgTool {
    /**
     * 发送一条消息
     *
     * @param contact     发送联系人 通过 [ContactUtils] 类创建
     * @param elementList 元素列表 通过 { CreateElement }创建元素
     */
    fun sendMsg(contact: Any?, elementList: ArrayList<Any?>?) {
        if (contact == null) {
            return
        }
        if (elementList == null) {
            return
        }
        val iMsgServiceClass = ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgService")
        val msgServer: Any? = TIMEnvTool.getQRouteApi(iMsgServiceClass)
        MethodUtils.create(msgServer!!.javaClass)
            .params(
                ClassUtils.findClass("com.tencent.qqnt.kernelpublic.nativeinterface.Contact"),
                ArrayList::class.java,
                ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback")
            )
            .returnType(Void.TYPE)
            .methodName("sendMsg")
            .invokeFirst<Any>(
                msgServer,
                contact,
                elementList,
                Proxy.newProxyInstance(
                    ClassUtils.getClassLoader(),
                    arrayOf<Class<*>?>(ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback"))
                ) { proxy, method, args -> // void onResult(int i2, String str);

                    null
                }
            )
    }

    /**
     * 分享消息
     *
     * @param msgIdList         消息id list
     * @param contact           消息id从哪个contact获取
     * @param targetContactList 目标分享聊天会话
     */
    fun forwardMsg(
        msgIdList: ArrayList<Long?>?,
        contact: Any?,
        targetContactList: ArrayList<Any?>?
    ) {
        try {
            val iMsgServiceClass = ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgService")
            val msgServer: Any? = TIMEnvTool.getQRouteApi(iMsgServiceClass)
            val callbackClass =
                ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.IForwardOperateCallback")
            val forwardMsgMethod: Method = MethodUtils.create(msgServer?.javaClass)
                .params(
                    ArrayList::class.java,
                    ClassUtils.findClass("com.tencent.qqnt.kernelpublic.nativeinterface.Contact"),
                    ArrayList::class.java,
                    ArrayList::class.java,
                    callbackClass
                )
                .returnType(Void.TYPE)
                .methodName("forwardMsg")
                .first()
            forwardMsgMethod.invoke(
                msgServer,
                msgIdList,
                contact,
                targetContactList,
                null,
                Proxy.newProxyInstance(
                    ClassUtils.getClassLoader(),
                    arrayOf<Class<*>?>(callbackClass)
                ) { proxy, method, args -> // void onResult(int i2, String str);
                    null
                }
            )
        } catch (e: Exception) {
            XLog.e(e)
        }
    }
}