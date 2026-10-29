package com.brijbymgt.util.exportimport

import android.content.Context
import android.net.Uri
import com.brijbymgt.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    suspend fun exportToExcel(
        context: Context,
        uri: Uri,
        students: List<StudentWithDetails>,
        selectedFields: List<String>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                val zipOut = ZipOutputStream(outputStream)

                // 1. [Content_Types].xml
                zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
                val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
                zipOut.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 2. _rels/.rels
                zipOut.putNextEntry(ZipEntry("_rels/.rels"))
                val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
                zipOut.write(relsXml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 3. xl/_rels/workbook.xml.rels
                zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
                zipOut.write(wbRelsXml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 4. xl/workbook.xml
                zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
                val wbXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Student Records" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""
                zipOut.write(wbXml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 5. xl/styles.xml (Bold Header style s="1")
                zipOut.putNextEntry(ZipEntry("xl/styles.xml"))
                val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font><sz val="11"/><name val="Calibri"/></font>
        <font><b/><sz val="11"/><color rgb="FF000000"/><name val="Calibri"/></font>
    </fonts>
    <fills count="2">
        <fill><patternFill fillType="none"/></fill>
        <fill><patternFill fillType="gray125"/></fill>
    </fills>
    <borders count="1">
        <border><left/><right/><top/><bottom/><diagonal/></border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    </cellXfs>
</styleSheet>"""
                zipOut.write(stylesXml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 6. xl/worksheets/sheet1.xml
                zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))

                val sheetSb = StringBuilder()
                sheetSb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                sheetSb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
                sheetSb.append("  <sheetViews>\n")
                sheetSb.append("    <sheetView tabSelected=\"1\" workbookViewId=\"0\">\n")
                sheetSb.append("      <pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>\n")
                sheetSb.append("    </sheetView>\n")
                sheetSb.append("  </sheetViews>\n")
                sheetSb.append("  <sheetData>\n")

                // Row 1: Bold Header
                sheetSb.append("    <row r=\"1\">\n")
                for (colIdx in selectedFields.indices) {
                    val colRef = getColumnRef(colIdx) + "1"
                    val headerVal = escapeXml(selectedFields[colIdx])
                    sheetSb.append("      <c r=\"$colRef\" t=\"inlineStr\" s=\"1\"><is><t>$headerVal</t></is></c>\n")
                }
                sheetSb.append("    </row>\n")

                // Data Rows
                for (rowIdx in students.indices) {
                    val rowNum = rowIdx + 2
                    val item = students[rowIdx]
                    sheetSb.append("    <row r=\"$rowNum\">\n")

                    for (colIdx in selectedFields.indices) {
                        val colRef = getColumnRef(colIdx) + rowNum
                        val fieldName = selectedFields[colIdx]
                        val rawValue = FieldExtractor.getFieldValue(fieldName, item)
                        val escapedVal = escapeXml(rawValue)

                        // String cell inlineStr preserves leading zeros for SR Number, PEN, Aadhaar, Mobile, etc.
                        sheetSb.append("      <c r=\"$colRef\" t=\"inlineStr\"><is><t>$escapedVal</t></is></c>\n")
                    }
                    sheetSb.append("    </row>\n")
                }

                sheetSb.append("  </sheetData>\n")
                sheetSb.append("</worksheet>")

                val sheetBytes = sheetSb.toString().toByteArray(StandardCharsets.UTF_8)
                zipOut.write(sheetBytes)
                zipOut.closeEntry()

                zipOut.finish()
                zipOut.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun getColumnRef(index: Int): String {
        var col = index
        val sb = StringBuilder()
        while (col >= 0) {
            sb.insert(0, ('A'.code + (col % 26)).toChar())
            col = (col / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

