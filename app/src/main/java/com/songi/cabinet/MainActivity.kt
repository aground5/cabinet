package com.songi.cabinet

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import com.songi.cabinet.databinding.ActivityMainBinding
import com.songi.cabinet.file.FileManager
import java.io.File


class MainActivity : AppCompatActivity() {
    private var TAG = "MainActivity"
    private var backPressedTime: Long = 0
    private val PERMISSION_READ_EXTERNAL_STORAGE = 1249
    private val TIME_INTERVAL: Long = 2000
    private var fileManager: FileManager? = null
    private var viewManager: ViewManager? = null

    private var mBinding : ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fileManager = FileManager(this)
        viewManager = ViewManager(fileManager!!, this, binding.fileContainer)
        viewManager!!.refreshView()

        binding.createFile.setOnClickListener {
            fileManager!!.createFile()
            viewManager!!.refreshView()
        }
        binding.createFolder.setOnClickListener {
            fileManager!!.createFolder()
            viewManager!!.refreshView()
        }
        binding.importFile.setOnClickListener {
            checkPermission()
        }
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
        var properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = File("${DialogConfigs.DEFAULT_DIR}/sdcard");
        properties.error_dir = File("${DialogConfigs.DEFAULT_DIR}/sdcard");
        properties.offset = File("${DialogConfigs.DEFAULT_DIR}/sdcard");
        properties.extensions = null;
        properties.show_hidden_files = false;

        val dialog = FilePickerDialog(this@MainActivity, properties)
        dialog.setTitle("Select a File")
        dialog.setDialogSelectionListener {
            for (file in it) {
                fileManager!!.saveFile(file)
            }
            viewManager!!.refreshView()
        }
        dialog.show()
    }

    override fun onBackPressed() {
        val isNotRoot = fileManager?.goPreviousDir()
        if (isNotRoot == null) {
            super.onBackPressed()
        } else {
            if (isNotRoot) {
                viewManager!!.refreshView()
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