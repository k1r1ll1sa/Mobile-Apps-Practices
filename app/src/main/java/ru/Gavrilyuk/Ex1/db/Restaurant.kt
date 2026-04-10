package ru.Gavrilyuk.Ex1

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rating: Float,
    val tags: String,
    val imageResource: Int
)