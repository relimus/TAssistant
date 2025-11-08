package io.live.timas.hook.utils

import de.robv.android.xposed.XposedBridge
import io.live.timas.R

object XLog {
    fun i(msg: Any? = null) {
        XposedBridge.log("[${R.string.app_name}/I] $msg")
    }
    fun d(msg: Any? = null) {
        XposedBridge.log("[${R.string.app_name}/D] $msg")
    }
    fun w(msg: Any? = null) {
        XposedBridge.log("[${R.string.app_name}/W] $msg")
    }
    fun e(msg: Any? = null, e: Throwable) {
        XposedBridge.log("[${R.string.app_name}/E] $msg")
        XposedBridge.log(e)
    }
}