package re.limus.timas.api

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import re.limus.timas.hook.utils.XLog
import re.limus.timas.util.PathTool
import re.limus.timas.util.DownloadManager
import re.limus.timas.util.FileUtils
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.ConstructorUtils
import top.sacz.xphelper.reflect.FieldUtils
import java.io.File
import java.io.IOException
import java.util.Locale

object CreateElement {
    fun createTextElement(text: String?): Any {
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createTextElement",
            arrayOf<Class<*>>(String::class.java),
            text
        )
    }

    fun createStickerElement(url: String): Any {
        val path = cachePicPath(url)
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createPicElement",
            arrayOf<Class<*>>(
                String::class.java,
                Boolean::class.java,
                Int::class.java
            ),
            path,
            true,
            1
        )
    }

    fun createPicElement(url: String): Any {
        val path = cachePicPath(url)
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createPicElement",
            arrayOf<Class<*>>(
                String::class.java,
                Boolean::class.java,
                Int::class.java
            ),
            path,
            true,
            0
        )
    }

    fun createAtTextElement(text: String?, peerUid: String?, atType: Int): Any { //0不艾特1全体2个人
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createAtTextElement",
            arrayOf<Class<*>>(String::class.java, String::class.java, Int::class.java),
            text,
            peerUid,
            atType
        )
    }

    /**
     * 创建艾特元素 并自动获取对方在群内名称
     */
    fun createAtTextElement(groupUin: String?, peerUid: String?): Any {
        var atType = 2
        var atText: String? = "@"
        if (TextUtils.isEmpty(peerUid) || peerUid == "0") {
            atText += "全体成员"
            atType = 1
        } else {
            atText += TIMTroopTool.getMemberName(groupUin, peerUid)
        }
        return createAtTextElement(atText, peerUid, atType)
    }

    fun createReplyElement(msgId: Long): Any {
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createReplyElement",
            arrayOf<Class<*>>(Long::class.javaPrimitiveType!!),
            msgId
        )
    }

    fun createFileElement(path: String?): Any {
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createFileElement",
            arrayOf<Class<*>>(String::class.java),
            path
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createPttElement(url: String?): Any {
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        val myList = ArrayList<Byte?>(
            mutableListOf<Byte?>(
                    28,
                    26,
                    43,
                    29,
                    31,
                    61,
                    34,
                    49,
                    51,
                    56,
                    52,
                    74,
                    41,
                    62,
                    66,
                    46,
                    25,
                    57,
                    51,
                    70,
                    33,
                    45,
                    39,
                    27,
                    68,
                    58,
                    46,
                    59,
                    59,
                    63
            )
        )
        return XposedHelpers.callMethod(
            o,
            "createPttElement",
            arrayOf<Class<*>>(
                String::class.java,
                Int::class.java,
                ArrayList::class.java
            ),
            url,
            getDuration(url).toInt(),
            myList
        )
    }

    fun cachePttPath(url: String): String {
        var copyTo = Environment.getExternalStorageDirectory()
            .toString() + "/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + TIMEnvTool.getCurrentUin() + "/ptt/"
        //url
        if (url.lowercase(Locale.getDefault())
                .startsWith("http:") || url.lowercase(Locale.getDefault()).startsWith("https:")
        ) {
            val mRandomPathName = (Math.random().toString()).substring(2)
            DownloadManager.download(url, "$copyTo$mRandomPathName.aac")
            copyTo += "$mRandomPathName.aac"
        } else {
            copyTo += File(url).getName()
            try {
                FileUtils.copyFile(url, copyTo)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        return copyTo
    }

    fun createVideoElement(path: String?): Any {
        val o: Any? =
            TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
        return XposedHelpers.callMethod(
            o,
            "createVideoElement",
            arrayOf<Class<*>>(String::class.java),
            path
        )
    }

    fun createJsonGrayTipElement(text: String?, url: String): Any? {
        val jsonObject = JSONObject()
        val empty = !(url.contains("http://") || url.contains("https://"))
        try {
            jsonObject.put("align", "center")
            val jsonObject1 = JSONObject()
            jsonObject1.put("col", 3)
            jsonObject1.put("jp", url)
            jsonObject1.put("txt", text)
            jsonObject1.put("type", if (empty) "nor" else "url")
            val jsonArray = JSONArray()
            jsonArray.put(jsonObject1)
            jsonObject.put("items", jsonArray)
            val jsonGrayElement = ConstructorUtils.newInstance(
                ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement"),
                arrayOf(
                    Long::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.XmlToJsonParam")
                ),
                if (empty) 1014 else 1015,
                jsonObject.toString(),
                "",
                false,
                null
            )

            val grayTipElement =
                ConstructorUtils.newInstance(ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.GrayTipElement"))

            FieldUtils.create(grayTipElement)
                .fieldName("jsonGrayTipElement")
                .setFirst(grayTipElement, 17)

            FieldUtils.create(grayTipElement)
                .fieldName("jsonGrayTipElement")
                .setLast(grayTipElement, jsonGrayElement)

            val msgElement =
                ConstructorUtils.newInstance(ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.MsgElement"))
            XposedHelpers.callMethod(
                msgElement,
                "setElementType",
                arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                8
            )
            XposedHelpers.callMethod(
                msgElement,
                "setGrayTipElement",
                arrayOf<Class<*>?>(ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.GrayTipElement")),
                grayTipElement
            )
            return msgElement
        } catch (e: Exception) {
            XLog.d("报错:createJsonGrayTipElement", e)
            return null
        }
    }

    fun createArkElement(card: String?): Any? {
        try {
            val cardData = ClassUtils.findClass("com.tencent.qqnt.msg.a.b")
            val cardDataObject: Any = cardData.newInstance()
            val o1 = XposedHelpers.callMethod(
                cardDataObject,
                "o",
                arrayOf<Class<*>>(String::class.java),
                card
            ) as Boolean
            if (!o1) {
                Toast.makeText(XpHelper.context, "卡片格式有问题: $card", Toast.LENGTH_SHORT).show()
                return null
            }
            val o: Any? =
                TIMEnvTool.getQRouteApi(ClassUtils.findClass("com.tencent.qqnt.msg.api.IMsgUtilApi"))
            return XposedHelpers.callMethod(
                o,
                "createArkElement",
                arrayOf<Class<*>?>(cardData),
                cardDataObject
            )
        } catch (e: IllegalAccessException) {
            //throw new RuntimeException(e);
            return null
        } catch (e: InstantiationException) {
            return null
        }
    }

    fun cachePicPath(path: String): String {
        val mPath = path.lowercase(Locale.getDefault())
        if (mPath.startsWith("http:") || mPath.startsWith("https:")) {
            val mRandomPathName = (Math.random().toString()).substring(2)
            val mRandomPath: String = PathTool.getModuleCachePath("img") + "/"
            DownloadManager.download(path, mRandomPath + mRandomPathName)
            return mRandomPath + mRandomPathName
        } else {
            return path
        }
    }

    /**
     * 获取 视频 或 音频 时长
     *
     * @param path 视频 或 音频 文件路径
     * @return 时长 毫秒值
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getDuration(path: String?): Long {
        var duration: Long = 0
        try {
            MediaMetadataRetriever().use { mmr ->
                if (path != null) {
                    mmr.setDataSource(path)
                }
                val time = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (time != null) {
                    duration = time.toLong()
                }
            }
        } catch (_: Exception) {
        }
        return duration
    }
}