package re.limus.timas.api

import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.FieldUtils
import top.sacz.xphelper.reflect.MethodUtils
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object TIMTroopTool {
    fun getMemberInfo(group: String?, uin: String?): Any? {
        try {
            val completableFuture = CompletableFuture<Any?>()
            val iTroopMemberListRepoApi: Any? =
                TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi"))

            val fetchTroopMemberInfo = MethodUtils.create(iTroopMemberListRepoApi?.javaClass)
                .methodName("fetchTroopMemberInfo")
                .returnType(Void.TYPE)
                .params(
                    String::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    ClassUtils.findClass("androidx.lifecycle.LifecycleOwner"),
                    String::class.java,
                    Any::class.java
                )
                .first()

            val parameterTypes = fetchTroopMemberInfo.parameterTypes
            val repoClass = parameterTypes[parameterTypes.size - 1]

            fetchTroopMemberInfo.invoke(
                iTroopMemberListRepoApi,
                group, uin, true, null, "TroopMemberListActivity", Proxy.newProxyInstance(
                    ClassUtils.getClassLoader(), arrayOf(repoClass),
                    InvocationHandler { _, method, args ->
                        // 1. 检查是否是回调方法
                        if (method.returnType == Void.TYPE && method.parameterTypes.size == 1) {
                            completableFuture.complete(args?.get(0))
                            return@InvocationHandler null
                        } else {
                            // 2. 修复点：使用 args ?: arrayOf() 确保非空后再展开
                            // 同时注意：invoke 的目标通常应该是 proxy 对象或实际实现对象
                            return@InvocationHandler method.invoke(iTroopMemberListRepoApi, *(args ?: arrayOf()))
                        }
                    }
                )
            )
            return completableFuture.get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            XLog.e("plugin api", e)
            return null
        }
    }

    fun getMemberName(group: String?, uin: String?): String? {
        try {
            val memberInfo = getMemberInfo(group, uin)
            val nickInfo = FieldUtils.create(memberInfo)
                .fieldName("nickInfo")
                .firstValue<Any?>(memberInfo)!!
            return MethodUtils.create(nickInfo)
                .methodName("getShowName")
                .returnType(String::class.java)
                .invokeFirst(nickInfo)
        } catch (e: Exception) {
            XLog.e("plugin api", e)
            return null
        }
    }
}