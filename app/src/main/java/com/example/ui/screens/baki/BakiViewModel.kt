package com.example.ui.screens.baki

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.SaleOrder
import com.example.data.repository.HardwareRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CustomerLedgerItem {
    abstract val id: Long
    abstract val timestamp: Long
    abstract val amount: Double
    abstract val note: String

    data class Sale(
        override val id: Long,
        override val timestamp: Long,
        override val amount: Double,
        override val note: String,
        val saleOrder: SaleOrder
    ) : CustomerLedgerItem()

    data class Payment(
        override val id: Long,
        override val timestamp: Long,
        override val amount: Double,
        override val note: String,
        val paymentMethod: String = "Cash"
    ) : CustomerLedgerItem()
}

@OptIn(ExperimentalCoroutinesApi::class)
class BakiViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getCustomerById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showAddCustomerDialog = MutableStateFlow(false)
    val showAddCustomerDialog: StateFlow<Boolean> = _showAddCustomerDialog.asStateFlow()

    private val _showRecordPaymentDialog = MutableStateFlow(false)
    val showRecordPaymentDialog: StateFlow<Boolean> = _showRecordPaymentDialog.asStateFlow()

    private val _showAddNoteDialog = MutableStateFlow(false)
    val showAddNoteDialog: StateFlow<Boolean> = _showAddNoteDialog.asStateFlow()

    private val _entryToEdit = MutableStateFlow<CustomerLedgerItem?>(null)
    val entryToEdit: StateFlow<CustomerLedgerItem?> = _entryToEdit.asStateFlow()

    private val _entryToDelete = MutableStateFlow<CustomerLedgerItem?>(null)
    val entryToDelete: StateFlow<CustomerLedgerItem?> = _entryToDelete.asStateFlow()

    val totalCustomerBaki: StateFlow<Double> = repository.totalUnpaidBaki
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        repository.allCustomers,
        _searchQuery
    ) { list, query ->
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) list else list.filter {
            it.name.lowercase().contains(q) || it.phone.contains(q)
        }
        filtered.sortedByDescending { it.totalBaki }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerLedger: StateFlow<List<CustomerLedgerItem>> = _selectedCustomerId.flatMapLatest { custId ->
        if (custId == null) {
            flowOf(emptyList())
        } else {
            combine(
                repository.getCreditSalesForCustomer(custId),
                repository.getPaymentsForCustomer(custId)
            ) { sales, payments ->
                val saleEntries = sales.map { sale ->
                    CustomerLedgerItem.Sale(
                        id = sale.id,
                        timestamp = sale.timestamp,
                        amount = sale.dueAmount,
                        note = sale.summaryItems,
                        saleOrder = sale
                    )
                }
                val paymentEntries = payments.map { payment ->
                    CustomerLedgerItem.Payment(
                        id = payment.id,
                        timestamp = payment.timestamp,
                        amount = payment.amount,
                        note = payment.note,
                        paymentMethod = "Cash"
                    )
                }
                (saleEntries + paymentEntries).sortedByDescending { it.timestamp }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomerId.value = customer?.id
    }

    fun openAddCustomerDialog() {
        _showAddCustomerDialog.value = true
    }

    fun closeAddCustomerDialog() {
        _showAddCustomerDialog.value = false
    }

    fun addCustomer(name: String, phone: String, address: String, note: String, initialBaki: Double) {
        viewModelScope.launch {
            val customer = Customer(
                name = name,
                phone = phone,
                address = address,
                note = note,
                totalBaki = initialBaki
            )
            val newId = repository.insertCustomer(customer)
            _showAddCustomerDialog.value = false
            _selectedCustomerId.value = newId
        }
    }

    fun openRecordPaymentDialog() {
        _showRecordPaymentDialog.value = true
    }

    fun closeRecordPaymentDialog() {
        _showRecordPaymentDialog.value = false
    }

    fun recordPayment(amount: Double, note: String) {
        val customer = selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.recordCustomerPayment(
                customerId = customer.id,
                amount = amount,
                note = note
            )
            _showRecordPaymentDialog.value = false
        }
    }

    fun openAddNoteDialog() {
        _showAddNoteDialog.value = true
    }

    fun closeAddNoteDialog() {
        _showAddNoteDialog.value = false
    }

    fun addCustomerNote(note: String) {
        val customer = selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.updateCustomerNote(customer.id, note)
            _showAddNoteDialog.value = false
        }
    }

    fun openEditEntry(entry: CustomerLedgerItem) {
        _entryToEdit.value = entry
    }

    fun closeEditEntry() {
        _entryToEdit.value = null
    }

    fun openDeleteEntry(entry: CustomerLedgerItem) {
        _entryToDelete.value = entry
    }

    fun closeDeleteEntry() {
        _entryToDelete.value = null
    }

    fun updatePaymentEntry(id: Long, newAmount: Double, newNote: String) {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.updateCustomerPayment(
                payment = CustomerPayment(
                    id = id,
                    customerId = customer.id,
                    amount = newAmount,
                    note = newNote,
                    timestamp = _entryToEdit.value?.timestamp ?: System.currentTimeMillis()
                )
            )
            _entryToEdit.value = null
        }
    }

    fun updateCreditSaleEntry(saleOrder: SaleOrder, newDueAmount: Double, newNote: String, newDueDate: Long?) {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.updateCreditSale(
                saleOrder = saleOrder.copy(
                    dueAmount = newDueAmount,
                    summaryItems = newNote,
                    dueDate = newDueDate
                )
            )
            _entryToEdit.value = null
        }
    }

    fun deletePaymentEntry(id: Long) {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.deleteCustomerPayment(id, customer.id)
            _entryToDelete.value = null
        }
    }

    fun deleteCreditSaleEntry(id: Long) {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            repository.deleteCreditSale(id, customer.id)
            _entryToDelete.value = null
        }
    }
}
