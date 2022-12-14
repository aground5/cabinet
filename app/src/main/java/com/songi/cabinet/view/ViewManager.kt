package com.songi.cabinet.view

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.lottie.LottieAnimationView
import com.songi.cabinet.Constants.FOLDER_TIME_INTERVAL
import com.songi.cabinet.Constants.IMAGEVIEW_ALPHA
import com.songi.cabinet.Constants.OBJECT_BLANK
import com.songi.cabinet.Constants.OBJECT_DIR
import com.songi.cabinet.Constants.OBJECT_EXCEL
import com.songi.cabinet.Constants.OBJECT_FILE
import com.songi.cabinet.Constants.OBJECT_IMAGE
import com.songi.cabinet.Constants.OBJECT_PDF
import com.songi.cabinet.Constants.OBJECT_PPT
import com.songi.cabinet.Constants.OBJECT_TEXT
import com.songi.cabinet.Constants.OBJECT_VIDEO
import com.songi.cabinet.Constants.OBJECT_WORD
import com.songi.cabinet.Constants.RENDER_BUILDER
import com.songi.cabinet.R
import com.songi.cabinet.file.FileManager
import com.songi.cabinet.file.OpenFilePlugin
import com.songi.cabinet.file.RefreshViewRequester
import com.songi.cabinet.file.ThumbnailTranslator.getThumbnailFile
import kotlinx.coroutines.*
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.Runnable
import java.util.*

class ViewManager(private val tag: String,
                  private val fileManager: FileManager,
                  private val context: Context,
                  private val contentObjectContainer: LinearLayout,
                  private val toolbar: Toolbar,
                  private val isDrawer: Boolean,
                  refreshViewRequester: RefreshViewRequester,
                  private val imageThumbnailSaver: ImageThumbnailSaver) {

    private val TAG = "ViewManager"
    var viewHidden = false
    var columnCount = 3
    init {
        refreshViewRequester.addOnRequestListener { requestTag ->
            if (tag == requestTag) {
                refreshView()
            }
        }
    }

    var objectImageList = mutableListOf<Pair<LottieAnimationView, File>>()
    var thumbnailRenderThread = ThumbnailRenderThread(objectImageList)

    /**
     * ????????? ???????????? ?????????. columCount??? ?????? ???????????????.
     */
    @DelicateCoroutinesApi
    fun refreshView() {
        objectImageList = mutableListOf()
        val arFiles = fileManager.refreshFile(viewHidden)
        contentObjectContainer.removeAllViews()
        var objects = mutableListOf<LinearLayout>()
        var linearLayout = LinearLayout(context)
        CoroutineScope(Dispatchers.Main).launch {
            for (i in arFiles.indices) {
                CoroutineScope(Dispatchers.Main).async {
                    val file = File(fileManager.mCurrent, arFiles[i])
                    /**
                     * ????????? ????????? ???????????????. ????????? ????????????, ?????? ??? ???????????? ??????????????????.
                     */
                    objects.add(
                        if (file.isDirectory) {
                            createObject(OBJECT_DIR, file)
                        } else {
                            val mimeType =
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                            if (mimeType?.contains("image/", true) == true) {
                                createObject(OBJECT_IMAGE, file)
                            } else if (mimeType?.contains("video/", true) == true) {
                                createObject(OBJECT_VIDEO, file)
                            } else {
                                when (file.extension.lowercase(Locale.getDefault())) {  // TODO: ?????? ?????? ?????? ?????????
                                    "pdf" -> createObject(OBJECT_PDF, file)
                                    "txt" -> createObject(OBJECT_TEXT, file)
                                    "xlsx", "xls", "csv" -> createObject(OBJECT_EXCEL, file)
                                    "pptx", "ppt", "pps" -> createObject(OBJECT_PPT, file)
                                    "doc", "docx" -> createObject(OBJECT_WORD, file)
                                    else -> createObject(OBJECT_FILE, file)
                                }
                            }
                        }
                    )
                }.await()
            }

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
                 * ????????? ????????? ???????????????. ????????? ????????????, ?????? ??? ???????????? ??????????????????.
                 */
                linearLayout.addView(objects[i])
            }

            repeat((columnCount - arFiles.size % columnCount) % columnCount) {
                linearLayout.addView(createObject(OBJECT_BLANK))
            }


            contentObjectContainer.addView(linearLayout)

            thumbnailRenderThread.interrupt()
            thumbnailRenderThread = ThumbnailRenderThread(objectImageList)
            thumbnailRenderThread.list = objectImageList
            try {
                thumbnailRenderThread.start()
            } catch (e: IllegalThreadStateException) {
                e.printStackTrace()
            }
        }
    }

    @DelicateCoroutinesApi
    private fun createObject(objectType: Int, file: File): LinearLayout {
        /**
         * ??????????????? ????????? ???????????? linearLayout?????????. ????????? imageView, popupLinearlayout??? ???????????? ?????? parentLinear??? ???????????????.
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
         * ????????? ????????? ???????????? ?????? ??? ?????????. LottieAnimationView??? ???????????? ?????? ?????????????????? ?????? ???????????? ??????????????????.
         */
        val imageView = LottieAnimationView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.image_cubic_size),
                context.resources.getDimensionPixelSize(R.dimen.image_cubic_size)
            )
            contentDescription = file.name
            val outValue = TypedValue()
            context.theme.resolveAttribute(R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)

            if (objectType == OBJECT_DIR) {
                setOnClickListener {
                    fileManager.goDir(file.name)
                    toolbar.title = file.name
                    refreshView()
                }
                setOnDragListener { v, event ->
                    if ((event.localState as Array<String>)[1] == v.contentDescription.toString() &&
                        (event.localState as Array<String>)[0] == fileManager.mCurrent) {
                        //Log.d(TAG, "This is right this folder.")
                        return@setOnDragListener true
                    }
                    when (event.action) {
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            setMaxProgress(0.5f)
                            playAnimation()
                            Log.d(TAG, "imageView_$file.name: DRAG_ENTERED")
                            handler.postDelayed(Runnable {
                                fileManager.goDir(file.name)
                                toolbar.title = file.name
                                refreshView()
                            }, FOLDER_TIME_INTERVAL)
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            try {
                                Log.d(TAG, "imageView_$file.name: DRAG_EXITED")
                                handler.removeCallbacksAndMessages(null)
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            }
                            setMaxProgress(1f)
                            resumeAnimation()
                        }
                        DragEvent.ACTION_DROP -> {
                            try {
                                Log.d(TAG, "imageView_$file.name: DROP")
                                handler.removeCallbacksAndMessages(null)
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            }
                            setMaxProgress(1f)
                            resumeAnimation()
                            if (!fileManager.moveFile(event.localState as Array<String>, v.contentDescription.toString())) {
                                return@setOnDragListener true
                            }
                            refreshView()
                        }
                    }

                    return@setOnDragListener true
                }
            } else {
                setOnClickListener {
                    OpenFilePlugin.intentFileOpen(fileManager.mCurrent, file.name, context)
                }
            }

            when (objectType) {     // TODO: ?????????, ?????????, PDF ?????? ???????????? ???????????????. ???????????? ????????? ?????? ????????????. ??? ????????? ?????? ??????.
                OBJECT_DIR -> setAnimation(R.raw.animated_folder)
                OBJECT_IMAGE -> {
                    setImageResource(R.drawable.ic_file)        // TODO: ic_image ??? ?????????

                    val thumbnailFile = getThumbnailFile(file)
                    if (thumbnailFile.exists()) {
                        val map = Pair(this, thumbnailFile)
                        objectImageList.add(map)
                    } else {
                        imageThumbnailSaver.addImage(ImageThumbnailVO(file.name, file.absolutePath, isProcessed = false, this))
                    }
                }
                OBJECT_FILE -> setImageResource(R.drawable.ic_file)
                else -> setImageResource(R.drawable.ic_file)
            }
            if (file.isHidden) {
                alpha = IMAGEVIEW_ALPHA
            }
        }

        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            maxWidth = context.resources.getDimensionPixelSize(R.dimen.object_text_maxWidth)
            text = file.name
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
                    if (objectType == OBJECT_DIR) {
                        menu.findItem(R.id.popup_copy_to_clipboard).isVisible = false   // TODO: ???????????? ??????????????? ???????????? ??????.
                    }
                    if (file.isHidden) {
                        menu.findItem(R.id.popup_hidden).setTitle(R.string.popup_show)
                    }
                    setOnMenuItemClickListener { items ->
                        when (items.itemId) {          // TODO:?????? ????????? ??????
                            R.id.popup_delete -> {
                                if (fileManager.removeSingleFile(objectType, file.name)) {
                                    linearLayout.visibility = LinearLayout.GONE
                                } else if (objectType == OBJECT_DIR) {
                                    AlertDialog.Builder(context).apply {
                                        setTitle(R.string.remove_file_alert)
                                        setPositiveButton(R.string.positive) { dialog, which ->
                                            fileManager.removeRecursively(file.name)
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
                                    setText(file.name, TextView.BufferType.EDITABLE)
                                }
                                AlertDialog.Builder(context).apply {
                                    setTitle(R.string.rename_file_title)
                                    setView(newFileName)
                                    setPositiveButton(R.string.positive) { dialog, which ->
                                        fileManager.renameFile(file.name, newFileName.text.toString())
                                        refreshView()
                                    }
                                    setNegativeButton(R.string.negative) { dialog, which ->
                                    }
                                    show()
                                }
                            }
                            //R.id.popup_copy -> {} TODO : ?????? ??????
                            R.id.popup_move_to_clipboard -> {
                                fileManager.moveFileToClipboard(file.name)
                                refreshView()
                            }
                            R.id.popup_copy_to_clipboard -> {
                                fileManager.copyFileToClipboard(file.name)
                            }
                            R.id.popup_hidden -> {
                                fileManager.switchHiddenAttrib(file.name)
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

        popupLinearLayout.addView(textView)
        popupLinearLayout.addView(expandArrow)
        linearLayout.addView(imageView)
        linearLayout.addView(popupLinearLayout)

        /**
         * ?????? ????????????, ????????? ??? ?????? ??????
         */
        imageView.setOnLongClickListener { view ->
            val item = ClipData.Item(view.tag as? CharSequence)
            Log.d(TAG, "imageView_$file.name: ${view.tag}")
            val dragData = ClipData(view.tag as? CharSequence, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
            val myShadow = View.DragShadowBuilder(linearLayout)
            val filePath: Array<String> = arrayOf(fileManager.mCurrent, file.name)
            // linearLayout.visibility = LinearLayout.INVISIBLE // TODO: ?????? ?????? ?????? ??????
            view.startDragAndDrop(dragData, myShadow, filePath, 0)

            return@setOnLongClickListener true
        }

        return linearLayout
    }

    private fun createObject(objectType: Int): LinearLayout {
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

        return linearLayout
    }
}