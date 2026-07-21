package re.limus.timas.hook.items.file

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.PluginHook
import re.limus.timas.hook.items.file.helper.PbandkMessageAccessor
import top.sacz.xphelper.XpHelper
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

@RegisterToUI
object ShowDownloadTimes : PluginHook() {

    override val name = "显示文件下载次数"

    override val description = "群文件显示具体下载次数"

    override val category = UiCategory.FILE

    override val pluginID = "troop_plugin.apk"

    private const val REPO_CLASS_NAME = "group_file.group_file_common.repo.GroupFileListRepo"
    private const val CONTINUATION_CLASS_NAME = "kotlin.coroutines.Continuation"
    private const val EXTRA_COUNT_MAP = "timas-show-download-times-count-map"
    private const val MUTABLE_STATE_CLASS_NAME = "androidx.compose.runtime.MutableState"

    private val adapterClassNames = arrayOf(
        "com.tencent.mobileqq.troop.file.data.TroopFileShowAdapter",
        "com.tencent.mobileqq.troop.data.TroopFileShowAdapter"
    )

    private val downloadTimesRegex = Regex("uint32_download_times=(\\d+)")
    private val fileIdRegex = Regex("fileId=([^,]+)")
    private val expiresAfterDaysRegex = Regex("\\d+天后到期")
    private val displayedDownloadTimesRegex = Regex("\\d+\\s*次")
    private val separatorBeforeExpiryRegex = Regex("\\s*·\\s*$")
    private val mutableStateFields = ConcurrentHashMap<Class<*>, List<Field>>()
    private val stateValueMethods = ConcurrentHashMap<Class<*>, Method>()

    override fun onPluginHook(ctx: Context, pluginLoader: ClassLoader) {
        installAdapterHook(pluginLoader)
        installRepoHooks(pluginLoader)
    }

    private fun installAdapterHook(pluginLoader: ClassLoader): Boolean {
        val adapterClass = findAdapterClass(pluginLoader) ?: return false
        val methods = (adapterClass.declaredMethods.asList() + adapterClass.methods.asList()).distinct()
        val getView = methods.firstOrNull { method ->
            method.name == "getView" &&
                View::class.java.isAssignableFrom(method.returnType) &&
                method.parameterTypes.contentEquals(
                    arrayOf(Int::class.javaPrimitiveType, View::class.java, ViewGroup::class.java)
                )
        } ?: return false
        val getItem = methods.firstOrNull { method ->
            method.name == "getItem" &&
                method.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
        } ?: return false

        getItem.isAccessible = true
        getView.hookAfter {
            if (args[1] != null) return@hookAfter

            val position = args[0] as? Int ?: return@hookAfter
            val fileInfo = getItem.invoke(thisObject, position) ?: return@hookAfter
            val root = result as? ViewGroup ?: return@hookAfter
            val descriptionView = root.findTextViewWithTag(fileInfo) ?: return@hookAfter

            descriptionView.doAfterTextChanged watcher@{ editable ->
                val content = editable ?: return@watcher
                val count = extract(descriptionView.tag?.toString().orEmpty(), downloadTimesRegex)
                    ?: return@watcher
                if (count != "0") {
                    content.insertDownloadTimes(count)
                }
            }
        }
        return true
    }

    private fun installRepoHooks(pluginLoader: ClassLoader): Int {
        val repoClass = loadTargetClass(REPO_CLASS_NAME, pluginLoader) ?: return 0
        val methods = repoClass.declaredMethods.filter { method ->
            val parameters = method.parameterTypes
            parameters.size == 3 &&
                parameters[0] == String::class.java &&
                parameters[2].name == CONTINUATION_CLASS_NAME &&
                PbandkMessageAccessor.isMessage(parameters[1])
        }

        methods.forEach { method ->
            method.hookBefore {
                val message = args[1] ?: return@hookBefore
                val fileList = PbandkMessageAccessor.getValue(message, 5) as? List<*>
                    ?: return@hookBefore
                val counts = mutableMapOf<String, Int>()

                for (file in fileList) {
                    if (file == null) continue
                    val fileId = PbandkMessageAccessor.getValue(file, 3, 1) as? String ?: ""
                    val count = (PbandkMessageAccessor.getValue(file, 3, 9) as? Number)?.toInt() ?: 0
                    counts[fileId] = count
                }
                setObjectExtra(EXTRA_COUNT_MAP, counts)
            }

            method.hookAfter {
                val localFiles = result as? List<*> ?: return@hookAfter
                if (localFiles.isEmpty()) return@hookAfter

                @Suppress("UNCHECKED_CAST")
                val counts = getObjectExtra(EXTRA_COUNT_MAP) as? Map<String, Int>
                    ?: return@hookAfter
                val isNewModel = localFiles[0].toString().contains("QQGroupFileInfo")

                localFiles.filterNotNull().forEach { item ->
                    if (isNewModel) {
                        updateNewModel(item, counts)
                    } else {
                        updateOldModel(item, counts)
                    }
                }
            }
        }
        return methods.size
    }

    private fun updateNewModel(item: Any, counts: Map<String, Int>) {
        val fileId = extract(item.toString(), fileIdRegex) ?: return
        val count = counts[fileId] ?: return
        val nameField = classHierarchy(item.javaClass).firstNotNullOfOrNull { current ->
            runCatching { current.getDeclaredField("x") }.getOrNull()
        } ?: return
        nameField.isAccessible = true
        val name = nameField.get(item)
        nameField.set(item, "$name".withDownloadTimes(count))
    }

    private fun updateOldModel(item: Any, counts: Map<String, Int>) {
        val model = findOriginalModel(item) ?: return
        val fileId = PbandkMessageAccessor.getValue(model, 1, 1) as? String ?: return
        val count = counts[fileId] ?: return

        PbandkMessageAccessor.replaceValue(model, 6, 2) { name ->
            "$name".withDownloadTimes(count)
        }
    }

    private fun String.withDownloadTimes(count: Int): String =
        SpannableStringBuilder(this).apply { insertDownloadTimes(count.toString()) }.toString()

    private fun Editable.insertDownloadTimes(count: String) {
        val text = toString()
        if (displayedDownloadTimesRegex.containsMatchIn(text)) return

        val countText = "${count}次"
        val expiry = expiresAfterDaysRegex.find(text)
        if (expiry == null) {
            append(if (text.isEmpty()) countText else " $countText")
            return
        }

        val expiryStart = expiry.range.first
        val insertStart = separatorBeforeExpiryRegex
            .find(text.substring(0, expiryStart))
            ?.range
            ?.first
            ?: expiryStart
        val prefix = if (insertStart > 0 && !text[insertStart - 1].isWhitespace()) " " else ""
        replace(insertStart, expiryStart, "$prefix$countText ")
    }

    private fun findOriginalModel(item: Any): Any? {
        val fields = mutableStateFields.getOrPut(item.javaClass) {
            classHierarchy(item.javaClass)
                .flatMap { it.declaredFields.asSequence() }
                .filter { it.type.name == MUTABLE_STATE_CLASS_NAME }
                .onEach { it.isAccessible = true }
                .toList()
        }

        for (field in fields) {
            val state = field.get(item) ?: continue
            val getValue = stateValueMethods[state.javaClass] ?: run {
                (state.javaClass.methods.asSequence() + state.javaClass.declaredMethods.asSequence())
                    .firstOrNull {
                        it.name == "getValue" && it.parameterCount == 0
                    }
                    ?.also {
                        it.isAccessible = true
                        stateValueMethods.putIfAbsent(state.javaClass, it)
                    }
            } ?: continue
            val value = getValue.invoke(state) ?: continue
            if (PbandkMessageAccessor.isMessage(value.javaClass)) return value
        }
        return null
    }

    private fun findAdapterClass(pluginLoader: ClassLoader): Class<*>? {
        adapterClassNames.firstNotNullOfOrNull { className ->
            loadTargetClass(className, pluginLoader)
        }?.let { return it }

        return adapterClassNames.firstNotNullOfOrNull { className ->
            runCatching {
                pluginLoader.loadClass("$className$1")
                    .getDeclaredField("this$0")
                    .type
            }.getOrNull()
        }
    }

    private fun loadTargetClass(className: String, pluginLoader: ClassLoader): Class<*>? {
        return runCatching {
            pluginLoader.loadClass(className)
        }.recoverCatching {
            XpHelper.classLoader.loadClass(className)
        }.getOrNull()
    }

    private fun ViewGroup.findTextViewWithTag(targetTag: Any): TextView? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is TextView && child.tag == targetTag) return child
            if (child is ViewGroup) {
                child.findTextViewWithTag(targetTag)?.let { return it }
            }
        }
        return null
    }

    private fun classHierarchy(type: Class<*>): Sequence<Class<*>> = sequence {
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            yield(current)
            current = current.superclass
        }
    }

    private fun extract(source: String, regex: Regex): String? {
        return regex.find(source)?.groupValues?.get(1)
    }
}
