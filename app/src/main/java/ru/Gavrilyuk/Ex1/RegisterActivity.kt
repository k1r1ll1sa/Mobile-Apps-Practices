package ru.Gavrilyuk.Ex1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.Gavrilyuk.Ex1.db.AppDatabase
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {

    private lateinit var avatarImageView: ImageView
    private var selectedAvatarUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAvatarUri = it
            loadAvatarIntoImageView(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        avatarImageView = findViewById(R.id.AvatarImage)
        val etLogin = findViewById<EditText>(R.id.editTextText)
        val etEmail = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val etPassword = findViewById<EditText>(R.id.editTextPassword)
        val etRepeat = findViewById<EditText>(R.id.editTextPassword0)
        val btnRegister = findViewById<Button>(R.id.buttonRegister)

        avatarImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRegister.setOnClickListener {
            val login = etLogin.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val repeat = etRepeat.text.toString()

            when {
                login.isEmpty() || email.isEmpty() || password.isEmpty() || repeat.isEmpty() ->
                    showToast("Заполните все поля")
                !isValidLogin(login) ->
                    showToast("Логин: 3-20 символов, только латиница и цифры")
                !isValidEmail(email) ->
                    showToast("Введите корректный email")
                !isValidPassword(password) ->
                    showToast("Пароль: мин. 8 символов + спец. символ")
                password != repeat ->
                    showToast("Пароли не совпадают")
                else -> {
                    btnRegister.isEnabled = false
                    btnRegister.text = "Регистрация..."

                    lifecycleScope.launch {
                        val result = registerUser(login, email, password)
                        btnRegister.isEnabled = true
                        btnRegister.text = "Зарегистрироваться"

                        when (result) {
                            RegistrationResult.Success -> {
                                getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                                    .putString("logged_in_login", login).apply()
                                showToast("Регистрация успешна")
                                startActivity(Intent(this@RegisterActivity, MainMenuActivity::class.java))
                                finish()
                            }
                            RegistrationResult.LoginTaken -> showToast("Пользователь с таким логином уже существует")
                            RegistrationResult.DatabaseError -> showToast("Ошибка базы данных")
                            RegistrationResult.UnknownError -> showToast("Неизвестная ошибка")
                        }
                    }
                }
            }
        }
    }

    private fun loadAvatarIntoImageView(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.setTargetSize(300, 300) }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            avatarImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            showToast("Не удалось загрузить изображение")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun isValidLogin(login: String): Boolean = login.matches(Regex("^[a-zA-Z0-9]{3,20}$"))
    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        return password.any { !it.isLetterOrDigit() }
    }

    private suspend fun registerUser(login: String, email: String, password: String): RegistrationResult {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val userDao = db.userDao()
                if (userDao.findByLogin(login) != null) {
                    return@withContext RegistrationResult.LoginTaken
                }
                val passwordHash = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
                val avatarBase64 = selectedAvatarUri?.let { uri ->
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.setTargetSize(300, 300) }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    bitmapToBase64(bitmap)
                }
                val user = User(
                    login = login,
                    email = email,
                    passwordHash = passwordHash,
                    cart = null,
                    address = null,
                    cardNumber = null,
                    avatar = avatarBase64
                )
                userDao.insert(user)
                RegistrationResult.Success
            } catch (e: Exception) {
                e.printStackTrace()
                RegistrationResult.UnknownError
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

enum class RegistrationResult {
    Success, LoginTaken, DatabaseError, UnknownError
}