package com.songi.cabinet

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.songi.cabinet.databinding.ActivityMainBinding
import com.songi.cabinet.file.FileManager

class ImportActivity : AppCompatActivity() {
    private val TAG = "ImportActivity"

    /* TODO: NOT WORKING AT ALL! */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        val fileManager = FileManager(this)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { string ->
                        findViewById<TextView>(R.id.textView).text = string
                    }
                } else {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                        findViewById<TextView>(R.id.textView).text = intent.type
                        fileManager.importFile(uri)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
                    findViewById<TextView>(R.id.textView).text = intent.type
                    for( i in it ) {
                        fileManager.importFile(i as Uri)
                    }
                }
            }
            else -> {
                Toast.makeText(this, "Error occurred in process of import.", Toast.LENGTH_SHORT).show()
            }
        }

    }
}