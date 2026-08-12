package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val expiryDate: String,
    val category: String = "عام",
    val lastUpdated: Long = System.currentTimeMillis()
)
