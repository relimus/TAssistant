package re.limus.timas.hook.items.function

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object RemoveForwardNumbersLimit : SwitchHook(
    R.string.hook_remove_forward_numbers_limit_name,
    R.string.hook_remove_forward_numbers_limit_description
) {

    override val category = UiCategory.FUNCTION

    override fun onHook(ctx: Context, loader: ClassLoader) {

        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.activity.ForwardRecentActivity".toClass()
        }.hookConstructorAfter {
            FieldUtils.create(thisObject)
                .fieldName("mForwardTargetMap")
                .fieldType(MutableMap::class.java)
                .setFirst(thisObject, UnlimitedMap<String?, Any?>())
        }
    }

    class UnlimitedMap<K, V>(
        private val backing: LinkedHashMap<K, V> = LinkedHashMap()
    ) : MutableMap<K, V> by backing {
        override val size: Int
            get() {
                val realSize = backing.size
                return if (realSize == 9) 8 else realSize
            }
    }
}
