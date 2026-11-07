package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.local.AppSettings
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppTopBar
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LanguageController
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.navigation.NavTab
import com.example.ui.screens.BusinessScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SellScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.HardwareManagerTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.business.BusinessViewModel
import com.example.ui.screens.business.SuppliersScreen

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.DarkSlateSurfaceVariant
import com.example.ui.theme.DarkSlateCardBorder
import com.example.ui.theme.VividEmeraldPrimary
import com.example.ui.theme.VividEmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AppTypographyScale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        com.example.data.local.UnitManager.init(this)

        val db = com.example.data.local.AppDatabase.getDatabase(this)
        val repo = com.example.data.repository.HardwareRepository(
            db.productDao(),
            db.saleOrderDao(),
            db.categoryDao(),
            db.purchaseDao(),
            db.customerDao(),
            db.supplierDao(),
            db.supplierPaymentDao(),
            db.customerPaymentDao(),
            db.expenseDao()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            repo.seedInitialHardwareDataIfNeeded()
        }

        enableEdgeToEdge()
        setContent {
            HardwareManagerTheme {
                ProvideAppLanguage {
                    HardwareManagerApp()
                }
            }
        }
    }
}

@Composable
fun HardwareManagerApp() {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var previousTab by remember { mutableStateOf(NavTab.HOME) }
    var showBakiScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showSuppliersScreen by remember { mutableStateOf(false) }
    var showBusinessUnlockDialog by remember { mutableStateOf(false) }
    var businessPinInput by remember { mutableStateOf("") }
    var businessPinError by remember { mutableStateOf(false) }
    val currentLanguage = LanguageController.currentLanguage

    // Android back handler to return from Settings, Baki, Suppliers screen or to Home tab
    BackHandler(enabled = showSettingsScreen || showBakiScreen || showSuppliersScreen || currentTab != NavTab.HOME) {
        when {
            showSettingsScreen -> showSettingsScreen = false
            showBakiScreen -> showBakiScreen = false
            showSuppliersScreen -> showSuppliersScreen = false
            else -> currentTab = NavTab.HOME
        }
    }

    fun selectTab(tab: NavTab) {
        showSettingsScreen = false
        showBakiScreen = false
        showSuppliersScreen = false
        if (tab == NavTab.BUSINESS && AppSettings.isBusinessLockEnabled && !AppSettings.isBusinessUnlocked) {
            showBusinessUnlockDialog = true
        } else {
            previousTab = currentTab
            currentTab = tab
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = when {
                    showSettingsScreen -> {
                        if (currentLanguage == AppLanguage.BANGLA) "সেটিংস ও ডাটা ব্যাকআপ" else "Settings & Backup"
                    }
                    showBakiScreen -> {
                        if (currentLanguage == AppLanguage.BANGLA) "বাকির খাতা" else "Customer Ledger"
                    }
                    showSuppliersScreen -> {
                        if (currentLanguage == AppLanguage.BANGLA) "মহাজন ও সাপ্লায়ার খাতা" else "Suppliers Ledger"
                    }
                    else -> currentTab.title(currentLanguage)
                },
                currentLanguage = currentLanguage,
                onLanguageChange = { newLang ->
                    LanguageController.setLanguage(newLang)
                },
                canNavigateBack = showSettingsScreen || showBakiScreen || showSuppliersScreen || currentTab != NavTab.HOME,
                onNavigateBack = {
                    when {
                        showSettingsScreen -> showSettingsScreen = false
                        showBakiScreen -> showBakiScreen = false
                        showSuppliersScreen -> showSuppliersScreen = false
                        else -> currentTab = NavTab.HOME
                    }
                },
                onOpenSettings = {
                    showBakiScreen = false
                    showSuppliersScreen = false
                    showSettingsScreen = true
                }
            )
        },
        bottomBar = {
            AppBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    selectTab(tab)
                },
                currentLanguage = currentLanguage
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showSettingsScreen -> {
                    SettingsScreen(
                        currentLanguage = currentLanguage,
                        onBack = { showSettingsScreen = false }
                    )
                }
                showBakiScreen -> {
                    com.example.ui.screens.baki.BakiScreen(
                        currentLanguage = currentLanguage,
                        onBack = { showBakiScreen = false }
                    )
                }
                showSuppliersScreen -> {
                    val businessVm: BusinessViewModel = viewModel()
                    SuppliersScreen(
                        viewModel = businessVm,
                        currentLanguage = currentLanguage,
                        onBackToDashboard = { showSuppliersScreen = false }
                    )
                }
                else -> {
                    Crossfade(
                        targetState = currentTab,
                        label = "tab_crossfade"
                    ) { tab ->
                        when (tab) {
                            NavTab.HOME -> HomeScreen(
                                currentLanguage = currentLanguage,
                                onNavigateToSell = { selectTab(NavTab.SELL) },
                                onNavigateToStock = { selectTab(NavTab.STOCK) },
                                onNavigateToBaki = { showBakiScreen = true },
                                onNavigateToSuppliers = { showSuppliersScreen = true },
                                onNavigateToHistory = { selectTab(NavTab.HISTORY) },
                                onNavigateToBusiness = { selectTab(NavTab.BUSINESS) },
                                onNavigateToSettings = { showSettingsScreen = true }
                            )
                            NavTab.SELL -> SellScreen(currentLanguage = currentLanguage)
                            NavTab.STOCK -> StockScreen(currentLanguage = currentLanguage)
                            NavTab.HISTORY -> HistoryScreen(currentLanguage = currentLanguage)
                            NavTab.BUSINESS -> BusinessScreen(
                                currentLanguage = currentLanguage,
                                onNavigateToBaki = { showBakiScreen = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBusinessUnlockDialog) {
        val isBangla = currentLanguage == AppLanguage.BANGLA
        AlertDialog(
            onDismissRequest = {
                showBusinessUnlockDialog = false
                businessPinInput = ""
                businessPinError = false
            },
            containerColor = DarkSlateSurface,
            title = {
                Text(
                    text = if (isBangla) "🔒 ব্যবসায়িক হিসাব আনলক করুন" else "🔒 Unlock Business Tab",
                    style = AppTypographyScale.screenTitle,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBangla)
                            "ব্যবসায়ের হিসাব ও লাভ-ক্ষতির তথ্য দেখতে আপনার সেট করা পিন কোডটি দিন:"
                        else
                            "Enter your PIN code to access business and financial reports:",
                        style = AppTypographyScale.hintText,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = businessPinInput,
                        onValueChange = {
                            businessPinInput = it
                            businessPinError = false
                        },
                        label = { Text(if (isBangla) "পিন কোড (PIN)" else "PIN Code", style = AppTypographyScale.hintText) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = businessPinError,
                        modifier = Modifier.fillMaxWidth().testTag("input_business_pin"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VividEmeraldPrimary,
                            unfocusedBorderColor = DarkSlateCardBorder,
                            focusedContainerColor = DarkSlateSurfaceVariant,
                            unfocusedContainerColor = DarkSlateSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorBorderColor = AlertRed
                        )
                    )
                    if (businessPinError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBangla) "ভুল পিন কোড! আবার চেষ্টা করুন।" else "Incorrect PIN code! Try again.",
                            color = AlertRed,
                            style = AppTypographyScale.hintText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (AppSettings.verifyBusinessPassword(businessPinInput)) {
                            AppSettings.unlockBusiness()
                            showBusinessUnlockDialog = false
                            businessPinInput = ""
                            businessPinError = false
                            previousTab = currentTab
                            currentTab = NavTab.BUSINESS
                        } else {
                            businessPinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary),
                    modifier = Modifier.testTag("btn_unlock_business")
                ) {
                    Text(if (isBangla) "আনলক করুন" else "Unlock", style = AppTypographyScale.buttonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBusinessUnlockDialog = false
                        businessPinInput = ""
                        businessPinError = false
                    }
                ) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = TextMuted)
                }
            }
        )
    }
}
