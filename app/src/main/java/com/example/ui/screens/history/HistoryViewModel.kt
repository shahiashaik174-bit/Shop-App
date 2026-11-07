package com.example.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.HardwareRepository
import com.example.ui.localization.toBengaliNumerals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _timeFilter = MutableStateFlow(TimePeriodFilter.TODAY)
    val timeFilter: StateFlow<TimePeriodFilter> = _timeFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val typeFilter: StateFlow<TransactionTypeFilter> = _typeFilter.asStateFlow()

    private val _isDetailedView = MutableStateFlow(false)
    val isDetailedView: StateFlow<Boolean> = _isDetailedView.asStateFlow()

    private val _showDatePickerDialog = MutableStateFlow(false)
    val showDatePickerDialog: StateFlow<Boolean> = _showDatePickerDialog.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<HistoryItem?>(null)
    val selectedItemForDetail: StateFlow<HistoryItem?> = _selectedItemForDetail.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    private val allHistoryItems: StateFlow<List<HistoryItem>> = combine(
        repository.allSales,
        repository.allPurchases
    ) { sales, purchases ->
        val saleItems = sales.map { sale ->
            val qty = if (sale.totalItemsCount > 0) sale.totalItemsCount else 1
            val unitPrice = if (qty > 0) sale.totalAmount / qty else sale.totalAmount
            HistoryItem(
                id = "sale_${sale.id}",
                title = sale.summaryItems.ifBlank { "পণ্য বিক্রয় (Sale)" },
                type = TransactionTypeFilter.SALES,
                isCredit = sale.isCredit,
                quantity = qty,
                unitPrice = unitPrice,
                totalAmount = sale.totalAmount,
                profit = (sale.totalAmount - sale.totalCost).coerceAtLeast(0.0),
                partyName = sale.customerName ?: "",
                timestamp = sale.timestamp,
                photoKey = "",
                category = "",
                referenceNumber = sale.invoiceNumber
            )
        }
        val purchaseItems = purchases.map { purch ->
            HistoryItem(
                id = "purch_${purch.id}",
                title = purch.productName.ifBlank { "পণ্য ক্রয় (Stock In)" },
                type = TransactionTypeFilter.PURCHASES,
                isCredit = purch.dueAmount > 0,
                quantity = purch.quantity,
                unitPrice = purch.unitBuyingPrice,
                totalAmount = purch.totalCost,
                profit = 0.0,
                partyName = purch.supplierName,
                timestamp = purch.timestamp,
                photoKey = purch.billPhotoUri ?: "",
                category = "",
                referenceNumber = "PUR-${purch.id}"
            )
        }
        (saleItems + purchaseItems).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodSummary: StateFlow<HistoryPeriodSummary> = combine(
        allHistoryItems,
        _timeFilter,
        _customDateRange
    ) { items, timeFilter, customRange ->
        val filtered = filterByTime(items, timeFilter, customRange)
        var bought = 0.0
        var sold = 0.0
        var profit = 0.0
        for (item in filtered) {
            when (item.type) {
                TransactionTypeFilter.SALES -> {
                    sold += item.totalAmount
                    profit += item.profit
                }
                TransactionTypeFilter.PURCHASES -> {
                    bought += item.totalAmount
                }
                else -> {}
            }
        }
        HistoryPeriodSummary(bought = bought, sold = sold, profit = profit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryPeriodSummary())

    val filteredGroupedHistory: StateFlow<List<DayGroupedHistory>> = combine(
        allHistoryItems,
        _timeFilter,
        _typeFilter,
        _customDateRange
    ) { items, timeFilter, typeFilter, customRange ->
        var list = filterByTime(items, timeFilter, customRange)
        if (typeFilter != TransactionTypeFilter.ALL) {
            list = list.filter { it.type == typeFilter }
        }

        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateFormatEn = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)

        val groups = list.groupBy { dateFormatKey.format(Date(it.timestamp)) }
        groups.map { (dateKey, groupItems) ->
            val firstItemDate = Date(groupItems.first().timestamp)
            val cal = Calendar.getInstance()
            val todayStr = dateFormatKey.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yestStr = dateFormatKey.format(cal.time)

            val dateLabelEn: String
            val dateLabelBn: String
            when (dateKey) {
                todayStr -> {
                    dateLabelEn = "Today (${dateFormatEn.format(firstItemDate)})"
                    dateLabelBn = "আজ"
                }
                yestStr -> {
                    dateLabelEn = "Yesterday (${dateFormatEn.format(firstItemDate)})"
                    dateLabelBn = "গতকাল"
                }
                else -> {
                    dateLabelEn = dateFormatEn.format(firstItemDate)
                    val dayNum = SimpleDateFormat("d", Locale.ENGLISH).format(firstItemDate)
                    val month = SimpleDateFormat("MMMM", Locale.ENGLISH).format(firstItemDate)
                    dateLabelBn = "${toBengaliNumerals(dayNum)} $month"
                }
            }

            DayGroupedHistory(
                dateKey = dateKey,
                dateLabelBn = dateLabelBn,
                dateLabelEn = dateLabelEn,
                items = groupItems
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filterByTime(items: List<HistoryItem>, filter: TimePeriodFilter, customRange: Pair<Long, Long>?): List<HistoryItem> {
        val now = Calendar.getInstance()
        val startOfToday: Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return when (filter) {
            TimePeriodFilter.TODAY -> items.filter { it.timestamp >= startOfToday }
            TimePeriodFilter.THIS_WEEK -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                items.filter { it.timestamp >= startOfWeek }
            }
            TimePeriodFilter.THIS_MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                items.filter { it.timestamp >= startOfMonth }
            }
            TimePeriodFilter.ALL -> items
            TimePeriodFilter.CUSTOM -> {
                if (customRange != null) {
                    val (start, end) = customRange
                    items.filter { it.timestamp in start..end }
                } else {
                    items
                }
            }
        }
    }

    fun setTimeFilter(filter: TimePeriodFilter) {
        _timeFilter.value = filter
    }

    fun setTypeFilter(filter: TransactionTypeFilter) {
        _typeFilter.value = filter
    }

    fun toggleDetailedView() {
        _isDetailedView.value = !_isDetailedView.value
    }

    fun openDatePickerDialog() {
        _showDatePickerDialog.value = true
    }

    fun closeDatePickerDialog() {
        _showDatePickerDialog.value = false
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _timeFilter.value = TimePeriodFilter.CUSTOM
        _showDatePickerDialog.value = false
    }

    fun selectItemForDetail(item: HistoryItem?) {
        _selectedItemForDetail.value = item
    }
}
