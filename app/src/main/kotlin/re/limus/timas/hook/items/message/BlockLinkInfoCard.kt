package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

// dartcv
@RegisterToUI
object BlockLinkInfoCard: SwitchHook(R.string.hook_block_link_info_card_name) {
    
    override val category = UiCategory.MESSAGE

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.qqnt.kernel.nativeinterface.LinkInfo".toClass()
            paramCount = 5
        }.hookConstructorBefore {
            if (args[0] != null) { 
                result = null
            }
        }
    }
}
