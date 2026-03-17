package ru.Gavrilyuk.Ex1
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.graphics.Typeface
import android.text.Spannable
import android.widget.Button
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        val button_address = findViewById<Button>(R.id.buttonAddress)
        val fullText_adress = "Изменить адрес\nсейчас: address..."
        val spannable_address = SpannableString(fullText_adress)

        // Стиль для первой строки (Изменить адрес)
        spannable_address.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            15,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_address.setSpan(
            ForegroundColorSpan(Color.WHITE),
            0,
            15,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_address.setSpan(
            RelativeSizeSpan(1.2f),
            0,
            15,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Стиль для второй строки (сейчас: adress...)
        spannable_address.setSpan(
            ForegroundColorSpan(Color.GRAY),
            15,
            fullText_adress.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_address.setSpan(
            RelativeSizeSpan(0.9f),
            16,
            fullText_adress.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        button_address.text = spannable_address

        val button_card = findViewById<Button>(R.id.buttonBankCard)
        val fullText_card = "Изменить карту\nсейчас: 1234567890..."
        val spannable_card = SpannableString(fullText_card)

        // Стиль для первой строки (Изменить карту)
        spannable_card.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            14,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_card.setSpan(
            ForegroundColorSpan(Color.WHITE),
            0,
            14,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_card.setSpan(
            RelativeSizeSpan(1.2f),
            0,
            14,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Стиль для второй строки (сейчас: 1234567890...)
        spannable_card.setSpan(
            ForegroundColorSpan(Color.GRAY),
            14,
            fullText_card.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable_card.setSpan(
            RelativeSizeSpan(0.9f),
            15,
            fullText_card.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        button_card.text = spannable_card

        val logoutButton = findViewById<Button>(R.id.buttonLogout)
        logoutButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }
}