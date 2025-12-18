package io.live.timas.hook.utils

import android.annotation.SuppressLint
import android.content.Context

/**
 * 仿照 YukiHookAPI
 *
 * 一个强转工具函数，用于替代 'as' 关键字。
 * 当你100%确定类型不会错时使用，效果等同于 'as T'。
 *
 * @return 转换成功后的 T 类型对象，如果转换失败则抛出 ClassCastException。
 */
inline fun <reified T> Any?.cast(): T {
    return this as T
}

/**
 * 获取Drawable资源ID方法
 *
 * 优先尝试通过反射直接从 R.drawable 类中获取字段值
 * 如果直接获取失败，则回退到使用 context.resources.getIdentifier 作为备用方案。
 *
 * @param context 上下文对象。
 * @param name 资源名称，例如 "app_icon"。
 * @return 资源的整数ID，如果两种方法都找不到则返回 0。
 */
@SuppressLint("DiscouragedApi")
fun getDrawableIdByName(context: Context, name: String): Int {
    return try {
        // 优先尝试通过反射直接访问 R.drawable 类
        val drawableClass = Class.forName($$"$${context.packageName}.R$drawable")
        val field = drawableClass.getDeclaredField(name)
        field.getInt(null)
    } catch (_: Exception) {
        // 如果失败，回退到 getIdentifier
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
