package com.songi.cabinet

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.setPadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.animation.keyframe.GradientColorKeyframeAnimation
import com.songi.cabinet.file.FileManager
import com.songi.cabinet.file.OpenFilePlugin
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

class ViewManager(private val fileManager: FileManager, private val context: Context, private val contentObjectContainer: LinearLayout, private val toolbar: Toolbar, val isDrawer: Boolean) {

    private val TAG = "ViewManager"
    private val OBJECT_BLANK = 224
    private val OBJECT_DIR = 225
    private val OBJECT_FILE = 226  /* TODO: 파일 세분화 */
    var viewHidden = false
    var columnCount = 3

    /**
     * 화면을 업데이트 합니다. columCount에 맞게 정렬됩니다.
     */
    fun refreshView() {
        val arFiles = fileManager.refreshFile(viewHidden)
        contentObjectContainer.removeAllViews()

        var linearLayout = LinearLayout(context)
        for (i in arFiles.indices) {
            if (i % columnCount == 0) {
                if (i != 0) {
                    contentObjectContainer.addView(linearLayout)
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
            val file = File(fileManager.mCurrent, arFiles[i])
            /**
             * 파일의 종류를 구분합니다. 현재는 디렉토리, 파일 두 종류로만 구분짓습니다.
             */
            if (file.isDirectory) {
                createObject(OBJECT_DIR, arFiles[i], linearLayout, File(fileManager.mCurrent, arFiles[i]).isHidden)
            } else {
                createObject(OBJECT_FILE, arFiles[i], linearLayout, File(fileManager.mCurrent, arFiles[i]).isHidden)
            }
        }
        repeat((columnCount - arFiles.size % columnCount) % columnCount) {
            createObject(OBJECT_BLANK, linearLayout)
        }
        contentObjectContainer.addView(linearLayout)
    }

    @DelicateCoroutinesApi
    private fun createObject(objectType: Int, fileName: String, parentLinear: LinearLayout, isHidden: Boolean) {
        /**
         * 오브젝트의 크기를 결정짓는 linearLayout입니다. 여기에 imageView, popupLinearlayout이 추가되어 최종 parentLinear에 들어갑니다.
         */
        val linearLayout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                context.resources.getDimensionPixelSize(R.dimen.object_maxHeight)
            )
            orientation = LinearLayout.VERTICAL
            setPadding(context.resources.getDimensionPixelSize(R.dimen.object_padding_size))
        }

        /**
         * 폴더나 파일의 이미지를 담는 뷰 입니다. LottieAnimationView를 적용하여 고급 애니메이션을 실행 가능하게 만들었습니다.
         */
        val imageView = LottieAnimationView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.image_cubic_size),
                context.resources.getDimensionPixelSize(R.dimen.image_cubic_size)
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
                setOnDragListener { v, event ->
                    if ((event.localState as Array<String>)[1] == v.contentDescription.toString()) {
                        return@setOnDragListener false
                    }
                    when (event.action) {
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            setMaxProgress(0.5f)
                            playAnimation()
                            Log.d(TAG, "imageView_$fileName: DRAG_ENTERED")
                            handler.postDelayed(Runnable {
                                fileManager.goDir(fileName)
                                toolbar.title = fileName
                                refreshView()
                            }, 2000)
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            try {
                                Log.d(TAG, "imageView_$fileName: DRAG_EXITED")
                                handler.removeCallbacksAndMessages(null)
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            }
                            setMaxProgress(1f)
                            resumeAnimation()
                        }
                        DragEvent.ACTION_DROP -> {
                            try {
                                Log.d(TAG, "imageView_$fileName: DROP")
                                handler.removeCallbacksAndMessages(null)
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            }
                            setMaxProgress(1f)
                            resumeAnimation()
                            fileManager.moveFile(event.localState as Array<String>, v.contentDescription.toString())
                            refreshView()
                        }
                    }

                    return@setOnDragListener true
                }
            } else {
                setOnClickListener {
                    OpenFilePlugin.intentFileOpen(fileManager.mCurrent, fileName, context)
                }
            }

            /**
             * 길게 눌렀을때, 드래그 앤 드롭 시작
             */
            setOnLongClickListener { view ->
                val item = ClipData.Item(view.tag as? CharSequence)
                Log.d(TAG, "imageView_$fileName: ${view.tag}")
                val dragData = ClipData(view.tag as? CharSequence, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
                val myShadow = View.DragShadowBuilder(this)
                val filePath: Array<String> = arrayOf(fileManager.mCurrent, fileName)
                linearLayout.visibility = LinearLayout.INVISIBLE
                view.startDragAndDrop(dragData, myShadow, filePath, 0)

                return@setOnLongClickListener true
            }
            when (objectType) {     // TODO: 이미지, 동영상, PDF 등은 썸네일을 아이콘으로. 나머지는 아이콘 따로 제작하기. 후 버전에 있을 예정.
                OBJECT_DIR -> {
                    setAnimation(R.raw.animated_folder)
                }
                OBJECT_FILE -> setImageResource(R.drawable.ic_file)
            }
            if (isHidden) {
                alpha = 0.5f
            }
        }

        val popupLinearLayout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setOnClickListener { view ->
                imageView.setMaxProgress(0.5f)
                imageView.playAnimation()
                PopupMenu(context, view).apply {
                    menuInflater.inflate(R.menu.popup_object, menu)
                    if (isDrawer) {
                        menu.findItem(R.id.popup_copy_to_clipboard).isVisible = false
                        menu.findItem(R.id.popup_move_to_clipboard).isVisible = false
                    }
                    setOnMenuItemClickListener { items ->
                        when (items.itemId) {          // TODO:메뉴 아이템 추가
                            R.id.popup_delete -> {
                                if (fileManager.removeSingleFile(fileName)) {
                                    refreshView()
                                } else {
                                    AlertDialog.Builder(context).apply {
                                        setTitle(R.string.remove_file_alert)
                                        setPositiveButton(R.string.positive) { dialog, which ->
                                            fileManager.removeRecursively(fileName)
                                            refreshView()
                                        }
                                        setNegativeButton(R.string.negative) { dialog, which ->
                                        }
                                        show()
                                    }
                                }

                            }
                            R.id.popup_rename -> {
                                val newFileName = EditText(context).apply {
                                    setText(fileName, TextView.BufferType.EDITABLE)
                                }
                                AlertDialog.Builder(context).apply {
                                    setTitle(R.string.rename_file_title)
                                    setView(newFileName)
                                    setPositiveButton(R.string.positive) { dialog, which ->
                                        fileManager.renameFile(fileName, newFileName.text.toString())
                                        refreshView()
                                    }
                                    setNegativeButton(R.string.negative) { dialog, which ->
                                    }
                                    show()
                                }
                            }
                            //R.id.popup_copy -> {} TODO : 복사 구현
                            R.id.popup_move_to_clipboard -> {
                                fileManager.moveFileToClipboard(fileName)
                                refreshView()
                            }
                            R.id.popup_copy_to_clipboard -> {
                                fileManager.copyFileToClipboard(fileName)
                                refreshView()
                            }
                            R.id.popup_hidden -> {
                                fileManager.switchHiddenAttrib(fileName)
                                refreshView()
                            }
                            else -> Toast.makeText(context, "We are trying to hard work!", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnMenuItemClickListener true
                    }
                    setOnDismissListener {
                        imageView.setMaxProgress(1f)
                        imageView.resumeAnimation()
                    }
                    show()
                }
            }
        }
        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            maxWidth = context.resources.getDimensionPixelSize(R.dimen.object_text_maxWidth)
            text = fileName
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        val expandArrow = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_expand_more)
        }

        popupLinearLayout.addView(textView)
        popupLinearLayout.addView(expandArrow)
        linearLayout.addView(imageView)
        linearLayout.addView(popupLinearLayout)
        parentLinear.addView(linearLayout)
    }

    private fun createObject(objectType: Int, parentLinear: LinearLayout) {
        if (objectType != OBJECT_BLANK) {
            throw IllegalArgumentException()
        }
        val linearLayout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                context.resources.getDimensionPixelSize(R.dimen.object_maxHeight)
            )
            orientation = LinearLayout.VERTICAL
            setPadding(context.resources.getDimensionPixelSize(R.dimen.object_padding_size))
            visibility = LinearLayout.INVISIBLE
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
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        linearLayout.addView(imageView)
        linearLayout.addView(textView)

        parentLinear.addView(linearLayout)
    }
}