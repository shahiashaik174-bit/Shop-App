package com.example.ui.screens.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Supplier
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.formatTaka
import com.example.ui.theme.AppTypographyScale
import com.example.ui.theme.DarkSlateBackground
import com.example.ui.theme.DarkSlateCardBorder
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.DarkSlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividEmeraldAccent
import com.example.ui.theme.VividEmeraldContainer
import com.example.ui.theme.VividEmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: BusinessViewModel,
    currentLanguage: AppLanguage,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var selectedSupplierForPayment by remember { mutableStateOf<Supplier?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_suppliers"),
        containerColor = DarkSlateBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBangla) "সরবরাহকারী ও মহাজন (Suppliers)" else "Suppliers & Vendors",
                        style = AppTypographyScale.screenTitle,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard, modifier = Modifier.testTag("btn_back_suppliers")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSlateSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSupplierDialog = true },
                containerColor = VividEmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_supplier")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Owed Header Card
            val totalOwed = suppliers.sumOf { it.amountOwed }
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_total_suppliers_owed"),
                colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                border = BorderStroke(1.dp, DarkSlateCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBangla) "মহাজনদের মোট দেনা" else "Total Supplier Dues",
                            style = AppTypographyScale.hintText,
                            color = TextMuted
                        )
                        Text(
                            text = formatTaka(totalOwed, currentLanguage),
                            style = AppTypographyScale.moneyNumber.copy(fontSize = 20.sp),
                            color = Color(0xFFEF4444)
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x33EF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFFEF4444))
                    }
                }
            }

            // Suppliers List
            if (suppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBangla) "কোনো সরবরাহকারী পাওয়া যায়নি" else "No suppliers found",
                        style = AppTypographyScale.hintText,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(suppliers, key = { it.id }) { supplier ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("supplier_card_${supplier.id}"),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateSurface),
                            border = BorderStroke(1.dp, DarkSlateCardBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(VividEmeraldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = VividEmeraldAccent)
                                    }
                                    Column {
                                        Text(
                                            text = supplier.name,
                                            style = AppTypographyScale.normalText.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        if (supplier.phone.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                                Text(text = supplier.phone, style = AppTypographyScale.hintText, color = TextMuted)
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatTaka(supplier.amountOwed, currentLanguage),
                                        style = AppTypographyScale.normalText.copy(fontWeight = FontWeight.Bold),
                                        color = if (supplier.amountOwed > 0) Color(0xFFEF4444) else VividEmeraldAccent
                                    )
                                    Button(
                                        onClick = { selectedSupplierForPayment = supplier },
                                        modifier = Modifier.height(32.dp).testTag("btn_pay_supplier_${supplier.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (isBangla) "পরিশোধ" else "Pay", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var initialDue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            containerColor = DarkSlateSurface,
            title = {
                Text(if (isBangla) "নতুন সরবরাহকারী যোগ করুন" else "Add New Supplier", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isBangla) "নাম" else "Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_supplier_name")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isBangla) "ফোন নম্বর" else "Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_supplier_phone")
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isBangla) "কোম্পানি / ঠিকানা" else "Company / Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialDue,
                        onValueChange = { initialDue = it },
                        label = { Text(if (isBangla) "পূর্বের দেনা (৳)" else "Initial Balance Owed (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val due = initialDue.toDoubleOrNull() ?: 0.0
                            viewModel.addSupplier(name, phone, address, due)
                            showAddSupplierDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary)
                ) {
                    Text(if (isBangla) "যোগ করুন" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = TextMuted)
                }
            }
        )
    }

    // Record Payment Dialog
    selectedSupplierForPayment?.let { supplier ->
        var amountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedSupplierForPayment = null },
            containerColor = DarkSlateSurface,
            title = {
                Text(
                    text = if (isBangla) "${supplier.name} - দেনা পরিশোধ" else "Pay Supplier: ${supplier.name}",
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBangla) "বর্তমান দেনা: ${formatTaka(supplier.amountOwed, currentLanguage)}" else "Current Due: ${formatTaka(supplier.amountOwed, currentLanguage)}",
                        color = Color(0xFFEF4444),
                        style = AppTypographyScale.hintText
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (isBangla) "পরিশোধের পরিমাণ (৳)" else "Payment Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_pay_supplier_amount")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isBangla) "নোট / বিবরণ" else "Note / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordSupplierPayment(supplier.id, amount, note)
                            selectedSupplierForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary)
                ) {
                    Text(if (isBangla) "পরিশোধ নিশ্চিত করুন" else "Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSupplierForPayment = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = TextMuted)
                }
            }
        )
    }
}
