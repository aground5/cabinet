package com.songi.cabinet

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.AnimationDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.airbnb.lottie.LottieAnimationView
import com.songi.cabinet.file.FileManager
import com.songi.cabinet.file.OpenFilePlugin
import java.io.File
import java.lang.IllegalArgumentException

class ViewManager(private val fileManager: FileManager, private val context: Context, private val fileContainer: LinearLayout, private val toolbar: Toolbar) {

    private val OBJECT_BLANK = 224
    private val OBJECT_DIR = 225
    private val OBJECT_FILE = 226  /* TODO: 파일 세분화 */
    var viewHidden = false

    fun refreshView() {
        val arFiles = fileManager.refreshFile(viewHidden)
        fileContainer.removeAllViews()

        var linearLayout = LinearLayout(context)
        for (i in arFiles.indices) {
            if (i % 3 == 0) {
                if (i != 0) {
                    fileContainer.addView(linearLayout)
                }
                linearLayout = LinearLayout(context)
                val layoutParams2 = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                linearLayout.layoutParams = layoutParams2
                linearLayout.orientation = LinearLayout.HORIZONTAL
                linearLayout.gravity = Gravity.CENTER
            }
            val file = File(fileManager.mCurrent, arFiles[i].name)
            if (file.isDirectory) {
                createObject(OBJECT_DIR, arFiles[i].name, linearLayout, arFiles[i].isHidden)
            } else {
                createObject(OBJECT_FILE, arFiles[i].name, linearLayout, arFiles[i].isHidden)
            }
        }
        repeat((3 - arFiles.size % 3) % 3) {
            createObject(OBJECT_BLANK, linearLayout)
        }
        fileContainer.addView(linearLayout)
    }

    private fun createObject(objectType: Int, fileName: String, parentLinear: LinearLayout, isHidden: Boolean) {
        val linearLayout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(context.resources.getDimensionPixelSize(R.dimen.object_padding_size))
        }

        val imageView = LottieAnimationView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.object_cubic_size),
                context.resources.getDimensionPixelSize(R.dimen.object_cubic_size)
            )
            contentDescription = fileName
            val outValue = TypedValue()
            context.theme.resolveAttribute(R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)
            if (objectType == OBJECT_DIR) {
                setOnClickListener {
                    fileManager.goDir(fileName)
                    toolbar.title = fileName
                    refreshView()
                }
            } else {
                setOnClickListener {
                    OpenFilePlugin.intentFileOpen(fileManager.mCurrent, fileName, context)
                }
            }
            setOnLongClickListener { view ->
                setMaxProgress(0.5f)
                playAnimation()
                val popupMenu = PopupMenu(context, view)
                popupMenu.menuInflater.inflate(R.menu.popup_object, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { items ->
                    when (items.itemId) {          // TODO:메뉴 아이템 추가
                        R.id.popup_delete -> {
                            if (fileManager.removeSingleFile(fileName)) {
                                refreshView()
                            } else {
                                val alertDialog = AlertDialog.Builder(context).apply {
                                    setTitle(R.string.remove_file_alert)
                                    setPositiveButton(R.string.positive, DialogInterface.OnClickListener { dialog, which ->
                                        fileManager.removeMultipleFile(fileName)
                                        refreshView()
                                    })
                                    setNegativeButton(R.string.negative, DialogInterface.OnClickListener { dialog, which ->
                                    })
                                    show()
                                }
                            }

                        }
                        R.id.popup_rename -> {
                            val newFileName = EditText(context).apply {
                                hint = context.getString(R.string.rename_file_hint)
                            }
                            val alertDialog = AlertDialog.Builder(context).apply {
                                setTitle(R.string.rename_file_title)
                                setView(newFileName)
                                setPositiveButton(R.string.positive, DialogInterface.OnClickListener { dialog, which ->
                                    fileManager.renameFile(fileName, newFileName.text.toString())
                                    refreshView()
                                })
                                setNegativeButton(R.string.negative, DialogInterface.OnClickListener { dialog, which ->

                                })
                                show()
                            }
                        }
                        //R.id.popup_copy -> {} TODO : 복사 구현
                        R.id.popup_hidden -> {
                            fileManager.switchHiddenAttrib(fileName)
                            refreshView()
                        }
                        else -> Toast.makeText(context, "We are trying to hard work!", Toast.LENGTH_SHORT).show()
                    }
                    return@setOnMenuItemClickListener true
                }
                popupMenu.show()
                return@setOnLongClickListener true
            }
            when (objectType) {     // TODO: 이미지, 동영상, PDF 등은 썸네일을 아이콘으로. 나머지는 아이콘 따로 제작하기. 후 버전에 있을 예정.
                OBJECT_DIR -> {
                    setAnimation(R.raw.animated_folder)
                    //folderAnimation = background as AnimationDrawable
                }
                OBJECT_FILE -> setImageResource(R.drawable.ic_file)
            }
            if (isHidden) {
                alpha = 0.6f
            }
        }

        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.resources.getDimensionPixelSize(R.dimen.object_text_maxline)
            )
            text = fileName
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        linearLayout.addView(imageView)
        linearLayout.addView(textView)
        parentLinear.addView(linearLayout)
    }

    private fun createObject(objectType: Int, parentLinear: LinearLayout) {
        if (objectType != OBJECT_BLANK) {
            throw IllegalArgumentException()
        }
        val linearLayout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            alpha = 0.0f
            setPadding(context.resources.getDimensionPixelSize(R.dimen.object_padding_size))
        }
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_folder)
        }

        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.resources.getDimensionPixelSize(R.dimen.object_text_maxline)
            )
        }

        linearLayout.addView(imageView)
        linearLayout.addView(textView)

        parentLinear.addView(linearLayout)
    }
}