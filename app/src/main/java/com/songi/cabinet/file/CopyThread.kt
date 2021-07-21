package com.songi.cabinet.file

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.songi.cabinet.R
import com.songi.cabinet.file.OpenFilePlugin.byteCalculation
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
        for (i in inputPath.indices) {
            alertDialog.setMessage("${i + 1} / ${inputPath.size}\n" +
                    "${outputPath[i][1]}\n" +
                    "${byteCalculation(0L)} / ${byteCalculation(size[i])}")
            progressBar.post {
                progressBar.setProgress(0, false)
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

            var copiedSize: Long = 0
            var time = System.currentTimeMillis()
            while (inputStream.read(buffer) > 0) {
                if (interrupted()) break
                copiedSize += 1024
                if (System.currentTimeMillis() - time > 1000L) {
                    alertDialog.setMessage("${i + 1} / ${inputPath.size}\n" +
                            "${outputPath[i][1]}\n" +
                            "${byteCalculation(copiedSize)} / ${byteCalculation(size[i])}")
                    progressBar.post {
                        val percentage = if (size[i] == 0L) {
                            0
                        } else {
                            (copiedSize / (size[i] / 200)).toInt()
                        }
                        progressBar.setProgress(percentage, true)
                    }
                    time = System.currentTimeMillis()
                }
                //Log.d(TAG, "filemanager: $copiedSize")
                outputStream.write(buffer)
            }

            inputStream.close()
            outputStream.close()
            if (interrupted()) {
                outputFile.delete()
                break
            }
            Log.d(TAG, "Finished Copy")
            alertDialog.setMessage("${i + 1} / ${inputPath.size}\n" +
                    "${outputPath[i][1]}\n" +
                    "${byteCalculation(copiedSize)} / ${byteCalculation(size[i])}")
            progressBar.post {
                progressBar.setProgress(200, true)
            }
        }

        try {
            alertDialog.setTitle(R.string.finish_copy)
            alertDialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}