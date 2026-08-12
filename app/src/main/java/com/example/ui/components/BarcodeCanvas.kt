package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun BarcodeCanvas(
    barcode: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color(0xFFFAFAFA)
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val width = size.width
            val height = size.height

            // Generate deterministic pseudo-bar widths from barcode string bytes
            val hash = barcode.ifBlank { "000000000000" }
            val bars = mutableListOf<Float>()

            // Start guard
            bars.add(1f); bars.add(1f); bars.add(1f)

            for (ch in hash) {
                val code = ch.code
                val w1 = ((code % 3) + 1).toFloat()
                val w2 = (((code / 3) % 3) + 1).toFloat()
                val w3 = (((code / 7) % 2) + 1).toFloat()
                bars.add(w1)
                bars.add(w2)
                bars.add(w3)
            }

            // End guard
            bars.add(1f); bars.add(1f); bars.add(1f)

            val totalWeight = bars.sum()
            val unitWidth = width / totalWeight

            var currentX = 0f
            var isBar = true

            for (w in bars) {
                val barW = w * unitWidth
                if (isBar) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(barW, height)
                    )
                }
                currentX += barW
                isBar = !isBar
            }
        }

        Text(
            text = barcode.ifBlank { "---" },
            style = MaterialTheme.typography.bodyLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
