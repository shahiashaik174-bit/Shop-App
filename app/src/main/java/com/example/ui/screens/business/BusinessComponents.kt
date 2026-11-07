package com.example.ui.screens.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.localization.AppLanguage
import com.example.ui.theme.AppTypographyScale
import com.example.ui.theme.DarkSlateCardBorder
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.DarkSlateSurfaceVariant
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividEmeraldAccent
import com.example.ui.theme.VividEmeraldPrimary
import java.io.File

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
        border = BorderStroke(1.dp, DarkSlateCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = AppTypographyScale.hintText,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = AppTypographyScale.normalText.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PeriodSelectorPills(
    selectedPeriod: BusinessTimePeriod,
    onPeriodSelected: (BusinessTimePeriod) -> Unit,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val options = listOf(
        BusinessTimePeriod.TODAY to if (isBangla) "আজ" else "Today",
        BusinessTimePeriod.SEVEN_DAYS to if (isBangla) "৭ দিন" else "7 Days",
        BusinessTimePeriod.THIS_MONTH to if (isBangla) "চলতি মাস" else "This Month",
        BusinessTimePeriod.THIS_YEAR to if (isBangla) "চলতি বছর" else "This Year",
        BusinessTimePeriod.CUSTOM to if (isBangla) "কাস্টম" else "Custom"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (period, label) ->
            val isSelected = selectedPeriod == period
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPeriodSelected(period) }
                    .testTag("period_${period.name.lowercase()}"),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) VividEmeraldPrimary else DarkSlateSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) VividEmeraldAccent else DarkSlateCardBorder
                )
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = AppTypographyScale.hintText.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

@Composable
fun SalesBarChartCanvas(
    bars: List<SalesChartBar>,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (currentLanguage == AppLanguage.BANGLA) "কোনো বিক্রির তথ্য নেই" else "No sales data",
                style = AppTypographyScale.hintText,
                color = TextMuted
            )
        }
        return
    }

    val maxAmount = (bars.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(10.0)

    Canvas(modifier = modifier) {
        val barCount = bars.size
        val availableWidth = size.width
        val barWidth = (availableWidth / (barCount * 1.5f)).coerceIn(12f, 40f)
        val spacing = (availableWidth - (barWidth * barCount)) / (barCount + 1)
        val chartHeight = size.height - 24f

        bars.forEachIndexed { i, bar ->
            val barHeight = ((bar.amount / maxAmount) * chartHeight).toFloat().coerceAtLeast(4f)
            val left = spacing + i * (barWidth + spacing)
            val top = chartHeight - barHeight

            // Draw bar
            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    slices: List<CategorySalesItem>,
    palette: List<Color>,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(70.dp)) {
                drawCircle(
                    color = Color.DarkGray.copy(alpha = 0.4f),
                    style = Stroke(width = 16f)
                )
            }
        }
        return
    }

    val total = slices.sumOf { it.totalSales }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = 18f
        val radius = (size.minDimension - strokeWidth) / 2
        val centerOffset = Offset(size.width / 2, size.height / 2)

        slices.forEachIndexed { index, slice ->
            val sweep = ((slice.totalSales / total) * 360f).toFloat()
            val color = palette[index % palette.size]
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun HardwareThumbnailIconSmall(
    photoKey: String?,
    category: String?,
    modifier: Modifier = Modifier
) {
    if (!photoKey.isNullOrBlank()) {
        val file = File(photoKey)
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            return
        }
    }

    Icon(
        imageVector = when (category?.lowercase()) {
            "tools", "যন্ত্রপাতি" -> Icons.Default.Hardware
            "pipes", "পাইপ" -> Icons.Default.Build
            else -> Icons.Default.Inventory
        },
        contentDescription = null,
        tint = VividEmeraldAccent,
        modifier = modifier.size(20.dp)
    )
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, note: String) -> Unit,
    currentLanguage: AppLanguage
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (isBangla) "দোকান খরচ" else "Shop Expense") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSlateSurface,
        title = {
            Text(
                text = if (isBangla) "নতুন খরচ যোগ করুন" else "Add Expense",
                style = AppTypographyScale.screenTitle,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBangla) "খরচের শিরোনাম" else "Expense Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = VividEmeraldAccent,
                        unfocusedBorderColor = DarkSlateCardBorder,
                        focusedLabelColor = VividEmeraldAccent,
                        unfocusedLabelColor = TextMuted
                    )
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isBangla) "টাকার পরিমাণ (৳)" else "Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = VividEmeraldAccent,
                        unfocusedBorderColor = DarkSlateCardBorder,
                        focusedLabelColor = VividEmeraldAccent,
                        unfocusedLabelColor = TextMuted
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isBangla) "নোট (ঐচ্ছিক)" else "Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_note"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = VividEmeraldAccent,
                        unfocusedBorderColor = DarkSlateCardBorder,
                        focusedLabelColor = VividEmeraldAccent,
                        unfocusedLabelColor = TextMuted
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onSave(title, amount, category, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary),
                modifier = Modifier.testTag("btn_save_expense")
            ) {
                Text(if (isBangla) "সংরক্ষণ করুন" else "Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বাতিল" else "Cancel", color = TextMuted)
            }
        }
    )
}
