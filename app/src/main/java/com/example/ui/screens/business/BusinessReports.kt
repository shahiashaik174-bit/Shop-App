package com.example.ui.screens.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.formatTaka
import com.example.ui.localization.toBengaliNumerals
import com.example.ui.theme.AppTypographyScale
import com.example.ui.theme.DarkSlateBackground
import com.example.ui.theme.DarkSlateCardBorder
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.DarkSlateSurfaceVariant
import com.example.ui.theme.LowStockRed
import com.example.ui.theme.LowStockRedContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividEmeraldAccent
import com.example.ui.theme.VividEmeraldContainer
import com.example.ui.theme.VividEmeraldPrimary
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = DarkSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = title, style = AppTypographyScale.screenTitle, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSlateSurface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

@Composable
fun OverviewReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val metrics by viewModel.periodMetrics.collectAsStateWithLifecycle()
    val moneyNow by viewModel.moneyNowMetrics.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "সামগ্রিক ব্যবসায়িক বিবরণ" else "Business Overview Report",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReportMetricCard(
                title = if (isBangla) "মোট বিক্রয়" else "Total Sales",
                value = formatTaka(metrics.totalSales, currentLanguage),
                color = TextPrimary
            )
            ReportMetricCard(
                title = if (isBangla) "নিট লাভ" else "Net Profit",
                value = formatTaka(metrics.profit, currentLanguage),
                color = VividEmeraldAccent
            )
            ReportMetricCard(
                title = if (isBangla) "মোট খরচ" else "Total Expenses",
                value = formatTaka(metrics.totalExpenses, currentLanguage),
                color = Color(0xFFEF4444)
            )
            ReportMetricCard(
                title = if (isBangla) "মোট অর্ডার সংখ্যা" else "Total Orders",
                value = if (isBangla) toBengaliNumerals(metrics.orderCount.toString()) else metrics.orderCount.toString(),
                color = TextPrimary
            )
            ReportMetricCard(
                title = if (isBangla) "বর্তমান স্টক মূল্য" else "Current Stock Value",
                value = formatTaka(moneyNow.stockValue, currentLanguage),
                color = VividEmeraldAccent
            )
        }
    }
}

@Composable
fun MoneyNowReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val moneyNow by viewModel.moneyNowMetrics.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "বর্তমান নগদ ও দায়দেনা" else "Money Now Breakdown",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReportMetricCard(
                title = if (isBangla) "দোকানে মজুদ মালের মূল্য" else "Inventory Stock Value",
                value = formatTaka(moneyNow.stockValue, currentLanguage),
                color = VividEmeraldAccent
            )
            ReportMetricCard(
                title = if (isBangla) "কাস্টমারদের কাছে বাকি পাবো" else "Customer Baki Owed to Me",
                value = formatTaka(moneyNow.bakiOwedToMe, currentLanguage),
                color = WarmAmber
            )
            ReportMetricCard(
                title = if (isBangla) "মহাজনদের দেনা দিতে হবে" else "I Owe to Suppliers",
                value = formatTaka(moneyNow.iOweSuppliers, currentLanguage),
                color = Color(0xFFEF4444)
            )
            val netWorth = moneyNow.stockValue + moneyNow.bakiOwedToMe - moneyNow.iOweSuppliers
            ReportMetricCard(
                title = if (isBangla) "প্রত্যাশিত মোট মূলধন (Net Position)" else "Net Capital Position",
                value = formatTaka(netWorth, currentLanguage),
                color = if (netWorth >= 0) VividEmeraldAccent else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun SalesChartReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val bars by viewModel.salesChartBars.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "দৈনিক বিক্রয় গ্রাফ" else "Daily Sales Trend",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                border = BorderStroke(1.dp, DarkSlateCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBangla) "গত ৭ দিনের বিক্রয় চিত্র" else "Past 7 Days Sales Trend",
                        style = AppTypographyScale.sectionHeading,
                        color = TextPrimary
                    )
                    SalesBarChartCanvas(
                        bars = bars,
                        currentLanguage = currentLanguage,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bars) { bar ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                        border = BorderStroke(1.dp, DarkSlateCardBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isBangla) bar.labelBn else bar.labelEn, color = TextPrimary)
                            Text(text = formatTaka(bar.amount, currentLanguage), color = VividEmeraldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRankingReportScreen(
    viewModel: BusinessViewModel,
    isBestSellers: Boolean,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val list by viewModel.bestSellers.collectAsStateWithLifecycle()
    val displayedList = if (isBestSellers) list else list.reversed()

    ReportScaffold(
        title = if (isBestSellers) {
            if (isBangla) "সর্বাধিক বিক্রীত পণ্য" else "Best Selling Products"
        } else {
            if (isBangla) "কম বিক্রীত পণ্য" else "Slow Selling Products"
        },
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayedList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                    border = BorderStroke(1.dp, DarkSlateCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            HardwareThumbnailIconSmall(photoKey = item.product.photoKey, category = item.product.category)
                            Column {
                                Text(
                                    text = if (isBangla) item.product.nameBn else item.product.nameEn,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "বিক্রি: ${toBengaliNumerals(item.quantitySold.toString())} টি" else "Sold: ${item.quantitySold} units",
                                    color = TextMuted,
                                    style = AppTypographyScale.hintText
                                )
                            }
                        }
                        Text(
                            text = formatTaka(item.revenue, currentLanguage),
                            color = VividEmeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SalesByCategoryReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val categories by viewModel.salesByCategory.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "ক্যাটাগরি ভিত্তিক বিক্রয়" else "Sales by Category",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                    border = BorderStroke(1.dp, DarkSlateCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat.categoryName, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(text = formatTaka(cat.totalSales, currentLanguage), color = VividEmeraldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LowStockReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val lowStockList by viewModel.lowStockList.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "কম স্টকের তালিকা" else "Low Stock Alerts",
        onBack = onBack
    ) {
        if (lowStockList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isBangla) "সকল পণ্যের পর্যাপ্ত স্টক রয়েছে" else "All products have sufficient stock",
                    color = VividEmeraldAccent
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lowStockList) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                        border = BorderStroke(1.dp, LowStockRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBangla) prod.nameBn else prod.nameEn,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "সীমা: ${toBengaliNumerals(prod.lowStockThreshold.toString())}" else "Threshold: ${prod.lowStockThreshold}",
                                    style = AppTypographyScale.hintText,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = if (isBangla) "মজুদ: ${toBengaliNumerals(prod.stockQuantity.toString())}" else "Stock: ${prod.stockQuantity}",
                                color = LowStockRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    ReportScaffold(
        title = if (isBangla) "খরচের বিস্তারিত বিবরণ" else "Expenses Report",
        onBack = onBack
    ) {
        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = if (isBangla) "কোনো খরচের রেকর্ড নেই" else "No expenses recorded", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                        border = BorderStroke(1.dp, DarkSlateCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = exp.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "${exp.category} • ${sdf.format(Date(exp.timestamp))}", style = AppTypographyScale.hintText, color = TextMuted)
                            }
                            Text(text = formatTaka(exp.amount, currentLanguage), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyReportScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val metrics by viewModel.periodMetrics.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "মাসিক প্রতিবেদন" else "Monthly Performance",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReportMetricCard(
                title = if (isBangla) "চলতি মাসের মোট বিক্রি" else "This Month's Sales",
                value = formatTaka(metrics.totalSales, currentLanguage),
                color = TextPrimary
            )
            ReportMetricCard(
                title = if (isBangla) "চলতি মাসের লাভ" else "This Month's Profit",
                value = formatTaka(metrics.profit, currentLanguage),
                color = VividEmeraldAccent
            )
            ReportMetricCard(
                title = if (isBangla) "চলতি মাসের খরচ" else "This Month's Expenses",
                value = formatTaka(metrics.totalExpenses, currentLanguage),
                color = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun DataHealthScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBack: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val issues by viewModel.dataHealthIssues.collectAsStateWithLifecycle()

    ReportScaffold(
        title = if (isBangla) "ডাটা স্বাস্থ্য পরীক্ষা" else "Data Health Audit",
        onBack = onBack
    ) {
        if (issues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VividEmeraldAccent, modifier = Modifier.size(48.dp))
                    Text(
                        text = if (isBangla) "সব ডাটা সঠিক আছে! কোনো সমস্যা নেই।" else "All store records are healthy & balanced!",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(issues) { issue ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                        border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarmAmber)
                                Text(text = issue.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text(text = issue.description, style = AppTypographyScale.hintText, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        border = BorderStroke(1.dp, DarkSlateCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = AppTypographyScale.normalText, color = TextMuted)
            Text(text = value, style = AppTypographyScale.moneyNumber.copy(fontSize = 18.sp), color = color)
        }
    }
}
