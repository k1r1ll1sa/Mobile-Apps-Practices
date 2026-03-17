package ru.Gavrilyuk.Ex1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class PayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pay_activity)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        val goProfileButton = findViewById<ImageButton>(R.id.imageButtonProfile)
        goProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        val goMapButton = findViewById<Button>(R.id.buttonGoToMap)
        goMapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }
    }
}
