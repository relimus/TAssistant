package re.limus.timas.hook.items.file

import android.content.Context
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.PluginHook
import re.limus.timas.hook.utils.XLog
import re.limus.timas.hook.utils.cast
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

@RegisterToUI
object TroopFilePermanent : PluginHook() {

    override val name = "群文件转存永久"

    override val description = "群文件长按菜单可转存为永久文件"

    override val category = UiCategory.FILE

    override val pluginID = "troop_plugin.apk"

    private const val KEY_FIELDS = "timas-fields"
    private const val KEY_INFO = "timas-info"
    private const val KEY_TAG = "timas-tag"

    override fun onPluginHook(ctx: Context, pluginLoader: ClassLoader) {
        val adapterClass = runCatching {
            pluginLoader.loadClass("com.tencent.mobileqq.troop.data.TroopFileShowAdapter\$1")
                .getDeclaredField("this\$0").type
        }.getOrElse {
            XLog.e("TroopFilePermanent: TroopFileShowAdapter not found", it)
            return
        }

        val infoClass = adapterClass.declaredFields
            .find { it.type == List::class.java }
            ?.genericType
            ?.let { (it.cast<ParameterizedType?>())?.actualTypeArguments?.get(0).cast<Class<*>>() }
            ?: run {
                XLog.e("TroopFilePermanent: info class not found")
                return
            }

        val itemClass = adapterClass.declaredFields
            .find { it.type == Map::class.java }
            ?.genericType
            ?.let { (it.cast<ParameterizedType?>())?.actualTypeArguments?.get(1).cast<Class<*>>() }
            ?: run {
                XLog.e("TroopFilePermanent: item class not found")
                return
            }

        val targetMethod = itemClass.declaredMethods.find {
            it.returnType == Boolean::class.java &&
                it.parameterTypes.contentEquals(arrayOf(View::class.java))
        } ?: run {
            XLog.e("TroopFilePermanent: target method not found")
            return
        }

        XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val view = param.args[0].cast<View>()
                param.setObjectExtra(KEY_TAG, view.tag)

                val info = getFirstByType(param.thisObject, infoClass) ?: return

                val fields = infoClass.declaredFields.filter { field ->
                    field.isAccessible = true
                    field.type == Int::class.javaPrimitiveType && field.getInt(info) == 102
                }
                if (fields.isEmpty()) return

                param.setObjectExtra(KEY_INFO, info)
                param.setObjectExtra(KEY_FIELDS, fields)
                fields.forEach { it.setInt(info, 114514) }
                view.tag = info
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val fields = param.getObjectExtra(KEY_FIELDS).cast<List<Field>?>() ?: return
                val info = param.getObjectExtra(KEY_INFO) ?: return
                val savedTag = param.getObjectExtra(KEY_TAG)

                fields.forEach { it.setInt(info, 102) }
                (param.args[0].cast<View>()).tag = savedTag
            }
        })
    }

    private fun getFirstByType(obj: Any, type: Class<*>): Any? {
        var clz: Class<*>? = obj.javaClass
        while (clz != null && clz != Any::class.java) {
            for (f in clz.declaredFields) {
                if (f.type != type) continue
                f.isAccessible = true
                return try {
                    f.get(obj)
                } catch (e: IllegalAccessException) {
                    XLog.e(e)
                }
            }
            clz = clz.superclass
        }
        return null
    }
}
