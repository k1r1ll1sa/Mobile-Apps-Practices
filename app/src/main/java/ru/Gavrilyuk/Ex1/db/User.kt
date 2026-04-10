package ru.Gavrilyuk.Ex1
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val login: String,
    val email: String,
    val passwordHash: String,
    val cart: String? = null,
    val address: String? = null,
    val cardNumber: String? = null,
    val avatar: String? = null,
    val dontCall: Boolean = false
)