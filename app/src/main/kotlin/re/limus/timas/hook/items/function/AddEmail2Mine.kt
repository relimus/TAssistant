package re.limus.timas.hook.items.function

import android.content.Context
import android.content.Intent
import android.view.View
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.DexkitFind
import re.limus.timas.hook.utils.XLog
import re.limus.timas.hook.utils.cast
import re.limus.timas.hook.utils.getDrawableIdByName
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.FieldUtils
import top.sacz.xphelper.reflect.MethodUtils

@RegisterToUI
object AddEmail2Mine : SwitchHook() {

    override val name = "添加 邮箱 快捷入口"

    override val description = "在\"我的\"页面添加基于网页的 邮箱 入口"

    override val category = UiCategory.FUNCTION

    private val iconResId = getDrawableIdByName(XpHelper.context, "l4x")
    private const val EMAIL_ID = 13L

    override fun onHook(ctx: Context, loader: ClassLoader) {
        // 在缓存加载前，将Hook项放入
        DexkitFind.mineA2u.hookBefore {
            // 在 'u' 方法执行前，直接修改传入的 list 参数
            val list = args[0].cast<ArrayList<Any>>()

            // 同样需要检查，防止重复添加（例如在 Mine$f 那里也可能被添加）
            val isItemExists = list.any { item ->
                val itemId = MethodUtils.create(item.javaClass)
                    .methodName("getId")
                    .invokeFirst<Long>(item)

                itemId == EMAIL_ID
            }
            if (!isItemExists) {
                // 在 TIM 清空和添加它自己的数据之前，我们就把我们的项加进去
                list.add(createMyItemInfo())
            }
        }

        // Hook i() 方法，阻止为自定义项设置点击事件
        DexkitFind.mineB2i.hookBefore {
            val itemInfo = args[0]
            val itemId = MethodUtils.create(itemInfo.javaClass)
                .methodName("getId")
                .invokeFirst<Long>(itemInfo)
            if (itemId == EMAIL_ID) {
                result = null
            }
        }

        // Hook h() 方法之后，为我们的自定义项设置我们自己的点击事件
        DexkitFind.mineB2h.hookAfter {
            val viewHolder = thisObject
            val itemInfo = args[0]
            val itemId = MethodUtils.create(itemInfo.javaClass)
                .methodName("getId")
                .invokeFirst<Long>(itemInfo)

            if (itemId == EMAIL_ID) {
                try {
                    val itemView = FieldUtils.create(viewHolder.javaClass)
                        .fieldName("f")
                        .first().get(viewHolder).cast<View>()

                    itemView.setOnClickListener {
                        try {
                            val browser = "com.tencent.mobileqq.activity.QQBrowserDelegationActivity".toClass()
                            ctx.startActivity(
                                Intent(ctx, browser).apply {
                                    putExtra("fling_action_key", 2)
                                    putExtra("fling_code_key", ctx.hashCode())
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 将自定义项的创建逻辑提取出来，方便复用
    private fun createMyItemInfo(): Any {
        return DexkitFind.addMineItem
            .newInstance(
                "EMail", // 用于辨识卡片的值
                13, // 使用一个不存在的数, 避免进入switch (该值必须 > 12)
                EMAIL_ID, // 卡片id
                "邮箱", // 标题
                "邮件收发", // 描述
                iconResId, // 图标id
                false, // 展示红点
                true, // 默认展示
            )
    }
}
