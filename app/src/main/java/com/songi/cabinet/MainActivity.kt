package com.songi.cabinet

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.songi.cabinet.databinding.ActivityMainBinding
import com.songi.cabinet.file.FileManager


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private var backPressedTime: Long = 0
    private val PERMISSION_READ_EXTERNAL_STORAGE = 1249
    private val TIME_INTERVAL: Long = 2000
    private var fileManager: FileManager? = null
    private lateinit var viewManager: ViewManager

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

        fileManager = FileManager(this)
        viewManager = ViewManager(fileManager!!, this, binding.fileContainer, binding.toolbar)
        viewManager.refreshView()

        mBinding!!.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.app_bar_search -> {}
                R.id.app_bar_switch -> {}
                R.id.app_bar_create_folder -> {
                    val editText = EditText(this).apply {
                        hint = getString(R.string.create_folder_hint)
                    }
                    val alertDialog = AlertDialog.Builder(this).apply {
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

        findViewById<Switch>(R.id.view_hidden_toggle).setOnCheckedChangeListener { buttonView, isChecked ->
            viewManager.viewHidden = isChecked
            viewManager.refreshView()
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        requestResult.launch(intent)
    }

    override fun onBackPressed() {
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
                    Toast.makeText(this@MainActivity, R.string.desc_exit, Toast.LENGTH_SHORT).show()
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
}