package com.songi.cabinet.view

import android.util.Log
import com.songi.cabinet.Constants.FILENAME_VIEW_COLUMN_CONFIG
import com.songi.cabinet.Constants.filesDir
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ViewColumnSaver() {
    private var TAG = "ViewColumnSaver"

    var portraitColumns: Int = 3; set(value) {
        field = value
        save()
    }
    var landscapeColumns: Int = 6; set(value) {
        field = value
        save()
    }
    val settingsDir = "$filesDir/settings"
    val absolutePath = "$settingsDir/$FILENAME_VIEW_COLUMN_CONFIG"
    init {
        val file = File(settingsDir, FILENAME_VIEW_COLUMN_CONFIG)
        if (!file.exists()) {
            File("$filesDir/settings").mkdirs()
            file.createNewFile()
            save()
        } else {
            load()
        }
    }

    private fun load() {
        val inputString = FileReader(absolutePath).use { it.readText() }
        Log.d(TAG, inputString)
        val splited = inputString.split(',')
        portraitColumns = splited[0].toInt()
        landscapeColumns = splited[1].toInt()
    }

    private fun save() {
        val outputString = "$portraitColumns,$landscapeColumns"
        FileWriter(absolutePath, false).use { it.write(outputString) }
    }
}