package com.songi.cabinet

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.songi.cabinet.file.FileManager
import com.songi.cabinet.file.OpenFilePlugin
import java.io.File

class ViewManager(private val fileManager: FileManager, val context: Context, val fileContainer: LinearLayout) {

    fun refreshView() {
        fileManager.refreshFile()
        fileContainer.removeAllViews()

        var linearLayout = LinearLayout(context)
        for (i in fileManager.arFiles.indices) {
            if (i % 3 == 0) {
                if (i != 0) {
                    fileContainer.addView(linearLayout)
                }
                linearLayout = LinearLayout(context)
                val layoutParams2 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                linearLayout.layoutParams = layoutParams2
                linearLayout.orientation = LinearLayout.HORIZONTAL
                linearLayout.gravity = Gravity.CENTER
            }
            val file = File(fileManager.mCurrent, fileManager.arFiles[i])
            if (file.isDirectory) {
                createViewDir(fileManager.arFiles[i], linearLayout)
            } else {
                createViewFile(fileManager.arFiles[i], linearLayout)
            }
        }
        repeat ((3 - fileManager.arFiles.size % 3) % 3) {
            createBlank(linearLayout)
        }
        fileContainer.addView(linearLayout)
    }

    fun createBlank(parentLinear: LinearLayout) {
        val linearLayout = LinearLayout(context)
        val layoutParams1 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        linearLayout.layoutParams = layoutParams1
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.alpha = 0.0f
        //linearLayout.setPadding(resources.getDimensionPixelSize(R.dimen.file_padding_size))

        val imageView = ImageView(context)
        val layoutParams2 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.setImageResource(R.drawable.ic_folder)
        imageView.layoutParams = layoutParams2

        val textView = TextView(context)
        textView.layoutParams = layoutParams2

        linearLayout.addView(imageView)
        linearLayout.addView(textView)

        parentLinear.addView(linearLayout)
    }

    fun createViewDir(fileName: String, parentLinear: LinearLayout) {
        val linearLayout = LinearLayout(context)
        val layoutParams1 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        linearLayout.layoutParams = layoutParams1
        linearLayout.orientation = LinearLayout.VERTICAL
        //linearLayout.setPadding(resources.getDimensionPixelSize(R.dimen.file_padding_size))

        val imageView = ImageView(context)
        val layoutParams2 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.setImageResource(R.drawable.ic_folder)
        imageView.layoutParams = layoutParams2
        imageView.contentDescription = fileName
        imageView.setOnClickListener {
            fileManager.goDir(imageView.contentDescription.toString())
            refreshView()
        }

        val textView = TextView(context)
        textView.layoutParams = layoutParams2
        textView.text = fileName
        textView.gravity = Gravity.CENTER

        linearLayout.addView(imageView)
        linearLayout.addView(textView)

        parentLinear.addView(linearLayout)
    }

    fun createViewFile(fileName: String, parentLinear: LinearLayout) {
        var linearLayout = LinearLayout(context)
        var layoutParams1 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        linearLayout.layoutParams = layoutParams1
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(context.resources.getDimensionPixelSize(R.dimen.file_padding_size))

        var imageView = ImageView(context)
        var layoutParams2 = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.setImageResource(R.drawable.ic_file)
        imageView.layoutParams = layoutParams2
        imageView.contentDescription = fileName
        imageView.setOnClickListener {
            OpenFilePlugin.intentFileOpen(
                "${fileManager.mCurrent}/${imageView.contentDescription}",
                context
            )
        }

        var textView = TextView(context)
        textView.layoutParams = layoutParams2
        textView.text = fileName
        textView.gravity = Gravity.CENTER

        linearLayout.addView(imageView)
        linearLayout.addView(textView)

        parentLinear.addView(linearLayout)
    }
}