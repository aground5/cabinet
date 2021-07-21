package com.songi.cabinet.view

import android.content.Context
import android.text.BoringLayout
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.*
import com.songi.cabinet.Constants
import com.songi.cabinet.Constants.FILENAME_IMAGE_THUMBNAIL
import com.songi.cabinet.Constants.FOLDER_THUMBNAIL
import com.songi.cabinet.Constants.IMAGE_BUILDER
import com.songi.cabinet.Constants.filesDir
import com.songi.cabinet.file.ThumbnailTranslator
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.charset.Charset

class ImageThumbnailSaver (val context: Context, val lifecycleOwner: LifecycleOwner) {
    private var TAG = "ImageThumbnailSaver"

    private var images = mutableListOf<ImageThumbnailVO>()
    private val settingsDir = "$filesDir/settings"
    private val absolutePath = "$settingsDir/$FILENAME_IMAGE_THUMBNAIL"
    private val settings = File(settingsDir, Constants.FILENAME_IMAGE_THUMBNAIL)

    init {
        run {
            if (settings.exists()) {
                if (load()) {
                    return@run
                }
            }
            File(settingsDir).mkdirs()
            settings.createNewFile()
        }
    }

    private fun load() : Boolean {
        try {
            val jsonObject = JsonParser.parseReader(settings.reader(Charset.defaultCharset()))
            val jsonArray = try {
                jsonObject.asJsonArray
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                settings.delete()
                return false
            }

            val index = 0
            while (index < jsonArray.size()) {
                val imageThumbnailVO = Gson().fromJson(jsonArray.get(index), ImageThumbnailVO::class.java)
                images.add(imageThumbnailVO)
            }
        } catch (e: Exception) {
            return when (e) {
                is JsonIOException -> {
                    e.printStackTrace()
                    load()
                }
                is JsonParseException, is JsonSyntaxException -> {
                    e.printStackTrace()
                    settings.delete()
                    false
                }
                else -> throw e
            }
        }
        return true
    }

    private fun save() {
        val jsonArray = JsonArray()
        for (i in images) {
            val jsonElement = JsonParser.parseString(Gson().toJson(i, ImageThumbnailVO::class.java))
            jsonArray.add(jsonElement)
        }
        val jsonToString = Gson().toJson(jsonArray)
        settings.writer(Charset.defaultCharset()).write(jsonToString)
        Log.d(TAG, "JSON Saved!")
    }

    private fun requestWork(index: Int) {
        val data = Data.Builder()
            .putAll(mapOf("absolutePath" to images[index].absolutePath,
                            "fileName" to images[index].name))
            .build()
        val thumbnailBuilder = OneTimeWorkRequestBuilder<ImageThumbnailWorker>().apply {
            setInputData(data)
            addTag(Constants.IMAGE_BUILDER)
            setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
        }
        val thumbnailWorker = thumbnailBuilder.build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMAGE_BUILDER, ExistingWorkPolicy.APPEND ,thumbnailWorker)
        val thumbnailFile = ThumbnailTranslator.getThumbnailFile(File(images[index].absolutePath))
        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(thumbnailWorker.id)
            .observe(lifecycleOwner, androidx.lifecycle.Observer { workInfo: WorkInfo? ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.FAILED -> {
                            images[index].isProcessed = false
                        }
                        WorkInfo.State.ENQUEUED -> {
                            images[index].isProcessed = true
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            images[index].isProcessed = true
                            val thread = ThumbnailRenderThread(mutableListOf(Pair(images[index].view, thumbnailFile)))
                            thread.start()
                        }
                    }
                }
            })
    }

    private fun addInImages(image: ImageThumbnailVO) : Boolean {
        for (i in images.indices) {
            if (images[i].name == image.name && images[i].absolutePath == image.absolutePath) {
                if (images[i].isProcessed == image.isProcessed) {
                    images[i].view = image.view
                    return false
                } else {
                    Log.d(TAG, "${image.name} process restart")
                    requestWork(i)
                }
            }
        }
        images.add(image)
        return true
    }

    fun addImage(image: ImageThumbnailVO) {
        if (addInImages(image)) {
            Log.d(TAG, "${image.name} added!")
            requestWork(images.lastIndex)
        }
        Log.d(TAG, "${image.name} already exists!, view changed!")
    }

    fun workForNotProcessed() {
        for (i in images.indices) {
            if (!images[i].isProcessed) {
                requestWork(i)
            }
        }
    }

    val mimeTypeMap = MimeTypeMap.getSingleton()
    fun resetAllImages(directory: File) {
        for (i in images) {
            i.isProcessed = false
        }
        File("$filesDir/$FOLDER_THUMBNAIL").deleteRecursively()
    }
}