package ru.Gavrilyuk.Ex1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cart_activity)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
        val goProfileButton = findViewById<ImageButton>(R.id.imageButtonProfile)
        goProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        val goPayButton = findViewById<Button>(R.id.buttonGoToPay)
        goPayButton.setOnClickListener {
            startActivity(Intent(this, PayActivity::class.java))
            finish()
        }
    }
}
