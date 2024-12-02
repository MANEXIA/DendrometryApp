package com.example.Dendrometry.dbmshelpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.Dendrometry.dbmshelpers.ClassificationDatabaseHelper.Companion

class UserDatabaseHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object{
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_USERNAME TEXT, $COLUMN_PASSWORD TEXT)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS ${UserDatabaseHelper.TABLE_NAME}"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }

    fun insertUser(name: String, username: String, password: String): Long{
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
        }
        val db = writableDatabase
        return db.insert(TABLE_NAME, null, values)
    }


    fun readUser(username: String, password: String): Cursor? {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)

        // Query the database and return the cursor
        val cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null)

        // Check if there are any rows and move to the first row if possible
        if (cursor != null && cursor.moveToFirst()) {
            return cursor
        }
        cursor?.close()  // Close the cursor if no data is found
        return null  // No matching user found
    }

}