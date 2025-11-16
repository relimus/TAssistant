package io.live.timas.hook.items.function

import android.content.Context
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object RemoveForwardNumbersLimit : SwitchHook() {

    override val name = "去除转发消息人数限制"

    override val description = "去除 转发消息 上限9人"

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