package ru.Gavrilyuk.Ex1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.Gavrilyuk.Ex1.db.AppDatabase
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    private lateinit var imageAvatar: ImageView
    private lateinit var textUsername: TextView
    private lateinit var buttonAddress: Button
    private lateinit var buttonBankCard: Button
    private lateinit var checkBoxDontCall: CheckBox

    private var currentUser: User? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) { decodeUriToBitmap(it) }
                if (bitmap != null) {
                    imageAvatar.setImageBitmap(bitmap)
                    saveAvatarToDb(bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        imageAvatar = findViewById(R.id.imageAvatar)
        textUsername = findViewById(R.id.textUsername)
        buttonAddress = findViewById(R.id.buttonAddress)
        buttonBankCard = findViewById(R.id.buttonBankCard)
        checkBoxDontCall = findViewById(R.id.checkBoxDontCall)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val logoutButton = findViewById<Button>(R.id.buttonLogout)

        val currentLogin = getCurrentUserLogin()
        if (currentLogin == null) {
            navigateToLogin()
            return
        }

        loadUserData(currentLogin)

        backButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }

        logoutButton.setOnClickListener {
            getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().remove(keyLogin).apply()
            navigateToLogin()
        }

        imageAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        textUsername.setOnClickListener {
            showNicknameDialog()
        }

        buttonAddress.setOnClickListener {
            showInputDialog("Изменение адреса", currentUser?.address, false) { newAddress ->
                lifecycleScope.launch {
                    updateUserField { user -> user.copy(address = newAddress) }
                }
            }
        }

        buttonBankCard.setOnClickListener {
            showInputDialog("Изменение карты", currentUser?.cardNumber, true) { newCard ->
                lifecycleScope.launch {
                    updateUserField { user -> user.copy(cardNumber = newCard) }
                }
            }
        }

        checkBoxDontCall.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val user = currentUser
                    if (user != null) {
                        AppDatabase.getDatabase(this@ProfileActivity).userDao().update(user.copy(dontCall = isChecked))
                    }
                }
            }
        }
    }

    private fun loadUserData(login: String) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@ProfileActivity).userDao().findByLogin(login)
            }
            if (user != null) {
                currentUser = user
                updateUI(user)
            } else {
                navigateToLogin()
            }
        }
    }

    private fun updateUI(user: User) {
        textUsername.text = user.login

        if (!user.avatar.isNullOrBlank()) {
            val bytes = Base64.decode(user.avatar, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageAvatar.setImageBitmap(bitmap)
        }

        val addressVal = user.address ?: "Не указан"
        formatButton(buttonAddress, "Адрес", addressVal)

        val cardVal = maskCardNumber(user.cardNumber)
        formatButton(buttonBankCard, "Банковская карта", cardVal)

        checkBoxDontCall.isChecked = user.dontCall
    }

    private fun maskCardNumber(cardNumber: String?): String {
        if (cardNumber.isNullOrBlank()) return "Не привязана"
        if (cardNumber.length < 4) return cardNumber
        val lastFour = cardNumber.takeLast(4)
        return "**** **** **** $lastFour"
    }

    private fun formatButton(button: Button, title: String, value: String) {
        val fullText = "$title\nсейчас: $value"
        val spannable = SpannableString(fullText)
        val titleLength = title.length

        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, titleLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.WHITE), 0, titleLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(RelativeSizeSpan(1.2f), 0, titleLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val valueStart = fullText.indexOf('\n') + 1
        spannable.setSpan(ForegroundColorSpan(Color.GRAY), valueStart, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(RelativeSizeSpan(0.9f), valueStart, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        button.text = spannable
    }

    private suspend fun updateUserField(updateFunction: (User) -> User) {
        val oldUser = currentUser ?: return
        val newUser = updateFunction(oldUser)

        withContext(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(this@ProfileActivity).userDao().update(newUser)
                currentUser = newUser
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext
            }
        }
        currentUser?.let { updateUI(it) }
    }

    private fun showInputDialog(title: String, currentValue: String?, isNumeric: Boolean, onSave: (String) -> Unit) {
        val input = EditText(this)
        input.setText(currentValue?.takeIf { it != "Не указан" && it != "Не привязана" })
        if (isNumeric) input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) onSave(text) else showToast("Введите данные")
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNicknameDialog() {
        val input = EditText(this)
        input.setText(currentUser?.login)
        input.hint = "Новый логин (латиница/цифры)"

        AlertDialog.Builder(this)
            .setTitle("Смена никнейма")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newLogin = input.text.toString().trim()
                if (newLogin.matches(Regex("^[a-zA-Z0-9]{3,20}$"))) {
                    changeNickname(newLogin)
                } else {
                    showToast("Логин: 3-20 символов, только латиница и цифры")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun changeNickname(newLogin: String) {
        lifecycleScope.launch {
            val exists = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@ProfileActivity).userDao().findByLogin(newLogin) != null
            }
            if (exists && newLogin != currentUser?.login) {
                showToast("Этот никнейм занят")
                return@launch
            }

            val oldUser = currentUser ?: return@launch
            val newUser = oldUser.copy(login = newLogin)

            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@ProfileActivity)
                db.userDao().insert(newUser)
                if (newLogin != oldUser.login) {
                    db.userDao().delete(oldUser)
                }
            }

            getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                .putString(keyLogin, newLogin).apply()

            currentUser = newUser
            updateUI(newUser)
            showToast("Никнейм изменен")
        }
    }

    private fun saveAvatarToDb(bitmap: Bitmap) {
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) { bitmapToBase64(bitmap) }
            updateUserField { user -> user.copy(avatar = base64) }
        }
    }

    private fun decodeUriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.setTargetSize(300, 300) }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    }

    private fun getCurrentUserLogin(): String? {
        return getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}