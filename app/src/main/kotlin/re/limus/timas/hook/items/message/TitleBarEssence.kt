package re.limus.timas.hook.items.message

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.children
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.api.ContactUtils
import re.limus.timas.api.TIMContactUpdateListener
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.XLog
import re.limus.timas.hook.utils.cast
import re.limus.timas.hook.utils.getDrawableIdByName
import re.limus.timas.util.LayoutHelper
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

@RegisterToUI
object TitleBarEssence : SwitchHook() {

    override val name = "为群聊顶栏添加 精华消息 入口"

    override val description = "点击即可查看历史 精华消息"

    override val category = UiCategory.MESSAGE
    private val Layout_Id = "TitleBarEssence".hashCode()

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = "com.tencent.tim.aio.titlebar.TimRight1VB".toClass()
            returnType = "com.tencent.mobileqq.aio.widget.RedDotImageView".toClass()
        }.hookAfter {
            val view = result.cast<View>()
            val rootView = view.parent.cast<ViewGroup>()

            if (!rootView.children.map { it.id }.contains(Layout_Id)) {
                val imageView = ImageView(view.context).apply {
                    layoutParams = RelativeLayout.LayoutParams(
                        LayoutHelper.dp2px(context, 20f),
                        LayoutHelper.dp2px(context, 20f)
                    ).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        addRule(RelativeLayout.CENTER_VERTICAL)
                        marginEnd = LayoutHelper.dp2px(view.context, 70f)
                    }
                    id = Layout_Id
                    val iconResId = getDrawableIdByName(ctx,"qui_tui_brand_products")
                    setImageResource(iconResId)
                    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                    setColorFilter(if (night) Color.WHITE else Color.BLACK)
                }
                imageView.setOnClickListener {
                    val troopUin = getCurrentGroupUin()
                    try {
                        val browser = loader.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
                        it.context.startActivity(
                            Intent(it.context, browser).apply {
                                putExtra("fling_action_key", 2)
                                putExtra("fling_code_key", it.context.hashCode())
                                putExtra("useDefBackText", true)
                                putExtra("param_force_internal_browser", true)
                                putExtra("url", "https://qun.qq.com/essence/index?gc=$troopUin")
                            }
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // 仅在群聊显示：先定义检查函数，再添加视图并调用
                fun checkAndUpdateVisibility() {
                    val aio = try {
                        TIMContactUpdateListener.getCurrentAIOContact()
                    } catch (_: Throwable) {
                        null
                    }
                    val getUin = aio?.let { ContactUtils.getGroupUinFromAIOContact(it) }
                    imageView.visibility =
                        if (getUin.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
                rootView.addView(imageView)
                checkAndUpdateVisibility()
                imageView.post { checkAndUpdateVisibility() }
            }
        }
    }
    fun getCurrentGroupUin(): String? {
        val aio = try {
            TIMContactUpdateListener.getCurrentAIOContact()
        } catch (e: Throwable) {
            XLog.e(e)
        }
        return aio.let { ContactUtils.getGroupUinFromAIOContact(it) }
    }
}