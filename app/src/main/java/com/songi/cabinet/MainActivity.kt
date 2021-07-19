package com.songi.cabinet

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.DragEvent
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.songi.cabinet.databinding.ActivityMainBinding
import com.songi.cabinet.file.FileManager
import java.lang.NullPointerException


class MainActivity : AppCompatActivity(), androidx.work.Configuration.Provider {
    private val TAG = "MainActivity"
    private var backPressedTime: Long = 0
    private val PERMISSION_READ_EXTERNAL_STORAGE = 1249
    private val TIME_INTERVAL: Long = 2000
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

        fileManager = FileManager(this, "${filesDir.absolutePath}/Cabinet_user_folder", this)
        viewManager = ViewManager(fileManager!!, this, binding.contentObjectContainer, binding.toolbar, isDrawer = false)
        viewManager.refreshView()
        drawerFileManager = FileManager(this, "${filesDir.absolutePath}/Cabinet_temp_folder", this)

        importAction()

        drawerViewManager = ViewManager(drawerFileManager!!, this, binding.drawerObjectContainer, binding.toolbar, isDrawer = true)
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
            contentScrollable.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        Log.d(TAG, "fileContainer: DROP")
                        fileManager!!.moveFile(event.localState as Array<String>)
                        viewManager.refreshView()
                    }
                }
                return@setOnDragListener true
            }

            seekBar.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    viewManager.columnCount = seekBar.progress
                    viewManager.refreshView()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
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
                        }, 1000)
                    }
                    DragEvent.ACTION_DROP -> {
                        Log.d(TAG, "drawerScrollable: DROP")
                        drawerFileManager!!.moveFile(event.localState as Array<String>)
                        drawerViewManager.refreshView()
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        try {
                            drawerScrollable.handler.removeCallbacksAndMessages(null)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                        drawerViewManager.refreshView()
                    }
                }
                return@setOnDragListener true
            }
            refreshLayout.setOnRefreshListener {
                viewManager.refreshView()
                refreshLayout.isRefreshing = false
            }
            drawerRefreshLayout.setOnRefreshListener {
                drawerViewManager.refreshView()
                drawerRefreshLayout.isRefreshing = false
            }
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
                        drawerFileManager!!.importFile(uri)
                    }
                }
                mBinding!!.drawer.open()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
                    for( i in it ) {
                        drawerFileManager!!.importFile(i as Uri)
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

    private var portraitColumns = 3
    private var landscapeColumns = 6
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                landscapeColumns = mBinding!!.seekBar.progress
                mBinding!!.seekBar.progress = portraitColumns
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                portraitColumns = mBinding!!.seekBar.progress
                mBinding!!.seekBar.progress = landscapeColumns
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