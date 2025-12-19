package re.limus.timas.hook.items.message.helper

import de.robv.android.xposed.XposedHelpers
import re.limus.timas.api.TIMEnvTool
import re.limus.timas.hook.utils.XLog
import re.limus.timas.hook.utils.cast
import top.sacz.xphelper.ext.toClass

/**
 * 解析群昵称：优先群名片，其次好友昵称，最后回退 uid
 * 通过反射调用 QRoute 和 ITroopMemberListRepoApi，避免编译期依赖
 */
object GroupNickResolver {
    data class NickInfo(val text: String)

    fun getNickOrNull(groupUin: String, memberUin: String): NickInfo? {
        return runCatching {
            val apiClass = "com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi".toClass()
            val api = TIMEnvTool.getQRouteApi(apiClass)
            val info = XposedHelpers.callMethod(
                api,
                "getTroopMemberFromCacheOrFetchAsync",
                groupUin,
                memberUin,
                null,
                "TAssistant-NickResolver",
                null
            ) ?: return null

            val nickInfo = runCatching { XposedHelpers.callMethod(info, "getNickInfo") }.getOrNull()
                ?: runCatching { XposedHelpers.getObjectField(info, "nickInfo") }.getOrNull()
                ?: return null

            val troopNick = (runCatching { XposedHelpers.callMethod(nickInfo, "getTroopNick") }.getOrNull()
                ?: runCatching { XposedHelpers.getObjectField(nickInfo, "troopNick") }.getOrNull()).cast<String?>()
            val friendNick = (runCatching { XposedHelpers.callMethod(nickInfo, "getFriendNick") }.getOrNull()
                ?: runCatching { XposedHelpers.getObjectField(nickInfo, "friendNick") }.getOrNull()).cast<String?>()

            val text = when {
                !troopNick.isNullOrBlank() -> troopNick
                !friendNick.isNullOrBlank() -> friendNick
                else -> null
            } ?: return null
            NickInfo(text)
        }.onFailure { e ->
            XLog.e(e)
        }.getOrNull()
    }
}