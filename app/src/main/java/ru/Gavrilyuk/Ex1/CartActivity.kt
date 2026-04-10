package ru.Gavrilyuk.Ex1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.Gavrilyuk.Ex1.db.AppDatabase

class CartActivity : AppCompatActivity() {
    private lateinit var productsContainer: LinearLayout
    private lateinit var textRestName: TextView
    private lateinit var textRestTime: TextView
    private lateinit var textResultPrice: TextView
    private lateinit var textResultCalories: TextView
    private lateinit var imageButtonProfile: ImageButton
    private lateinit var restaurantImageView: ImageView

    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cart_activity)

        productsContainer = findViewById(R.id.productsContainer)

        textRestName = findViewById(R.id.textViewRestName)
        textRestTime = findViewById(R.id.textViewTimeDeliv)
        textResultPrice = findViewById(R.id.textViewResult)
        textResultCalories = findViewById(R.id.textViewCalories)
        imageButtonProfile = findViewById(R.id.imageButtonProfile)
        restaurantImageView = findViewById(R.id.imageView8)

        imageButtonProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        imageButtonProfile.setBackgroundResource(R.drawable.avatar_rounded_bg)

        loadUserAvatar()
        setupNavigation()
        loadCart()
    }

    private fun setupNavigation() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
        imageButtonProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.buttonGoToPay).setOnClickListener {
            startActivity(Intent(this, PayActivity::class.java))
            finish()
        }
    }

    private fun loadUserAvatar() {
        val login = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null) ?: return
        lifecycleScope.launch {
            val avatarBase64 = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@CartActivity).userDao().findByLogin(login)?.avatar
            }
            if (!avatarBase64.isNullOrBlank()) {
                val bitmap = decodeBase64ToBitmap(avatarBase64)
                if (bitmap != null) imageButtonProfile.setImageBitmap(getRoundedBitmap(bitmap, 50))
            }
        }
    }

    private fun decodeBase64ToBitmap(base64: String): Bitmap? = try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) { null }

    private fun getRoundedBitmap(bitmap: Bitmap, radius: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun loadCart() {
        val login = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null) ?: return
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@CartActivity).userDao().findByLogin(login)
            }
            if (user != null && !user.cart.isNullOrBlank()) {
                parseAndDisplayCart(user.cart!!)
            } else {
                showEmptyCart()
            }
        }
    }

    private fun showEmptyCart() {
        productsContainer.removeAllViews()
        textRestName.text = "Корзина пуста"
        textRestTime.text = ""
        restaurantImageView.setImageResource(R.drawable.rostics)
        updateTotals(0, 0)
    }

    private fun parseAndDisplayCart(cartJson: String) {
        try {
            val cartObj = JSONObject(cartJson)

            val restaurantName = cartObj.optString("restaurantName", "Ресторан")
            textRestName.text = restaurantName
            textRestTime.text = "30-45 мин"
            updateRestaurantImage(restaurantName)

            productsContainer.removeAllViews()

            val itemsArray = cartObj.optJSONArray("items") ?: JSONArray()
            var totalPrice = 0
            var totalCalories = 0

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val productName = item.getString("productName")
                val price = item.getInt("price")
                val quantity = item.getInt("quantity")
                val calories = item.optInt("calories", 0)

                totalPrice += price * quantity
                totalCalories += calories * quantity

                addProductCard(productName, price, quantity, calories) { newQuantity ->
                    updateCartItemQuantity(cartObj, i, newQuantity)
                }
            }
            updateTotals(totalPrice, totalCalories)

        } catch (e: Exception) {
            e.printStackTrace()
            showEmptyCart()
        }
    }

    private fun updateRestaurantImage(name: String) {
        val resId = when {
            name.contains("rostic", ignoreCase = true) || name.contains("ростик", ignoreCase = true) -> R.drawable.rostics
            name.contains("вкусно", ignoreCase = true) -> R.drawable.vkusno_i_tochka
            name.contains("burger", ignoreCase = true) || name.contains("бургер", ignoreCase = true) -> R.drawable.burger_king
            else -> R.drawable.rostics // Дефолтная заглушка
        }
        restaurantImageView.setImageResource(resId)
    }

    private fun addProductCard(
        productName: String, price: Int, initialQuantity: Int, calories: Int,
        onQuantityChange: (Int) -> Unit
    ) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.product_card_template, productsContainer, false) as LinearLayout

        val imageView = cardView.findViewById<ImageView>(R.id.productImage)
        val nameView = cardView.findViewById<TextView>(R.id.productName)
        val priceView = cardView.findViewById<TextView>(R.id.productPrice)
        val minusBtn = cardView.findViewById<Button>(R.id.buttonMinus)
        val plusBtn = cardView.findViewById<Button>(R.id.buttonPlus)
        val quantityView = cardView.findViewById<TextView>(R.id.productQuantity)

        val imageRes = when {
            productName.contains("фри", ignoreCase = true) -> R.drawable.fries
            productName.contains("нагет", ignoreCase = true) -> R.drawable.nugets
            else -> R.drawable.fries
        }
        imageView.setImageResource(imageRes)
        nameView.text = productName
        priceView.text = "$price ₽"
        quantityView.text = initialQuantity.toString()

        var quantity = initialQuantity
        minusBtn.setOnClickListener {
            if (quantity > 0) {
                quantity--
                quantityView.text = quantity.toString()
                onQuantityChange(quantity)
            }
        }
        plusBtn.setOnClickListener {
            quantity++
            quantityView.text = quantity.toString()
            onQuantityChange(quantity)
        }
        productsContainer.addView(cardView)
    }

    private fun updateCartItemQuantity(cartObj: JSONObject, itemIndex: Int, newQuantity: Int) {
        lifecycleScope.launch {
            val login = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null) ?: return@launch
            val itemsArray = cartObj.optJSONArray("items") ?: JSONArray()

            if (newQuantity == 0) {
                itemsArray.remove(itemIndex)
            } else {
                itemsArray.getJSONObject(itemIndex).put("quantity", newQuantity)
            }
            cartObj.put("items", itemsArray)
            saveCartAndReload(cartObj.toString())
        }
    }

    private fun saveCartAndReload(cartJson: String) {
        lifecycleScope.launch {
            val login = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null) ?: return@launch
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@CartActivity)
                val user = db.userDao().findByLogin(login)
                if (user != null) db.userDao().update(user.copy(cart = cartJson))
            }
            loadCart()
        }
    }

    private fun updateTotals(price: Int, calories: Int) {
        textResultPrice.text = "$price ₽"
        textResultCalories.text = "$calories ккал"
    }
}