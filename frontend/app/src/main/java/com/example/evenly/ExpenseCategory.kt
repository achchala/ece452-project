package com.example.evenly

import androidx.compose.ui.graphics.Color

enum class ExpenseCategory(
    val displayName: String,
    val color: Color,
    val description: String = ""
) {
    FOOD_DRINKS(
        displayName = "Food & Drinks",
        color = Color(0xFF4CAF50),
        description = "Restaurants, groceries, coffee, etc."
    ),
    TRANSPORT(
        displayName = "Transport",
        color = Color(0xFF2196F3),
        description = "Gas, parking, public transport, rideshare"
    ),
    ENTERTAINMENT(
        displayName = "Entertainment",
        color = Color(0xFF9C27B0),
        description = "Movies, concerts, games, activities"
    ),
    SHOPPING(
        displayName = "Shopping",
        color = Color(0xFFFF9800),
        description = "Clothes, electronics, gifts"
    ),
    TRAVEL(
        displayName = "Travel",
        color = Color(0xFF00BCD4),
        description = "Flights, hotels, vacation expenses"
    ),
    UTILITIES(
        displayName = "Utilities",
        color = Color(0xFFFF5722),
        description = "Electricity, water, internet, phone"
    ),
    HEALTH(
        displayName = "Health",
        color = Color(0xFFE91E63),
        description = "Medical expenses, pharmacy, fitness"
    ),
    EDUCATION(
        displayName = "Education",
        color = Color(0xFF3F51B5),
        description = "Books, courses, tuition, supplies"
    ),
    HOME(
        displayName = "Home",
        color = Color(0xFF795548),
        description = "Rent, furniture, repairs, maintenance"
    ),
    WORK(
        displayName = "Work",
        color = Color(0xFF607D8B),
        description = "Business expenses, office supplies"
    ),
    OTHER(
        displayName = "Other",
        color = Color(0xFF9E9E9E),
        description = "Miscellaneous expenses"
    );

    companion object {
        fun fromString(value: String?): ExpenseCategory {
            return values().find { it.name == value } ?: OTHER
        }
    }
} 