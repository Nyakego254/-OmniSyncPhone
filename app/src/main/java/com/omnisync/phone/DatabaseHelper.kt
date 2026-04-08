package com.omnisync.phone

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "omnisync.db"
        private const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_CALLS = "calls"
        const val TABLE_SMS = "sms"
        const val TABLE_NOTIFICATIONS = "notifications"

        // Common columns
        const val COLUMN_ID = "id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SYNCED = "synced"   // 0 = not synced, 1 = synced

        // Calls columns
        const val COLUMN_PHONE_NUMBER = "phone_number"
        const val COLUMN_CALL_TYPE = "call_type"   // 1=incoming, 2=outgoing, 3=missed

        // SMS columns
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_BODY = "body"

        // Notifications columns
        const val COLUMN_PACKAGE_NAME = "package_name"
        const val COLUMN_TITLE = "title"
        const val COLUMN_TEXT = "text"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Calls table
        db.execSQL("""
            CREATE TABLE $TABLE_CALLS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PHONE_NUMBER TEXT NOT NULL,
                $COLUMN_CALL_TYPE INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent())

        // SMS table
        db.execSQL("""
            CREATE TABLE $TABLE_SMS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ADDRESS TEXT NOT NULL,
                $COLUMN_BODY TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Notifications table
        db.execSQL("""
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PACKAGE_NAME TEXT NOT NULL,
                $COLUMN_TITLE TEXT,
                $COLUMN_TEXT TEXT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CALLS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        onCreate(db)
    }

    // Insert a call
    fun insertCall(phoneNumber: String, callType: Int, timestamp: Long): Long {
        val values = ContentValues().apply {
            put(COLUMN_PHONE_NUMBER, phoneNumber)
            put(COLUMN_CALL_TYPE, callType)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        return writableDatabase.insert(TABLE_CALLS, null, values)
    }

    // Insert an SMS
    fun insertSms(address: String, body: String, timestamp: Long): Long {
        val values = ContentValues().apply {
            put(COLUMN_ADDRESS, address)
            put(COLUMN_BODY, body)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        return writableDatabase.insert(TABLE_SMS, null, values)
    }

    // Insert a notification
    fun insertNotification(packageName: String, title: String?, text: String?, timestamp: Long): Long {
        val values = ContentValues().apply {
            put(COLUMN_PACKAGE_NAME, packageName)
            put(COLUMN_TITLE, title)
            put(COLUMN_TEXT, text)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        return writableDatabase.insert(TABLE_NOTIFICATIONS, null, values)
    }

    // Get unsynced calls
    fun getUnsyncedCalls(): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        val cursor = readableDatabase.query(
            TABLE_CALLS, null, "$COLUMN_SYNCED = 0", null, null, null, "$COLUMN_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val number = it.getString(it.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER))
                val type = it.getInt(it.getColumnIndexOrThrow(COLUMN_CALL_TYPE))
                val ts = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                list.add(CallRecord(id, number, type, ts))
            }
        }
        return list
    }

    // Get unsynced SMS
    fun getUnsyncedSms(): List<SmsRecord> {
        val list = mutableListOf<SmsRecord>()
        val cursor = readableDatabase.query(
            TABLE_SMS, null, "$COLUMN_SYNCED = 0", null, null, null, "$COLUMN_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val address = it.getString(it.getColumnIndexOrThrow(COLUMN_ADDRESS))
                val body = it.getString(it.getColumnIndexOrThrow(COLUMN_BODY))
                val ts = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                list.add(SmsRecord(id, address, body, ts))
            }
        }
        return list
    }

    // Get unsynced notifications
    fun getUnsyncedNotifications(): List<NotificationRecord> {
        val list = mutableListOf<NotificationRecord>()
        val cursor = readableDatabase.query(
            TABLE_NOTIFICATIONS, null, "$COLUMN_SYNCED = 0", null, null, null, "$COLUMN_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val pkg = it.getString(it.getColumnIndexOrThrow(COLUMN_PACKAGE_NAME))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                val ts = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                list.add(NotificationRecord(id, pkg, title, text, ts))
            }
        }
        return list
    }

    // Mark records as synced after upload
    fun markCallsSynced(ids: List<Long>) {
        val db = writableDatabase
        ids.forEach { id ->
            val values = ContentValues().apply { put(COLUMN_SYNCED, 1) }
            db.update(TABLE_CALLS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        }
    }

    fun markSmsSynced(ids: List<Long>) {
        val db = writableDatabase
        ids.forEach { id ->
            val values = ContentValues().apply { put(COLUMN_SYNCED, 1) }
            db.update(TABLE_SMS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        }
    }

    fun markNotificationsSynced(ids: List<Long>) {
        val db = writableDatabase
        ids.forEach { id ->
            val values = ContentValues().apply { put(COLUMN_SYNCED, 1) }
            db.update(TABLE_NOTIFICATIONS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        }
    }

    // Data classes
    data class CallRecord(val id: Long, val number: String, val type: Int, val timestamp: Long)
    data class SmsRecord(val id: Long, val address: String, val body: String, val timestamp: Long)
    data class NotificationRecord(val id: Long, val packageName: String, val title: String?, val text: String?, val timestamp: Long)
}