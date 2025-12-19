package re.limus.timas.api

import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.MethodUtils
import java.lang.reflect.Proxy


class TIMSendMsgTool {
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
}