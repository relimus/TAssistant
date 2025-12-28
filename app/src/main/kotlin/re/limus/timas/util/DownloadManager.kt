package re.limus.timas.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

object DownloadManager {

    /**
     * 异步下载
     */
    @JvmStatic
    fun downloadAsync(url: String, path: String, callback: Runnable) {
        thread {
            try {
                download(url, path)
                callback.run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun download(url: String, path: String) {
        try {
            val downloadFile = File(path)

            // Ensure parent directory exists
            downloadFile.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }

            // Clean up existing file
            if (downloadFile.exists()) {
                downloadFile.delete()
            }
            downloadFile.createNewFile()

            val client = OkHttpClient()
            val request = Request.Builder()
                .addHeader("User-Agent", "Android/TimNT TimTool")
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            // Use 'use' to automatically close streams even if an exception occurs
            response.body.byteStream().use { inputStream ->
                BufferedInputStream(inputStream).use { bufIn ->
                    BufferedOutputStream(FileOutputStream(downloadFile)).use { bufOut ->
                        val buf = ByteArray(2048)
                        var len: Int
                        while (bufIn.read(buf).also { len = it } != -1) {
                            bufOut.write(buf, 0, len)
                        }
                        bufOut.flush()
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}