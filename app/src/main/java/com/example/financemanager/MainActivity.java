package com.example.financemanager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.mariuszgromada.math.mxparser.*;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity {//Activity для создания записи
    EditText textDate, textPrice, textDescription;  //объект для текстовых полей
    Spinner spinWallet, spinCategory;               //объект для раскрывающегося списка
    DBhelper dbHelper;                              //объект для работы с БД

    private String selectButton;                    //переменная для хранения выбранной Кнопки
    private int spinnerIndex;                       //переменная для хранения выбранного пунка раскрывающегося списка
    private String[] allInfo;                       //переменная для хранения всей переданной информации при нажатии на RecyclerView
    private int idTransaction;

    ArrayAdapter<String> adapterCategory;           //адаптер для раскрывающегося списка Категорий
    ArrayAdapter<String> adapterWallet;

    private int userID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentViewInFrame(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        selectButton = getIntent().getStringExtra("selectButton");              //получение названия нажатой кнопки (доход/расход, income/expense)
        spinnerIndex = getIntent().getIntExtra("spinnerIndex", 0);  //получение индекса выбранного кошелька
        allInfo = getIntent().getStringArrayExtra("allInfo");               //получение данных выбранного платежа


        textDate = findViewById(R.id.textDate);                         //определение элементов в Activity
        textPrice = findViewById(R.id.textPrice);
        textDescription = findViewById(R.id.textDescription);
        spinWallet = findViewById(R.id.spinWallet);
        spinCategory = findViewById(R.id.spinCategory);

        dbHelper = new DBhelper(this);                          //подключение БД

        setTextDate();                                                  //установка текущей даты в TextView

        userID = UserSession.getCurrentUserId();    //получение ид пользователя

        if (allInfo != null) selectButton = dbHelper.getTypeCategory(userID, allInfo[1]);

        List<String> categoriesWallets = dbHelper.getAllWallet(userID);      //получение списков для Spinner
        adapterWallet = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesWallets);     //инициализация для раюоты с разскрывающимися списками
        adapterWallet.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinWallet.setAdapter(adapterWallet);
        spinWallet.setSelection(spinnerIndex);                          //установка выбранного кошелька


        List<String> categoriesCategory = dbHelper.getAllCategories(userID, selectButton);
        adapterCategory = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesCategory);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCategory.setAdapter(adapterCategory);

        // ✅ ПРОВЕРКА: если нет категорий - показываем диалог
        if (categoriesCategory.isEmpty()) {
            showNewCategoryDialog();  // Ваш переименованный метод
            return;  // Выходим, ждем создания категории
        }

        if (allInfo != null) {
            setAllInfo();
        }
    }
    private void setTextDate (){                            //метод для получения и вывода даты в textedit
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String selectedDate = day + "." + (month + 1) + "." + year;
        textDate.setText(selectedDate);
    }
    public void buttonDelete(View view) {
        if (allInfo != null ) delData(null);
        else onBackPressed();
    }
    public void buttonSave (View view){
        if (allInfo != null ) editData(view);
        else saveData(view);
    }
    public void saveData(View view) {                       //кнопка Сохранить
        String summ = textPrice.getText().toString();       //записываем данные из текстовых элементов
        String date = textDate.getText().toString();
        String description = textDescription.getText().toString();
        String selectedWallet = spinWallet.getSelectedItem().toString();
        String selectedCategory = spinCategory.getSelectedItem().toString();


Log.d("selectedCategory",selectedCategory);
        String text = mathSumm();
        if (text == "-1") {
            return;
        }else summ=text;
        summ = summ.replace(',', '.'); // Заменяем запятую на точку

        if (!summ.isEmpty() && !date.isEmpty()) {
            Boolean checkInsertData = dbHelper.insertTransactions(userID, selectedWallet, selectedCategory, summ, date, description);  //вызываем созданный метод

            if (checkInsertData) Toast.makeText(this, "Хорошо. Значения добавлены.", Toast.LENGTH_SHORT).show();    //вызываем всплывающее окно
            else Toast.makeText(this, "Ошибка. Значения НЕ добавлены", Toast.LENGTH_SHORT).show();

            dbHelper.updateBudgetProgress(userID,selectedWallet, selectedCategory, summ);  // amount — сумма расхода

            dbHelper.updateWalletBalanceByName(userID, selectedWallet);                         //обновление баланса кошелька
            Intent intent = new Intent(this, startActivity.class);          //запускаем главное Activity
            intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition()); // Передача индекса
            startActivity(intent);
        }
        else Toast.makeText(this, "Ошибка. Запишите значения", Toast.LENGTH_SHORT).show();
    }
    public String mathSumm (){

        Expression e = new Expression();    //объявление и создание объекта е для решения математических задач
        String text = textPrice.getText().toString();
        e.setExpressionString(text);            // используем метод для РЕШЕНИЕ задачи
        double answer = e.calculate();

        if (answer <= 0 || answer >= 9999999 || Double.toString(answer) == "NaN"){
            Toast.makeText(this, "Ошибка. Некорректная сумма", Toast.LENGTH_SHORT).show();
            return "-1";
        }
        DecimalFormat df = new DecimalFormat("#.##");
        text = df.format(answer);
        if (text.substring((text.length() - 2)).equals(".0")) text = text.substring(0,text.length()-2); //убирает .0 в конце целого числа
        Log.d("Log.m", String.valueOf(answer));
        return text;
    }
    public void editData(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить данные?");
        builder.setMessage("Вы уверены, что хотите изменить данные?");

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = mathSumm();
                if (text == "-1") {
                    return;
                }
                text = text.replace(',', '.'); // Заменяем запятую на точку
                ContentValues values = new ContentValues();                         // Создаем объект ContentValues для новых значений

                String selectedWallet = spinWallet.getSelectedItem().toString();
                String walletID = String.valueOf(dbHelper.getIdByName(userID,"Wallets",selectedWallet));
                values.put("wallet_id", walletID);

                String categoryID = String.valueOf(dbHelper.getIdByName(userID,"Category",spinCategory.getSelectedItem().toString()));
                values.put("category_id", categoryID);


                values.put("summ", text);
                values.put("date", textDate.getText().toString());
                values.put("description", textDescription.getText().toString());

                int result = dbHelper.updateTransaction(userID, idTransaction, values);

                if (result==1){
                    Toast.makeText(MainActivity.this, "Данные Изменены", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Ошибка при изменении", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHelper.updateAllWalletBalances();                         //обновление баланса ВСЕХ кошельков
                Intent intent = new Intent(MainActivity.this, startActivity.class);          //запускаем главное Activity
                intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition()); // Передача индекса
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Закрыть диалог
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void delData(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удалить данные?");
        builder.setMessage("Вы уверены, что хотите удалить эти данные?");

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int result = dbHelper.deleteTransaction(userID,idTransaction);
                if (result==1){
                    Toast.makeText(MainActivity.this, "Данные удалены", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHelper.updateAllWalletBalances();                         //обновление баланса ВСЕХ кошельков
                Intent intent = new Intent(MainActivity.this, startActivity.class);          //запускаем главное Activity
                intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition()); // Передача индекса
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Закрыть диалог
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void setDate(View view) {                        //метод для работы с каленадрем
        String dateString = textDate.getText().toString();
        String[] dateParts = dateString.split("\\.");
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int year = Integer.parseInt(dateParts[2]);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,      //создание Календаря
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        String selectedDate = selectedDay + "." + (selectedMonth+1) + "." + selectedYear;
                        textDate.setText(selectedDate);     // Обновляем TextView с выбранной датой
                    }
                }, year, (month-1), day);
        datePickerDialog.show();                            //вывод Календаря
    }
    void setAllInfo (){                                     //allInfo 0-textDate 1-spinCategory 2-textPrice
        textDate.setText(allInfo[0]);
        textPrice.setText(allInfo[2]);

        int position = ((ArrayAdapter<String>) spinCategory.getAdapter()).getPosition(allInfo[1]);
        if (position >= 0) {
            spinCategory.setSelection(position);
        } else {
            spinCategory.setSelection(0);
        }


        String walletName = spinWallet.getSelectedItem().toString();
        String description = dbHelper.getDescription(new String[]{allInfo[0], allInfo[1], allInfo[2], walletName});//0-Transactions.date 1-Category.name 2-Transactions.summ 3-Wallets.name

        textDescription.setText(description);

        idTransaction = dbHelper.getIDTransaction (userID, allInfo[0], walletName, allInfo[1], allInfo[2], description);

    }
    public void showAddWalletDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить новый кошелек");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_wallet, null);
        builder.setView(dialogView);

        final EditText editTextWalletName = dialogView.findViewById(R.id.textWalletName);
        final EditText editTextCurrency = dialogView.findViewById(R.id.editTextCurrency);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String walletName = editTextWalletName.getText().toString().trim();
            String currency = editTextCurrency.getText().toString().trim();

            if (!walletName.isEmpty() && !currency.isEmpty()) {
                if (dbHelper.isWalletNameUnique(userID, walletName)) {
                    dbHelper.saveWalletToDatabase(userID, walletName, currency, MainActivity.this);
                    List<String> wallets = dbHelper.getAllWallet(userID);
                    adapterWallet.clear();
                    adapterWallet.addAll(wallets);
                    adapterWallet.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Кошелек с таким названием уже существует", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Введите все данные", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Стилизация кнопок
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.color_green));
            positiveButton.setTypeface(null, Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.color_red));
        }
    }



    public void addWallet(View view) {
        showAddWalletDialog();
    }

    public void showEditCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Новая категория");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_category, null);
        builder.setView(dialogView);

        final EditText editTextCategoryName = dialogView.findViewById(R.id.editTextCategoryName);
        final RadioGroup radioGroupCategoryType = dialogView.findViewById(R.id.radioGroupCategoryType);

        Log.d("SelectButton", selectButton);
        if ("Доход".equals(selectButton)) {
            radioGroupCategoryType.check(R.id.radioButtonIncome);
        } else {
            radioGroupCategoryType.check(R.id.radioButtonExpense);
        }

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String categoryName = editTextCategoryName.getText().toString().trim();
            String categoryType = "";

            int selectedId = radioGroupCategoryType.getCheckedRadioButtonId();
            if (selectedId == R.id.radioButtonIncome) {
                categoryType = "Доход";
            } else if (selectedId == R.id.radioButtonExpense) {
                categoryType = "Расход";
            }

            if (!categoryName.isEmpty()) {
                if (dbHelper.isCategoryUnique(userID, categoryName)) {
                    dbHelper.saveCategoryToDatabase(userID, categoryName, categoryType, MainActivity.this);
                    List<String> categories = dbHelper.getAllCategories(userID, selectButton);
                    adapterCategory.clear();
                    adapterCategory.addAll(categories);
                    adapterCategory.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Категория с таким названием уже существует", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Введите название категории", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.color_green)); // #10B981
            positiveButton.setTypeface(null, Typeface.BOLD);
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.color_red)); // #EF4444
        }
    }



    public void addCategory(View view) {
        showEditCategoryDialog();
    }
    public void showNewCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Новая категория");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_category, null);
        builder.setView(dialogView);

        final EditText editTextCategoryName = dialogView.findViewById(R.id.editTextCategoryName);
        final RadioGroup radioGroupCategoryType = dialogView.findViewById(R.id.radioGroupCategoryType);

        if ("Доход".equals(selectButton)) {
            radioGroupCategoryType.check(R.id.radioButtonIncome);
        } else {
            radioGroupCategoryType.check(R.id.radioButtonExpense);
        }

        // ✅ Убираем кнопку "Отмена" - только "Сохранить"
        // builder.setNegativeButton("Отмена", null);  // УДАЛЕНО
        builder.setPositiveButton("Сохранить", null);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);  // ✅ Блокируем кнопку Назад
        dialog.setCanceledOnTouchOutside(false);  // ✅ Блокируем касание вне диалога
        dialog.show();

        // ✅ Перехватываем кнопку "Сохранить" - НЕ закрываем при пустой категории
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String categoryName = editTextCategoryName.getText().toString().trim();
            String categoryType = "";

            int selectedId = radioGroupCategoryType.getCheckedRadioButtonId();
            if (selectedId == R.id.radioButtonIncome) {
                categoryType = "Доход";
            } else if (selectedId == R.id.radioButtonExpense) {
                categoryType = "Расход";
            }

            // ✅ Если категория пустая - НЕ закрываем диалог
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Введите название категории!", Toast.LENGTH_SHORT).show();
                return;  // ✅ Остаемся в диалоге
            }

            // ✅ Сохраняем только при корректных данных
            if (dbHelper.isCategoryUnique(userID, categoryName)) {
                dbHelper.saveCategoryToDatabase(userID, categoryName, categoryType, MainActivity.this);
                List<String> categories = dbHelper.getAllCategories(userID, selectButton);
                adapterCategory.clear();
                adapterCategory.addAll(categories);
                adapterCategory.notifyDataSetChanged();
                spinCategory.setSelection(0);
                dialog.dismiss();  // ✅ Закрываем ТОЛЬКО при успехе
            } else {
                Toast.makeText(this, "Категория '" + categoryName + "' уже существует", Toast.LENGTH_SHORT).show();
                // ✅ Остаемся в диалоге
            }
        });

        // Стилизация кнопки
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.color_green));
            positiveButton.setTypeface(null, Typeface.BOLD);
        }
    }


}