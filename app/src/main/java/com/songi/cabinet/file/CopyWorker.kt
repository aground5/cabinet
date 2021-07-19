package com.songi.cabinet.file

import android.R
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ProgressBar
import androidx.core.view.setPadding
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.songi.cabinet.Constants.COPY_PROGRESS
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.Duration
import kotlin.time.toDuration

class CopyWorker (context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private var TAG = "CopyWorker"
    val appContext = applicationContext

    override suspend fun doWork(): Result {
        val inputPath: Array<String?> = inputData.keyValueMap.get("inputPath") as Array<String?>
        val outputPath: Array<String> = inputData.keyValueMap.get("outputPath") as Array<String>
        val size: Long = inputData.keyValueMap.get("size") as Long

        val inputStream = if (inputPath[0] == null) {
            appContext.contentResolver.openInputStream(Uri.parse(inputPath[1])) as FileInputStream
        } else {
            val inputFile = File(inputPath[0] as String, inputPath[1] as String)
            FileInputStream(inputFile)
        }
        val outputStream = FileOutputStream(File(outputPath[0], outputPath[1]))

        val buffer = ByteArray(1024)
        Log.d(TAG, "Start Copy")

        var copiedSize: Long = 0
        var time = System.currentTimeMillis()
        while (inputStream.read(buffer) > 0) {
            copiedSize += 1024
            if (System.currentTimeMillis() - time > 1000L) {
                time = System.currentTimeMillis()
                setProgress(workDataOf(COPY_PROGRESS to copiedSize))
            }
            //Log.d(TAG, "filemanager: $copiedSize")
            outputStream.write(buffer)
        }

        inputStream.close()
        outputStream.close()
        Log.d(TAG, "Finished Copy")

        return Result.success()
    }
}