package re.limus.timas.hook.items.file

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import re.limus.timas.R
import re.limus.timas.activity.SAFAgentActivity
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.XLog

@RegisterToUI
object SAFSelector : SwitchHook(
    R.string.hook_saf_selector_name,
    R.string.hook_saf_selector_description
) {

    override val category = UiCategory.FILE

    private const val FM_ACTIVITY_MARKER = "filemanager.activity.FMActivity"

    override fun onHook(ctx: Context, loader: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = param.args.firstOrNull { it is Intent } as? Intent ?: return
                if (!isFMActivityIntent(intent)) return

                val targetUin = intent.getStringExtra("targetUin")
                    ?: intent.getStringExtra("key_peerUin")
                    ?: intent.getLongExtra("key_peerUin", -1)
                        .takeIf { it > 0 }?.toString()
                targetUin?.toLongOrNull()?.let { if (it in 1L..9999L) return }

                if (isGuildContext()) return

                val context = param.thisObject as? Context ?: return
                try {
                    SAFAgentActivity().launch(context, extractChatExtras(intent))
                    param.result = null
                } catch (e: Throwable) {
                    XLog.e("SAFSelector: launch failed", e)
                }
            }
        }
        XposedBridge.hookAllMethods(ContextWrapper::class.java, "startActivity", hook)
        XposedBridge.hookAllMethods(Activity::class.java, "startActivity", hook)
        XposedBridge.hookAllMethods(Activity::class.java, "startActivityForResult", hook)
    }

    private fun isFMActivityIntent(intent: Intent): Boolean {
        val cls = intent.component?.className ?: return false
        return FM_ACTIVITY_MARKER in cls && !intent.getBooleanExtra("is_decorated", false)
    }

    private fun isGuildContext(): Boolean {
        return Thread.currentThread().stackTrace.any {
            it.className.contains("guild", ignoreCase = true)
        }
    }

    private fun extractChatExtras(intent: Intent): Bundle {
        val extras = intent.extras
        return Bundle().apply {
            extras?.getString("targetUin")?.let { putString("targetUin", it) }
            extras?.getString("key_peerUin")?.let { putString("key_peerUin", it) }
            if (extras?.containsKey("key_peerUin") == true) {
                putLong("key_peerUin_long", extras.getLong("key_peerUin", 0))
            }
            if (extras?.containsKey("peerType") == true) {
                putInt("peerType", extras.getInt("peerType"))
            }
            if (extras?.containsKey("key_chat_type") == true) {
                putInt("key_chat_type", extras.getInt("key_chat_type"))
            }
        }
    }
}
