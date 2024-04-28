package com.example.imageemojiconvertor

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class DisplayImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_display_image)

        val emojiBitmap:Bitmap = intent.getParcelableExtra("emoji_bitmap")!!
        val emojiImageView = findViewById<ImageView>(R.id.ImageView1)
        emojiImageView.setImageBitmap(emojiBitmap)
    }
}