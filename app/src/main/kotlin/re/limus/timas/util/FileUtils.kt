package re.limus.timas.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object FileUtils {
    private const val BYTE_SIZE = 1024

    private fun renameSuffix(path: String, suffix: String?) {
        val file = File(path)
        val oldName = path.substring(0, path.lastIndexOf("."))
        file.renameTo(File(file.absolutePath, oldName + suffix))
    }

    fun writeBytesToFile(path: String, data: ByteArray) {
        val file = File(path)
        try {
            //先创建文件夹
            if (!file.getParentFile()!!.exists()) file.getParentFile()!!.mkdirs()
            //再创建文件 FileOutputStream会自动创建文件但是不能创建多级目录
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            BufferedOutputStream(FileOutputStream(path)).use { bufOut ->
                bufOut.write(data)
            }
        } catch (ioException: IOException) {
            throw RuntimeException(ioException)
        }
    }

    /**
     * 文件转byte
     */
    fun readAllByteArrayFromFile(file: File): ByteArray {
        try {
            return readAllByte(FileInputStream(file), file.length().toInt())
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }
    }

    fun getFileMD5(file: File): String? {
        if (!file.isFile()) {
            return null
        }
        var digest: MessageDigest?
        var `in`: FileInputStream?
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance("MD5")
            `in` = FileInputStream(file)
            while ((`in`.read(buffer, 0, 1024).also { len = it }) != -1) {
                digest.update(buffer, 0, len)
            }
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        val bigInt = BigInteger(1, digest.digest())
        return bigInt.toString(16).uppercase(Locale.getDefault())
    }

    @Throws(IOException::class)
    fun readAllBytes(inp: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var read: Int
        while ((inp.read(buffer).also { read = it }) != -1) out.write(buffer, 0, read)
        return out.toByteArray()
    }

    fun readAllByte(stream: InputStream, size: Int): ByteArray {
        try {
            val buffer = ByteArray(BYTE_SIZE)
            val bytearrayOut = ByteArrayOutputStream()
            var read: Int
            while ((stream.read(buffer).also { read = it }) != -1) {
                bytearrayOut.write(buffer, 0, read)
            }
            return bytearrayOut.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * 复制文件夹
     *
     * @param sourceDir 原文件夹
     * @param targetDir 复制后的文件夹
     */
    fun copyDir(sourceDir: File, targetDir: File) {
        if (!sourceDir.isDirectory()) {
            return
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val files = sourceDir.listFiles()
        for (f in files!!) {
            if (f.isDirectory()) {
                copyDir(f, File(targetDir.path, f.getName()))
            } else if (f.isFile()) {
                try {
                    copyFile(f, File(targetDir.path, f.getName()))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteFile(file: File?) {
        try {
            if (file == null) {
                return
            }
            if (file.isFile()) file.delete()
            val files = file.listFiles() ?: return
            //遍历该目录下的文件对象
            for (f in files) {
                if (f.isDirectory()) {
                    deleteFile(f) //目录下有文件夹调用本方法删除(递归)
                } else {
                    try {
                        f.delete()
                    } catch (e: Exception) {
                    }
                }
            }
            try {
                file.delete()
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
        }
    }

    fun getDirSize(file: File): Long {
        //判断文件是否存在
        if (file.exists()) {
            //如果是目录则递归计算其内容的总大小
            if (file.isDirectory()) {
                val children = file.listFiles()
                var size: Long = 0
                if (children == null) return 0
                for (f in children) size += getDirSize(f)
                return size
            } else { //如果是文件则直接返回其大小,以“兆”为单位
                return file.length()
            }
        } else {
            return 0
        }
    }

    @Throws(IOException::class)
    fun readFileText(filePath: String): String {
        val path = File(filePath)
        //此路径无文件
        if (!path.exists()) {
            throw IOException("path No exists :" + path.absolutePath)
        } else if (path.isDirectory())  /*此文件是目录*/ {
            throw IOException("Non-file type :" + path.absolutePath)
        }
        val stringBuilder = StringBuilder()
        BufferedReader(FileReader(filePath)).use { reader ->
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                stringBuilder.append(line)
                stringBuilder.append("\n")
            }
        }
        if (stringBuilder.isNotEmpty()) stringBuilder.deleteCharAt(stringBuilder.length - 1)
        return stringBuilder.toString()
    }

    /**
     * 文件写入文本
     *
     * @param path     路径
     * @param content  内容
     * @param isAppend 是否追写 不是的话会覆盖
     */
    fun writeTextToFile(path: String, content: String, isAppend: Boolean) {
        val file = File(path)
        try {
            //先创建文件夹
            if (!file.getParentFile()!!.exists()) file.getParentFile()!!.mkdirs()
            //再创建文件 FileOutputStream会自动创建文件但是不能创建多级目录
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(file, isAppend),
                    StandardCharsets.UTF_8
                )
            ).use { writer ->
                writer.write(content)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun copyFile(sourceFile: String, targetPath: String) {
        val file = File(sourceFile)
        if (!file.exists()) {
            throw IOException("path No exists(源文件不存在) : " + file.absolutePath)
        } else if (file.isDirectory()) {
            throw IOException("Not a file, but a directory(不是文件) : " + file.absolutePath)
        }
        copyFile(FileInputStream(file), File(targetPath))
    }

    @Throws(IOException::class)
    fun copyFile(sourceFile: File, target: File) {
        copyFile(FileInputStream(sourceFile), target)
    }

    @Throws(IOException::class)
    fun copyFileText(inputStream: InputStream, target: File) {
        if (!target.exists()) {
            if (!target.getParentFile()!!.exists()) {
                target.getParentFile()!!.mkdirs()
            }
            if (!target.createNewFile()) {
                throw IOException("create File Fail :" + target.absolutePath)
            }
        }
        val builder = StringBuilder()
        inputStream.use {
            BufferedReader(InputStreamReader(inputStream)).use { sourceFileReader ->
                BufferedWriter(
                    FileWriter(target)
                ).use { destStream ->
                    var line: String?
                    while ((sourceFileReader.readLine().also { line = it }) != null) {
                        builder.append(line)
                        builder.append("\n")
                    }
                    //删除换行符
                    if (builder.isNotEmpty()) builder.deleteCharAt(builder.length - 1)
                    destStream.write(builder.toString())
                }
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(inputStream: InputStream, target: File) {
        if (!target.exists()) {
            if (!target.getParentFile()!!.exists()) {
                target.getParentFile()!!.mkdirs()
            }
            if (!target.createNewFile()) {
                throw IOException("create File Fail :" + target.absolutePath)
            }
        }
        BufferedInputStream(inputStream).use { sourceFile ->
            BufferedOutputStream(FileOutputStream(target)).use { destStream ->
                val bytes = ByteArray(BYTE_SIZE)
                var len: Int
                while ((sourceFile.read(bytes).also { len = it }) != -1) {
                    destStream.write(bytes, 0, len)
                }
                destStream.flush()
            }
        }
    }
}