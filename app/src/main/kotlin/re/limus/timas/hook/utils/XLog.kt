package re.limus.timas.hook.utils

import de.robv.android.xposed.XposedBridge

object XLog {
    fun i(msg: Any? = null) {
        XposedBridge.log("[TAssistant/I] $msg")
    }
    fun d(msg: Any? = null) {
        XposedBridge.log("[TAssistant/D] $msg")
    }
    fun w(msg: Any? = null) {
        XposedBridge.log("[TAssistant/W] $msg")
    }
    fun e(msg: Any? = null) {
        XposedBridge.log("[TAssistant/E] $msg")
    }
}