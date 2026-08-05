package re.limus.timas.hook.items.function

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.View
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.core.view.isVisible
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.TIMPttTool
import re.limus.timas.hook.base.PluginHook
import re.limus.timas.hook.utils.XLog
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.FieldUtils
import java.lang.reflect.Modifier

@RegisterToUI
object SendFavoritePtt : PluginHook(
    R.string.hook_send_favorite_ptt_name,
    R.string.hook_send_favorite_ptt_description
) {
    override val category = UiCategory.FUNCTION
    override val pluginID = "qqfav.apk"

    private const val ACTION = "re.limus.timas.SEND_FAVORITE_PTT"
    @Volatile private var receiverRegistered = false

    override fun onHook(ctx: Context, loader: ClassLoader) {
        registerReceiver()
        super.onHook(ctx, loader)
    }

    override fun onPluginHook(ctx: Context, pluginLoader: ClassLoader) {
        try {
            val favDataClass = pluginLoader.loadClass("com.qqfav.data.FavoriteData")
            val favServiceClass = pluginLoader.loadClass("com.qqfav.FavoriteService")
            val flaClass = pluginLoader.loadClass("com.qqfav.activity.FavoritesListActivity")
            val audioHolderClass = pluginLoader.loadClass("com.qqfav.activity.AudioItemViewHolder")
            val qfavInterfaceClass = pluginLoader.loadClass("com.qqfav.QfavAppInterface")

            // 1. 绕过安全校验
            favServiceClass.declaredMethods.find {
                it.returnType == favDataClass &&
                        it.parameterTypes.contentEquals(arrayOf(Long::class.java, Boolean::class.java))
            }?.hookAfter {
                val data = result ?: return@hookAfter
                FieldUtils.create(data).fieldName("mSecurityBeat").setFirst(data, 0)
            }

            // 2. 去除不支持转发的数量提示
            val adapterField = flaClass.declaredFields.firstOrNull {
                it.type.superclass == BaseAdapter::class.java
            }?.apply { isAccessible = true } ?: return

            adapterField.type.declaredMethods.find {
                it.returnType == Int::class.javaPrimitiveType &&
                    it.parameterTypes.contentEquals(arrayOf(List::class.java))
            }?.hookBefore { result = 0 }

            // 3. 解析共享反射引用
            val ivfField = adapterField.type.declaredFields
                .firstOrNull { Modifier.isPublic(it.modifiers) }
                ?.apply { isAccessible = true } ?: return

            val getUin = ivfField.type.declaredMethods.find {
                it.returnType == String::class.java && it.parameterTypes.isEmpty()
            }?.apply { isAccessible = true }

            val getUinType = ivfField.type.declaredMethods.find {
                it.returnType == Int::class.javaPrimitiveType && it.parameterTypes.isEmpty()
            }?.apply { isAccessible = true }

            val qfavField = TIMPttTool.findFieldByType(flaClass, qfavInterfaceClass)
                ?.apply { isAccessible = true } ?: return

            val getFavService = qfavField.type.declaredMethods.find {
                it.name == "getFavoriteService" && it.parameterTypes.isEmpty()
            }?.apply { isAccessible = true }

            val getFilePath = favServiceClass.declaredMethods.find {
                Modifier.isPublic(it.modifiers) && Modifier.isFinal(it.modifiers) &&
                    it.returnType == String::class.java &&
                    it.parameterTypes.contentEquals(arrayOf(Long::class.javaPrimitiveType))
            }?.apply { isAccessible = true } ?: return

            // 4. 拦截收藏列表转发
            flaClass.declaredMethods.find {
                it.parameterTypes.contentEquals(arrayOf(java.util.ArrayList::class.java))
            }?.hookBefore hook@{
                val favIds = args[0] as? java.util.ArrayList<*> ?: return@hook
                val adapter = adapterField.get(thisObject) ?: return@hook
                val ivf = ivfField.get(adapter) ?: return@hook
                val uin = getUin?.invoke(ivf) as? String ?: return@hook
                val uinType = getUinType?.invoke(ivf) as? Int ?: return@hook
                val service = getFavService?.invoke(qfavField.get(thisObject)) ?: return@hook

                var hasOther = false
                for (idObj in favIds) {
                    val id = (idObj as? Number)?.toLong()
                    if (id == null) { hasOther = true; continue }
                    val path = getFilePath.invoke(service, id) as? String
                    if (!path.isNullOrEmpty()) broadcast(path, uin, uinType)
                    else hasOther = true
                }
                if (!hasOther) {
                    (thisObject as? Activity)?.finish()
                    result = null
                }
            }

            // 5. 拦截搜索结果中的语音单击
            val cbField = TIMPttTool.findFieldByType(audioHolderClass, CheckBox::class.java)
                ?.apply { isAccessible = true } ?: return
            val ivfHolderField = audioHolderClass.superclass?.declaredFields?.firstOrNull {
                it.type.name.contains("com.qqfav.activity")
            }?.apply { isAccessible = true } ?: return
            val favDataField = TIMPttTool.findFieldByType(audioHolderClass, favDataClass)
                ?.apply { isAccessible = true } ?: return
            val getFavId = favDataClass.getMethod("getId").apply { isAccessible = true }
            val baseActivityClass = ClassUtils.findClass("mqq.app.BaseActivity")
            val activityField = TIMPttTool.findFieldByType(audioHolderClass, baseActivityClass)
                ?.apply { isAccessible = true } ?: return

            audioHolderClass.declaredMethods.find {
                it.name == "onClick" && it.parameterTypes.contentEquals(arrayOf(View::class.java))
            }?.hookBefore hook@{
                if (args[0] !is FrameLayout) return@hook
                val cb = cbField.get(thisObject) as? CheckBox ?: return@hook
                if (cb.isVisible) return@hook

                val ivf = ivfHolderField.get(thisObject) ?: return@hook
                val uin = getUin?.invoke(ivf) as? String ?: return@hook
                val uinType = getUinType?.invoke(ivf) as? Int ?: return@hook
                val favData = favDataField.get(thisObject) ?: return@hook
                val favId = getFavId.invoke(favData) as? Long ?: return@hook
                val activity = activityField.get(thisObject) as? Activity ?: return@hook
                val service = getFavService?.invoke(qfavField.get(activity)) ?: return@hook
                val path = getFilePath.invoke(service, favId) as? String

                if (!path.isNullOrEmpty()) broadcast(path, uin, uinType)
                activity.finish()
                result = null
            }
        } catch (t: Throwable) {
            XLog.e(t)
        }
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        if (receiverRegistered) return
        val isMain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            !Application.getProcessName().contains(":")
        } else {
            true
        }
        if (!isMain) return

        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
                    val path = intent.getStringExtra("file_path") ?: return
                    val uin = intent.getStringExtra("uin") ?: return
                    val uinType = intent.getIntExtra("uinType", -1)
                    if (uinType == -1) return
                    try {
                        TIMPttTool.sendPtt(path, uin, uinType)
                    } catch (t: Throwable) {
                        XLog.e(t)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                XpHelper.context.registerReceiver(receiver, IntentFilter(ACTION), Context.RECEIVER_NOT_EXPORTED)
            } else {
                XpHelper.context.registerReceiver(receiver, IntentFilter(ACTION))
            }
            receiverRegistered = true
        } catch (t: Throwable) {
            XLog.e(t)
        }
    }

    private fun broadcast(filePath: String, uin: String, uinType: Int) {
        try {
            XpHelper.context.sendBroadcast(
                Intent(ACTION)
                    .putExtra("file_path", filePath)
                    .putExtra("uin", uin)
                    .putExtra("uinType", uinType)
                    .setPackage("com.tencent.tim")
            )
        } catch (t: Throwable) {
            XLog.e(t)
        }
    }
}
