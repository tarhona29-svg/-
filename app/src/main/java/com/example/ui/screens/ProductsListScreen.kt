package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.BarcodeInventoryViewModel
import com.example.ui.ProductFilter
import com.example.ui.components.ExpiryStatus
import com.example.ui.components.ProductEditDialog
import com.example.ui.components.getExpiryStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    viewModel: BarcodeInventoryViewModel,
    onBackClick: () -> Unit,
    onSelectBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val allProductsList by viewModel.allProducts.collectAsState()

    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Totals calculations
    val totalCost = allProductsList.sumOf { it.costPrice }
    val totalSelling = allProductsList.sumOf { it.sellingPrice }
    val totalExpectedProfit = totalSelling - totalCost

    val currencyFmt = DecimalFormat("#,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "قائمة المخزون والأصناف",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (allProductsList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_all_products_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح كافة البيانات",
                                tint = RoseError
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة صنف")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Financial Summary Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "إجمالي التكلفة", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "${currencyFmt.format(totalCost)} د.ل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "إجمالي البيع", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "${currencyFmt.format(totalSelling)} د.ل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EmeraldGreen
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "الأرباح المتوقعة", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "${currencyFmt.format(totalExpectedProfit)} د.ل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("بحث باسم الصنف أو رقم الباركود...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("inventory_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ProductFilter.ALL,
                    onClick = { viewModel.setFilter(ProductFilter.ALL) },
                    label = { Text("الكل (${allProductsList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = selectedFilter == ProductFilter.EXPIRING_SOON,
                    onClick = { viewModel.setFilter(ProductFilter.EXPIRING_SOON) },
                    label = { Text("تنتهي قريباً") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberContainer,
                        selectedLabelColor = AmberWarning
                    )
                )

                FilterChip(
                    selected = selectedFilter == ProductFilter.EXPIRED,
                    onClick = { viewModel.setFilter(ProductFilter.EXPIRED) },
                    label = { Text("منتهية الصلاحية") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoseContainer,
                        selectedLabelColor = RoseError
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث" else "قاعدة البيانات فارغة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.barcode }) { product ->
                        ProductRowCard(
                            product = product,
                            onSelect = {
                                viewModel.scanBarcode(product.barcode)
                                onSelectBarcode(product.barcode)
                            },
                            onEdit = { editingProduct = it },
                            onDelete = { viewModel.deleteProduct(it) }
                        )
                    }
                }
            }
        }
    }

    if (isAddingNew) {
        ProductEditDialog(
            onDismiss = { isAddingNew = false },
            onSave = { newProduct ->
                viewModel.saveProduct(newProduct)
                isAddingNew = false
            }
        )
    }

    if (editingProduct != null) {
        ProductEditDialog(
            product = editingProduct,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                viewModel.saveProduct(updated)
                editingProduct = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("تأكيد مسح كافة الأصناف") },
            text = { Text("هل أنت تأكد من رغبتك في مسح جميع الأصناف المخزنة في قاعدة البيانات؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllProducts()
                        showClearConfirm = false
                    }
                ) {
                    Text("مسح الكل", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ProductRowCard(
    product: ProductEntity,
    onSelect: () -> Unit,
    onEdit: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val currencyFmt = DecimalFormat("#,##0.00")
    val expiryStatus = getExpiryStatus(product.expiryDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_row_${product.barcode}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "باركود: ${product.barcode}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Expiry Badge
                    val (badgeBg, badgeText, badgeColor) = when (expiryStatus) {
                        ExpiryStatus.VALID -> Triple(EmeraldContainer, "صالحة", EmeraldGreen)
                        ExpiryStatus.EXPIRING_SOON -> Triple(AmberContainer, "قريبة الانتهاء", AmberWarning)
                        ExpiryStatus.EXPIRED -> Triple(RoseContainer, "منتهية", RoseError)
                        ExpiryStatus.UNKNOWN -> Triple(Color.LightGray.copy(alpha = 0.3f), product.expiryDate, Color.DarkGray)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "التكلفة: ${currencyFmt.format(product.costPrice)} د.ل",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "البيع: ${currencyFmt.format(product.sellingPrice)} د.ل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onEdit(product) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onDelete(product) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = RoseError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
