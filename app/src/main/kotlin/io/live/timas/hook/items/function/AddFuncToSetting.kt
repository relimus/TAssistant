package io.live.timas.hook.items.function

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import io.live.timas.R
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import io.live.timas.hook.utils.XLog
import top.sacz.xphelper.XpHelper.classLoader
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@RegisterToUI
object AddFuncToSetting : SwitchHook() {

    override val name = "添加一些功能至设置页"

    override val description = "添加 QQ频道 & QQ邮箱 入口至设置页"

    override val category = UiCategory.FUNCTION

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

                    // 获取TIM内部图标资源

                    val hashtag = context.resources.getIdentifier(
                        "qui_hashtag",
                        "drawable",
                        context.packageName
                    )

                    val mail = context.resources.getIdentifier(
                        "qui_mail",
                        "drawable",
                        context.packageName
                    )


                    // 创建设置项
                    val item1 = createSettingItem(
                        context, itemClass, R.id.setting_hashtag, "QQ 频道", hashtag
                    ) ?: continue

                    val item2 = createSettingItem(
                        context, itemClass, R.id.setting_mail, "QQ 邮箱", mail
                    ) ?: continue


                    // 创建第二个设置组并添加到列表
                    val itemGroup = ArrayList<Any?>().apply {
                        add(item1)
                        add(item2)
                    }
                    val wrapper = DexFinder.findMethod {
                        declaredClass = wrapperClass
                        paramCount = 5
                    }.firstConstructor()
                        .apply { isAccessible = true }
                        .newInstance(itemGroup, null, null, 6, null) ?: continue

                    // 将两个设置组添加到列表顶部
                    itemGroupList.add(0, wrapper)
                    return itemGroupList
                } catch (_: Exception) {
                    continue
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    /**
     * 创建设置项并设置点击事件
     */
    private fun createSettingItem(
        context: Context,
        itemClass: Class<*>,
        itemId: Int,
        title: String,
        iconResId: Int
    ): Any? {
        return try {
            // 创建设置项
            val item = DexFinder.findMethod {
                parameters = arrayOf(
                    Context::class.java,
                    Int::class.java,
                    CharSequence::class.java,
                    Int::class.java
                )
            }.firstConstructor()
                .apply { isAccessible = true }
                .newInstance(context, itemId, title, iconResId)

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
                            OnClickListener(context, itemClass, itemId)
                        )
                    )
                }
            item
        } catch (_: Exception) {
            null
        }
    }

    private class OnClickListener(
        private val context: Context,
        private val itemClass: Class<*>,
        private val itemId: Int
    ) : InvocationHandler {
        private fun startPdActivity(context: Context) {
            try {
                val browser = "com.tencent.mobileqq.activity.QQBrowserDelegationActivity".toClass()
                context.startActivity(
                    Intent(context, browser).apply {
                        putExtra("fling_action_key", 2)
                        putExtra("fling_code_key", context.hashCode())
                        putExtra("useDefBackText", true)
                        putExtra("param_force_internal_browser", true)
                        putExtra("url", "https://pd.qq.com/")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (e: Exception) {
                XLog.e("启动频道失败: $e")
            }
        }

        private fun startMailActivity(context: Context) {
            try {
                val browser = "com.tencent.mobileqq.activity.QQBrowserDelegationActivity".toClass()
                context.startActivity(
                    Intent(context, browser).apply {
                        putExtra("fling_action_key", 2)
                        putExtra("fling_code_key", context.hashCode())
                        putExtra("useDefBackText", true)
                        putExtra("param_force_internal_browser", true)
                        putExtra("url", "https://mail.qq.com/")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (e: Exception) {
                XLog.e("启动邮箱失败: $e")
            }
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
                    // 根据 itemId 执行不同的操作
                    when (itemId) {
                        R.id.setting_hashtag -> startPdActivity(context)
                        R.id.setting_mail -> startMailActivity(context)
                    }
                }
            } catch (e: Exception) {
                XLog.e(e)
            }
            return null
        }
    }
}