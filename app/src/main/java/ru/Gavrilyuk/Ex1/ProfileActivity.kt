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
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.Gavrilyuk.Ex1.db.AppDatabase
import java.io.ByteArrayOutputStream
import android.util.Log

class ProfileActivity : AppCompatActivity() {

    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    private lateinit var imageAvatar: ImageView
    private lateinit var textUsername: TextView
    private lateinit var buttonAddress: Button
    private lateinit var buttonBankCard: Button
    private lateinit var checkBoxDontCall: CheckBox

    private var addressSearchJob: Job? = null

    private var lastNominatimRequestTime = 0L

    private lateinit var addressAdapter: ArrayAdapter<String>

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
            showAddressAutocompleteDialog()
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
                        AppDatabase.getDatabase(this@ProfileActivity).userDao()
                            .update(user.copy(dontCall = isChecked))
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

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            titleLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.WHITE),
            0,
            titleLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            RelativeSizeSpan(1.2f),
            0,
            titleLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val valueStart = fullText.indexOf('\n') + 1
        spannable.setSpan(
            ForegroundColorSpan(Color.GRAY),
            valueStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            RelativeSizeSpan(0.9f),
            valueStart,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

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

    private fun showInputDialog(
        title: String,
        currentValue: String?,
        isNumeric: Boolean,
        onSave: (String) -> Unit
    ) {
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
                AppDatabase.getDatabase(this@ProfileActivity).userDao()
                    .findByLogin(newLogin) != null
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
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(
                        300,
                        300
                    )
                }
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

    private fun showAddressAutocompleteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_address_autocomplete, null)
        val autoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteAddress)
        autoComplete.setText(currentUser?.address?.takeIf { it != "Не указан" })

        addressAdapter = UnfilteredArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        autoComplete.setAdapter(addressAdapter)
        autoComplete.threshold = 2

        val dialog = AlertDialog.Builder(this)
            .setTitle("Введите адрес доставки")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val selected = autoComplete.text.toString().trim()
                if (selected.isNotEmpty()) {
                    lifecycleScope.launch { updateUserField { it.copy(address = selected) } }
                } else {
                    showToast("Введите адрес")
                }
            }
            .setNegativeButton("Отмена", null)
            .setOnDismissListener { addressSearchJob?.cancel() }
            .create()

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                addressSearchJob?.cancel()

                if (query.length < 2) {
                    addressAdapter.clear()
                    addressAdapter.notifyDataSetChanged()
                    autoComplete.dismissDropDown()
                    return
                }

                addressSearchJob = lifecycleScope.launch {
                    try {
                        delay(800)
                        val now = System.currentTimeMillis()
                        val diff = now - lastNominatimRequestTime
                        if (diff < 1100) delay(1100 - diff)
                        lastNominatimRequestTime = System.currentTimeMillis()

                        val suggestions = fetchNominatimSuggestions(query)

                        withContext(Dispatchers.Main) {
                            autoComplete.dismissDropDown()
                            addressAdapter.clear()
                            addressAdapter.addAll(suggestions)
                            addressAdapter.notifyDataSetChanged()

                            if (suggestions.isNotEmpty()) {
                                autoComplete.post { autoComplete.showDropDown() }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { showToast("Ошибка поиска адреса") }
                    }
                }
            }
        })

        autoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            autoComplete.setText(selected)
            autoComplete.dismissDropDown()
        }

        dialog.show()
    }



    private suspend fun fetchNominatimSuggestions(query: String): List<String> =
        withContext(Dispatchers.IO) {
            val tag = "NominatimDebug"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

            val viewbox = "91.0,55.0,94.0,57.0"
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/search?" +
                        "format=json&q=$encodedQuery&limit=10&addressdetails=1&countrycodes=ru&viewbox=$viewbox"
            )

            Log.d(tag, "$url")

            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "DeliveryApp/1.0 (k1r1ll1sa@yandex.ru)")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            return@withContext try {
                Log.d(tag, "Код ответа: ${conn.responseCode}")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(tag, "JSON: $response")

                    val jsonArray = org.json.JSONArray(response)
                    val suggestions = mutableListOf<String>()
                    var hasStreetResults = false

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val address = item.optJSONObject("address")
                        val type = item.optString("type", "")
                        val displayName = item.optString("display_name", "")
                        val name = item.optString("name", "")

                        val hasStreet = address?.has("road") == true ||
                                address?.has("street") == true ||
                                address?.has("house_number") == true ||
                                type in listOf("house", "residential", "street", "road", "footway")

                        if (hasStreet) {
                            hasStreetResults = true
                            val formatted = formatAddressForDisplay(displayName, address)
                            suggestions.add(formatted)
                            if (suggestions.size >= 20) break
                        }
                    }

                    if (!hasStreetResults && suggestions.isEmpty()) {
                        for (i in 0 until minOf(jsonArray.length(), 3)) {
                            val item = jsonArray.getJSONObject(i)
                            val type = item.optString("type", "")
                            val name = item.optString("name", "")
                            val displayName = item.optString("display_name", "")

                            if (type in listOf("city", "town", "village", "suburb") && name.isNotBlank()) {
                                suggestions.add("$name")
                            }
                        }
                    }

                    Log.d(tag, "Распарсено адресов: ${suggestions.size}")
                    suggestions
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    Log.e(tag, "HTTP ${conn.responseCode}: $error")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(tag, "Err: ${e.javaClass.simpleName} | ${e.message}")
                e.printStackTrace()
                emptyList()
            } finally {
                conn.disconnect()
            }
        }

    private fun getFirstNonEmpty(address: org.json.JSONObject?, vararg keys: String): String {
        for (key in keys) {
            val value = address?.optString(key)
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun formatAddressForDisplay(fullName: String, address: org.json.JSONObject?): String {
        val street = getFirstNonEmpty(address, "road", "street", "pedestrian", "footway")
        val house = address?.optString("house_number") ?: ""

        val settlement = getFirstNonEmpty(address, "city", "town", "village", "hamlet", "suburb", "neighbourhood")

        val state = address?.optString("state") ?: ""
        val country = address?.optString("country") ?: ""

        return when {
            street.isNotBlank() && house.isNotBlank() -> {
                if (settlement.isNotBlank()) {
                    "$settlement, $street $house"
                } else if (state.isNotBlank()) {
                    "$state, $street $house"
                } else {
                    "$street $house"
                }
            }
            street.isNotBlank() -> {
                if (settlement.isNotBlank()) {
                    "$settlement, $street"
                } else if (state.isNotBlank()) {
                    "$state, $street"
                } else {
                    street
                }
            }
            settlement.isNotBlank() -> {
                if (state.isNotBlank()) {
                    "$settlement, $state"
                } else {
                    settlement
                }
            }
            else -> {
                fullName.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).joinToString(", ")
            }
        }
    }

    private class UnfilteredArrayAdapter(
        context: Context,
        @androidx.annotation.LayoutRes resource: Int,
        private val items: List<String>
    ) : android.widget.ArrayAdapter<String>(context, resource, items) {

        override fun getFilter(): android.widget.Filter {
            return object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): android.widget.Filter.FilterResults {
                    val results = android.widget.Filter.FilterResults()
                    results.values = ArrayList(items)
                    results.count = items.size
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: android.widget.Filter.FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }
}


















