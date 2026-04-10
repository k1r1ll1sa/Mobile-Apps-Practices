package ru.Gavrilyuk.Ex1

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class RestMenuActivity : AppCompatActivity() {
    private lateinit var productsContainer: LinearLayout
    private var currentRestaurantId: Int = 0
    private var currentRestaurantName: String = ""
    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reustaran_activity)

        productsContainer = findViewById(R.id.productsContainer)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val gotoCartButton = findViewById<Button>(R.id.goto_cart_bttn)
        val titleView = findViewById<TextView>(R.id.textTitle)

        currentRestaurantId = intent.getIntExtra("restaurant_id", 0)
        currentRestaurantName = intent.getStringExtra("restaurant_name") ?: "Ресторан"
        titleView.text = currentRestaurantName

        backButton.setOnClickListener { finish() }
        gotoCartButton.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }

        loadProducts()
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@RestMenuActivity)
            val dao = db.restaurantDao()
            if (dao.countProducts() == 0) seedProducts(dao)

            val products = dao.getProductsByRestaurant(currentRestaurantId)
            productsContainer.removeAllViews()
            for (product in products) addProductCard(product)
        }
    }

    private suspend fun seedProducts(dao: ru.Gavrilyuk.Ex1.db.RestaurantDao) {
        val restaurants = dao.getAllRestaurants()
        for (restaurant in restaurants) {
            dao.insertProduct(Product(name = "Картошка фри", price = 150, calories = 300, imageResource = R.drawable.fries, restaurantId = restaurant.id))
            dao.insertProduct(Product(name = "Нагетсы", price = 100, calories = 250, imageResource = R.drawable.nugets, restaurantId = restaurant.id))
        }
    }

    private fun addProductCard(product: Product) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.product_card_template, productsContainer, false) as LinearLayout

        val imageView = cardView.findViewById<ImageView>(R.id.productImage)
        val nameView = cardView.findViewById<TextView>(R.id.productName)
        val priceView = cardView.findViewById<TextView>(R.id.productPrice)
        val minusBtn = cardView.findViewById<Button>(R.id.buttonMinus)
        val plusBtn = cardView.findViewById<Button>(R.id.buttonPlus)
        val quantityView = cardView.findViewById<TextView>(R.id.productQuantity)

        imageView.setImageResource(product.imageResource)
        nameView.text = product.name
        priceView.text = "${product.price} ₽"
        quantityView.text = "0"

        var quantity = 0
        minusBtn.setOnClickListener {
            if (quantity > 0) {
                quantity--
                quantityView.text = quantity.toString()
                updateCart(product, quantity)
            }
        }
        plusBtn.setOnClickListener {
            quantity++
            quantityView.text = quantity.toString()
            updateCart(product, quantity)
        }
        productsContainer.addView(cardView)
    }

    private fun updateCart(product: Product, newQuantity: Int) {
        lifecycleScope.launch {
            val login = getCurrentUserLogin() ?: return@launch
            val db = AppDatabase.getDatabase(this@RestMenuActivity)
            val user = db.userDao().findByLogin(login) ?: return@launch

            var cartJson = user.cart ?: "{}"
            var cartObj = JSONObject(cartJson)

            if (cartObj.has("restaurantId")) {
                if (cartObj.getInt("restaurantId") != currentRestaurantId) {
                    cartObj = JSONObject()
                    cartObj.put("restaurantId", currentRestaurantId)
                    cartObj.put("restaurantName", currentRestaurantName)
                    cartObj.put("items", JSONArray())
                }
            } else {
                cartObj.put("restaurantId", currentRestaurantId)
                cartObj.put("restaurantName", currentRestaurantName)
                cartObj.put("items", JSONArray())
            }

            val itemsArray = if (cartObj.has("items")) cartObj.getJSONArray("items") else JSONArray().also { cartObj.put("items", it) }

            var found = false
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                if (item.getInt("productId") == product.id) {
                    if (newQuantity > 0) {
                        item.put("quantity", newQuantity)
                    } else {
                        itemsArray.remove(i)
                    }
                    found = true
                    break
                }
            }

            if (!found && newQuantity > 0) {
                val newItem = JSONObject().apply {
                    put("productId", product.id)
                    put("productName", product.name)
                    put("price", product.price)
                    put("calories", product.calories)
                    put("quantity", newQuantity)
                }
                itemsArray.put(newItem)
            }

            db.userDao().update(user.copy(cart = cartObj.toString()))
        }
    }

    private fun getCurrentUserLogin(): String? {
        return getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(keyLogin, null)
    }
}