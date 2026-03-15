package com.example.financemanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class startActivity extends BaseActivity  {      //стартовая страница с таблицей

    RecyclerView recyclerView;      //объект для вывода элементов таблицы
    Spinner spinWallet;             //объект для раскрывающегося списка

    ArrayList<String> date, category, summ, description;    //объекты для хранения данных из БД
    DBhelper dbHelper;              //объект для работы с БД
    MyAdapter adapter;              //адаптер для работы RecyclerView

    String selectedWallet;          //название кошелька
    int userID;

    // Новые переменные для сортировки
    private String currentSortColumn = "date";  // Текущая колонка сортировки (date, category, summ)
    private boolean isAscending = false;  // Направление: true - ASC, false - DESC
    private TextView textViewDate, textViewCategory, textViewSum;  // Ссылки на TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {        //стандартный метод, запускается для начала работы программы
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentViewInFrame(R.layout.activity_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DBhelper(startActivity.this);          //инициализация класса для работы с БД

        int spinnerIndex = getIntent().getIntExtra("spinnerIndex", 0);//получение индекса выбранного кошелька
        userID = UserSession.getCurrentUserId();    //получение ид пользователя

        date = new ArrayList<>();                                      //инициализация элементов для хранения данных из БД
        category = new ArrayList<>();
        summ = new ArrayList<>();
        description = new ArrayList<>();

        spinWallet = findViewById(R.id.spinWallet);                   //инициализация раскрывающегося списка

        // Инициализация TextView для заголовков
        textViewDate = findViewById(R.id.textView3);
        textViewCategory = findViewById(R.id.textView4);
        textViewSum = findViewById(R.id.textView5);

        List<String> categoriesWallets = dbHelper.getAllWallet(userID);       //получение списков для Spinner
        ArrayAdapter<String> adapterWallet = new ArrayAdapter<>(startActivity.this, android.R.layout.simple_spinner_item, categoriesWallets);     //определение Адаптера для раскрывающегося списка
        adapterWallet.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);       //опредление внешнего вида элементов
        spinWallet.setAdapter(adapterWallet);           //подкючение Адаптера к раскрывающемуся списку
        spinWallet.setSelection(spinnerIndex);          //выбор переданного кошелька в раскрывающемся списке
        spinWallet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {//обработка нажатий на элемент раскрывающегося списка
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                displayData();                          //запуск метода для обновления данных
                selectedWallet = parent.getItemAtPosition(position).toString();
                dbHelper.updateWalletBalanceByName(userID, selectedWallet);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {            }
        });

        adapter = new MyAdapter(startActivity.this, date, category, summ, description, spinWallet);   //инициализация адаптера для RecyclerView
        recyclerView = findViewById(R.id.recyclerView); //инициализация RecyclerView
        recyclerView.setAdapter(adapter);               //подключение адаптера к RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(startActivity.this));       //определение внешнего вида элементов



        // Инициализация сортировки по умолчанию
        updateSortIndicators();  // Установить стрелку на начальную сортировку
        displayData();
    }

    // Метод для обновления текста заголовков со стрелками
    private void updateSortIndicators() {
        String arrow = isAscending ? " ↑" : " ↓";  // Стрелка вверх для ASC, вниз для DESC
        textViewDate.setText("Дата" + (currentSortColumn.equals("date") ? arrow : ""));
        textViewCategory.setText("Категория" + (currentSortColumn.equals("category") ? arrow : ""));
        textViewSum.setText("Сумма" + (currentSortColumn.equals("summ") ? arrow : ""));
    }

    // Обработчики нажатий для сортировки
    public void onSortByDate(View view) {
        if (currentSortColumn.equals("date")) {
            isAscending = !isAscending;  // Переключить направление
        } else {
            currentSortColumn = "date";
            isAscending = true;  // Начать с ASC
        }
        updateSortIndicators();
        displayData();
    }

    public void onSortByCategory(View view) {
        if (currentSortColumn.equals("category")) {
            isAscending = !isAscending;
        } else {
            currentSortColumn = "category";
            isAscending = true;
        }
        updateSortIndicators();
        displayData();
    }

    public void onSortBySum(View view) {
        if (currentSortColumn.equals("summ")) {
            isAscending = !isAscending;
        } else {
            currentSortColumn = "summ";
            isAscending = true;
        }
        updateSortIndicators();
        displayData();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void displayData() {
        date.clear();
        category.clear();
        summ.clear();
        description.clear();

        List<String> wallets = dbHelper.getAllWallet(userID);
        if (wallets == null || wallets.isEmpty()) {
            showCreateWalletDialogForced();
            return;
        }

        String selectedWallet = spinWallet.getSelectedItem() != null
                ? spinWallet.getSelectedItem().toString()
                : wallets.get(0);

        String balance = dbHelper.getWalletBalanceAndCurrency(userID, selectedWallet);
        toolbar.setTitle(balance != null ? balance : "Баланс: 0 ₽");

        String sortBy;
        String orderDirection = isAscending ? " ASC" : " DESC";

        if (currentSortColumn.equals("date")) {
            // YYYYMMDD для дат DD.MM.YYYY
            sortBy = "(substr(Transactions.date, 7, 4) || substr(Transactions.date, 4, 2) || substr(Transactions.date, 1, 2))" + orderDirection + ", Transactions.id DESC";

        } else if (currentSortColumn.equals("category")) {
            sortBy = "category" + orderDirection + ", Transactions.id DESC";
        } else if (currentSortColumn.equals("summ")) {
            // ЧИСЛОВАЯ сортировка суммы (TEXT как число)
            sortBy = "CAST(Transactions.summ AS REAL)" + orderDirection + ", Transactions.id DESC";
        } else {
            sortBy = "Transactions.id" + orderDirection;
        }

        Cursor cursor = dbHelper.getTransactions(userID, selectedWallet, sortBy);
        if (cursor != null && cursor.getCount() > 0) {
            int dateIndex = cursor.getColumnIndex("date");
            int categoryIndex = cursor.getColumnIndex("category");
            int summIndex = cursor.getColumnIndex("summ");
            int descriptionIndex = cursor.getColumnIndex("description");

            while (cursor.moveToNext()) {
                date.add(cursor.getString(dateIndex) != null ? cursor.getString(dateIndex) : "");
                category.add(cursor.getString(categoryIndex) != null ? cursor.getString(categoryIndex) : "");
                summ.add(cursor.getString(summIndex) != null ? cursor.getString(summIndex) : "");
                description.add(cursor.getString(descriptionIndex) != null ? cursor.getString(descriptionIndex) : "");
            }
        }
        if (cursor != null) cursor.close();

        adapter.notifyDataSetChanged();
    }


    // ✅ ОБЯЗАТЕЛЬНЫЙ ДИАЛОГ (НЕ ЗАКРЫВАЕТСЯ)
    private void showCreateWalletDialogForced() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создайте кошелек");
        builder.setCancelable(false); // ❌ Нельзя закрыть по Back/Outside

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_wallet, null);
        builder.setView(dialogView);

        final EditText editTextWalletName = dialogView.findViewById(R.id.textWalletName);
        final EditText editTextCurrency = dialogView.findViewById(R.id.editTextCurrency);

        // ❌ Убираем стандартные кнопки, добавляем свои
        builder.setPositiveButton("Сохранить", null); // null = не закрывает диалог
        builder.setNegativeButton("Отмена", null);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false); // ❌ Нельзя закрыть касанием вне диалога
        dialog.show();

        // ✅ Перехватываем кнопки ПОСЛЕ show()
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Сохранить - только если данные введены
        positiveButton.setOnClickListener(v -> {
            String walletName = editTextWalletName.getText().toString().trim();
            String currency = editTextCurrency.getText().toString().trim();

            if (!walletName.isEmpty() && !currency.isEmpty()) {
                dbHelper.saveWalletToDatabase(userID, walletName, currency, this);
                List<String> wallets = dbHelper.getAllWallet(userID);
                spinWallet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wallets));
                dialog.dismiss(); // ✅ Закрываем только после создания
                displayData(); // Обновляем данные
            } else {
                Toast.makeText(this, "Введите все данные", Toast.LENGTH_SHORT).show();
            }
        });

        // Отмена - ничего не делаем, диалог остается открыт
        negativeButton.setOnClickListener(v -> {
            Toast.makeText(this, "Создайте кошелек для продолжения", Toast.LENGTH_SHORT).show();
        });

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.color_green)); // #10B981
            positiveButton.setTypeface(null, Typeface.BOLD);
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.color_red)); // #EF4444
        }
    }


    public void fooButMinus(View view) {                                                //кнопка Расходы
        Intent intent = new Intent(this, MainActivity.class);               //для запуска нового Activity
        intent.putExtra("selectButton", "Расход");                         //передача названия кнопки "Расходы"
        intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition());    //Передача индекса выбранного кошелька в раскрывающемся списке
        startActivity(intent);                                                          //запуск Activity
    }
    public void fooButPlus(View view) {                                                 //кнопка Доходы
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selectButton", "Доход");
        intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition());
        startActivity(intent);
    }

}
