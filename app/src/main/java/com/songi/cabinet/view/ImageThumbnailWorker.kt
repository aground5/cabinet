package com.songi.cabinet.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.songi.cabinet.Constants
import com.songi.cabinet.Constants.FOLDER_THUMBNAIL
import com.songi.cabinet.Constants.filesDir
import com.songi.cabinet.R
import java.io.File
import java.io.IOException


class ImageThumbnailWorker (val context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private var TAG = "ImageThumbnailWorker"

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) {
            return Result.failure()
        }
        val absolutePath: String = inputData.keyValueMap["absolutePath"] as String
        val fileName: String = inputData.keyValueMap["fileName"] as String

        val path = absolutePath.substring(absolutePath.indexOf(Constants.PACKAGE_NAME) + "${Constants.PACKAGE_NAME}/files".length, absolutePath.lastIndexOf('/'))

        val thumbnailPath = "$filesDir/$FOLDER_THUMBNAIL$path"
        val thumbnailName = "$fileName.thumbnail"
        val thumbnailFile = File(thumbnailPath, thumbnailName)

        if (thumbnailFile.exists()) {
            thumbnailFile.delete()
        }
        thumbnailFile.parentFile.mkdirs()
        thumbnailFile.createNewFile()

        try {
            var bitmap = BitmapFactory.decodeFile(absolutePath)
            val orientation = getOrientationOfImage(absolutePath)
            Log.d(TAG, "$absolutePath: $orientation")
            val m = Matrix()
            m.setRotate(orientation.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            val width: Int
            val height: Int
            if (bitmap.width > bitmap.height) {
                width = context.resources.getDimensionPixelSize(R.dimen.image_cubic_size)
                height = ((bitmap.height.toDouble() / bitmap.width.toDouble()) * width).toInt()
            } else {
                height = context.resources.getDimensionPixelSize(R.dimen.image_cubic_size)
                width = ((bitmap.width.toDouble() / bitmap.height.toDouble()) * height).toInt()
            }
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, thumbnailFile.outputStream())

        } catch (e: IOException) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    fun getOrientationOfImage(filePath: String): Int {
        var exif: ExifInterface? = null
        ExifInterface(filePath)

        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
        if (orientation != -1) {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            }
        }
        return 0
    }
}