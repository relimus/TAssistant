package io.live.timas.hook.items.file

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.live.timas.annotations.RegisterToUI
import io.live.timas.annotations.UiCategory
import io.live.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.FieldUtils

@RegisterToUI
object RenameApk : SwitchHook() {

    override val name = "格式化上传应用名"

    override val description = "将QQ修改的 base.apk.1 改为 应用名_版本号.Apk 格式"

    override val category = UiCategory.FILE

    private lateinit var appContext: Context

    override fun onHook(ctx: Context, loader: ClassLoader) {
        appContext = ctx

        //私聊
        val friendMethod =
            DexFinder.findMethod {
                declaredClass = "com.tencent.mobileqq.filemanager.nt.NTFileManageBridger".toClass()
                parameters = arrayOf(
                    "com.tencent.mobileqq.filemanager.data.FileManagerEntity".toClass(),
                    Runnable::class.java,
                    String::class.java,
                    String::class.java
                )
            }.first()

        friendMethod.hookAfter {

            val fileManagerEntity = args[0]

            val fileName: String =
                FieldUtils.getField(fileManagerEntity, "fileName", String::class.java)

            val localFile: String =
                FieldUtils.getField(fileManagerEntity, "strFilePath", String::class.java)

            val meetHitConditions = meetHitConditions(fileName, localFile)

            if (meetHitConditions) {
                FieldUtils.setField(
                    fileManagerEntity,
                    "fileName",
                    getFormattedFileNameByPath(localFile)
                )
            }
        }

        friendMethod.hookAfter {

            val fileManagerEntity = args[0]

            val fileName: String =
                FieldUtils.getField(fileManagerEntity, "fileName", String::class.java)

            if (fileName.endsWith(".apk")) {
                FieldUtils.setField(
                    fileManagerEntity,
                    "fileName",
                    fileName.replace(".apk", ".Apk")
                )
            }
        }

        //群组
        val troopMethod =
            DexFinder.findMethod {
                declaredClass =
                    "com.tencent.mobileqq.troop.filemanager.TroopFileTransferMgr".toClass()
                parameters = arrayOf(
                    Long::class.java,
                    ClassUtils.findClass($$"com.tencent.mobileqq.troop.utils.TroopFileTransferManager$Item")
                )
                returnType = Void.TYPE
            }.first()

        troopMethod.hookAfter {

            val item = args[1]

            val fileName: String = FieldUtils.getField(item, "FileName", String::class.java)

            val localFile: String? = FieldUtils.getField(item, "LocalFile", String::class.java)

            localFile?.let { file ->
                if (meetHitConditions(fileName, file)) {
                    FieldUtils.setField(item, "FileName", getFormattedFileNameByPath(file))
                }
            }
        }

        troopMethod.hookAfter {

            val item = args[1]

            val fileName: String = FieldUtils.getField(item, "FileName", String::class.java)

            if (fileName.endsWith(".apk")) {
                FieldUtils.setField(item, "FileName", fileName.replace(".apk", ".Apk"))
            }
        }
    }
    /**
     * 判断是否符合命中规则
     */
    private fun meetHitConditions(fileName: String, filePath: String): Boolean {
        // base.apk / base(1).apk 特判
        if (fileName.matches("^base(\\([0-9]+\\))?.apk$".toRegex(RegexOption.IGNORE_CASE))) {
            return true
        }
        // 必须是 .apk 才考虑
        val dot = fileName.lastIndexOf('.')
        if (dot == -1) return false
        val ext = fileName.substring(dot)
        if (!ext.equals(".apk", ignoreCase = true)) return false

        val prefix = fileName.take(dot)
        val appInfo = getAppInfoByFilePath(filePath) ?: return false

        val pm = appContext.packageManager
        val label = try {
            appInfo.loadLabel(pm).toString()
        } catch (_: Throwable) { "" }
        val pkg = appInfo.packageName ?: ""
        val name = appInfo.name ?: ""

        // 规范化比较，容忍空格、下划线、连字符、点、大小写差异
        fun norm(s: String): String = s.lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(".", "")

        val p = norm(prefix)
        if (p.isEmpty()) return false

        val candidates = listOf(pkg, name, label).map { norm(it) }.filter { it.isNotEmpty() }
        return candidates.any { it == p }
    }

    private fun getAppInfoByFilePath(filePath: String?): ApplicationInfo? {
        return try {
            if (filePath.isNullOrEmpty()) return null
            val packageManager: PackageManager = appContext.packageManager
            val packageArchiveInfo = packageManager.getPackageArchiveInfo(filePath, 0)
            val appInfo = packageArchiveInfo?.applicationInfo ?: return null
            // 关键：为未安装 APK 设置资源路径，确保 loadLabel 正常工作
            appInfo.sourceDir = filePath
            appInfo.publicSourceDir = filePath
            appInfo
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getFormattedFileNameByPath(apkPath: String): String {
        try {
            val packageManager: PackageManager = appContext.packageManager
            val packageArchiveInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            val applicationInfo = packageArchiveInfo?.applicationInfo ?: return "base.Apk"
            // 确保离线 APK 的资源可被读取
            applicationInfo.sourceDir = apkPath
            applicationInfo.publicSourceDir = apkPath
            val currentBaseApkFormat = "%n_%v.Apk"
            val appName = applicationInfo.loadLabel(packageManager).toString()
            val pkg = applicationInfo.packageName ?: ""
            val verName = packageArchiveInfo.versionName ?: ""
            return currentBaseApkFormat
                .replace("%n", appName)
                .replace("%p", pkg)
                .replace("%v", verName)
                .replace("%c", try { packageArchiveInfo.versionCode.toString() } catch (_: Throwable) { "" })
        } catch (_: Exception) {
            return "base.Apk"
        }
    }
}