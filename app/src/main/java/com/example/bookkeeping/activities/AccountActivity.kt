package com.example.bookkeeping.activities


import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.example.bookkeeping.DatabaseHelper
import com.example.bookkeeping.R
import okhttp3.*
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round


class AccountActivity : AppCompatActivity() {
    private lateinit var name: TextView
    private lateinit var funds: TextView
    private lateinit var currencies: Array<String>
    private lateinit var dialog : Dialog
    private lateinit var transaction: Button
    private lateinit var top_up : Button
    private lateinit var spinner : Spinner
    private lateinit var transaction_name : TextView
    private lateinit var sum : EditText
    private lateinit var comment : EditText
    private lateinit var transaction_type : String
    private lateinit var databaseHelper : DatabaseHelper
    private lateinit var list : ListView
    private lateinit var toolbar : Toolbar
    private lateinit var trash : ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        // Устанавливаем тулбар
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        databaseHelper = DatabaseHelper(this) // // Инициализация БД
        name = findViewById(R.id.account_name)
        funds = findViewById(R.id.account_funds)
        transaction = findViewById(R.id.transaction)
        top_up = findViewById(R.id.top_up)

        // Получаем значения из родительской активности
        name.text = intent.getStringExtra("name")
        funds.text = intent.getStringExtra("funds")
        initAddTransactionDialog() // Инициализация диалогового окна
        trash = toolbar.findViewById(R.id.trash)

        // Приходная транзакция
        top_up.setOnClickListener{
            transaction_type = getString(R.string.top_up_account)
            transaction_name.text = transaction_type
            dialog.show()
        }

        // Расходная транзакция
        transaction.setOnClickListener{
            transaction_type = getString(R.string.transaction)
            transaction_name.text = transaction_type
            dialog.show()
        }

        // Кнопка "Назад"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Удаление счета
        trash.setOnClickListener{
            val account = intent.getStringExtra("id")
            val alert =  AlertDialog.Builder(this)
                    .setTitle("Сообщение")
                    .setMessage("Вы действительно хотите удалить счет ?")
                    .setPositiveButton("Да") { _: DialogInterface, _: Int ->
                        val db = databaseHelper.writableDatabase
                        db.beginTransaction()

                        try {
                            db.execSQL("DELETE FROM ${databaseHelper.table_accounts} " +
                                    "WHERE ${databaseHelper.field_id} = $account;")
                            db.execSQL("DELETE FROM ${databaseHelper.table_transactions} " +
                                    "WHERE ${databaseHelper.field_account} = $account")
                            db.setTransactionSuccessful()
                        }
                        finally {
                            db.endTransaction()
                        }
                        finish()
                    }
                    .setNegativeButton("Нет"){_: DialogInterface, _: Int ->

                    }.create()
            alert.show()


        }
    }

    // Функция для получения текущей даты и времени
    private fun currentDate () : String{
        val date: Date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.US)
        return dateFormat.format(date)
    }

    override fun onResume() {
        super.onResume()
        showItems()
    }

    // Получение транзакций с БД и инициализация адаптера для ListView
    private fun showItems(){
        val account = intent.getStringExtra("id")
        list = findViewById(R.id.transactionView)
        val headers = arrayOf("date", "comment", "funds", "currency", "funds_rub" )
        val db = databaseHelper.readableDatabase

        val userCursor = db.rawQuery("SELECT * FROM ${databaseHelper.table_transactions} WHERE " +
                "${databaseHelper.field_account} = $account ORDER BY ${databaseHelper.field_id} DESC"
                ,null)
        val userAdapter =  SimpleCursorAdapter(this, R.layout.transaction_list_item,
                userCursor, headers, intArrayOf(R.id.date, R.id.comment, R.id.funds, R.id.currency, R.id.rub), 0)

        list.adapter = userAdapter
    }



    private fun initAddTransactionDialog() {
        // Инициализация диалогового окна
        val db = databaseHelper.writableDatabase
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_transaction)
        dialog.setCanceledOnTouchOutside(false)

        // Поиск элементов по id
        val okButton = dialog.findViewById<Button>(R.id.ok_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        spinner = dialog.findViewById(R.id.spinner)
        transaction_name = dialog.findViewById(R.id.transaction_name)
        sum = dialog.findViewById(R.id.sum)
        comment = dialog.findViewById(R.id.comment)

        // Адаптер для спиннера
        val adapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(this, R.array.currency,
                R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Создание новой транзакции
        okButton.setOnClickListener {
            val account = intent.getStringExtra("id")
            var sum_value: Double
            // Ошибка, если сумма пустая или равна 0
            try {
                sum_value = sum.text.toString().toDouble()
                if (sum_value == 0.0){
                    val alert =  AlertDialog.Builder(this)
                            .setTitle("Ошибка")
                            .setMessage("Введите сумму больше 0!")
                            .setPositiveButton("ОК") { _: DialogInterface, _: Int ->
                            }.create()
                    alert.show()
                    return@setOnClickListener
                }
            }
            catch (e : NumberFormatException){

                val alert =  AlertDialog.Builder(this)
                        .setTitle("Ошибка")
                        .setMessage("Введите сумму!")
                        .setPositiveButton("ОК") { _: DialogInterface, _: Int ->
                        }.create()
                alert.show()

                return@setOnClickListener
            }

            // Получаем все значения
            val currency_value = spinner.selectedItem.toString()
            val comment_value = comment.text.toString().capitalize()
            var funds_value = funds.text.toString().toDouble()
            val current_date = currentDate()



            // Находим курс валюты, которая задана пользователем
            val cursor = db.rawQuery("SELECT ${databaseHelper.field_value} FROM " +
                    "${databaseHelper.table_exchange_rates} WHERE " +
                    "${databaseHelper.field_currency} = '$currency_value'" , null)
            cursor.moveToNext()
            val exchange = cursor.getDouble(0)
            // Переводим в рубли
            var rub = sum_value.times(exchange)
            // Округляем до 2 знаков после запятой
            rub = round(rub * 100.0) / 100.0

            // Расходная транзакция
            if (transaction_type == getString(R.string.transaction)){
                // Ошибка если транзакция расходная и средств недостаточно
                if (rub > funds_value){

                    val alert =  AlertDialog.Builder(this)
                            .setTitle("Ошибка")
                            .setMessage("Недостаточно средств!")
                            .setPositiveButton("ОК") { _: DialogInterface, _: Int ->
                            }.create()
                    alert.show()
                    return@setOnClickListener
                }
                // Переводим введенную сумму в отрицательную
                sum_value *= -1
                rub *= -1

            }

            // Складываем или отнимаем текущую сумму с полученной и округляем
            funds_value += rub
            funds_value = round(funds_value * 100.0) / 100.0


            // Создаем запрос на создание новой транзакции
            db.beginTransaction()
            try {
                db.execSQL("INSERT INTO ${databaseHelper.table_transactions} (${databaseHelper.field_date}, " +
                        "${databaseHelper.field_funds_rub}, ${databaseHelper.field_funds}, " +
                        "${databaseHelper.field_currency}, ${databaseHelper.field_comment}, " +
                        "${databaseHelper.field_account}) VALUES (" +
                        "'$current_date', $rub, $sum_value, '$currency_value', " +
                        "'$comment_value', $account);")

                db.execSQL("UPDATE ${databaseHelper.table_accounts} SET " +
                        "${databaseHelper.field_funds} = $funds_value " +
                        "WHERE ${databaseHelper.field_id} = $account" )
                db.setTransactionSuccessful()
            }

            finally {
                db.endTransaction()
            }

            // Обновляем значения в интерфейсе
            funds.text = funds_value.toString()
            sum.setText("")
            comment.setText("")
            showItems()
            dialog.dismiss()
            
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

    }




}