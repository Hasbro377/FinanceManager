package com.example.financemanager;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddEditBudgetActivity extends BaseActivity {

    private EditText textDate, textPrice;
    private Spinner spinWallet, spinCategory;
    private Button butDel, butEdit, changeDate;
    private DBhelper dbHelper;
    private int budgetId = -1; // -1 для нового бюджета
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewInFrame(R.layout.activity_add_edit_budget);

        // Инициализация DBhelper
        dbHelper = new DBhelper(this);

        // Инициализация UI элементов
        textDate = findViewById(R.id.textDate);
        textPrice = findViewById(R.id.textPrice);
        spinWallet = findViewById(R.id.spinWallet);
        spinCategory = findViewById(R.id.spinCategory);
        butDel = findViewById(R.id.butDel);
        butEdit = findViewById(R.id.butEdit);
        changeDate = findViewById(R.id.changeDate);

        userId = UserSession.getCurrentUserId();

        // Получаем budgetId для редактирования
        budgetId = getIntent().getIntExtra("budget_id", -1);

        // Заполняем спиннеры
        loadWallets();
        loadCategories();

        // Если редактирование, загружаем данные
        if (budgetId != -1) {
            loadBudgetData();
        } else {
            // Для нового бюджета: устанавливаем дату на месяц вперед
            setDateOneMonthAhead();
            butDel.setVisibility(View.GONE); // Скрываем кнопку удаления для нового бюджета
        }

        // Обработчики
        changeDate.setOnClickListener(v -> setDate());
        butEdit.setOnClickListener(v -> buttonSave());
        butDel.setOnClickListener(v -> buttonDelete());
    }

    private void setDateOneMonthAhead() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);  // Добавляем 1 месяц
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String date = day + "." + (month + 1) + "." + year;
        textDate.setText(date);
    }

    private void loadWallets() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM Wallets WHERE user_id = ?", new String[]{String.valueOf(userId)});
        List<String> wallets = new ArrayList<>();
        List<Integer> walletIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            walletIds.add(cursor.getInt(0));
            wallets.add(cursor.getString(1));
        }
        cursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wallets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinWallet.setAdapter(adapter);
        spinWallet.setTag(walletIds);
    }

    private void loadCategories() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM Category", null);
        List<String> categories = new ArrayList<>();
        List<Integer> categoryIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            categoryIds.add(cursor.getInt(0));
            categories.add(cursor.getString(1));
        }
        cursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCategory.setAdapter(adapter);
        spinCategory.setTag(categoryIds);
    }

    private void loadBudgetData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT wallet_id, category_id, limitAmount, endDate, startDate FROM Budget WHERE id = ? AND user_id = ?",
                new String[]{String.valueOf(budgetId), String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            int walletId = cursor.getInt(0);
            int categoryId = cursor.getInt(1);
            setSpinnerSelection(spinWallet, walletId);
            setSpinnerSelection(spinCategory, categoryId);

            textPrice.setText(cursor.getString(2));
            textDate.setText(cursor.getString(3));
        }
        cursor.close();
    }

    private void setSpinnerSelection(Spinner spinner, int id) {
        List<Integer> ids = (List<Integer>) spinner.getTag();
        if (ids != null) {
            int position = ids.indexOf(id);
            if (position != -1) {
                spinner.setSelection(position);
            }
        }
    }

    public void setDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, day1) -> {
            String date = day1 + "." + (month1 + 1) + "." + year1;
            textDate.setText(date);
        }, year, month, day);
        datePickerDialog.show();
    }

    public void buttonSave() {
        String limitAmount = textPrice.getText().toString().trim();
        String endDate = textDate.getText().toString().trim();
        String startDate = getCurrentDate();  // startDate = сегодняшняя дата

        if (limitAmount.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> walletIds = (List<Integer>) spinWallet.getTag();
        List<Integer> categoryIds = (List<Integer>) spinCategory.getTag();
        int walletId = walletIds.get(spinWallet.getSelectedItemPosition());
        int categoryId = categoryIds.get(spinCategory.getSelectedItemPosition());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("wallet_id", walletId);
        values.put("category_id", categoryId);
        values.put("limitAmount", limitAmount);
        values.put("endDate", endDate);
        values.put("progressAmount", "0");
        values.put("startDate", startDate);

        if (budgetId == -1) {
            long result = db.insert("Budget", null, values);
            if (result != -1) {
                Toast.makeText(this, "Бюджет добавлен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка добавления", Toast.LENGTH_SHORT).show();
            }
        } else {
            int result = db.update("Budget", values, "id = ?", new String[]{String.valueOf(budgetId)});
            if (result > 0) {
                Toast.makeText(this, "Бюджет обновлен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
            }
        }
        db.close();
        finish();
    }

    private String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day + "." + (month + 1) + "." + year;
    }

    public void buttonDelete() {
        if (budgetId != -1) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int result = db.delete("Budget", "id = ? AND user_id = ?", new String[]{String.valueOf(budgetId), String.valueOf(userId)});
            if (result > 0) {
                Toast.makeText(this, "Бюджет удален", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
            }
            db.close();
            finish();
        }
    }
}
