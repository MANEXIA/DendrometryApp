package com.example.myappkotlin

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ClassificationDatabaseHelper(Context: Context) : SQLiteOpenHelper(Context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        private const val DATABASE_NAME = "classification.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "classifications"
        private const val COLUMN_ID = "id"
        private const val COLUMN_HEIGHT = "height"
        private const val COLUMN_DIAMETER = "diameter"
        private const val COLUMN_VOLUME = "volume"
        private const val COLUMN_DIAMETER_CLASS = "diameter_class"
        private const val COLUMN_DATE = "date"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_HEIGHT REAL, $COLUMN_DIAMETER REAL, $COLUMN_VOLUME REAL, $COLUMN_DIAMETER_CLASS TEXT, $COLUMN_DATE TEXT)"
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
            put(COLUMN_HEIGHT, dataclass.height)
            put(COLUMN_DIAMETER, dataclass.diameter)
            put(COLUMN_VOLUME, dataclass.volume)
            put(COLUMN_DIAMETER_CLASS, dataclass.diameterClass)
            put(COLUMN_DATE, dataclass.date)
        }
        db.insert(TABLE_NAME, null, values)
    }


}