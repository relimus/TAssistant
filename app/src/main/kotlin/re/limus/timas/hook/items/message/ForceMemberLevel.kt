package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.dexkit.DexFinder

@RegisterToUI
object ForceMemberLevel : SwitchHook(
    R.string.hook_force_member_level_name,
    R.string.hook_force_member_level_description
) {

    private val rankComputationDepth = ThreadLocal<Int>()

    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        installSimpleUiGate(loader)
        installRankComputationScope(loader)
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
