package com.smartshare.app.ui.preview

import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.smartshare.app.R

/**
 * PUBLIC_INTERFACE
 * PreviewActivity displays a simple preview for images and text files selected by user.
 */
class PreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getStringExtra("uri")?.let(Uri::parse)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        if (uri != null) {
            val mime = contentResolver.getType(uri) ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString())) ?: "application/octet-stream"

            if (mime.startsWith("image/")) {
                val iv = ImageView(this)
                root.addView(iv)
                Glide.with(this).load(uri).into(iv)
            } else {
                root.addView(TextView(this).apply {
                    text = "Preview not available for $mime.\nURI: $uri"
                })
            }
        } else {
            root.addView(TextView(this).apply { text = "No file to preview." })
        }

        setContentView(root)
    }
}
