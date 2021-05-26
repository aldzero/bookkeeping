package com.example.bookkeeping

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class DatabaseHelper(var context: Context) : SQLiteOpenHelper(context, "app.db", null,
        7) {

    val table_accounts = "accounts"
    val table_transactions = "transactions"
    val table_exchange_rates = "exchange"
    val field_id = "_id"
    val field_name = "name"
    val field_funds = "funds"
    val field_date = "date"
    val field_funds_rub = "funds_rub"
    val field_currency = "currency"
    val field_account = "account"
    val field_comment = "comment"
    val field_value = "field_value"

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try{
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table_accounts + " (" +
                    field_id + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    field_name + " TEXT NOT NULL UNIQUE, " +
                    field_funds + " REAL NOT NULL CHECK(" + field_funds + ">= 0));")
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table_transactions + " (" +
                    field_id + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    field_date + " TEXT NOT NULL, " +
                    field_funds_rub + " REAL NOT NULL CHECK(" + field_funds_rub + "!= 0), " +
                    field_funds + " REAL NOT NULL CHECK(" + field_funds + "!= 0), " +
                    field_currency + " TEXT NOT NULL, " +
                    field_comment + " TEXT, " +
                    field_account + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + field_account + ") REFERENCES " + table_accounts + "(" +
                    field_id + "));")

            db.execSQL("CREATE TABLE IF NOT EXISTS $table_exchange_rates (" +
                    "$field_currency TEXT NOT NULL UNIQUE, " +
                    "$field_value REAL NOT NULL);")

            db.execSQL("INSERT INTO $table_exchange_rates ($field_currency, $field_value) " +
                    "VALUES ('RUB', 1.0), " +
                    "('USD', 73.4154), " +
                    "('EUR', 89.8788), " +
                    "('SGD', 55.3543), " +
                    "('MYR', 17.7105), " +
                    "('AUD', 56.9387), " +
                    "('JPY', 0.67471), " +
                    "('CHN', 11.4600), " +
                    "('HKD', 9.45707), " +
                    "('CAD', 60.9124), " +
                    "('INR', 1.00723), " +
                    "('DKK', 12.0858), " +
                    "('GBP', 103.953), " +
                    "('NZD', 103.953), " +
                    "('MXN', 3.69342), " +
                    "('IDR', 0.00512), " +
                    "('TWD', 2.63147), " +
                    "('THB', 2.33914), " +
                    "('VND', 0.00318) "

            )

            db.setTransactionSuccessful()
        }
        finally {
            db.endTransaction()
        }
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.beginTransaction()
        try {
            db.execSQL("DROP TABLE IF EXISTS $table_accounts")
            db.execSQL("DROP TABLE IF EXISTS $table_transactions")
            db.execSQL("DROP TABLE IF EXISTS $table_exchange_rates")
            db.setTransactionSuccessful()
        }
        finally {
            db.endTransaction()
        }
        onCreate(db)
    }


}