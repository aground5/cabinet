package com.songi.cabinet.file

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.songi.cabinet.R
import kotlinx.coroutines.flow.merge
import java.io.*
import java.lang.Exception
import java.text.Collator
import java.util.*

class FileManager(val context: Context, root: String) {
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

    fun refreshFile(viewHidden: Boolean) : MutableList<String> {
        var directories = if (viewHidden) {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter pathname.isDirectory
            })
        } else {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isHidden && pathname.isDirectory
            })
        }
        var files = if (viewHidden) {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isDirectory
            })
        } else {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isHidden && !pathname.isDirectory
            })
        }

        val collator = Collator.getInstance(Locale(context.applicationContext.resources.configuration.locales.get(0).language))
        var mutableDirectories = mutableListOf<String>()
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
        val fileName = getFileName(uri)
        if (fileName != null) {
            Log.d(TAG, fileName)
        }
        var fileDescriptor: ParcelFileDescriptor? = null
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e : FileNotFoundException) {
            e.printStackTrace()
        }

        val internalFile = File(mCurrent, fileName)
        try {
            val externalFileInputStream = FileInputStream(fileDescriptor?.fileDescriptor)
            val internalFileOutputStream = FileOutputStream(internalFile)

            copyWithBuffer(externalFileInputStream, internalFileOutputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copyWithBuffer(inputStream: FileInputStream, outputStream: FileOutputStream) {
        var buffer: ByteArray = ByteArray(1024)

        while (inputStream.read(buffer) > 0) {
            outputStream.write(buffer)
        }

        inputStream.close()
        outputStream.close()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun moveFile(filePath: Array<String>): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$filePath[1]")

        return file.renameTo(File(mCurrent, filePath[1]))
    }

    fun moveFile(filePath: Array<String>, droppedDir: String): Boolean {
        val file = File(filePath[0], filePath[1])
        Log.d(TAG, "$filePath[0]/$filePath[1] --> $mCurrent/$droppedDir/$filePath[1]")

        return file.renameTo(File("$mCurrent/$droppedDir", filePath[1]))
    }

    fun moveFileToClipboard(fileName: String): Boolean {
        val file = File(mCurrent, fileName)
        return file.renameTo(File("${context.filesDir.absolutePath}/Cabinet_temp_folder", fileName))
    }

    fun copyFile(origFilePath: Array<String>) {
        val inputStream = FileInputStream(File(origFilePath[0], origFilePath[1]))
        val outputStream = FileOutputStream(File(mCurrent, origFilePath[1]))
        try {
            copyWithBuffer(inputStream, outputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copyFileToClipboard(fileName: String) {
        val inputStream = FileInputStream(File(mCurrent, fileName))
        val outputStream = FileOutputStream(File("${context.filesDir.absolutePath}/Cabinet_temp_folder", fileName))
        try {
            copyWithBuffer(inputStream, outputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun renameFile(origFileName: String, newFileName: String) {
        val file = File(mCurrent, origFileName)
        file.renameTo(File(mCurrent, newFileName))
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
        val file = File(mCurrent, folderName)
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
}