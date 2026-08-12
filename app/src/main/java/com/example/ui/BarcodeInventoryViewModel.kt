package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProductEntity
import com.example.data.ProductRepository
import com.example.util.ColumnMapping
import com.example.util.ExcelImporter
import com.example.util.SoundEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProductFilter {
    ALL, EXPIRING_SOON, EXPIRED
}

class BarcodeInventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProductRepository(database.productDao())
    }

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productCount: StateFlow<Int> = repository.productCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    private val _scannedProduct = MutableStateFlow<ProductEntity?>(null)
    val scannedProduct: StateFlow<ProductEntity?> = _scannedProduct.asStateFlow()

    private val _isNotFound = MutableStateFlow(false)
    val isNotFound: StateFlow<Boolean> = _isNotFound.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ProductFilter.ALL)
    val selectedFilter: StateFlow<ProductFilter> = _selectedFilter.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        _searchQuery,
        _selectedFilter,
        repository.allProducts
    ) { query, filter, products ->
        var list = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) || it.barcode.contains(query, ignoreCase = true)
            }
        }

        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

        when (filter) {
            ProductFilter.ALL -> list
            ProductFilter.EXPIRING_SOON -> list.filter {
                val time = parseDateMillis(it.expiryDate)
                time != null && time > now && (time - now) <= thirtyDaysMillis
            }
            ProductFilter.EXPIRED -> list.filter {
                val time = parseDateMillis(it.expiryDate)
                time != null && time <= now
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setFilter(filter: ProductFilter) {
        _selectedFilter.value = filter
    }

    fun scanBarcode(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return

        _scannedBarcode.value = clean
        viewModelScope.launch {
            val product = repository.getProductByBarcode(clean)
            if (product != null) {
                _scannedProduct.value = product
                _isNotFound.value = false
                SoundEffects.playBeep()
                SoundEffects.vibrate(getApplication())
            } else {
                _scannedProduct.value = null
                _isNotFound.value = true
                SoundEffects.playErrorBeep()
                SoundEffects.vibrate(getApplication())
            }
        }
    }

    fun clearScannedResult() {
        _scannedBarcode.value = null
        _scannedProduct.value = null
        _isNotFound.value = false
    }

    fun importExcelFile(uri: Uri, customMapping: ColumnMapping? = null) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "جاري قراءة واستيراد ملف الإكسل..."

            val result = ExcelImporter.parseFile(getApplication(), uri, customMapping)

            if (result.products.isNotEmpty()) {
                repository.insertAll(result.products)
                _importStatusMessage.value = "تم استيراد ${result.successCount} صنف بنجاح من أصل ${result.totalCount} صف في الملف!"
            } else {
                _importStatusMessage.value = result.errorMessage ?: "حدث خطأ أثناء استيراد الملف"
            }
            _isImporting.value = false
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "جاري تحميل البيانات النموذجية..."
            val samples = ExcelImporter.getSampleProducts()
            repository.insertAll(samples)
            _importStatusMessage.value = "تم تحميل ${samples.size} أصناف نموذجية بنجاح إلى قاعدة البيانات!"
            _isImporting.value = false
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertProduct(product)
            // If currently viewing this barcode, update scanned view
            if (_scannedBarcode.value == product.barcode) {
                _scannedProduct.value = product
                _isNotFound.value = false
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            if (_scannedBarcode.value == product.barcode) {
                clearScannedResult()
            }
        }
    }

    fun clearAllProducts() {
        viewModelScope.launch {
            repository.deleteAllProducts()
            clearScannedResult()
            _importStatusMessage.value = "تم مسح قاعدة البيانات بالكامل"
        }
    }

    fun dismissImportStatus() {
        _importStatusMessage.value = null
    }

    private fun parseDateMillis(dateStr: String): Long? {
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }
}
