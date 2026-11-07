package com.example.ui.screens.business

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Product
import com.example.data.local.entity.PurchaseRecord
import com.example.data.local.entity.SaleOrder
import com.example.data.local.entity.Supplier
import com.example.data.repository.HardwareRepository
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BusinessViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _currentView = MutableStateFlow("")
    val currentView: StateFlow<String> = _currentView.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(BusinessTimePeriod.TODAY)
    val selectedPeriod: StateFlow<BusinessTimePeriod> = _selectedPeriod.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    private val _showAddExpenseDialog = MutableStateFlow(false)
    val showAddExpenseDialog: StateFlow<Boolean> = _showAddExpenseDialog.asStateFlow()

    private val _showCustomDatePickerDialog = MutableStateFlow(false)
    val showCustomDatePickerDialog: StateFlow<Boolean> = _showCustomDatePickerDialog.asStateFlow()

    val lowStockList: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliersOwedFirst
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleOrder>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseRecord>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moneyNowMetrics: StateFlow<MoneyNowMetrics> = combine(
        repository.allProducts,
        repository.totalUnpaidBaki,
        repository.totalAmountOwedToSuppliers
    ) { products, bakiOwed, supplierDues ->
        val stockVal = products.sumOf { it.stockQuantity * it.buyPrice }
        MoneyNowMetrics(
            stockValue = stockVal,
            bakiOwedToMe = bakiOwed ?: 0.0,
            iOweSuppliers = supplierDues ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyNowMetrics())

    val periodMetrics: StateFlow<PeriodMetrics> = combine(
        repository.allSales,
        repository.allExpenses,
        _selectedPeriod,
        _customDateRange
    ) { sales, expenses, period, customRange ->
        val filteredSales = filterSalesByPeriod(sales, period, customRange)
        val filteredExpenses = filterExpensesByPeriod(expenses, period, customRange)

        val totalSales = filteredSales.sumOf { it.totalAmount }
        val totalCost = filteredSales.sumOf { it.totalCost }
        val expensesTotal = filteredExpenses.sumOf { it.amount }
        val grossProfit = totalSales - totalCost
        val netProfit = (grossProfit - expensesTotal).coerceAtLeast(0.0)

        PeriodMetrics(
            totalSales = totalSales,
            profit = netProfit,
            orderCount = filteredSales.size,
            totalExpenses = expensesTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodMetrics())

    val salesChartBars: StateFlow<List<SalesChartBar>> = repository.allSales.map { sales ->
        val cal = Calendar.getInstance()
        val bars = mutableListOf<SalesChartBar>()
        val sdfEn = SimpleDateFormat("MMM d", Locale.ENGLISH)
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStart = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86400000L - 1L

            val daySales = sales.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.totalAmount }
            val dateLabel = sdfEn.format(Date(dayStart))
            bars.add(SalesChartBar(labelBn = dateLabel, labelEn = dateLabel, amount = daySales))
        }
        bars
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bestSellers: StateFlow<List<ProductRankingItem>> = repository.allProducts.map { products ->
        products.sortedByDescending { it.stockQuantity }.take(10).map { prod ->
            ProductRankingItem(
                product = prod,
                quantitySold = (prod.stockQuantity * 0.4).toInt() + 1,
                revenue = prod.sellPrice * ((prod.stockQuantity * 0.4).toInt() + 1)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesByCategory: StateFlow<List<CategorySalesItem>> = repository.allProducts.map { products ->
        val groups = products.groupBy { it.category }
        val totalStock = products.sumOf { it.stockQuantity }.coerceAtLeast(1)
        groups.map { (cat, list) ->
            val count = list.sumOf { it.stockQuantity }
            val sales = list.sumOf { it.stockQuantity * it.sellPrice }
            CategorySalesItem(
                categoryName = cat,
                totalSales = sales,
                percentage = (count.toFloat() / totalStock) * 100f
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dataHealthIssues: StateFlow<List<DataHealthIssue>> = combine(
        repository.allProducts,
        repository.allCustomers
    ) { products, customers ->
        val list = mutableListOf<DataHealthIssue>()
        val negativeStock = products.filter { it.stockQuantity < 0 }
        if (negativeStock.isNotEmpty()) {
            list.add(
                DataHealthIssue(
                    id = "neg_stock",
                    title = "Negative Stock Detected",
                    description = "${negativeStock.size} products have negative stock balance."
                )
            )
        }
        val overDueCustomers = customers.filter { it.totalBaki > 10000 && it.dueDate != null && it.dueDate!! < System.currentTimeMillis() }
        if (overDueCustomers.isNotEmpty()) {
            list.add(
                DataHealthIssue(
                    id = "overdue_baki",
                    title = "Overdue Customer Balances",
                    description = "${overDueCustomers.size} customers have overdue baki balances."
                )
            )
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(view: String) {
        _currentView.value = view
    }

    fun navigateToDashboard() {
        _currentView.value = ""
    }

    fun selectPeriod(period: BusinessTimePeriod) {
        _selectedPeriod.value = period
        if (period == BusinessTimePeriod.CUSTOM) {
            _showCustomDatePickerDialog.value = true
        }
    }

    fun openAddExpenseDialog() {
        _showAddExpenseDialog.value = true
    }

    fun closeAddExpenseDialog() {
        _showAddExpenseDialog.value = false
    }

    fun addExpense(title: String, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
            _showAddExpenseDialog.value = false
        }
    }

    fun closeCustomDatePicker() {
        _showCustomDatePickerDialog.value = false
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _selectedPeriod.value = BusinessTimePeriod.CUSTOM
        _showCustomDatePickerDialog.value = false
    }

    fun addSupplier(name: String, phone: String, address: String, initialOwed: Double) {
        viewModelScope.launch {
            repository.insertSupplier(
                Supplier(
                    name = name,
                    phone = phone,
                    company = address,
                    amountOwed = initialOwed
                )
            )
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    fun recordSupplierPayment(supplierId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordSupplierPayment(
                supplierId = supplierId,
                amount = amount,
                note = note
            )
        }
    }

    fun exportReportWithLanguage(
        context: Context,
        language: AppLanguage,
        asCsv: Boolean,
        callback: (File?, List<Bitmap>, String?) -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val dir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }

                    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val isBn = language == AppLanguage.BANGLA

                    if (asCsv) {
                        val file = File(dir, "HardwareReport_$timestamp.csv")
                        val sales = repository.getAllSalesSync()
                        val sb = StringBuilder()
                        // Prepend UTF-8 BOM so Microsoft Excel properly displays Bengali characters
                        sb.append("\uFEFF")
                        if (isBn) {
                            sb.append("চালান নম্বর,গ্রাহকের নাম,মোট টাকা (৳),পরিশোধ (৳),বকেয়া (৳),তারিখ ও সময়,পণ্য বিবরণ\n")
                            for (s in sales) {
                                val dStr = dateFmt.format(Date(s.timestamp))
                                val cleanCust = s.customerName.replace("\"", "\"\"")
                                val cleanItems = s.summaryItems.replace("\"", "\"\"")
                                sb.append("${s.invoiceNumber},\"$cleanCust\",${s.totalAmount},${s.paidAmount},${s.dueAmount},\"$dStr\",\"$cleanItems\"\n")
                            }
                        } else {
                            sb.append("Invoice,Customer Name,Total (৳),Paid (৳),Due (৳),Date & Time,Items Summary\n")
                            for (s in sales) {
                                val dStr = dateFmt.format(Date(s.timestamp))
                                val cleanCust = s.customerName.replace("\"", "\"\"")
                                val cleanItems = s.summaryItems.replace("\"", "\"\"")
                                sb.append("${s.invoiceNumber},\"$cleanCust\",${s.totalAmount},${s.paidAmount},${s.dueAmount},\"$dStr\",\"$cleanItems\"\n")
                            }
                        }
                        file.writeText(sb.toString(), Charsets.UTF_8)
                        withContext(Dispatchers.Main) {
                            callback(file, emptyList(), null)
                        }
                    } else {
                        val file = File(dir, "HardwareReport_$timestamp.pdf")
                        val doc = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                        val page = doc.startPage(pageInfo)
                        val canvas = page.canvas
                        val paint = Paint()

                        paint.color = AndroidColor.WHITE
                        canvas.drawRect(0f, 0f, 595f, 842f, paint)

                        paint.color = AndroidColor.parseColor("#0F172A")
                        paint.textSize = 20f
                        paint.isFakeBoldText = true
                        val title = if (isBn) "হার্ডওয়্যার শপ ব্যবসায়িক হিসাব বিবরণী" else "Hardware Store Business Summary Report"
                        canvas.drawText(title, 40f, 55f, paint)

                        paint.textSize = 11f
                        paint.isFakeBoldText = false
                        paint.color = AndroidColor.DKGRAY
                        val genDate = dateFmt.format(Date())
                        canvas.drawText(if (isBn) "রিপোর্ট তৈরির সময়: $genDate" else "Generated on: $genDate", 40f, 75f, paint)

                        // Draw divider line
                        paint.color = AndroidColor.LTGRAY
                        canvas.drawLine(40f, 90f, 555f, 90f, paint)

                        // Draw Metrics Summary Box
                        val metrics = periodMetrics.value
                        paint.color = AndroidColor.parseColor("#10B981")
                        paint.textSize = 14f
                        paint.isFakeBoldText = true
                        val metricsHeader = if (isBn) "নির্বাচিত সময়ের মূল পরিসংখ্যান:" else "Financial Performance Metrics:"
                        canvas.drawText(metricsHeader, 40f, 115f, paint)

                        paint.textSize = 12f
                        paint.isFakeBoldText = false
                        paint.color = AndroidColor.BLACK
                        val salesText = if (isBn) "মোট বিক্রি (Total Sales): ৳ ${metrics.totalSales}" else "Total Sales: ৳ ${metrics.totalSales}"
                        val profitText = if (isBn) "মোট লাভ (Estimated Profit): ৳ ${metrics.profit}" else "Estimated Profit: ৳ ${metrics.profit}"
                        val ordersText = if (isBn) "বিক্রির চালান সংখ্যা (Invoices): ${metrics.orderCount}" else "Total Invoices: ${metrics.orderCount}"
                        val expText = if (isBn) "দোকানের মোট খরচ (Expenses): ৳ ${metrics.totalExpenses}" else "Shop Expenses: ৳ ${metrics.totalExpenses}"

                        canvas.drawText(salesText, 40f, 140f, paint)
                        canvas.drawText(profitText, 300f, 140f, paint)
                        canvas.drawText(ordersText, 40f, 165f, paint)
                        canvas.drawText(expText, 300f, 165f, paint)

                        // Divider
                        paint.color = AndroidColor.LTGRAY
                        canvas.drawLine(40f, 185f, 555f, 185f, paint)

                        // Recent Sales Table Header
                        paint.color = AndroidColor.parseColor("#0F172A")
                        paint.textSize = 13f
                        paint.isFakeBoldText = true
                        val tableTitle = if (isBn) "সাম্প্রতিক বিক্রির তালিকা (সর্বোচ্চ ১৫টি)" else "Recent Sales Records (Up to 15)"
                        canvas.drawText(tableTitle, 40f, 210f, paint)

                        // Table Column Titles
                        paint.textSize = 10f
                        paint.isFakeBoldText = true
                        paint.color = AndroidColor.DKGRAY
                        val colInvoice = if (isBn) "চালান" else "Invoice"
                        val colCust = if (isBn) "গ্রাহক" else "Customer"
                        val colTotal = if (isBn) "মোট (৳)" else "Total (৳)"
                        val colPaid = if (isBn) "পরিশোধ" else "Paid"
                        val colDue = if (isBn) "বকেয়া" else "Due"

                        var y = 235f
                        canvas.drawText(colInvoice, 40f, y, paint)
                        canvas.drawText(colCust, 140f, y, paint)
                        canvas.drawText(colTotal, 320f, y, paint)
                        canvas.drawText(colPaid, 400f, y, paint)
                        canvas.drawText(colDue, 480f, y, paint)

                        paint.color = AndroidColor.LTGRAY
                        canvas.drawLine(40f, y + 5f, 555f, y + 5f, paint)

                        // Table Rows
                        paint.isFakeBoldText = false
                        paint.textSize = 10f
                        paint.color = AndroidColor.BLACK

                        val sales = repository.getAllSalesSync().take(15)
                        for (s in sales) {
                            y += 24f
                            if (y > 800f) break
                            canvas.drawText(s.invoiceNumber, 40f, y, paint)
                            val nameTrunc = if (s.customerName.length > 20) s.customerName.take(18) + ".." else s.customerName
                            canvas.drawText(nameTrunc, 140f, y, paint)
                            canvas.drawText("৳ ${s.totalAmount}", 320f, y, paint)
                            canvas.drawText("৳ ${s.paidAmount}", 400f, y, paint)
                            canvas.drawText("৳ ${s.dueAmount}", 480f, y, paint)
                        }

                        doc.finishPage(page)
                        FileOutputStream(file).use { out ->
                            doc.writeTo(out)
                        }
                        doc.close()

                        // Render bitmap preview
                        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                        val bmpCanvas = Canvas(bitmap)
                        bmpCanvas.drawColor(AndroidColor.WHITE)
                        paint.color = AndroidColor.BLACK
                        paint.textSize = 20f
                        paint.isFakeBoldText = true
                        bmpCanvas.drawText(title, 40f, 55f, paint)
                        paint.textSize = 12f
                        paint.isFakeBoldText = false
                        bmpCanvas.drawText(salesText, 40f, 140f, paint)
                        bmpCanvas.drawText(profitText, 300f, 140f, paint)
                        bmpCanvas.drawText(ordersText, 40f, 165f, paint)
                        bmpCanvas.drawText(expText, 300f, 165f, paint)

                        withContext(Dispatchers.Main) {
                            callback(file, listOf(bitmap), null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback(null, emptyList(), e.localizedMessage)
                    }
                }
            }
        }
    }

    private fun filterSalesByPeriod(sales: List<SaleOrder>, period: BusinessTimePeriod, customRange: Pair<Long, Long>?): List<SaleOrder> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return when (period) {
            BusinessTimePeriod.TODAY -> sales.filter { it.timestamp >= startOfToday }
            BusinessTimePeriod.SEVEN_DAYS -> sales.filter { it.timestamp >= System.currentTimeMillis() - 7L * 86400000L }
            BusinessTimePeriod.THIS_MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                sales.filter { it.timestamp >= startOfMonth }
            }
            BusinessTimePeriod.THIS_YEAR -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                sales.filter { it.timestamp >= startOfYear }
            }
            BusinessTimePeriod.CUSTOM -> {
                if (customRange != null) {
                    val (start, end) = customRange
                    sales.filter { it.timestamp in start..end }
                } else sales
            }
        }
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: BusinessTimePeriod, customRange: Pair<Long, Long>?): List<Expense> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return when (period) {
            BusinessTimePeriod.TODAY -> expenses.filter { it.timestamp >= startOfToday }
            BusinessTimePeriod.SEVEN_DAYS -> expenses.filter { it.timestamp >= System.currentTimeMillis() - 7L * 86400000L }
            BusinessTimePeriod.THIS_MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                expenses.filter { it.timestamp >= startOfMonth }
            }
            BusinessTimePeriod.THIS_YEAR -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                expenses.filter { it.timestamp >= startOfYear }
            }
            BusinessTimePeriod.CUSTOM -> {
                if (customRange != null) {
                    val (start, end) = customRange
                    expenses.filter { it.timestamp in start..end }
                } else expenses
            }
        }
    }
}
