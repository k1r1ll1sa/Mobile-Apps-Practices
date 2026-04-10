package ru.Gavrilyuk.Ex1

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(
        entity = Restaurant::class,
        parentColumns = ["id"],
        childColumns = ["restaurantId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Int,
    val calories: Int,
    val imageResource: Int,
    val restaurantId: Int
)