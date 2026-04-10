package ru.Gavrilyuk.Ex1.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.Gavrilyuk.Ex1.Product
import ru.Gavrilyuk.Ex1.Restaurant
import ru.Gavrilyuk.Ex1.User
import ru.Gavrilyuk.Ex1.UserDao

@Database(entities = [User::class, Restaurant::class, Product::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun restaurantDao(): RestaurantDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}