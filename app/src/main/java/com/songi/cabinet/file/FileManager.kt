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
import java.io.*
import java.lang.Exception

class FileManager(val context: Context) {
    private var TAG = "FileManager"

    private val mRoot = "${context.filesDir.absolutePath}/Cabinet_user_folder"
    var mCurrent = mRoot; private set
    var mCurrentFolder = mCurrent.substring(mCurrent.lastIndexOf('/') + 1, mCurrent.length); private set
    init {
        val fileObjectMRoot = File(mRoot)
        if (!fileObjectMRoot.exists()) {
            fileObjectMRoot.mkdirs()
        }
    }

    fun refreshFile(viewHidden: Boolean) : Array<File> {
        var arFiles = if (viewHidden) {
            File(mCurrent).listFiles()
        } else {
            File(mCurrent).listFiles(FileFilter { pathname ->
                return@FileFilter !pathname.isHidden
            })
        }
        mCurrentFolder = mCurrent.substring(mCurrent.lastIndexOf('/') + 1, mCurrent.length)
        return arFiles
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

        val externalFileInputStream = FileInputStream(fileDescriptor?.fileDescriptor)
        val internalFile = File(mCurrent, fileName)
        val internalFileOutputStream = FileOutputStream(internalFile)
        try {
            internalFileOutputStream.write(externalFileInputStream.readBytes())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            externalFileInputStream.close()
            internalFileOutputStream.close()
        }
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

    fun renameFile(origFileName: String, newFileName: String) {
        val file = File(mCurrent, origFileName)
        file.renameTo(File(mCurrent, newFileName))
    }

    fun removeSingleFile(fileName: String) : Boolean{
        val file = File(mCurrent, fileName)
        return file.delete()
    }

    fun removeMultipleFile(fileName: String) : Boolean{
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