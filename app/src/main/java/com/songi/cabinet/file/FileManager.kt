package com.songi.cabinet.file

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOError
import java.io.IOException

class FileManager(val context: Context) {
    private var TAG = "FileManager"
    var fileCount = 0
    var folderCount = 0

    var mRoot = context.filesDir.absolutePath
    var mCurrent = mRoot
    var arFiles = File(mCurrent).list()

    fun refreshFile() {
        arFiles = File(mCurrent).list()
    }

    fun saveFile(absoluteFilePath: String) {
        val externalFile = File(absoluteFilePath)
        val filename = absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/'), absoluteFilePath.length)
        val internalFile = File(mCurrent + filename)
        val internalFileOutputStream = FileOutputStream(internalFile)
        try {
            internalFileOutputStream.write(externalFile.readBytes())
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }

    }

    fun createFile() {
        fileCount++
        var file = File(mCurrent, fileCount.toString() + ".file")

        file.createNewFile()
    }

    fun createFolder() {
        folderCount++
        var file = File("$mCurrent/$folderCount")

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