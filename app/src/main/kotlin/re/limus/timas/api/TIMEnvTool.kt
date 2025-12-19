package re.limus.timas.api

import de.robv.android.xposed.XposedHelpers
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.MethodUtils


object TIMEnvTool {

    fun getCurrentAccountNickName(): String? {
        try {
            val runTime: Any = getAppRuntime() ?: return null
            return MethodUtils.create(runTime)
                .methodName("getCurrentNickname")
                .returnType(String::class.java)
                .invokeFirst(runTime)
        } catch (_: Exception) {
            return null
        }
    }

    fun getCurrentUin(): String? {
        try {
            val runTime = getAppRuntime() ?: return null
            return MethodUtils.create(runTime)
                .methodName("getCurrentAccountUin")
                .returnType(String::class.java)
                .invokeFirst(runTime)
        } catch (_: Exception) {
            return null
        }
    }

    fun getAppRuntime(): Any? {
        val application: Any =
            MethodUtils.create(ClassUtils.findClass("com.tencent.common.app.BaseApplicationImpl"))
                .methodName("getApplication")
                .returnType(ClassUtils.findClass("com.tencent.common.app.BaseApplicationImpl"))
                .invokeFirstStatic()

        return MethodUtils.create(application)
            .methodName("getRuntime")
            .returnType(ClassUtils.findClass("mqq.app.AppRuntime"))
            .invokeFirst(application)
    }

    /**
     * uin转peerUid
     */
    fun getUidFromUin(uin: String?): String? {
        val o =
            getQRouteApi(ClassUtils.findClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi"))
        return XposedHelpers.callMethod(o, "getUidFromUin", uin) as String?
    }

    /**
     * peerUid转uin
     */
    fun getUinFromUid(uid: String?): String? {
        val o =
            getQRouteApi(ClassUtils.findClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi"))
        return XposedHelpers.callMethod(o, "getUinFromUid", uid) as String?
    }

    fun getQRouteApi(clz: Class<*>?): Any? {
        return MethodUtils.create("com.tencent.mobileqq.qroute.QRoute")
            .methodName("api")
            .params(Class::class.java)
            .invokeFirstStatic(clz)
    }
}