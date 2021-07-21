package com.songi.cabinet

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.songi.cabinet.Constants.BACK_SPACE_TIME_INTERVAL
import com.songi.cabinet.Constants.DRAWER_TIME_INTERVAL
import com.songi.cabinet.Constants.EXTEND_SPACE_ALPHA
import com.songi.cabinet.Constants.FOLDER_CLIPBOARD
import com.songi.cabinet.Constants.FOLDER_USER
import com.songi.cabinet.Constants.PERMISSION_READ_EXTERNAL_STORAGE
import com.songi.cabinet.Constants.TIME_INTERVAL
import com.songi.cabinet.databinding.ActivityMainBinding
import com.songi.cabinet.file.FileManager
import com.songi.cabinet.file.RefreshViewRequester
import com.songi.cabinet.view.ImageThumbnailSaver
import com.songi.cabinet.view.ThumbnailRenderThread
import com.songi.cabinet.view.ViewColumnSaver
import com.songi.cabinet.view.ViewManager

class MainActivity : AppCompatActivity(), androidx.work.Configuration.Provider {
    private val TAG = "MainActivity"
    private var backPressedTime: Long = 0
    private var fileManager: FileManager? = null
    private lateinit var viewManager: ViewManager
    private var drawerFileManager: FileManager? = null
    private lateinit var drawerViewManager: ViewManager

    private var mBinding : ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private val requestResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data!!.also { uri ->
                fileManager!!.importFile(uri)
                viewManager.refreshView()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.songi.cabinet.Constants.filesDir = filesDir.absolutePath

        Log.d(TAG, Constants.filesDir!!)

        val refreshViewRequester = RefreshViewRequester()
        val imageThumbnailSaver = ImageThumbnailSaver(applicationContext, this)
        viewColumnSaver = ViewColumnSaver()


        fileManager = FileManager("CONTENT", this, "${filesDir}/$FOLDER_USER", this, refreshViewRequester)
        viewManager = ViewManager("CONTENT", fileManager!!, this, binding.contentObjectContainer, binding.toolbar, isDrawer = false, refreshViewRequester, imageThumbnailSaver)
        viewManager.refreshView()
        drawerFileManager = FileManager("DRAWER", this, "${filesDir}/$FOLDER_CLIPBOARD", this, refreshViewRequester)

        importAction()

        drawerViewManager = ViewManager("DRAWER", drawerFileManager!!, this, binding.drawerObjectContainer, binding.toolbar, isDrawer = true, refreshViewRequester, imageThumbnailSaver)
        drawerViewManager.columnCount = 1
        drawerViewManager.refreshView()

        mBinding!!.apply {
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.app_bar_search -> {}
                    R.id.app_bar_switch -> {}
                    R.id.app_bar_create_folder -> {
                        val editText = EditText(this@MainActivity).apply {
                            hint = getString(R.string.create_folder_hint)
                        }
                        val alertDialog = AlertDialog.Builder(this@MainActivity).apply {
                            setTitle(R.string.create_folder_title)
                            setView(editText)
                            setPositiveButton(R.string.positive, DialogInterface.OnClickListener { dialog, which ->
                                val folderName = editText.text.toString()
                                fileManager!!.createFolder(folderName)
                                viewManager.refreshView()
                            })
                            setNegativeButton(R.string.negative, DialogInterface.OnClickListener { dialog, which ->

                            })
                            show()
                        }
                    }
                    R.id.app_bar_import -> checkPermission()
                    else -> {}
                }
                return@setOnMenuItemClickListener true
            }
            backSpace.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d(TAG, "backSpace: EXITED")
                        backSpace.handler.postDelayed(Runnable {
                            onBackPressed()
                        }, BACK_SPACE_TIME_INTERVAL)
                        backSpace.alpha = EXTEND_SPACE_ALPHA
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        Log.d(TAG, "backSpace: ENTERED")
                        backSpace.alpha = 0.0f
                        try {
                            backSpace.handler.removeCallbacksAndMessages(null)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                    DragEvent.ACTION_DROP -> {
                        Log.d(TAG, "backSpace: DROP")
                        return@setOnDragListener false
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        backSpace.alpha = 0.0f
                        try {
                            backSpace.handler.removeCallbacksAndMessages(null)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }

                return@setOnDragListener true
            }
            drawerExtendSpace.setOnDragListener { v, event ->
                if (!drawer.isOpen) {
                    when (event.action) {
                        DragEvent.ACTION_DRAG_ENTERED -> {
                            Log.d(TAG, "drawerScrollable: EXITED")
                            drawerScrollable.handler.postDelayed(Runnable {
                                drawer.open()
                                drawerExtendSpace.alpha = 0.0f
                            }, DRAWER_TIME_INTERVAL)
                            drawerExtendSpace.alpha = EXTEND_SPACE_ALPHA
                        }
                        DragEvent.ACTION_DRAG_EXITED -> {
                            Log.d(TAG, "drawerScrollable: ENTERED")
                            drawer.close()
                            drawerExtendSpace.alpha = 0.0f
                            try {
                                drawerScrollable.handler.removeCallbacksAndMessages(null)
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                            }
                        }
                        DragEvent.ACTION_DROP -> {
                            Log.d(TAG, "drawerScrollable: DROP")
                        }
                    }
                }

                Log.d(TAG,"drawer.isOpen: ${drawer.isOpen}")

                return@setOnDragListener true
            }
            contentScrollable.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        Log.d(TAG, "fileContainer: DROP")
                        if (fileManager!!.moveFile(event.localState as Array<String>)) {
                            viewManager.refreshView()
                            drawerViewManager.refreshView()
                            return@setOnDragListener true
                        } else {
                            return@setOnDragListener false
                        }
                    }
                }
                return@setOnDragListener true
            }

            drawerScrollable.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        Log.d(TAG, "drawerScrollable: ENTERED")
                        drawer.open()
                        try {
                            drawerScrollable.handler.removeCallbacksAndMessages(null)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        Log.d(TAG, "drawerScrollable: EXITED")
                        drawerScrollable.handler.postDelayed(Runnable {
                            drawer.close()
                        }, DRAWER_TIME_INTERVAL)
                    }
                    DragEvent.ACTION_DROP -> {
                        Log.d(TAG, "drawerScrollable: DROP")
                        if (drawerFileManager!!.moveFile(event.localState as Array<String>)) {
                            viewManager.refreshView()
                            drawerViewManager.refreshView()
                            return@setOnDragListener true
                        } else {
                            return@setOnDragListener false
                        }
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        try {
                            drawerScrollable.handler.removeCallbacksAndMessages(null)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }
                return@setOnDragListener true
            }
            drawer.addDrawerListener(object: DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                }

                override fun onDrawerOpened(drawerView: View) {
                    toolbar.title = if (drawerFileManager!!.isCurrentRoot()) {
                        getString(R.string.app_name)
                    } else {
                        drawerFileManager!!.mCurrentFolder
                    }
                }

                override fun onDrawerClosed(drawerView: View) {
                    toolbar.title = if (fileManager!!.isCurrentRoot()) {
                        getString(R.string.app_name)
                    } else {
                        fileManager!!.mCurrentFolder
                    }
                }

                override fun onDrawerStateChanged(newState: Int) {
                }

            })
            seekBar.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    if (Build.VERSION.SDK_INT < 26 && seekBar.progress == 0) {
                        return
                    }
                    viewManager.columnCount = seekBar.progress
                    viewManager.refreshView()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            refreshLayout.setOnRefreshListener {
                viewManager.refreshView()
                refreshLayout.isRefreshing = false
            }
            drawerRefreshLayout.setOnRefreshListener {
                drawerViewManager.refreshView()
                drawerRefreshLayout.isRefreshing = false
            }
            /*contentScrollable.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                viewManager.thumbnailRenderThread.interrupt()
                try {
                    viewManager.thumbnailRenderThread.start()
                } catch (e: IllegalThreadStateException) {
                    e.printStackTrace()
                }
                Log.d(TAG, "contentScrollable.setOnScrollChangeListener")
            }
            drawerScrollable.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                drawerViewManager.thumbnailRenderThread.interrupt()
                try {
                    drawerViewManager.thumbnailRenderThread.start()
                } catch (e: IllegalThreadStateException) {
                    e.printStackTrace()
                }

                Log.d(TAG, "drawerScrollable.setOnScrollChangeListener")
            }*/
        }

        findViewById<Switch>(R.id.view_hidden_toggle).setOnCheckedChangeListener { buttonView, isChecked ->
            viewManager.viewHidden = isChecked
            viewManager.refreshView()
            drawerViewManager.viewHidden = isChecked
            drawerViewManager.refreshView()
        }
    }

    private fun importAction() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { string ->

                    }
                } else {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                        fileManager!!.importFile(uri)
                    }
                }
                mBinding!!.drawer.open()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
                    for( i in it ) {
                        fileManager!!.importFile(i as Uri)
                    }
                }
                mBinding!!.drawer.open()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewManager.refreshView()
    }

    private fun checkPermission() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            importFile()
        } else {
            requestPermissions(arrayOf((android.Manifest.permission.READ_EXTERNAL_STORAGE)), PERMISSION_READ_EXTERNAL_STORAGE)
        }
    }

    private fun importFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        requestResult.launch(intent)
    }

    override fun onBackPressed() {
        if (mBinding!!.drawer.isOpen)
        {
            val isNotRoot = drawerFileManager?.goPreviousDir()
            if (isNotRoot == null) {
                super.onBackPressed()
            } else {
                if (isNotRoot) {
                    drawerViewManager.refreshView()
                    if (drawerFileManager!!.isCurrentRoot()) {
                        mBinding!!.toolbar.title = getString(R.string.app_name)
                    } else {
                        mBinding!!.toolbar.title = drawerFileManager!!.mCurrentFolder
                    }
                } else {
                    mBinding!!.drawer.close()
                }
            }
        } else {
            val isNotRoot = fileManager?.goPreviousDir()
            if (isNotRoot == null) {
                super.onBackPressed()
            } else {
                if (isNotRoot) {
                    viewManager.refreshView()
                    if (fileManager!!.isCurrentRoot()) {
                        mBinding!!.toolbar.title = getString(R.string.app_name)
                    } else {
                        mBinding!!.toolbar.title = fileManager!!.mCurrentFolder
                    }
                } else {
                    val currentTime = System.currentTimeMillis()
                    val intervalTime = currentTime - backPressedTime
                    if (intervalTime in 0..TIME_INTERVAL) finish()
                    else {
                        backPressedTime = currentTime
                        Toast.makeText(this@MainActivity, R.string.desc_exit, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importFile()
            } else {
                Toast.makeText(this@MainActivity, R.string.permission_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var viewColumnSaver: ViewColumnSaver

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                viewColumnSaver.landscapeColumns = mBinding!!.seekBar.progress
                mBinding!!.seekBar.progress = viewColumnSaver.portraitColumns
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                viewColumnSaver.portraitColumns = mBinding!!.seekBar.progress
                mBinding!!.seekBar.progress = viewColumnSaver.landscapeColumns
            }
        }
    }

    override fun getWorkManagerConfiguration(): androidx.work.Configuration {
        return if (BuildConfig.DEBUG) {
            androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        } else {
            androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.ERROR)
                .build()
        }
    }
}