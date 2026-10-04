package re.limus.timas.hook.items.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import de.robv.android.xposed.XposedHelpers
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.TIMEnvTool
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object ForceMemberLevel : SwitchHook() {

    private const val BROWSER_ACTIVITY =
        "com.tencent.mobileqq.activity.QQBrowserDelegationActivity"
    private const val GROUP_IDENTITY_URL = "https://qun.qq.com/interactive/userhonor"

    private val rankComputationDepth = ThreadLocal<Int>()

    override val name = "强制显示群员头衔"

    override val description = "恢复 群成员等级与头衔 显示，点击头衔可进入群身份设置"

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        installSimpleUiGate(loader)
        installRankComputationScope(loader)
        installMemberLevelClick(loader)
    }

    private fun installMemberLevelClick(loader: ClassLoader) {
        runCatching {
            val blockClass =
                loader.loadClass("com.tencent.qqnt.aio.nick.memberlevel.AIOTroopMemberLevelBlock")
            val aioMsgItemClass = loader.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            val memberLevelViewGetter = blockClass.declaredMethods.single {
                it.parameterCount == 0 && View::class.java.isAssignableFrom(it.returnType)
            }.apply {
                isAccessible = true
            }

            DexFinder.findMethod {
                declaredClass = blockClass
                parameters = arrayOf(aioMsgItemClass, List::class.java)
                returnType = Void.TYPE
                paramCount = 2
            }.hookAfter {
                val msgItem = args.getOrNull(0) ?: return@hookAfter
                val msgRecord = XposedHelpers.callMethod(msgItem, "getMsgRecord") ?: return@hookAfter
                val troopUin = normalizeUin(
                    XposedHelpers.getObjectField(msgRecord, "peerUin")
                )
                    ?: return@hookAfter
                val memberUin = normalizeUin(
                    XposedHelpers.getObjectField(msgRecord, "senderUin")
                )
                    ?: return@hookAfter
                val memberLevelView = memberLevelViewGetter.invoke(thisObject) as? View
                    ?: return@hookAfter

                memberLevelView.setOnClickListener {
                    startGroupIdentityPage(it.context, troopUin, memberUin)
                }
            }
        }.onFailure {
            XLog.e("Failed to hook troop member level click", it)
        }
    }

    private fun normalizeUin(rawUin: Any?): String? {
        val value = rawUin?.toString()?.takeIf { it.isNotBlank() && it != "0" } ?: return null
        if (value.all(Char::isDigit)) return value
        return runCatching { TIMEnvTool.getUinFromUid(value) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it != "0" }
    }

    private fun startGroupIdentityPage(context: Context, troopUin: String, memberUin: String) {
        runCatching {
            val url = Uri.parse(GROUP_IDENTITY_URL).buildUpon()
                .appendQueryParameter("gc", troopUin)
                .appendQueryParameter("uin", memberUin)
                .appendQueryParameter("_wv", "3")
                .appendQueryParameter("_wwv", "128")
                .build()
                .toString()
            val intent = Intent(context, BROWSER_ACTIVITY.toClass()).apply {
                putExtra("fling_action_key", 2)
                putExtra("fling_code_key", context.hashCode())
                putExtra("useDefBackText", true)
                putExtra("param_force_internal_browser", true)
                putExtra("url", url)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }.onFailure {
            XLog.e("Failed to open group identity browser page", it)
        }
    }

    private fun installSimpleUiGate(loader: ClassLoader) {
        runCatching {
            DexFinder.findMethod {
                declaredClass = loader.loadClass("com.tencent.mobileqq.simpleui.SimpleUIUtil")
                methodName = "getSimpleUISwitch"
            }.hookAfter {
                if ((rankComputationDepth.get() ?: 0) <= 0 || result != true) {
                    return@hookAfter
                }
                result = false
            }
        }.onFailure {
            XLog.e("Failed to hook Simple UI rank gate", it)
        }
    }

    private fun installRankComputationScope(loader: ClassLoader) {
        runCatching {
            val troopInfoClass = loader.loadClass("com.tencent.mobileqq.data.troop.TroopInfo")
            val troopMemberInfoClass =
                loader.loadClass("com.tencent.mobileqq.data.troop.TroopMemberInfo")

            val method = DexFinder.findMethod {
                declaredClass = loader.loadClass("com.tencent.mobileqq.troop.memberlevel.api.impl.TroopMemberLevelUtilsApiImpl")
                methodName = "getTroopMemberRankItem"
                parameters = arrayOf(
                    troopInfoClass,
                    troopMemberInfoClass
                )
            }

            method.hookBefore {
                if (args.getOrNull(0) == null || args.getOrNull(1) == null) return@hookBefore
                rankComputationDepth.set((rankComputationDepth.get() ?: 0) + 1)
                setObjectExtra("ForceMemberLevel.rankScope", true)
            }

            method.hookAfter {
                if (getObjectExtra("ForceMemberLevel.rankScope") != true) return@hookAfter
                val depth = rankComputationDepth.get() ?: 0
                if (depth <= 1) {
                    rankComputationDepth.remove()
                } else {
                    rankComputationDepth.set(depth - 1)
                }
            }
        }.onFailure {
            XLog.e("Failed to hook troop member rank scope", it)
        }
    }
}