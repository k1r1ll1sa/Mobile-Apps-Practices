package ru.Gavrilyuk.Ex1.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.Gavrilyuk.Ex1.Product
import ru.Gavrilyuk.Ex1.Restaurant

@Dao
interface RestaurantDao {
    @Insert
    suspend fun insert(restaurant: Restaurant)

    @Insert
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM restaurants ORDER BY id")
    suspend fun getAllRestaurants(): List<Restaurant>

    @Query("SELECT * FROM products WHERE restaurantId = :restaurantId ORDER BY id")
    suspend fun getProductsByRestaurant(restaurantId: Int): List<Product>

    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun countRestaurants(): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int
}