package com.songi.cabinet.file

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.songi.cabinet.R
import com.songi.cabinet.file.OpenFilePlugin.byteCalculation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.util.concurrent.Executor

class CopyThread(
    val context: Context,
    val inputPath: ArrayList<Array<String?>>,
    val outputPath: ArrayList<Array<String>>,
    val size: ArrayList<Long>,
    val progressBar: ProgressBar,
    val alertDialog: AlertDialog
) : Thread() {
    private var TAG = "CopyThread"

    override fun run() {
        var totalCopiedSize: Long = 0
        var totalSize: Long = 0
        var time = System.currentTimeMillis()
        for (i in size) {
            totalSize += i
        }
        for (i in inputPath.indices) {
            CoroutineScope(Dispatchers.Main).launch {
                alertDialog.setMessage(
                    "${i + 1} / ${inputPath.size}\n" +
                            "${outputPath[i][1]}\n" +
                            "${byteCalculation(totalCopiedSize)} / ${byteCalculation(totalSize)}"
                )
            }
            val inputStream = if (inputPath[i][0] == null) {
                context.contentResolver.openInputStream(Uri.parse(inputPath[i][1])) as FileInputStream
            } else {
                val inputFile = File(inputPath[i][0], inputPath[i][1]!!)
                FileInputStream(inputFile)
            }
            val outputFile = File(outputPath[i][0], outputPath[i][1])
            val outputStream = outputFile.outputStream()

            val buffer = ByteArray(1024)
            Log.d(TAG, "Start Copy")
            var copiedSize = 0L
            while (inputStream.read(buffer) > 0) {
                if (interrupted()) {
                    Log.e(TAG, "Interrupted ---> File Deleted")
                    outputFile.delete()
                    break
                }
                copiedSize += 1024
                if (System.currentTimeMillis() - time > 1000L) {
                    CoroutineScope(Dispatchers.Main).launch {
                        alertDialog.setMessage(
                            "${i + 1} / ${inputPath.size}\n" +
                                    "${outputPath[i][1]}\n" +
                                    "${byteCalculation(totalCopiedSize + copiedSize)} / ${byteCalculation(totalSize)}"
                        )
                        progressBar.post {
                            val percentage = if (size[i] == 0L) {
                                0
                            } else {
                                (totalCopiedSize / (totalSize / 200)).toInt()
                            }
                            progressBar.setProgress(percentage, true)
                        }
                    }
                    time = System.currentTimeMillis()
                }
                //Log.d(TAG, "filemanager: $copiedSize")
                outputStream.write(buffer)
            }
            totalCopiedSize += size[i]

            inputStream.close()
            outputStream.close()

            Log.d(TAG, "Finished Copy")
        }

        CoroutineScope(Dispatchers.Main).launch {
            alertDialog.setMessage(
                "${inputPath.size} / ${inputPath.size}\n" +
                        "${outputPath[inputPath.size - 1][1]}\n" +
                        "${byteCalculation(totalSize)} / ${byteCalculation(totalSize)}"
            )
            progressBar.post {
                progressBar.setProgress(200, true)
            }
            alertDialog.setTitle(R.string.finish_copy)
            alertDialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.VISIBLE
        }
    }
}