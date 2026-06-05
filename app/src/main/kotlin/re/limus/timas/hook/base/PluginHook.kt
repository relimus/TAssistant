package re.limus.timas.hook.base

import android.app.Application
import android.content.Context
import dalvik.system.BaseDexClassLoader
import re.limus.timas.hook.utils.XLog
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

abstract class PluginHook : SwitchHook() {

    abstract val pluginID: String

    private var pluginClassLoader: ClassLoader? = null
    private var hostClassLoader: ClassLoader? = null
    private var loaderHookPending = false
    private var applicationHookPending = false
    @Volatile
    private var hookDone = false

    override fun onHook(ctx: Context, loader: ClassLoader) {
        if (pluginID.isEmpty()) {
            XLog.e("pluginID is empty for ${this::class.simpleName}")
            return
        }

        hostClassLoader = loader
        hookDone = false
        pluginClassLoader = null
        loaderHookPending = false
        applicationHookPending = false

        try {
            installLoadedPluginHook(ctx, loader)
            hookPluginClassLoaderCreation(ctx, loader)
        } catch (t: Throwable) {
            XLog.d("Hook plugin class loader failed, will retry after Application.onCreate", t)
            hookApplicationOnCreate()
        }
    }

    private fun hookApplicationOnCreate() {
        if (applicationHookPending) return
        applicationHookPending = true

        try {
            val onCreateMethod = Application::class.java.getDeclaredMethod("onCreate")
            onCreateMethod.hookAfter {
                val app = thisObject as? Application ?: return@hookAfter
                if (app.packageName != "com.tencent.tim") return@hookAfter

                try {
                    val loader = hostClassLoader ?: app.classLoader
                    installLoadedPluginHook(app, loader)
                    hookPluginClassLoaderCreation(app, loader)
                } catch (e: Throwable) {
                    XLog.e("Failed to hook plugin loader after onCreate: $pluginID", e)
                }
            }
        } catch (e: Throwable) {
            XLog.e("Failed to hook Application.onCreate for plugin loader: $pluginID", e)
        }
    }

    private fun installLoadedPluginHook(ctx: Context, loader: ClassLoader) {
        if (hookDone || !isLoad) return
        val classLoader = findCachedPluginClassLoader(loader) ?: return
        installPluginHook(ctx, classLoader)
    }

    private fun hookPluginClassLoaderCreation(ctx: Context, loader: ClassLoader) {
        if (loaderHookPending) return
        loaderHookPending = true

        hookPluginStaticClassLoaderMethods(ctx, loader)
        hookBaseDexClassLoaderConstructors(ctx)
    }

    private fun hookPluginStaticClassLoaderMethods(ctx: Context, loader: ClassLoader) {
        val pluginStaticClass = runCatching {
            Class.forName("com.tencent.mobileqq.pluginsdk.PluginStatic", false, loader)
        }.getOrNull() ?: return

        pluginStaticClass.declaredMethods
            .filter { method ->
                ClassLoader::class.java.isAssignableFrom(method.returnType) &&
                    method.parameterTypes.any { it == String::class.java }
            }
            .forEach { method ->
                method.hookAfter {
                    if (hookDone || !isLoad || !args.containsPluginId()) return@hookAfter
                    val classLoader = result as? ClassLoader ?: return@hookAfter
                    installPluginHook(args.firstContextOr(ctx), classLoader)
                }
            }
    }

    private fun hookBaseDexClassLoaderConstructors(ctx: Context) {
        BaseDexClassLoader::class.java.declaredConstructors.forEach { constructor ->
            constructor.hookAfter {
                if (hookDone || !isLoad || !args.containsPluginId()) return@hookAfter
                val classLoader = thisObject as? ClassLoader ?: return@hookAfter
                installPluginHook(ctx, classLoader)
            }
        }
    }

    @Synchronized
    private fun installPluginHook(ctx: Context, classLoader: ClassLoader) {
        if (hookDone || !isLoad) return

        try {
            pluginClassLoader = classLoader
            onPluginHook(ctx, classLoader)
            hookDone = true
        } catch (t: Throwable) {
            pluginClassLoader = null
            XLog.e("Failed to hook plugin: $pluginID", t)
        }
    }

    private fun findCachedPluginClassLoader(loader: ClassLoader): ClassLoader? {
        val pluginStaticClass = runCatching {
            Class.forName("com.tencent.mobileqq.pluginsdk.PluginStatic", false, loader)
        }.getOrNull() ?: return null

        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        pluginStaticClass.declaredFields.forEach { field ->
            if (!Modifier.isStatic(field.modifiers)) return@forEach
            val value = runCatching {
                field.isAccessible = true
                field.get(null)
            }.getOrNull()
            findClassLoaderInValue(value, seen)?.let { return it }
        }
        return null
    }

    private fun findClassLoaderInValue(
        value: Any?,
        seen: MutableSet<Any>,
        ownerMatchesPlugin: Boolean = false
    ): ClassLoader? {
        if (value == null || !seen.add(value)) return null

        if (value is ClassLoader) {
            return if (ownerMatchesPlugin || value.toString().containsPluginId()) value else null
        }

        if (value is Map<*, *>) {
            value.entries.forEach { entry ->
                val entryMatches = ownerMatchesPlugin ||
                    entry.key?.toString()?.containsPluginId() == true ||
                    entry.value?.toString()?.containsPluginId() == true
                findClassLoaderInValue(entry.value, seen, entryMatches)?.let { return it }
            }
            return null
        }

        if (value is Iterable<*>) {
            value.forEach { item ->
                findClassLoaderInValue(item, seen, ownerMatchesPlugin)?.let { return it }
            }
            return null
        }

        val className = value.javaClass.name
        if (className.startsWith("java.") || className.startsWith("android.")) return null

        var currentClass: Class<*>? = value.javaClass
        while (currentClass != null && currentClass != Any::class.java) {
            currentClass.declaredFields.forEach { field ->
                if (Modifier.isStatic(field.modifiers)) return@forEach
                val fieldValue = runCatching {
                    field.isAccessible = true
                    field.get(value)
                }.getOrNull()
                val fieldMatches = ownerMatchesPlugin ||
                    field.name.contains("loader", ignoreCase = true) ||
                    fieldValue?.toString()?.containsPluginId() == true
                findClassLoaderInValue(fieldValue, seen, fieldMatches)?.let { return it }
            }
            currentClass = currentClass.superclass
        }
        return null
    }

    private fun Array<Any?>.containsPluginId(): Boolean {
        return any { (it as? String)?.containsPluginId() == true }
    }

    private fun Array<Any?>.firstContextOr(defaultContext: Context): Context {
        return filterIsInstance<Context>().firstOrNull() ?: defaultContext
    }

    private fun String.containsPluginId(): Boolean {
        val idWithoutSuffix = pluginID.removeSuffix(".apk")
        return equals(pluginID, ignoreCase = true) ||
            contains(pluginID, ignoreCase = true) ||
            equals(idWithoutSuffix, ignoreCase = true) ||
            contains(idWithoutSuffix, ignoreCase = true)
    }

    abstract fun onPluginHook(ctx: Context, pluginLoader: ClassLoader)
}
