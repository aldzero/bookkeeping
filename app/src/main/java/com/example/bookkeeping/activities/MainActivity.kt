package com.example.bookkeeping.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.example.bookkeeping.DatabaseHelper
import com.example.bookkeeping.R
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var addAccountButton : ImageButton
    private lateinit var dialog : Dialog
    private lateinit var accountName : EditText
    private lateinit var databaseHelper : DatabaseHelper
    private lateinit var list : ListView
    private lateinit var toolbar : Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        updateCurrencies()
        addAccountButton = findViewById(R.id.addAccount)
        list = findViewById(R.id.accountView)
        databaseHelper = DatabaseHelper(this)

        initAddAccountDialog()
        itemClick()

        addAccountButton.setOnClickListener {
            dialog.show()
        }


    }


    override fun onResume() {
        super.onResume()
        showItems()
    }


    private fun showItems(){
        list = findViewById(R.id.accountView)
        val headers = arrayOf("name", "funds")
        val db = databaseHelper.readableDatabase

        val userCursor = db.rawQuery("SELECT * FROM " + databaseHelper.table_accounts, null)
        val userAdapter =  SimpleCursorAdapter(this, R.layout.account_list_item,
                userCursor, headers, intArrayOf(R.id.text1, R.id.text2), 0)

        list.adapter = userAdapter
    }

    private fun itemClick(){
        list.onItemClickListener = AdapterView.OnItemClickListener{ _: AdapterView<*>, itemClicked: View, _: Int, id: Long ->

            val name : TextView = itemClicked.findViewById(R.id.text1)
            val funds : TextView = itemClicked.findViewById(R.id.text2)
            val accountActivity = Intent(this, AccountActivity::class.java)

            accountActivity.putExtra("name", name.text.toString())
            accountActivity.putExtra("funds", funds.text.toString())
            accountActivity.putExtra("id", id.toString())
            startActivity(accountActivity)

        }
    }


    private fun initAddAccountDialog(){
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_account)
        dialog.setCanceledOnTouchOutside(false)
        accountName = dialog.findViewById(R.id.editAccountName)
        val addButton = dialog.findViewById<Button>(R.id.add_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

        addButton.setOnClickListener{
            val db = databaseHelper.writableDatabase
            val name = accountName.text.toString().capitalize()
            if (name == ""){
                val alert =  AlertDialog.Builder(this)
                        .setTitle("Ошибка")
                        .setMessage("Введите название счета!")
                        .setPositiveButton("ОК") { _: DialogInterface, _: Int ->
                        }.create()
                alert.show()
                return@setOnClickListener
            }
            db.beginTransaction()
            try {
                db.execSQL("INSERT INTO accounts (name, funds) VALUES ('$name', 0);")
                db.setTransactionSuccessful()
                showItems()
            }
            catch (e: SQLiteConstraintException){

                val alert =  AlertDialog.Builder(this)
                        .setTitle("Ошибка")
                        .setMessage("Счет с таким названием уже существует!")
                        .setPositiveButton("ОК") { _: DialogInterface, _: Int ->
                        }.create()
                alert.show()

                return@setOnClickListener
            }
            finally {
                db.endTransaction();
            }
            accountName.setText("")
            dialog.dismiss()
        }
        cancelButton.setOnClickListener{
            dialog.dismiss()
        }
    }


    private fun updateCurrencies() {

        val currencies = arrayOf("SGD", "MYR", "EUR", "USD", "AUD", "JPY", "CNH", "HKD", "CAD", "INR", "DKK",
                "GBP", "RUB", "NZD", "MXN", "IDR", "TWD", "THB", "VND")
        val client = OkHttpClient()

        for (currency in currencies) {


            val request = Request.Builder()
                    .url("https://currency-exchange.p.rapidapi.com/exchange?to=RUB&from=$currency&q=1.0")
                    .get()
                    .addHeader("x-rapidapi-key", "e9d6182cccmsh661154e1e93fd18p130ea4jsnc3964b8c4715")
                    .addHeader("x-rapidapi-host", "currency-exchange.p.rapidapi.com")
                    .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val json = response.body?.string()

                        val db = databaseHelper.writableDatabase
                        db.beginTransaction()
                        try {
                            db.execSQL("UPDATE ${databaseHelper.table_exchange_rates} SET " +
                                    "${databaseHelper.field_value} = ${json?.toDouble()} WHERE " +
                                    "${databaseHelper.field_currency} = '$currency'")
                            db.setTransactionSuccessful()
                        }
                        finally{
                            db.endTransaction()
                        }

                    }
                }

            })

        }
    }

}