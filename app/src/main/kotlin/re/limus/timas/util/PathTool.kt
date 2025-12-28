package re.limus.timas.util

import android.content.Context
import android.os.Environment
import re.limus.timas.hook.HookEnv
import java.io.File

object PathTool {

    fun getDataSavePath(context: Context, dirName: String?): String {
        //getExternalFilesDir()：SDCard/Android/data/你的应用的包名/files/dirName
        return context.getExternalFilesDir(dirName)!!.absolutePath
    }

    val storageDirectory: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    val moduleDataPath: String
        get() {
            //        String directory = getStorageDirectory() + "/Download/QStory";//只有创建该文件的进程才能访问文件 不适用
            //        String directory = getStorageDirectory() + "/Android/media/" + HookEnv.getCurrentHostAppPackageName() + "/QStory";//LSPatch在某些机型上无法使用media文件夹
            val directory =
                storageDirectory + "/Android/data/" + HookEnv.hostAppPackageName + "/TAssistant"
            val file = File(directory)
            if (!file.exists()) {
                file.mkdirs()
            }
            return directory
        }

    fun getModuleCachePath(dirName: String?): String {
        val cache = File("$moduleDataPath/cache/$dirName")
        if (!cache.exists()) cache.mkdirs()
        return cache.absolutePath
    }
}