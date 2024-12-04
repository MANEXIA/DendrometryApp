package com.example.Dendrometry.dbmshelpers


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.net.toFile
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.crypt.EncryptionMode
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.OutputStream

class ClassificationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        private const val DATABASE_NAME = "classification.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "classifications"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TREE_SPECIES = "tree_species"
        private const val COLUMN_HEIGHT = "height"
        private const val COLUMN_DIAMETER = "diameter"
        private const val COLUMN_VOLUME = "volume"
        private const val COLUMN_DIAMETER_CLASS = "diameter_class"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_OWNER = "owner"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TREE_SPECIES TEXT, $COLUMN_HEIGHT REAL, $COLUMN_DIAMETER REAL, $COLUMN_VOLUME REAL, $COLUMN_DIAMETER_CLASS TEXT, $COLUMN_DATE TEXT, $COLUMN_OWNER TEXT)"
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
         val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
         db?.execSQL(dropTableQuery)
         onCreate(db)
    }

    fun insertClassification(dataclass: DataClassification){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TREE_SPECIES, dataclass.treeSpecies)
            put(COLUMN_HEIGHT, dataclass.height)
            put(COLUMN_DIAMETER, dataclass.diameter)
            put(COLUMN_VOLUME, dataclass.volume)
            put(COLUMN_DIAMETER_CLASS, dataclass.diameterClass)
            put(COLUMN_DATE, dataclass.date)
            put(COLUMN_OWNER, dataclass.owner)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun getClassifications(owner: String): List<DataClassification> {
        val classificationlist = mutableListOf<DataClassification>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_OWNER = '$owner'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val treeSpecies = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TREE_SPECIES))
            val height = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT))
            val diameter = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER))
            val volume = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_VOLUME))
            val diameterClass = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER_CLASS))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val owner = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OWNER))

            val classification = DataClassification(id, treeSpecies, height, diameter, volume, diameterClass, date, owner)
            classificationlist.add(classification)
        }

        cursor.close()
        db.close()
        return classificationlist
    }

//THIS IS LATEST APP
    fun exportToExcelFile(context: Context, fileName: String, owner: String, password: String) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_OWNER = '$owner' ORDER BY $COLUMN_TREE_SPECIES ASC", null)

        try {
            val formattedFileName = if (!fileName.endsWith(".xlsx")) "$fileName.xlsx" else fileName

            // Create an Excel workbook and sheet
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("TreeData")

            // Set column widths for better readability
            for (i in 0..5) sheet.setColumnWidth(i, 5000)

            // Add watermark
            val totalRows = cursor.count + 1
            val watermarkRowIndex = totalRows + 2
            val watermarkRow = sheet.createRow(watermarkRowIndex)
            watermarkRow.heightInPoints = 30f

            val watermarkCell = watermarkRow.createCell(0, CellType.STRING)
            watermarkCell.setCellValue(owner)

            sheet.addMergedRegion(CellRangeAddress(watermarkRowIndex, watermarkRowIndex, 0, 5))

            val watermarkStyle = workbook.createCellStyle()
            val font = workbook.createFont().apply {
                fontHeightInPoints = 18
                bold = true
                italic = true
                color = IndexedColors.GREY_40_PERCENT.index
            }
            watermarkStyle.setFont(font)
            watermarkStyle.alignment = HorizontalAlignment.CENTER
            watermarkStyle.verticalAlignment = VerticalAlignment.CENTER
            watermarkCell.cellStyle = watermarkStyle

            // Add headers
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("Tree Species", "Height", "Diameter", "Volume(m³)", "Diameter Class", "Date")
            headers.forEachIndexed { index, header ->
                val headerCell = headerRow.createCell(index, CellType.STRING)
                headerCell.setCellValue(header)
            }

            // Populate rows
            var rowIndex = 1
            while (cursor.moveToNext()) {
                val row = sheet.createRow(rowIndex++)
                row.createCell(0, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TREE_SPECIES)))
                row.createCell(1, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT)))
                row.createCell(2, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER)))
                row.createCell(3, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOLUME)))
                row.createCell(4, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER_CLASS)))
                row.createCell(5, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)))
            }

            // Encrypt the workbook
            val encryptedFile = POIFSFileSystem()
            val encryptionInfo = EncryptionInfo(EncryptionMode.standard)
            val encryptor = encryptionInfo.encryptor
            encryptor.confirmPassword(password)

            // Write the unencrypted workbook to a ByteArrayOutputStream
            val byteArrayOutputStream = ByteArrayOutputStream()
            workbook.write(byteArrayOutputStream)
            workbook.close()

            // Convert the ByteArrayOutputStream to an InputStream
            val inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())

            // Encrypt the InputStream and write it to the POIFSFileSystem
            val encryptedDataStream = encryptor.getDataStream(encryptedFile)
            inputStream.use { it.copyTo(encryptedDataStream) } // Copy the unencrypted data to the encrypted stream
            encryptedDataStream.close()

            // Write the encrypted file to storage
            val finalOutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, formattedFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/dendrometry/exports")
                }
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val externalStorageDir = Environment.getExternalStorageDirectory()
                val exportDirPath = File(externalStorageDir, "dendrometry/exports")
                if (!exportDirPath.exists()) exportDirPath.mkdirs()
                FileOutputStream(File(exportDirPath, formattedFileName))
            }

            // Write the encrypted POIFSFileSystem to the final file
            finalOutputStream?.use { fos ->
                encryptedFile.writeFilesystem(fos)
                Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            cursor.close()
        }
    }


    fun deleteClassificationItem(itemId: Int){
        val db = writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(itemId.toString())
        db.delete(TABLE_NAME, whereClause, whereArgs)
        db.close()
    }

}