package re.limus.timas.hook.items.file

import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.view.LayoutInflater
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.databinding.DialogCustomDirectoryBinding
import re.limus.timas.hook.base.SwitchHook
import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.util.ConfigUtils
import java.io.File

@RegisterToUI
object CustomDownloadDirectory : SwitchHook() {

    override val name = "重定向文件下载目录"

    override val description = "单击可自定义下载目录 (需重启)"

    override val category = UiCategory.FILE

    override val needRestart = true
    private const val DOWNLOAD_PATH_KEY = "downloadPath"
    private val config = ConfigUtils("CustomDownloadDirectory")

    private fun defaultDownloadPath(): String =
        "${Environment.getExternalStorageDirectory().absolutePath}/Download/TIM"

    private fun getDownloadPath(): String {
        val defaultPath = defaultDownloadPath()
        return config.getString(DOWNLOAD_PATH_KEY, defaultPath)
            .takeIf { it.isNotBlank() && File(it).isAbsolute }
            ?: defaultPath
    }

    private fun originalDownloadPath(): String =
        "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/com.tencent.tim/Tencent/TIMfile_recv/"

    override fun onclick(context: Context) {
        val binding = DialogCustomDirectoryBinding.inflate(LayoutInflater.from(context))
        binding.pathInput.apply {
            setText(getDownloadPath())
            setSelection(text?.length ?: 0)
            doAfterTextChanged {
                binding.pathInputLayout.error = null
            }
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.custom_directory_title)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val path = binding.pathInput.text?.toString()?.trim().orEmpty()
                if (path.isEmpty() || !File(path).isAbsolute) {
                    binding.pathInputLayout.error =
                        context.getString(R.string.custom_directory_path_error)
                    return@setOnClickListener
                }

                config.put(DOWNLOAD_PATH_KEY, path)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onHook(ctx: Context, loader: ClassLoader) {
        DexFinder.findMethod {
            declaredClass = loader.loadClass("com.tencent.mobileqq.vfs.VFSAssistantUtils")
            methodName = "getSDKPrivatePath"
            returnType = String::class.java
        }.hookAfter {
            val realResult = result as String
            val file = File(realResult)
            if (file.exists() && file.isFile) return@hookAfter // 如果文件存在则不处理,防止已下载的文件出现异常

            val sourceRoot = File(originalDownloadPath()).toPath().normalize()
            val sourcePath = file.toPath().normalize()
            if (sourcePath.startsWith(sourceRoot)) {
                val relativePath = sourceRoot.relativize(sourcePath)
                result = File(getDownloadPath()).toPath()
                    .normalize()
                    .resolve(relativePath)
                    .toFile()
                    .absolutePath
            }
        }
    }
}
