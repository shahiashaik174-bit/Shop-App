package com.example.ui.screens.business

import com.example.data.local.entity.Product

enum class BusinessTimePeriod {
    TODAY,
    SEVEN_DAYS,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM
}

data class PeriodMetrics(
    val totalSales: Double = 0.0,
    val profit: Double = 0.0,
    val orderCount: Int = 0,
    val totalExpenses: Double = 0.0
)

data class MoneyNowMetrics(
    val stockValue: Double = 0.0,
    val bakiOwedToMe: Double = 0.0,
    val iOweSuppliers: Double = 0.0
)

data class SalesChartBar(
    val labelBn: String,
    val labelEn: String,
    val amount: Double
)

data class ProductRankingItem(
    val product: Product,
    val quantitySold: Int,
    val revenue: Double
)

data class CategorySalesItem(
    val categoryName: String,
    val totalSales: Double,
    val percentage: Float = 0f
)

data class DataHealthIssue(
    val id: String,
    val title: String,
    val description: String,
    val type: String = "mismatch"
)
