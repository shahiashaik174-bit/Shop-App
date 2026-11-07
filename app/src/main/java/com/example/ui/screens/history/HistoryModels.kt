package com.example.ui.screens.history

enum class TimePeriodFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL,
    CUSTOM
}

enum class TransactionTypeFilter {
    ALL,
    SALES,
    PURCHASES
}

data class HistoryPeriodSummary(
    val bought: Double = 0.0,
    val sold: Double = 0.0,
    val profit: Double = 0.0
)

data class HistoryItem(
    val id: String,
    val title: String,
    val type: TransactionTypeFilter,
    val isCredit: Boolean,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val profit: Double,
    val partyName: String,
    val timestamp: Long,
    val photoKey: String = "",
    val category: String = "",
    val referenceNumber: String = ""
)

data class DayGroupedHistory(
    val dateKey: String,
    val dateLabelBn: String,
    val dateLabelEn: String,
    val items: List<HistoryItem>
)
