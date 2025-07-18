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
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        const val COLUMN_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_USERNAME TEXT, $COLUMN_PASSWORD TEXT, $COLUMN_STATUS TEXT)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS ${UserDatabaseHelper.TABLE_NAME}"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }

    fun insertUser(name: String, username: String, password: String, status: String): Long{
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_STATUS, status)
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


    fun updateUserStatus(username: String, newStatus: String): Int {
        // Get writable database
        val db = writableDatabase

        // Prepare the new values to update
        val values = ContentValues().apply {
            put(COLUMN_STATUS, newStatus) // Set the new status
        }

        // Define the selection condition for identifying the user
        val selection = "$COLUMN_USERNAME = ?"
        val selectionArgs = arrayOf(username) // Argument for the username placeholder in the condition

        // Update the status in the database and return the number of rows affected
        val rowsUpdated = db.update(
            TABLE_NAME,    // The table where the update will happen
            values,        // The new values to update
            selection,     // The condition to find the specific user
            selectionArgs  // The value for the username placeholder
        )

        // Close the database
        db.close()

        return rowsUpdated // Return the number of rows updated (should be 1 for a successful update)
    }



}