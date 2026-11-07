package com.example.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupSummary
import com.example.data.backup.RestoreResult
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.ViewMode
import com.example.data.local.entity.Category
import com.example.data.repository.HardwareRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LanguageController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
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
    private val backupManager = BackupManager(repository)

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _pendingRestoreSummary = MutableStateFlow<BackupSummary?>(null)
    val pendingRestoreSummary: StateFlow<BackupSummary?> = _pendingRestoreSummary.asStateFlow()

    private var pendingJsonString: String? = null

    init {
        refreshLocalBackups()
    }

    fun refreshLocalBackups() {
        viewModelScope.launch {
            val list = backupManager.listLocalBackups(getApplication())
            _localBackups.value = list
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setLanguage(language: AppLanguage) {
        LanguageController.setLanguage(language)
    }

    fun setViewMode(mode: ViewMode) {
        AppSettings.setViewMode(mode, getApplication())
    }

    fun moveCategory(category: Category, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveCategory(category, moveUp)
        }
    }

    fun toggleCategoryVisibility(category: Category) {
        viewModelScope.launch {
            repository.setCategoryVisibility(category, !category.isVisible)
        }
    }

    fun addCategory(nameEn: String, nameBn: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    nameEn = nameEn,
                    nameBn = nameBn,
                    emoji = emoji,
                    sortOrder = 0,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun saveBackupToPhoneFile(uri: Uri, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = backupManager.generateBackupJson(getApplication())
                withContext(Dispatchers.IO) {
                    val cr = getApplication<Application>().contentResolver
                    cr.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                }
                backupManager.saveBackupToLocalFile(getApplication(), json)
                refreshLocalBackups()
                _isBusy.value = false
                callback(true, null)
            } catch (e: Exception) {
                _isBusy.value = false
                callback(false, e.localizedMessage)
            }
        }
    }

    fun prepareRestoreFromUri(uri: Uri, callback: (String?) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: throw IllegalStateException("Could not read from file")
                }
                val summary = backupManager.inspectBackupJson(json)
                pendingJsonString = json
                _pendingRestoreSummary.value = summary
                _isBusy.value = false
                callback(null)
            } catch (e: Exception) {
                _isBusy.value = false
                callback(e.localizedMessage ?: "Invalid backup file")
            }
        }
    }

    fun prepareRestoreFromFile(file: File, callback: (String?) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = withContext(Dispatchers.IO) { file.readText() }
                val summary = backupManager.inspectBackupJson(json)
                pendingJsonString = json
                _pendingRestoreSummary.value = summary
                _isBusy.value = false
                callback(null)
            } catch (e: Exception) {
                _isBusy.value = false
                callback(e.localizedMessage ?: "Invalid backup file")
            }
        }
    }

    fun confirmRestore(callback: (RestoreResult) -> Unit) {
        val json = pendingJsonString ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = backupManager.restoreFromJson(json, getApplication())
            _isBusy.value = false
            _pendingRestoreSummary.value = null
            pendingJsonString = null
            refreshLocalBackups()
            callback(result)
        }
    }

    fun dismissRestoreConfirmation() {
        _pendingRestoreSummary.value = null
        pendingJsonString = null
    }

    fun shareToGoogleDrive(callback: (Intent) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val json = backupManager.generateBackupJson(getApplication())
                val shareIntent = backupManager.createGoogleDriveShareIntent(getApplication(), json)
                _isBusy.value = false
                callback(shareIntent)
            } catch (e: Exception) {
                _isBusy.value = false
                _statusMessage.value = e.localizedMessage ?: "Failed to generate share backup"
            }
        }
    }
}
