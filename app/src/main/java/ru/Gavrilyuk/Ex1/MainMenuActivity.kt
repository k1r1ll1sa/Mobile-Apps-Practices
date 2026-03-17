package ru.Gavrilyuk.Ex1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu_activity)

        val goProfileButton = findViewById<ImageButton>(R.id.imageButtonProfile)
        goProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        val goCartButton = findViewById<ImageButton>(R.id.imageButtonGoToCart)
        goCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }
    }
}