package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Product
import com.example.data.local.entity.SaleOrder
import com.example.data.repository.HardwareRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.toBengaliNumerals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HardwareRepository(
        database.productDao(),
        database.saleOrderDao(),
        database.categoryDao(),
        database.purchaseDao(),
        database.customerDao(),
        database.supplierDao(),
        database.supplierPaymentDao(),
        database.customerPaymentDao(),
        database.expenseDao()
    )

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSales: StateFlow<List<SaleOrder>> = repository.recentSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerBaki: StateFlow<Double> = repository.totalUnpaidBaki
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayMetrics: StateFlow<HomeSummaryMetrics> = repository.getTodaySales()
        .map { sales ->
            val totalSales = sales.sumOf { it.totalAmount }
            val totalProfit = sales.sumOf { it.totalAmount - it.totalCost }
            val ordersCount = sales.size
            val itemsSold = sales.sumOf { it.totalItemsCount }
            HomeSummaryMetrics(
                totalSales = totalSales,
                totalProfit = totalProfit,
                ordersCount = ordersCount,
                itemsSoldCount = itemsSold
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSummaryMetrics())

    fun getFormattedDate(currentLanguage: AppLanguage): String {
        val now = Date()
        return if (currentLanguage == AppLanguage.BANGLA) {
            val enDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(now)
            val dayNum = SimpleDateFormat("d", Locale.ENGLISH).format(now)
            val enMonth = SimpleDateFormat("MMMM", Locale.ENGLISH).format(now)
            val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(now)

            val bnDay = when (enDay) {
                "Saturday" -> "শনিবার"
                "Sunday" -> "রবিবার"
                "Monday" -> "সোমবার"
                "Tuesday" -> "মঙ্গলবার"
                "Wednesday" -> "বুধবার"
                "Thursday" -> "বৃহস্পতিবার"
                "Friday" -> "শুক্রবার"
                else -> enDay
            }
            val bnMonth = when (enMonth) {
                "January" -> "জানুয়ারি"
                "February" -> "ফেব্রুয়ারি"
                "March" -> "মার্চ"
                "April" -> "এপ্রিল"
                "May" -> "মে"
                "June" -> "জুন"
                "July" -> "জুলাই"
                "August" -> "আগস্ট"
                "September" -> "সেপ্টেম্বর"
                "October" -> "অক্টোবর"
                "November" -> "নভেম্বর"
                "December" -> "ডিসেম্বর"
                else -> enMonth
            }
            "$bnDay, ${toBengaliNumerals(dayNum)} $bnMonth ${toBengaliNumerals(year)}"
        } else {
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(now)
        }
    }
}
