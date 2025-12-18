package io.live.timas.hook

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import io.live.timas.R
import io.live.timas.annotations.ApiItems
import io.live.timas.hook.base.XBridge
import io.live.timas.hook.utils.XLog
import io.live.timas.ui.SettingActivity
import top.sacz.xphelper.XpHelper.classLoader
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@ApiItems
object InjectSetting : XBridge() {

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.mobileqq.setting.main.MainSettingConfigProvider".toClass()
            parameters = arrayOf(Context::class.java)
            returnType = List::class.java
        }.hookAfter {
            try {
                val ctx = args?.get(0) as? Context ?: return@hookAfter
                val list = result as? List<*> ?: return@hookAfter
                processSettingList(ctx, list)?.let { result = it }
            } catch (e: Exception) {
                XLog.e("Hook MainSettingConfigProvider 失败: $e")
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun processSettingList(context: Context, originalResult: List<*>): List<Any?>? {
        try {
            val itemGroupList = originalResult.toMutableList()
            if (itemGroupList.isEmpty()) return null

            val wrapperClass = itemGroupList[0]!!.javaClass

            for (wrapper in itemGroupList) {
                try {
                    val itemList = wrapper!!.javaClass.declaredFields
                        .singleOrNull { it.type == List::class.java }
                        ?.apply { isAccessible = true }
                        ?.get(wrapper) as? List<*> ?: continue

                    if (itemList.isEmpty()) continue

                    val firstItem = itemList[0] ?: continue
                    if (!firstItem.javaClass.name.contains("com.tencent.mobileqq.setting")) continue

                    val itemClass = firstItem.javaClass

                    // 获取TIM内部图标资源ID
                    val iconResId = context.resources.getIdentifier(
                        "qui_tuning",
                        "drawable",
                        context.packageName
                    )

                    // 创建设置项
                    val item = DexFinder.findMethod {
                        parameters = arrayOf(
                            Context::class.java,
                            Int::class.java,
                            CharSequence::class.java,
                            Int::class.java
                        )
                    }.firstConstructorOrNull()
                        .apply { isAccessible = true }.newInstance(
                        context, R.id.inject_setting, "TAssistant", iconResId
                    )

                    // 设置点击事件
                    val functionClass = classLoader.loadClass("kotlin.jvm.functions.Function0")
                    DexFinder.findMethod {
                        declaredClass = itemClass
                        paramCount = 1
                        returnType = Void.TYPE
                    }.first()
                        .apply {
                            isAccessible = true
                            invoke(
                                item, Proxy.newProxyInstance(
                                    classLoader, arrayOf(functionClass),
                                    OnClickListener(context, itemClass)
                                )
                            )
                        } ?: continue

                    // 创建设置组并添加到列表
                    val itemGroup = ArrayList<Any?>().apply { add(item) }

                    DexFinder.findMethod {
                        declaredClass = wrapperClass
                        paramCount = 5
                    }.firstConstructorOrNull()
                        .apply { isAccessible = true }
                        .newInstance(itemGroup, null, null, 6, null)
                        .let {
                            itemGroupList.add(0, it)
                            return itemGroupList
                        }
                } catch (_: Exception) {
                    continue
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private class OnClickListener(
        private val context: Context,
        private val itemClass: Class<*>
    ) : InvocationHandler {

        private fun startSettingsActivity(context: Context) {
            // 创建 Intent
            val intent = Intent(context, SettingActivity::class.java)
            intent.putExtra(
                "proxy_target_activity",
                "com.tencent.mobileqq.activity.GeneralSettingActivity"
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
            try {
                if (Thread.currentThread().stackTrace.any {
                        it.className.startsWith(itemClass.name) &&
                                try {
                                    val stackClass =
                                        Class.forName(it.className, false, itemClass.classLoader)
                                    stackClass.interfaces.isNotEmpty() &&
                                            stackClass.interfaces[0] == View.OnClickListener::class.java &&
                                            it.methodName == "onClick"
                                } catch (_: Exception) {
                                    false
                                }
                    }) {
                    startSettingsActivity(context)
                }
            } catch (e: Exception) {
                XLog.e(e)
            }
            return null
        }
    }
}