package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExpiryStatus {
    VALID, EXPIRING_SOON, EXPIRED, UNKNOWN
}

fun getExpiryStatus(expiryStr: String): ExpiryStatus {
    if (expiryStr.isBlank() || expiryStr == "غير محدد") return ExpiryStatus.UNKNOWN

    val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy")
    var date: Date? = null
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            date = sdf.parse(expiryStr)
            if (date != null) break
        } catch (e: Exception) {
            // continue
        }
    }

    if (date == null) return ExpiryStatus.UNKNOWN

    val now = System.currentTimeMillis()
    val expiryTime = date.time
    val thirtyDays = 30L * 24 * 60 * 60 * 1000

    return when {
        expiryTime <= now -> ExpiryStatus.EXPIRED
        (expiryTime - now) <= thirtyDays -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.VALID
    }
}

@Composable
fun ProductDetailCard(
    product: ProductEntity,
    onEditClick: (ProductEntity) -> Unit,
    onDeleteClick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFmt = DecimalFormat("#,##0.00")
    val costStr = currencyFmt.format(product.costPrice)
    val sellStr = currencyFmt.format(product.sellingPrice)
    val profit = product.sellingPrice - product.costPrice
    val profitStr = currencyFmt.format(profit)
    val profitMarginPercent = if (product.costPrice > 0) {
        ((profit / product.costPrice) * 100).toInt()
    } else 0

    val expiryStatus = getExpiryStatus(product.expiryDate)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_detail_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row: Category Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = product.category.ifBlank { "عام" },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { onEditClick(product) },
                        modifier = Modifier.testTag("edit_product_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الصنف",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(product) },
                        modifier = Modifier.testTag("delete_product_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الصنف",
                            tint = RoseError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode Canvas Display
            BarcodeCanvas(
                barcode = product.barcode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))

            // Prices Grid Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cost Price Box
                PriceCardBox(
                    title = "سعر التكلفة",
                    amount = "$costStr د.ل",
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Selling Price Box
                PriceCardBox(
                    title = "سعر البيع",
                    amount = "$sellStr د.ل",
                    backgroundColor = EmeraldContainer,
                    textColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profit & Expiry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profit Margin Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profit >= 0) EmeraldContainer.copy(alpha = 0.5f) else RoseContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (profit >= 0) EmeraldGreen else RoseError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "هامش الربح",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+$profitStr د.ل ($profitMarginPercent%)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (profit >= 0) EmeraldGreen else RoseError
                        )
                    }
                }

                // Expiry Badge Box
                val (expBg, expColor, expIcon, expLabel) = when (expiryStatus) {
                    ExpiryStatus.VALID -> Quadruple(EmeraldContainer, EmeraldGreen, Icons.Default.CheckCircle, "صالحة")
                    ExpiryStatus.EXPIRING_SOON -> Quadruple(AmberContainer, AmberWarning, Icons.Default.Warning, "تنتهي قريباً")
                    ExpiryStatus.EXPIRED -> Quadruple(RoseContainer, RoseError, Icons.Default.EventBusy, "منتهية الصلاحية!")
                    ExpiryStatus.UNKNOWN -> Quadruple(Color.LightGray.copy(alpha = 0.3f), Color.DarkGray, Icons.Default.CalendarToday, "غير محدد")
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = expBg)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = expIcon,
                                contentDescription = null,
                                tint = expColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الصلاحية: $expLabel",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = expColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.expiryDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = expColor
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun PriceCardBox(
    title: String,
    amount: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = textColor
            )
        }
    }
}
