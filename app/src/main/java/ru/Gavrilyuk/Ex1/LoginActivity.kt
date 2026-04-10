package ru.Gavrilyuk.Ex1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.Gavrilyuk.Ex1.db.AppDatabase

class LoginActivity : AppCompatActivity() {

    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        val etLogin = findViewById<EditText>(R.id.editTextText)
        val etPassword = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnReg = findViewById<Button>(R.id.buttonReg)

        btnLogin.setOnClickListener {
            val login = etLogin.text.toString().trim()
            val password = etPassword.text.toString()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Вход..."

            lifecycleScope.launch {
                val isAuthorized = authorizeUser(login, password)
                btnLogin.isEnabled = true
                btnLogin.text = "Войти"

                if (isAuthorized) {
                    getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                        .putString(keyLogin, login).apply()
                    Toast.makeText(this@LoginActivity, "Добро пожаловать!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainMenuActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReg.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private suspend fun authorizeUser(login: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@LoginActivity)
                val user = db.userDao().findByLogin(login)
                if (user != null) {
                    val inputHash = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
                    return@withContext user.passwordHash == inputHash
                }
                return@withContext false
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }
}