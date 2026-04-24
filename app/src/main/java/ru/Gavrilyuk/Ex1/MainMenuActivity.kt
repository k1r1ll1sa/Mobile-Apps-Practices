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
import ru.Gavrilyuk.Ex1.db.AppDatabase

class MainMenuActivity : AppCompatActivity() {

    private lateinit var restaurantsContainer: LinearLayout
    private lateinit var mapSection: android.view.View
    private val prefsName = "UserPrefs"
    private val keyLogin = "logged_in_login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu_activity)

        restaurantsContainer = findViewById(R.id.restaurantsContainer)

        val profileButton = findViewById<ImageButton>(R.id.imageButtonProfile)
        val cartButton = findViewById<ImageButton>(R.id.imageButtonGoToCart)
        val gotomapButton = findViewById<Button>(R.id.button)

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        cartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        gotomapButton.setOnClickListener {
            startActivity(Intent(this, RestaurantMapActivity::class.java))
        }

        loadUserAvatar(profileButton)
        loadRestaurants()
    }

    private fun loadUserAvatar(profileButton: ImageButton) {
        val login = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keyLogin, null) ?: return

        lifecycleScope.launch {
            val avatarBase64 = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@MainMenuActivity)
                db.userDao().findByLogin(login)?.avatar
            }

            if (!avatarBase64.isNullOrBlank()) {
                val bitmap = decodeBase64ToBitmap(avatarBase64)
                if (bitmap != null) {
                    val roundedBitmap = getRoundedBitmap(bitmap, 50)
                    profileButton.setImageBitmap(roundedBitmap)
                }
            }
        }
    }

    private fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun getRoundedBitmap(bitmap: Bitmap, radius: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun loadRestaurants() {
        lifecycleScope.launch {
            val restaurants: List<Restaurant> = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@MainMenuActivity)
                db.restaurantDao().getAllRestaurants()
            }

            if (restaurants.isEmpty()) {
                insertSampleRestaurants()
                loadRestaurants()
                return@launch
            }

            for (restaurant in restaurants) {
                addRestaurantCard(restaurant)
            }
        }
    }

    private suspend fun insertSampleRestaurants() {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainMenuActivity)
            val dao = db.restaurantDao()

            dao.insert(Restaurant(name = "Rostic's", rating = 4.5f, tags = "burger,chicken", imageResource = R.drawable.rostics))
            dao.insert(Restaurant(name = "Вкусно — и точка", rating = 4.2f, tags = "burger,pizza", imageResource = R.drawable.vkusno_i_tochka))
            dao.insert(Restaurant(name = "Burger King", rating = 4.0f, tags = "burger", imageResource = R.drawable.burger_king))
        }
    }

    private fun addRestaurantCard(restaurant: Restaurant) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.restaurant_card_template, restaurantsContainer, false) as LinearLayout

        val imageView = cardView.findViewById<ImageView>(R.id.restaurantImage)
        val nameView = cardView.findViewById<TextView>(R.id.restaurantName)
        val ratingView = cardView.findViewById<TextView>(R.id.restaurantRating)

        imageView.setImageResource(restaurant.imageResource)
        nameView.text = restaurant.name
        ratingView.text = String.format("%.1f", restaurant.rating)

        cardView.setOnClickListener {
            val intent = Intent(this, RestMenuActivity::class.java)
            intent.putExtra("restaurant_id", restaurant.id)
            intent.putExtra("restaurant_name", restaurant.name)
            startActivity(intent)
        }

        restaurantsContainer.addView(cardView, restaurantsContainer.childCount - 1)
    }
}