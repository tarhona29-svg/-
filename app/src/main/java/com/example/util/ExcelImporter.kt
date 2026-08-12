package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.ProductEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

data class ImportResult(
    val successCount: Int,
    val totalCount: Int,
    val products: List<ProductEntity>,
    val errorMessage: String? = null
)

data class ColumnMapping(
    var barcodeCol: Int = 0,
    var nameCol: Int = 1,
    var costCol: Int = 2,
    var sellingCol: Int = 3,
    var expiryCol: Int = 4
)

object ExcelImporter {

    fun parseFile(context: Context, uri: Uri, customMapping: ColumnMapping? = null): ImportResult {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = getFileName(context, uri).lowercase(Locale.ROOT)

            if (fileName.endsWith(".csv") || fileName.endsWith(".txt") || mimeType.contains("csv") || mimeType.contains("plain")) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    parseCsvStream(stream, customMapping)
                } ?: ImportResult(0, 0, emptyList(), "تعذر فتح ملف CSV")
            } else {
                // Try XLSX Zip parsing
                contentResolver.openInputStream(uri)?.use { stream ->
                    parseXlsxStream(stream, customMapping)
                } ?: ImportResult(0, 0, emptyList(), "تعذر فتح ملف الإكسل")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(0, 0, emptyList(), "خطأ أثناء قراءة الملف: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file.xlsx"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex("_display_name")
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: "file.xlsx"
            }
        }
        return name
    }

    private fun parseCsvStream(inputStream: InputStream, customMapping: ColumnMapping?): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(0, 0, emptyList(), "الملف فارغ")

        // Detect delimiter
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            else -> ","
        }

        val rows = lines.map { parseCsvLine(it, delimiter) }
        return processParsedRows(rows, customMapping)
    }

    private fun parseCsvLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        for (ch in line.toCharArray()) {
            if (ch == '"') {
                insideQuotes = !insideQuotes
            } else if (ch.toString() == delimiter && !insideQuotes) {
                tokens.add(sb.toString().trim().removeSurrounding("\""))
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\""))
        return tokens
    }

    private fun parseXlsxStream(inputStream: InputStream, customMapping: ColumnMapping?): ImportResult {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        var sheetStream: InputStream? = null
        val zip = ZipInputStream(inputStream)

        var entry = zip.nextEntry
        val tempSheetBytesMap = mutableMapOf<String, ByteArray>()

        while (entry != null) {
            val entryName = entry.name
            if (entryName == "xl/sharedStrings.xml") {
                sharedStrings.addAll(parseSharedStrings(zip))
            } else if (entryName.startsWith("xl/worksheets/sheet1.xml") || entryName == "xl/worksheets/sheet.xml") {
                tempSheetBytesMap[entryName] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val sheetBytes = tempSheetBytesMap.values.firstOrNull()
            ?: return ImportResult(0, 0, emptyList(), "لم يتم العثور على ورقة عمل داخل ملف XLSX")

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(sheetBytes.inputStream(), "UTF-8")

        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var currentCellRef = ""
        var currentCellType = ""
        var cellValue = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = StringBuilder()
                        }
                        "v", "t" -> {
                            cellValue = StringBuilder()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    cellValue.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            val rawVal = cellValue.toString().trim()
                            val formattedVal = if (currentCellType == "s") {
                                val idx = rawVal.toIntOrNull() ?: -1
                                if (idx in 0 until sharedStrings.size) sharedStrings[idx] else rawVal
                            } else {
                                rawVal
                            }
                            currentRow.add(formattedVal)
                        }
                        "t" -> {
                            if (currentCellType == "inlineStr") {
                                currentRow.add(cellValue.toString().trim())
                            }
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                sheetRows.add(currentRow.toList())
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return processParsedRows(sheetRows, customMapping)
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var textBuffer = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        insideT = true
                        textBuffer = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        textBuffer.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        insideT = false
                        strings.add(textBuffer.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun processParsedRows(rows: List<List<String>>, customMapping: ColumnMapping?): ImportResult {
        if (rows.isEmpty()) return ImportResult(0, 0, emptyList(), "لم يتم العثور على بيانات بالملف")

        var mapping = customMapping ?: autoDetectMapping(rows.first())
        var startIndex = 0

        // Check if header row exists
        if (isHeaderRow(rows.first())) {
            mapping = autoDetectMapping(rows.first())
            startIndex = 1
        }

        val products = mutableListOf<ProductEntity>()

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            val barcode = getCellString(row, mapping.barcodeCol)
            val name = getCellString(row, mapping.nameCol)
            val costStr = getCellString(row, mapping.costCol)
            val sellingStr = getCellString(row, mapping.sellingCol)
            val expiryStr = getCellString(row, mapping.expiryCol)

            if (barcode.isNotBlank() && name.isNotBlank()) {
                val cost = parseDouble(costStr)
                val selling = parseDouble(sellingStr)
                val expiry = normalizeExpiryDate(expiryStr)

                products.add(
                    ProductEntity(
                        barcode = cleanBarcode(barcode),
                        name = name.trim(),
                        costPrice = cost,
                        sellingPrice = selling,
                        expiryDate = expiry,
                        category = "مستورد",
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }

        return if (products.isNotEmpty()) {
            ImportResult(products.size, rows.size - startIndex, products, null)
        } else {
            ImportResult(0, rows.size, emptyList(), "لم يتم العثور على أصناف صالحة في الملف. يرجى التأكد من التنسيق.")
        }
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val rowStr = row.joinToString(" ").lowercase(Locale.ROOT)
        return rowStr.contains("باركود") || rowStr.contains("barcode") ||
                rowStr.contains("صنف") || rowStr.contains("اسم") ||
                rowStr.contains("تكلفة") || rowStr.contains("بيع") ||
                rowStr.contains("صلاحية") || rowStr.contains("price") ||
                rowStr.contains("cost") || rowStr.contains("expiry")
    }

    private fun autoDetectMapping(headerRow: List<String>): ColumnMapping {
        val mapping = ColumnMapping(0, 1, 2, 3, 4)
        headerRow.forEachIndexed { idx, colText ->
            val text = colText.lowercase(Locale.ROOT).trim()
            when {
                text.contains("باركود") || text.contains("barcode") || text.contains("كود") -> mapping.barcodeCol = idx
                text.contains("اسم") || text.contains("صنف") || text.contains("منتج") || text.contains("name") || text.contains("title") -> mapping.nameCol = idx
                text.contains("تكلفة") || text.contains("شراء") || text.contains("cost") || text.contains("buy") -> mapping.costCol = idx
                text.contains("بيع") || text.contains("مستهلك") || text.contains("price") || text.contains("sale") -> mapping.sellingCol = idx
                text.contains("صلاحية") || text.contains("انتهاء") || text.contains("expiry") || text.contains("exp") -> mapping.expiryCol = idx
            }
        }
        return mapping
    }

    private fun getCellString(row: List<String>, colIdx: Int): String {
        return if (colIdx >= 0 && colIdx < row.size) row[colIdx].trim() else ""
    }

    private fun parseDouble(str: String): Double {
        val cleaned = str.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun cleanBarcode(raw: String): String {
        // Handle scientific notation from Excel like 6.2911E+12
        if (raw.contains("E+") || raw.contains("e+")) {
            val d = raw.toDoubleOrNull()
            if (d != null) {
                return String.format(Locale.US, "%.0f", d)
            }
        }
        return raw.replace(Regex("[^a-zA-Z0-9]"), "").trim()
    }

    private fun normalizeExpiryDate(raw: String): String {
        if (raw.isBlank()) return "غير محدد"
        // Try parsing standard formats or Excel Julian days
        val numDays = raw.toDoubleOrNull()
        if (numDays != null && numDays > 30000) {
            // Excel serial date number
            val millis = (numDays - 25569) * 86400 * 1000
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(millis.toLong()))
        }
        return raw
    }

    fun getSampleProducts(): List<ProductEntity> {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return listOf(
            ProductEntity(
                barcode = "6281001000123",
                name = "حليب كامل الدسم المراعي 1 لتر",
                costPrice = 4.50,
                sellingPrice = 6.00,
                expiryDate = sdf.format(Date(now + 15 * dayMillis)), // Expiring soon
                category = "ألبان",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "6291100002345",
                name = "زيت زيتون بكر ممتاز 500 مل",
                costPrice = 18.00,
                sellingPrice = 25.00,
                expiryDate = sdf.format(Date(now + 365 * dayMillis)),
                category = "زيوت",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "6221000034567",
                name = "عصير برتقال طبيعي 250 مل",
                costPrice = 2.00,
                sellingPrice = 3.50,
                expiryDate = sdf.format(Date(now - 2 * dayMillis)), // Expired!
                category = "مشروبات",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "7622210004568",
                name = "شوكولاتة جالاكسي بالنعناع 90جم",
                costPrice = 5.00,
                sellingPrice = 7.50,
                expiryDate = sdf.format(Date(now + 120 * dayMillis)),
                category = "حلويات",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "8806088005679",
                name = "معجون أسنان كولجيت بالنعناع 100 مل",
                costPrice = 11.00,
                sellingPrice = 16.00,
                expiryDate = sdf.format(Date(now + 500 * dayMillis)),
                category = "عناية شخصية",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "6281007006780",
                name = "أرز أبيض بسمتي الشعلان 5 كجم",
                costPrice = 38.00,
                sellingPrice = 48.00,
                expiryDate = sdf.format(Date(now + 400 * dayMillis)),
                category = "مواد غذائية",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "6291011007891",
                name = "ماء نقي هنا 40 × 330 مل",
                costPrice = 14.00,
                sellingPrice = 19.50,
                expiryDate = sdf.format(Date(now + 200 * dayMillis)),
                category = "مشروبات",
                lastUpdated = now
            ),
            ProductEntity(
                barcode = "6222000089012",
                name = "شاي كبوس أسود 100 خيط",
                costPrice = 12.50,
                sellingPrice = 17.00,
                expiryDate = sdf.format(Date(now + 600 * dayMillis)),
                category = "مشروبات ساخنة",
                lastUpdated = now
            )
        )
    }

    fun generateSampleCsvText(): String {
        return """
رقم باركود الصنف,اسم الصنف,سعر التكلفة,سعر البيع,الصلاحية
6281001000123,حليب كامل الدسم 1 لتر,4.50,6.00,2026-11-30
6291100002345,زيت زيتون ممتاز 500 مل,18.00,25.00,2027-05-15
6221000034567,عصير برتقال طبيعي,2.00,3.50,2026-08-10
7622210004568,شوكولاتة سادة,5.00,7.50,2027-01-20
8806088005679,شامبو بالصبار 400 مل,15.00,22.00,2027-10-01
        """.trimIndent()
    }
}
