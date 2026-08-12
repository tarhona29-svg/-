package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BarcodeInventoryViewModel
import com.example.ui.screens.ImportExcelDialog
import com.example.ui.screens.MainScanScreen
import com.example.ui.screens.ProductsListScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MAIN_SCAN, INVENTORY_LIST
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BarcodeInventoryApp()
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeInventoryApp(
    viewModel: BarcodeInventoryViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_SCAN) }
    var showImportDialog by remember { mutableStateOf(false) }

    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val isNotFound by viewModel.isNotFound.collectAsState()
    val productCount by viewModel.productCount.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()

    when (currentScreen) {
        AppScreen.MAIN_SCAN -> {
            MainScanScreen(
                viewModel = viewModel,
                scannedProduct = scannedProduct,
                scannedBarcode = scannedBarcode,
                isNotFound = isNotFound,
                productCount = productCount,
                importStatusMessage = importStatusMessage,
                onNavigateToInventory = { currentScreen = AppScreen.INVENTORY_LIST },
                onOpenImportDialog = { showImportDialog = true }
            )
        }

        AppScreen.INVENTORY_LIST -> {
            ProductsListScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = AppScreen.MAIN_SCAN },
                onSelectBarcode = { barcode ->
                    currentScreen = AppScreen.MAIN_SCAN
                }
            )
        }
    }

    if (showImportDialog) {
        ImportExcelDialog(
            onDismiss = { showImportDialog = false },
            onImportFileSelected = { uri, customMapping ->
                viewModel.importExcelFile(uri, customMapping)
            },
            onLoadSampleData = {
                viewModel.loadSampleData()
            }
        )
    }
}
