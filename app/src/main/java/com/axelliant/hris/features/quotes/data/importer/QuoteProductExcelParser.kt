package com.axelliant.hris.features.quotes.data.importer

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class QuoteProductExcelParser @Inject constructor() {

    fun parse(inputStream: InputStream): QuoteProductExcelParseResult {
        val entries = inputStream.use(::readXlsxEntries)
        val sharedStrings = entries[SHARED_STRINGS_ENTRY]
            ?.let(::parseSharedStrings)
            .orEmpty()
        val sheetXml = entries[SHEET_ENTRY]
            ?: entries.entries.firstOrNull { it.key.startsWith(WORKSHEETS_PREFIX) }?.value
            ?: throw QuoteProductExcelParseException("No worksheet found in the Excel file.")

        val rows = parseSheetRows(sheetXml, sharedStrings)
        if (rows.isEmpty()) {
            throw QuoteProductExcelParseException("Excel file is empty.")
        }

        val headerCells = rows.first().cells
        val skuColumn = headerCells.columnFor(HEADER_SKU)
        val mfgColumn = headerCells.columnFor(HEADER_MFG_PART_NO)
        val quantityColumn = headerCells.columnFor(HEADER_QUANTITY)

        if (skuColumn == null || mfgColumn == null || quantityColumn == null) {
            throw QuoteProductExcelParseException("Excel must contain SKU, MFG Part No, and Quantity columns.")
        }

        var ignoredRows = 0
        val products = rows.drop(1)
            .take(MAX_IMPORT_ROWS)
            .mapNotNull { row ->
                val sku = row.cells[skuColumn].orEmpty().trim()
                val mfgPartNo = row.cells[mfgColumn].orEmpty().trim()
                val quantity = row.cells[quantityColumn].orEmpty().trim().toPositiveQuantityOrNull()
                val isEmptyRow = sku.isBlank() && mfgPartNo.isBlank() && row.cells[quantityColumn].orEmpty().isBlank()

                when {
                    isEmptyRow -> null
                    quantity == null || (sku.isBlank() && mfgPartNo.isBlank()) -> {
                        ignoredRows += 1
                        null
                    }
                    else -> QuoteProductExcelRow(
                        rowNumber = row.number,
                        sku = sku,
                        manufacturerPartNumber = mfgPartNo,
                        quantity = quantity
                    )
                }
            }

        ignoredRows += (rows.size - 1 - MAX_IMPORT_ROWS).coerceAtLeast(0)
        return QuoteProductExcelParseResult(
            rows = products,
            ignoredRows = ignoredRows
        )
    }

    private fun readXlsxEntries(inputStream: InputStream): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(inputStream.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name in REQUIRED_XLSX_ENTRIES) {
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                } else if (!entry.isDirectory && entry.name.startsWith(WORKSHEETS_PREFIX)) {
                    entries.putIfAbsent(entry.name, zip.readBytes().toString(Charsets.UTF_8))
                }
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val document = parseDocument(xml)
        val items = document.getElementsByTagName("si")
        return (0 until items.length).map { index ->
            val item = items.item(index)
            item.childElements("t").joinToString(separator = "") { it.textContent.orEmpty() }
        }
    }

    private fun parseSheetRows(xml: String, sharedStrings: List<String>): List<SheetRow> {
        val document = parseDocument(xml)
        val rowNodes = document.getElementsByTagName("row")
        return (0 until rowNodes.length).mapNotNull { rowIndex ->
            val rowElement = rowNodes.item(rowIndex) as? Element ?: return@mapNotNull null
            val rowNumber = rowElement.getAttribute("r").toIntOrNull() ?: (rowIndex + 1)
            val cells = mutableMapOf<String, String>()

            rowElement.childElements("c").forEach { cell ->
                val column = cell.getAttribute("r").takeLetters().ifBlank { return@forEach }
                val rawValue = when (cell.getAttribute("t")) {
                    "s" -> cell.childText("v")
                        .toIntOrNull()
                        ?.let { sharedStrings.getOrNull(it) }
                        .orEmpty()
                    "inlineStr" -> cell.childText("t")
                    else -> cell.childText("v")
                }
                cells[column] = rawValue.trim()
            }

            SheetRow(rowNumber, cells)
        }
    }

    private fun parseDocument(xml: String) = DocumentBuilderFactory.newInstance()
        .apply {
            isNamespaceAware = false
            isIgnoringComments = true
        }
        .newDocumentBuilder()
        .parse(InputSource(StringReader(xml)))

    private fun Element.childText(tagName: String): String {
        return getElementsByTagName(tagName)
            .item(0)
            ?.textContent
            .orEmpty()
    }

    private fun Node.childElements(tagName: String): List<Element> {
        val nodes = (this as? Element)?.getElementsByTagName(tagName) ?: return emptyList()
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun Map<String, String>.columnFor(header: String): String? {
        val expected = header.normalizedHeader()
        return entries.firstOrNull { (_, value) -> value.normalizedHeader() == expected }?.key
    }

    private fun String.normalizedHeader(): String {
        return lowercase().filter { it.isLetterOrDigit() }
    }

    private fun String.takeLetters(): String {
        return takeWhile { it.isLetter() }
    }

    private fun String.toPositiveQuantityOrNull(): Int? {
        val quantity = toDoubleOrNull() ?: return null
        if (quantity <= 0.0 || quantity % 1.0 != 0.0) return null
        return quantity.toInt()
    }

    private data class SheetRow(
        val number: Int,
        val cells: Map<String, String>
    )

    private companion object {
        const val SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml"
        const val SHEET_ENTRY = "xl/worksheets/sheet1.xml"
        const val WORKSHEETS_PREFIX = "xl/worksheets/"
        const val HEADER_SKU = "SKU"
        const val HEADER_MFG_PART_NO = "MFG Part No"
        const val HEADER_QUANTITY = "Quantity"
        const val MAX_IMPORT_ROWS = 100

        val REQUIRED_XLSX_ENTRIES = setOf(
            SHARED_STRINGS_ENTRY,
            SHEET_ENTRY
        )
    }
}

data class QuoteProductExcelParseResult(
    val rows: List<QuoteProductExcelRow>,
    val ignoredRows: Int
)

data class QuoteProductExcelRow(
    val rowNumber: Int,
    val sku: String,
    val manufacturerPartNumber: String,
    val quantity: Int
) {
    val searchCandidates: List<QuoteProductExcelSearchCandidate>
        get() = listOf(
            QuoteProductExcelSearchCandidate(
                value = manufacturerPartNumber,
                type = QuoteProductExcelSearchType.ManufacturerPartNumber
            ),
            QuoteProductExcelSearchCandidate(
                value = sku,
                type = QuoteProductExcelSearchType.Sku
            )
        )
            .map { it.copy(value = it.value.trim()) }
            .filter { it.value.isNotBlank() }
            .distinctBy { it.type to it.value.lowercase() }
}

data class QuoteProductExcelSearchCandidate(
    val value: String,
    val type: QuoteProductExcelSearchType
)

enum class QuoteProductExcelSearchType {
    ManufacturerPartNumber,
    Sku
}

class QuoteProductExcelParseException(message: String) : IllegalArgumentException(message)
