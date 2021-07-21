package com.songi.cabinet.file

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ProgressBar
import androidx.core.view.setPadding
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.songi.cabinet.Constants
import com.songi.cabinet.Constants.OBJECT_IMAGE
import com.songi.cabinet.R
import com.songi.cabinet.file.ThumbnailTranslator.getThumbnailFile
import kotlinx.coroutines.*
import java.io.*
import java.text.Collator
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.ArrayList

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

    fun importFile(uris: ArrayList<Uri>) {
        val filesInfo = arrayListOf<Array<String?>>()

        for (uri in uris) {
            filesInfo.add(getFileName(uri))
        }
        val inputPathList = arrayListOf<Array<String?>>()
        val outputPathList = arrayListOf<Array<String>>()
        val sizeList = arrayListOf<Long>()

        for (i in filesInfo.indices) {
            if (filesInfo[i][1] == null) {
                inputPathList.add(arrayOf(null, uris[i].toString()))
                outputPathList.add(arrayOf(mCurrent, isFileExists(mCurrent, filesInfo[i][0]!!)))
                sizeList.add(0L)
            } else {
                inputPathList.add(arrayOf(null, uris[i].toString()))
                outputPathList.add(arrayOf(mCurrent, isFileExists(mCurrent, filesInfo[i][0]!!)))
                sizeList.add(filesInfo[i][1]!!.toLong())
            }
        }
        copyWithBuffer(inputPathList, outputPathList, sizeList)
    }

    fun importFile(uri: Uri) {
        val fileInfo = getFileName(uri)

        if (fileInfo[0] == null) {
            // TODO: 파일 이름 지정하라고 창 띄우기
        }
        if (fileInfo[1] == null) {
            copyWithBuffer(arrayOf(null, uri.toString()),
                arrayOf(mCurrent, isFileExists(mCurrent, fileInfo[0]!!)),
                0)
        } else {
            copyWithBuffer(arrayOf(null, uri.toString()),
                arrayOf(mCurrent, isFileExists(mCurrent, fileInfo[0]!!)),
                fileInfo[1]!!.toLong())
        }
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

    private fun copyWithBuffer(inputPath: ArrayList<Array<String?>>, outputPath: ArrayList<Array<String>>, size: ArrayList<Long>) {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 200
            isIndeterminate = false
            setPadding(context.resources.getDimensionPixelSize(R.dimen.progressbar_padding_size))
        }
        val alertDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.copying_files)
            setView(progressBar)
            setMessage("")
            setCancelable(false)
            setPositiveButton(R.string.positive) { dialog, which ->
                refreshViewRequester.request(tag)
            }
        }.create()
        val thread = CopyThread(context, inputPath, outputPath, size, progressBar, alertDialog)
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.negative)) {dialog, which ->
            thread.interrupt()
        }
        thread.start()
        alertDialog.show()
    }

    private fun copyWithBuffer(inputPath: Array<String?>, outputPath: Array<String>, size: Long) {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 200
            isIndeterminate = false
            setPadding(context.resources.getDimensionPixelSize(R.dimen.progressbar_padding_size))
        }
        val alertDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.copying_files)
            setView(progressBar)
            setMessage("")
            setCancelable(false)
            setPositiveButton(R.string.positive) { dialog, which ->
                refreshViewRequester.request(tag)
            }
        }.create()
        val thread = CopyThread(context, arrayListOf(inputPath), arrayListOf(outputPath), arrayListOf(size), progressBar, alertDialog)
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.negative)) {dialog, which ->
            thread.interrupt()
        }
        alertDialog.show()
        thread.start()
    }

    fun moveFile(filePath: Array<String>): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$filePath[1]")
        if (filePath[0] == mCurrent) {
            return false
        }
        return file.renameTo(File(mCurrent, isFileExists(mCurrent, filePath[1])))
    }

    fun moveFile(filePath: Array<String>, droppedDir: String): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$droppedDir/$filePath[1]")
        if (filePath[0] == "$mCurrent/$droppedDir") {
            return false
        }
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

    fun removeSingleFile(objectType: Int, fileName: String) : Boolean{
        val file = File(mCurrent, fileName)
        when (objectType) {
            OBJECT_IMAGE -> getThumbnailFile(file).delete()
        }
        return file.delete()
    }

    fun removeRecursively(fileName: String) : Boolean{
        val file = File(mCurrent, fileName)
        val absolutePath = file.absolutePath
        val path = absolutePath.substring(absolutePath.indexOf(Constants.PACKAGE_NAME) + "${Constants.PACKAGE_NAME}/files".length, absolutePath.lastIndexOf('/'))
        val thumbnailPath = "${Constants.filesDir}/${Constants.FOLDER_THUMBNAIL}$path"
        val thumbnailName = "${file.name}"
        File(thumbnailPath, thumbnailName).deleteRecursively()
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