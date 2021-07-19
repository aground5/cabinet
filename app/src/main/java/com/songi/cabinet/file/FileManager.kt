package com.songi.cabinet.file

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.setPadding
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.songi.cabinet.R
import kotlinx.coroutines.*
import java.io.*
import java.text.Collator
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class FileManager(private val tag: String,
                  private val context: Context,
                  root: String,
                  private val lifecycleOwner: LifecycleOwner,
                  private val refreshViewRequester: RefreshViewRequester) {
    private var TAG = "FileManager"

    private val mRoot = root
    var mCurrent = mRoot; private set
    var mCurrentFolder = mCurrent.substring(mCurrent.lastIndexOf('/') + 1, mCurrent.length); private set
    init {
        val fileObjectMRoot = File(mRoot)
        if (!fileObjectMRoot.exists()) {
            fileObjectMRoot.mkdirs()
        }
    }

    fun refreshFile(viewHidden: Boolean) : MutableList<String>{
        val directories = if (viewHidden) {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter pathname.isDirectory
            })
        } else {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isHidden && pathname.isDirectory
            })
        }
        val files = if (viewHidden) {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isDirectory
            })
        } else {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isHidden && !pathname.isDirectory
            })
        }

        val collator = Collator.getInstance(Locale(context.applicationContext.resources.configuration.locales.get(0).language))
        val mutableDirectories = mutableListOf<String>()
        for(i in directories) {
            mutableDirectories.add(i.name)
        }
        var mutableFiles = mutableListOf<String>()
        for(i in files) {
            mutableFiles.add(i.name)
        }
        Collections.sort(mutableDirectories, collator)
        Collections.sort(mutableFiles, collator)
        mutableDirectories.addAll(mutableFiles)
        mCurrentFolder = mCurrent.substring(mCurrent.lastIndexOf('/') + 1, mCurrent.length)

        return mutableDirectories
    }

    @Deprecated("Use importFile(Uri) instead. Not absolute file path.", ReplaceWith("importFile(index)"))
    fun saveFile(absoluteFilePath: String) {
        val externalFile = File(absoluteFilePath)
        val fileName = absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/') + 1, absoluteFilePath.length)
        val internalFile = File(mCurrent, fileName)
        val internalFileOutputStream = FileOutputStream(internalFile)
        try {
            internalFileOutputStream.write(externalFile.readBytes())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun importFile(uri: Uri) {
        val fileInfo = getFileName(uri)
        copyWithBuffer(arrayOf(null, uri.toString()),
            arrayOf(mCurrent, isFileExists(mCurrent, fileInfo?.get(0)!!)),
            fileInfo[1]!!.toLong())
    }

    private fun getFileName(uri: Uri): Array<String?> {
        var result: Array<String?>? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = arrayOf(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)),
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE)))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = arrayOf(uri.path)
            val cut = result[0]!!.lastIndexOf('/')
            if (cut != -1) {
                result[0] = result[0]!!.substring(cut + 1)
            }
        }
        return result
    }

    val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 200
        isIndeterminate = false
        setPadding(context.resources.getDimensionPixelSize(R.dimen.progressbar_padding_size))
    }
    val alertDialog = AlertDialog.Builder(context).apply {
        setTitle("파일 복사하는 중...")
        setView(progressBar)
        setMessage("")
        setCancelable(false)
        setPositiveButton(R.string.positive) { dialog, which ->
            refreshViewRequester.request(tag)
        }
    }.create()

    private fun copyWithBuffer(inputPath: Array<String?>, outputPath: Array<String>, size: Long) {
        val data = Data.Builder()
            .putAll(mapOf("inputPath" to inputPath, "outputPath" to outputPath, "size" to size))
            .build()
        val copyBuilder = OneTimeWorkRequestBuilder<CopyWorker>()
        copyBuilder.setInputData(data)
        copyBuilder.addTag("COPY_BUILDER")
        val copyWorker = copyBuilder.build()
        WorkManager.getInstance(context).enqueue(copyWorker)

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(copyWorker.id)
            .observe(lifecycleOwner, androidx.lifecycle.Observer { workInfo: WorkInfo? ->
                if (workInfo != null) {
                    if (workInfo.state == WorkInfo.State.ENQUEUED) {
                        Log.d(TAG, "WorkInfo.State.ENQUEUED")
                    } else if (workInfo.state == WorkInfo.State.RUNNING) {
                        Log.d(TAG, "WorkInfo.State.RUNNING")
                        val copiedSize = workInfo.progress.getLong("PROGRESS", 0)

                        Log.d(TAG, "progress : $copiedSize")
                        val percentage = (copiedSize / (size / 200)).toInt()

                        progressBar.setProgress(percentage, true)
                        alertDialog.setMessage("${outputPath[1]}\n${byteCalculation(copiedSize)} / ${byteCalculation(size)}")
                        alertDialog.show()
                        alertDialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.GONE
                    } else if (workInfo.state.isFinished) {
                        Log.d(TAG, "workInfo.state.isFinished")
                        progressBar.setProgress(200, true)
                        alertDialog.setMessage("${outputPath[1]}\n${byteCalculation(size)} / ${byteCalculation(size)}")
                        alertDialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.VISIBLE
                    }
                }
            })
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

    fun moveFile(filePath: Array<String>): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$filePath[1]")

        return file.renameTo(File(mCurrent, isFileExists(mCurrent, filePath[1])))
    }

    fun moveFile(filePath: Array<String>, droppedDir: String): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$droppedDir/$filePath[1]")

        return file.renameTo(File("$mCurrent/$droppedDir", isFileExists("$mCurrent/$droppedDir", filePath[1])))
    }

    fun moveFileToClipboard(fileName: String) : Boolean {
        val file = File(mCurrent, fileName)
        val result = file.renameTo(File("${context.filesDir.absolutePath}/Cabinet_temp_folder", isFileExists("${context.filesDir.absolutePath}/Cabinet_temp_folder", fileName)))
        refreshViewRequester.request("DRAWER")
        return result
    }

    
    fun copyFile(origFilePath: Array<String>) {
        val inputFile = File(origFilePath[0], origFilePath[1])
        copyWithBuffer(arrayOf(origFilePath[0], origFilePath[1]),
            arrayOf(mCurrent, isFileExists(mCurrent, origFilePath[1])),
            inputFile.totalSpace)
    }

    
    fun copyFileToClipboard(fileName: String) {
        val inputFile = File(mCurrent, fileName)
        try {
            copyWithBuffer(arrayOf(mCurrent, fileName),
                    arrayOf("${context.filesDir.absolutePath}/Cabinet_temp_folder", isFileExists("${context.filesDir.absolutePath}/Cabinet_temp_folder", fileName)),
                inputFile.length())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        refreshViewRequester.request("DRAWER")
    }

    fun renameFile(origFileName: String, newFileName: String) {
        val file = File(mCurrent, origFileName)
        file.renameTo(File(mCurrent, isFileExists(mCurrent, newFileName)))
    }

    fun removeSingleFile(fileName: String) : Boolean{
        val file = File(mCurrent, fileName)
        return file.delete()
    }

    fun removeRecursively(fileName: String) : Boolean{
        val file = File(mCurrent, fileName)
        return file.deleteRecursively()
    }

    fun switchHiddenAttrib(fileName: String) {
        val file = File(mCurrent, fileName)
        if (file.isHidden) {
            file.renameTo(File(mCurrent, fileName.substring(1, fileName.length)))
        } else {
            file.renameTo(File(mCurrent, ".$fileName"))
        }
    }

    fun createFolder(folderName: String) {
        val file = File(mCurrent, isFileExists(mCurrent, folderName))
        file.mkdirs()
    }

    fun isCurrentRoot() : Boolean {
        return mRoot.equals(mCurrent)
    }

    fun goPreviousDir() : Boolean {
        if (isCurrentRoot()) {
            return false
        } else {
            mCurrent = mCurrent.substring(0, mCurrent.lastIndexOf('/'))
            return true
        }
    }

    fun goDir(dir: String) {
        mCurrent = "${mCurrent}/${dir}"
    }

    fun isFileExists(filePath: String, fileName: String) : String {
        val file = File(filePath, fileName)
        if (fileName.lastIndexOf(".") <= 0 )
        {
            if (file.exists()) {
                var i: Int = 1
                while (File(filePath, "$fileName ($i)").exists()) {
                    i++
                }
                Log.d(TAG, "$fileName ($i)")
                return "$fileName ($i)"
            } else {
                Log.d(TAG, fileName)
                return fileName
            }
        }
        val fileNameOnly = fileName.substring(0, fileName.lastIndexOf("."))
        val fileTypeStr = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
        if (file.exists()) {
            var i: Int = 1
            while (File(filePath, "$fileNameOnly ($i).$fileTypeStr").exists()) {
                i++
            }
            Log.d(TAG, "$fileNameOnly ($i).$fileTypeStr")
            return "$fileNameOnly ($i).$fileTypeStr"
        } else {
            Log.d(TAG, fileName)
            return fileName
        }
    }
}