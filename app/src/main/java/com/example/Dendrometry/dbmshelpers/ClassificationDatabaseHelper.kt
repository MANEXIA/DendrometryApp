package com.example.Dendrometry.dbmshelpers


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ClassificationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        private const val DATABASE_NAME = "classification.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "classifications"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TREE_SPECIES = "tree_species"
        private const val COLUMN_HEIGHT = "height"
        private const val COLUMN_DIAMETER = "diameter"
        private const val COLUMN_VOLUME = "volume"
        private const val COLUMN_DIAMETER_CLASS = "diameter_class"
        private const val COLUMN_DATE = "date"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TREE_SPECIES TEXT, $COLUMN_HEIGHT REAL, $COLUMN_DIAMETER REAL, $COLUMN_VOLUME REAL, $COLUMN_DIAMETER_CLASS TEXT, $COLUMN_DATE TEXT)"
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
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun getClassifications(): List<DataClassification> {
        val classificationlist = mutableListOf<DataClassification>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val treeSpecies = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TREE_SPECIES))
            val height = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT))
            val diameter = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER))
            val volume = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_VOLUME))
            val diameterClass = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIAMETER_CLASS))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))

            val classification = DataClassification(id, treeSpecies, height, diameter, volume, diameterClass, date)
            classificationlist.add(classification)
        }

        cursor.close()
        db.close()
        return classificationlist
    }

    fun exportToExcelFile(context: Context, fileName: String) {
        val db = writableDatabase
        // Sort by tree species
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_TREE_SPECIES ASC", null)

        try {
            // Format the file name
            val formattedFileName = if (!fileName.endsWith(".xlsx")) "$fileName.xlsx" else fileName

            // Create an Excel workbook and sheet
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("TreeData")

            // Write header row
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("Tree Species", "Height", "Diameter", "Volume(m³)", "Diameter Class", "Date")
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index, CellType.STRING)
                cell.setCellValue(header)
            }

            // Write data rows
            var rowIndex = 1
            while (cursor.moveToNext()) {
                val row = sheet.createRow(rowIndex++)
                row.createCell(0, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_TREE_SPECIES
                )))
                row.createCell(1, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_HEIGHT
                )))
                row.createCell(2, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_DIAMETER
                )))
                row.createCell(3, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_VOLUME
                )))
                row.createCell(4, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_DIAMETER_CLASS
                )))
                row.createCell(5, CellType.STRING).setCellValue(cursor.getString(cursor.getColumnIndexOrThrow(
                    COLUMN_DATE
                )))
            }

            // Open output stream for API 29+ or fallback for older versions
            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android Q and above
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, formattedFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/dendrometry/exports")
                }
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                // Legacy storage path for older devices
                val externalStorageDir = Environment.getExternalStorageDirectory()
                val exportDirPath = File(externalStorageDir, "dendrometry/exports")
                if (!exportDirPath.exists()) {
                    exportDirPath.mkdirs() // Create the directory if it does not exist
                }
                FileOutputStream(File(exportDirPath, formattedFileName))
            }

            outputStream?.use { os ->
                workbook.write(os)
                workbook.close()
                Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
        } finally {
            cursor.close() // Ensure the cursor is closed
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