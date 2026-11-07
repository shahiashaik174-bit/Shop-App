package com.example.ui.screens.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Category
import com.example.data.local.entity.Product
import com.example.data.local.entity.Supplier
import com.example.data.repository.HardwareRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.visibleCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = combine(_searchQuery, _selectedCategory) { query, cat ->
        Pair(query, cat)
    }.flatMapLatest { (query, cat) ->
        repository.filterProducts(query, cat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalProductCount: StateFlow<Int> = repository.allProducts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliersOwedFirst
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _productToEdit = MutableStateFlow<Product?>(null)
    val productToEdit: StateFlow<Product?> = _productToEdit.asStateFlow()

    private val _productForAddStock = MutableStateFlow<Product?>(null)
    val productForAddStock: StateFlow<Product?> = _productForAddStock.asStateFlow()

    private val _showCategoryManager = MutableStateFlow(false)
    val showCategoryManager: StateFlow<Boolean> = _showCategoryManager.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun openAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun closeAddProductDialog() {
        _showAddProductDialog.value = false
    }

    fun openEditProduct(product: Product) {
        _productToEdit.value = product
    }

    fun closeEditProduct() {
        _productToEdit.value = null
    }

    fun openAddStock(product: Product) {
        _productForAddStock.value = product
    }

    fun closeAddStock() {
        _productForAddStock.value = null
    }

    fun openCategoryManager() {
        _showCategoryManager.value = true
    }

    fun closeCategoryManager() {
        _showCategoryManager.value = false
    }

    fun softDeleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.softDeleteProduct(productId)
            if (_selectedProduct.value?.id == productId) {
                _selectedProduct.value = null
            }
        }
    }

    fun saveNewProduct(
        nameBn: String,
        nameEn: String,
        category: String,
        description: String,
        photoKey: String?,
        stockQuantity: Int,
        buyPrice: Double,
        sellPrice: Double,
        lowLimit: Int,
        unitBn: String,
        unitEn: String
    ) {
        viewModelScope.launch {
            val newProduct = Product(
                nameBn = nameBn,
                nameEn = nameEn,
                category = category,
                description = description,
                photoKey = photoKey ?: "",
                stockQuantity = stockQuantity,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                lowStockThreshold = lowLimit,
                unitBn = unitBn,
                unitEn = unitEn
            )
            repository.insertProduct(newProduct)
            _showAddProductDialog.value = false
        }
    }

    fun updateExistingProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _productToEdit.value = null
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = product
            }
        }
    }

    fun recordAddStock(
        productId: Long,
        productName: String,
        addedQty: Int,
        unitBuyingPrice: Double,
        supplierId: Long?,
        supplierName: String,
        paidAmount: Double,
        dueAmount: Double,
        billPhotoUri: String?,
        note: String
    ) {
        viewModelScope.launch {
            repository.addStock(
                productId = productId,
                productName = productName,
                addedQty = addedQty,
                unitBuyingPrice = unitBuyingPrice,
                supplierName = supplierName,
                note = note,
                supplierId = supplierId,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                billPhotoUri = billPhotoUri
            )
            _productForAddStock.value = null
        }
    }

    fun addCategory(nameEn: String, nameBn: String, emoji: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    nameEn = nameEn,
                    nameBn = nameBn,
                    emoji = emoji,
                    sortOrder = 0
                )
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun toggleCategoryVisibility(category: Category) {
        viewModelScope.launch {
            repository.setCategoryVisibility(category, !category.isVisible)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
