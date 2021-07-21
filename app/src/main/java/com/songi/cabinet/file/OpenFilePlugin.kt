package com.songi.cabinet.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.songi.cabinet.BuildConfig
import java.io.File
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

object OpenFilePlugin {
    fun intentFileOpen(path: String, fileName: String, context: Context) {
        var intent = Intent()
        var file = File(path, fileName)
        var uri: Uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file)
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.setDataAndType(uri, MimeTypeMap.getSingleton().getExtensionFromMimeType(file.extension))

        startActivity(context, intent, null)
    }

    fun byteCalculation(bytes: Long): String? {
        var retFormat = "0"
        val size = bytes.toDouble()
        val s = arrayOf("bytes", "KB", "MB", "GB", "TB", "PB")
        if (bytes != 0L) {
            val idx = floor(ln(size) / ln(1024.0)).toInt()
            val df = DecimalFormat("#,###.##")
            val ret = size / 1024.0.pow(floor(idx.toDouble()))
            retFormat = df.format(ret).toString() + " " + s[idx]
        } else {
            retFormat += " " + s[0]
        }
        return retFormat
    }

    @Deprecated("Use getExtensionFromMimeType() instead.")
    private fun getFileType(fileName: String): String {
        val fileTypeStr = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
        return when (fileTypeStr) {
            "3gp" -> "video/3gpp"
            "torrent" -> "application/x-bittorrent"
            "kml" -> "application/vnd.google-earth.kml+xml"
            "gpx" -> "application/gpx+xml"
            "apk" -> "application/vnd.android.package-archive"
            "asf" -> "video/x-ms-asf"
            "avi" -> "video/x-msvideo"
            "bin", "class", "exe" -> "application/octet-stream"
            "bmp" -> "image/bmp"
            "c" -> "text/plain"
            "conf" -> "text/plain"
            "cpp" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls", "csv" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "gif" -> "image/gif"
            "gtar" -> "application/x-gtar"
            "gz" -> "application/x-gzip"
            "h" -> "text/plain"
            "htm" -> "text/html"
            "html" -> "text/html"
            "jar" -> "application/java-archive"
            "java" -> "text/plain"
            "jpeg" -> "image/jpeg"
            "jpg" -> "image/jpeg"
            "js" -> "application/x-javascript"
            "log" -> "text/plain"
            "m3u" -> "audio/x-mpegurl"
            "m4a" -> "audio/mp4a-latm"
            "m4b" -> "audio/mp4a-latm"
            "m4p" -> "audio/mp4a-latm"
            "m4u" -> "video/vnd.mpegurl"
            "m4v" -> "video/x-m4v"
            "mov" -> "video/quicktime"
            "mp2" -> "audio/x-mpeg"
            "mp3" -> "audio/x-mpeg"
            "mp4" -> "video/mp4"
            "mpc" -> "application/vnd.mpohun.certificate"
            "mpe" -> "video/mpeg"
            "mpeg" -> "video/mpeg"
            "mpg" -> "video/mpeg"
            "mpg4" -> "video/mp4"
            "mpga" -> "audio/mpeg"
            "msg" -> "application/vnd.ms-outlook"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "pps" -> "application/vnd.ms-powerpoint"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "prop" -> "text/plain"
            "rc" -> "text/plain"
            "rmvb" -> "audio/x-pn-realaudio"
            "rtf" -> "application/rtf"
            "sdocx" -> "application/sdoc"
            "sh" -> "text/plain"
            "tar" -> "application/x-tar"
            "tgz" -> "application/x-compressed"
            "txt" -> "text/plain"
            "vcf" -> "text/x-vcard"
            "wav" -> "audio/x-wav"
            "wma" -> "audio/x-ms-wma"
            "wmv" -> "audio/x-ms-wmv"
            "wps" -> "application/vnd.ms-works"
            "xml" -> "text/plain"
            "z" -> "application/x-compress"
            "zip" -> "application/x-zip-compressed"
            else -> "*/*"
        }
    }
}