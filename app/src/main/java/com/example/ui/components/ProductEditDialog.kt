package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProductEntity

@Composable
fun ProductEditDialog(
    initialBarcode: String = "",
    product: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var barcode by remember { mutableStateOf(product?.barcode ?: initialBarcode) }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var costPriceStr by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPriceStr by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var expiryDate by remember { mutableStateOf(product?.expiryDate ?: "2027-01-01") }
    var category by remember { mutableStateOf(product?.category ?: "عام") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "إضافة صنف جديد للقاعدة" else "تعديل بيانات الصنف",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("رقم باركود الصنف") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_barcode_input"),
                    singleLine = true,
                    enabled = product == null // Barcode is primary key
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("سعر التكلفة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_cost_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("سعر البيع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_selling_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("تاريخ الصلاحية (مثال: 2026-12-31)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_expiry_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("الفئة / القسم") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_category_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (barcode.isBlank() || name.isBlank()) {
                        errorMessage = "يرجى إدخال رقم الباركود واسم الصنف"
                        return@Button
                    }
                    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                    val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0

                    onSave(
                        ProductEntity(
                            barcode = barcode.trim(),
                            name = name.trim(),
                            costPrice = cost,
                            sellingPrice = sell,
                            expiryDate = expiryDate.trim().ifBlank { "غير محدد" },
                            category = category.trim().ifBlank { "عام" },
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_product_btn")
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
