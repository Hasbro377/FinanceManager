package com.example.financemanager;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DBhelper extends SQLiteOpenHelper {                         //класс для работы с БД
    public DBhelper(Context context) {                                   //конструктор (контект, имя БД, не используемые настройки, версия БД)
        super(context, "financeManager", null, 1);  //создание БД
    }
    @Override
    public void onCreate(SQLiteDatabase db) {                           //метод для создания таблиц в БД
        db.execSQL("CREATE TABLE Transactions (id INTEGER PRIMARY KEY, user_id INTEGER, wallet_id INTEGER, category_id INTEGER, summ TEXT, date TEXT, description TEXT, "+
                "FOREIGN KEY (user_id) REFERENCES Users(id), FOREIGN KEY (wallet_id) REFERENCES Wallets(id), FOREIGN KEY (category_id) REFERENCES Category(id))");   //запрос к БД на создание таблицы (строк)
        db.execSQL("CREATE TABLE Wallets (id INTEGER PRIMARY KEY, user_id INTEGER, name TEXT, currency TEXT, balance TEXT, FOREIGN KEY (user_id) REFERENCES Users(id))");
        db.execSQL("CREATE TABLE Category (id INTEGER PRIMARY KEY, user_id INTEGER, name TEXT, type TEXT, FOREIGN KEY (user_id) REFERENCES Users(id))");
        db.execSQL("CREATE TABLE Users (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL);");
        db.execSQL("CREATE TABLE Budget (id INTEGER PRIMARY KEY, user_id INTEGER, wallet_id INTEGER, category_id INTEGER, limitAmount TEXT NOT NULL, endDate TEXT NOT NULL, startDate TEXT NOT NULL, progressAmount TEXT DEFAULT '0'," +
                "FOREIGN KEY (user_id) REFERENCES Users(id), FOREIGN KEY (wallet_id) REFERENCES Wallets(id), FOREIGN KEY (category_id) REFERENCES Category(id))");


    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {  //метод для обновления при изменении версии БД
        db.execSQL("DROP TABLE IF EXISTS Transactions");                        //удаление таблиц
        db.execSQL("DROP TABLE IF EXISTS Wallets");
        db.execSQL("DROP TABLE IF EXISTS Category");
        db.execSQL("DROP TABLE IF EXISTS Users");
        db.execSQL("DROP TABLE IF EXISTS Budget");
    }
    public void updateWalletBalanceByName(int userId, String wallet_string) {         // Подсчет баланса для указанного кошелька
        SQLiteDatabase db = this.getWritableDatabase();                  //открываем БД для работы
        int walletId = getIdByName(userId,"Wallets", wallet_string);  // Подсчет баланса для указанного кошелька
        String updateBalanceQuery = "UPDATE Wallets SET balance = " +
                "(SELECT COALESCE(SUM(CASE WHEN c.type = 'Расход' THEN -CAST(t.summ AS REAL) " +
                "WHEN c.type = 'Доход' THEN CAST(t.summ AS REAL) ELSE 0 END), 0) " +
                "FROM Transactions t " +
                "JOIN Category c ON t.category_id = c.id " +
                "WHERE t.wallet_id = Wallets.id) " +
                "WHERE id = ?";

        db.execSQL(updateBalanceQuery, new Object[]{walletId});         // Выполнение запроса с параметром
    }

    public void updateAllWalletBalances() {
        SQLiteDatabase db = this.getWritableDatabase();                  // открываем БД для работы

        // Обновляем баланс для всех кошельков
        String updateBalanceQuery = "UPDATE Wallets SET balance = " +
                "(SELECT COALESCE(SUM(CASE WHEN c.type = 'Расход' THEN -CAST(t.summ AS REAL) " +
                "WHEN c.type = 'Доход' THEN CAST(t.summ AS REAL) ELSE 0 END), 0) " +
                "FROM Transactions t " +
                "JOIN Category c ON t.category_id = c.id " +
                "WHERE t.wallet_id = Wallets.id)";

        // Выполнение запроса
        db.execSQL(updateBalanceQuery);
    }
    public int getIdByName(int userId, String tableName, String name) {  // метод для получения ID категории (название таблицы, название категории)
        int id = -1;  // Инициализируем переменную, чтобы обозначить, что id не найден
        String query = "SELECT id FROM " + tableName + " WHERE name = ? AND user_id = ?";  // Используем параметризированный запрос с фильтром по userId
        SQLiteDatabase db = this.getReadableDatabase();  // открываем БД для чтения
        Cursor cursor = db.rawQuery(query, new String[]{name, String.valueOf(userId)});  // запрос для получения объекта Курсор с данными об ID
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {  // Перемещаем курсор к первой строке
                    id = cursor.getInt(0);  // Получаем значение из первого столбца
                }
            } finally {
                cursor.close();  // Закрываем курсор
            }
        }
        return id;  // Возвращаем полученное значение
    }

    public List<String> getAllCategories(int userId, String typeCategory) {  // Метод для получения всех категорий (получаем тип Доход/Расход)
        List<String> categories = new ArrayList<>();  // определяем Лист для возврата
        SQLiteDatabase db = this.getReadableDatabase();  // открываем БД
        Cursor cursor = db.rawQuery("SELECT name FROM Category WHERE type = ? AND user_id = ?", new String[]{typeCategory, String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            do {
                categories.add(cursor.getString(nameIndex));  // Получить столбец name
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();
        return categories;
    }

    public List<String> getAllWallet(int userId) {  // для получения списка Кошельков
        List<String> wallets = new ArrayList<>();  // определяем Лист
        SQLiteDatabase db = this.getReadableDatabase();  // открываем БД
        Cursor cursor = db.rawQuery("SELECT name FROM Wallets WHERE user_id = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            do {
                String name = cursor.getString(nameIndex);
                wallets.add(name);  // добавление названий кошельков в лист
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();
        return wallets;
    }
    public String getWalletBalanceAndCurrency(int userId, String walletName) {
        String result = "0 USD";  // Значение по умолчанию
        SQLiteDatabase db = this.getReadableDatabase();  // Открываем базу для чтения

        String query = "SELECT balance, currency FROM Wallets WHERE name = ? AND user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{walletName, String.valueOf(userId)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {  // Проверяем, есть ли результаты
                    int balanceIndex = cursor.getColumnIndex("balance");
                    int currencyIndex = cursor.getColumnIndex("currency");

                    // Проверка на -1
                    if (balanceIndex == -1 || currencyIndex == -1) {
                        Log.e("DB", "Столбцы не найдены: balanceIndex=" + balanceIndex + ", currencyIndex=" + currencyIndex);
                        return result;  // Возвращаем значение по умолчанию
                    }

                    double balance = cursor.getDouble(balanceIndex);
                    String currency = cursor.getString(currencyIndex);
                    result = String.format("%.2f %s", balance, currency);  // Форматируем строку
                }
            } finally {
                cursor.close();  // Закрываем курсор
            }
        }
        db.close();  // Закрываем базу
        return result;  // Возвращаем результат
    }

    public Boolean insertTransactions(int userId, String wallet_String, String category_String, String summ, String date, String description) {  // метод для записи данных в БД
        SQLiteDatabase db = this.getWritableDatabase();         // вызывается метод на текущий экземпляр класса для записи в БД, если она не создана создается
        // возвращается объект класса SQLiteDatabase для работы с БД
        int wallet_id = getIdByName(userId,"Wallets", wallet_String);  // Предполагаем, что getIdByName обновлен для фильтрации по userId (если нет, обновите его аналогично)
        int category_id = getIdByName(userId,"Category", category_String);
        if (wallet_id == -1 || category_id == -1) Log.d("Log.m", "Не найдена категория/кошелек при записи данных в Transactions");
        ContentValues contentValues = new ContentValues();      // Создание объекта для хранения пар ключ-значение для вставки данных в БД
        contentValues.put("user_id", userId);                   // Добавлено для связи с пользователем
        contentValues.put("wallet_id", wallet_id);              // метод для добавления данных в объект
        contentValues.put("category_id", category_id);
        contentValues.put("summ", summ);
        contentValues.put("date", date);
        contentValues.put("description", description);
        long result = db.insert("Transactions", null, contentValues);   // метод возвращает кол-во вставленных строк, или -1, если ошибка
        db.close();  // Закрываем базу
        if (result == -1) return false;
        else return true;
    }

    public Cursor getTransactions(int userId, String selectedWallet, String sortBy) {  // Добавлен параметр sortBy
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT Transactions.date, " +
                "Category.name AS category, " +
                "Transactions.summ, " +
                "CASE " +
                "   WHEN Transactions.description IS NOT NULL AND Transactions.description != '' THEN '|...' " +
                "   ELSE '|   ' " +
                "END AS description " +
                "FROM Transactions " +
                "JOIN Wallets ON Transactions.wallet_id = Wallets.id " +
                "JOIN Category ON Transactions.category_id = Category.id " +
                "WHERE Wallets.name = ? AND Transactions.user_id = ? AND Wallets.user_id = ? AND Category.user_id = ? " +
                "ORDER BY " + sortBy;  // Добавлена сортировка
        Cursor ret = db.rawQuery(query, new String[]{selectedWallet, String.valueOf(userId), String.valueOf(userId), String.valueOf(userId)});
        return ret;
    }// метод для извлечения данных из БД


    public String getTypeCategory (int userId, String name){                //метод для получения типа категории Доход или Расход
        SQLiteDatabase db = this.getWritableDatabase();
        String categoryType = "";
        Cursor cursor = db.rawQuery("SELECT type FROM Category WHERE name = '"+ name +"' AND user_id =  '"+ userId +"' ", null);
        int categoryIndex = cursor.getColumnIndex("type");
        if (cursor.moveToFirst()) {
        categoryType = cursor.getString(categoryIndex);
        }
        db.close();
        cursor.close();
        return categoryType;
    }
    public String getDescription (String[] allInfo){                //метод для получения описания allInfo 0-Transactions.date 1-Category.name 2-Transactions.summ 3-Wallets.name
        SQLiteDatabase db = this.getWritableDatabase();
        String description = "";
        String query = "SELECT Transactions.description FROM Transactions " +
                "JOIN Wallets ON Transactions.wallet_id = Wallets.id " +
                "JOIN Category ON Transactions.category_id = Category.id " +
                "WHERE Transactions.date = ? AND Category.name = ? AND Transactions.summ = ? AND Wallets.name = ?";
        Cursor cursor = db.rawQuery(query, allInfo);
        int descriptionIndex = cursor.getColumnIndex("description");
        if (cursor.moveToFirst()) {
            description = cursor.getString(descriptionIndex);
        }
        cursor.close();
        db.close();
        return description;
    }
    public int deleteTransaction(int userId, int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "id = ? AND user_id = ?";
        String[] whereArgs = new String[]{String.valueOf(id), String.valueOf(userId)};

        int rowsDeleted = db.delete("Transactions", whereClause, whereArgs);

        db.close();
        return rowsDeleted;
    }

    public int updateTransaction(int userId, int id, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result;
        String whereClause = "id = ? AND user_id = ?";
        String[] whereArgs = new String[]{String.valueOf(id), String.valueOf(userId)};
        result = db.update("Transactions", values, whereClause, whereArgs);
        db.close();
        return result;
    }

    public int getIDTransaction(int userId, String date, String wallet_String, String category_String, String summ, String description) {
        SQLiteDatabase db = this.getReadableDatabase();  // Используем getReadableDatabase для чтения
        int id = -1;
        String query = "SELECT id FROM Transactions WHERE " +
                "user_id = ? AND " +  // Добавлено условие по userId
                "summ = ? AND " +
                "date = ? AND " +
                "description = ? AND " +
                "wallet_id = (SELECT id FROM Wallets WHERE name = ? AND user_id = ?) AND " +  // Добавлен user_id в подзапрос для Wallets
                "category_id = (SELECT id FROM Category WHERE name = ? AND user_id = ?)";  // Добавлен user_id в подзапрос для Category


        Cursor cursor = db.rawQuery(query, new String[] {
                String.valueOf(userId),  // Добавлено для Transactions
                summ,
                date,
                description,
                wallet_String,
                String.valueOf(userId),  // Для Wallets
                category_String,
                String.valueOf(userId)   // Для Category
        });


        if (cursor != null) {
            int idID = cursor.getColumnIndex("id");
            if (cursor.moveToFirst()) {
                id = cursor.getInt(idID);
            }
            cursor.close();
        }
        return id;
    }

    public void saveWalletToDatabase(int userId, String walletName, String currency, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("name", walletName);
        values.put("currency", currency);

        long newRowId = db.insert("Wallets", null, values);
        if (newRowId == -1) {
            Toast.makeText(context, "Ошибка при добавлении кошелька", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Кошелек добавлен", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveCategoryToDatabase(int userId, String categoryName, String categoryType, Context context) {
        SQLiteDatabase db = this.getWritableDatabase(); // открываем БД для работы

        Log.d ("Id", String.valueOf(userId));
        Log.d ("categoryName", categoryName);
        Log.d ("categoryType", categoryType);

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("name", categoryName);
        values.put("type", categoryType);

        // Вставляем новую категорию в таблицу
        long newRowId = db.insert("Category", null, values);
        if (newRowId == -1) {
            Toast.makeText(context, "Ошибка при добавлении категории", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Категория добавлена", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean isCategoryUnique(int userId, String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM Category WHERE user_id = ? AND name = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), categoryName});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        return count == 0; // Если count == 0, категория уникальна для этого пользователя
    }

    public boolean isWalletNameUnique(int userId, String walletName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM Wallets WHERE user_id = ? AND name = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), walletName});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        return count == 0; // Если count == 0, название уникально для этого пользователя
    }

    public ArrayList<PieEntry> getDataForPieChart(String type, String walletName) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Запрос для извлечения данных
        String query = "SELECT c.name, SUM(t.summ) as total " +
                "FROM Transactions t " +
                "JOIN Category c ON t.category_id = c.id " +
                "JOIN Wallets w ON t.wallet_id = w.id " +
                "WHERE c.type = ? AND w.name = ? " +
                "GROUP BY c.name";

        Cursor cursor = db.rawQuery(query, new String[]{type, walletName});

        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            int sumIndex = cursor.getColumnIndex("total");
            do {
                String categoryName = cursor.getString(nameIndex);
                float totalSum = cursor.getFloat(sumIndex);

                entries.add(new PieEntry(totalSum, categoryName));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }
    // Метод для регистрации пользователя
    public boolean registerUser (String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Проверка на существование пользователя с таким же email
        Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE email = ?", new String[]{email});
        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Пользователь с таким email уже существует
        }
        cursor.close();

        // Вставка нового пользователя
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("password", password);
        long result = db.insert("Users", null, values);
        db.close();

        return result != -1; // Возвращаем true, если вставка прошла успешно
    }
    // Метод для входа пользователя
    @SuppressLint("Range")
    public int loginUser (String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        int userId = 0; // Изначально устанавливаем ID пользователя в 0

        // Выполняем запрос для получения ID пользователя по email и password
        Cursor cursor = db.rawQuery("SELECT id FROM Users WHERE email = ? AND password = ?", new String[]{email, password});

        // Проверяем, есть ли результаты
        if (cursor != null && cursor.moveToFirst()) {
            // Извлекаем ID пользователя
            userId = cursor.getInt(cursor.getColumnIndex("id"));
        }
        // Закрываем курсор и базу данных
        cursor.close();
        db.close();

        return userId; // Возвращаем ID пользователя или 0, если не найден
    }
    // Метод для получения списка уникальных категорий для кошелька и пользователя
    public List<String> getCategoriesForWallet(String wallet, int userID) {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT Category.name FROM Transactions " +
                        "JOIN Wallets ON Transactions.wallet_id = Wallets.id " +
                        "JOIN Category ON Transactions.category_id = Category.id " +
                        "WHERE Wallets.name = ? AND Transactions.user_id = ? AND Wallets.user_id = ? AND Category.user_id = ?",
                new String[]{wallet, String.valueOf(userID), String.valueOf(userID), String.valueOf(userID)}
        );
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));  // Category.name
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    // Метод для получения суммы по типу, категории, кошельку и пользователю
    public float getSumForCategoryAndType(String type, String category, String wallet, int userID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(Transactions.summ), 0) FROM Transactions " +
                        "JOIN Wallets ON Transactions.wallet_id = Wallets.id " +
                        "JOIN Category ON Transactions.category_id = Category.id " +
                        "WHERE Category.name = ? AND Category.type = ? AND Wallets.name = ? AND Transactions.user_id = ? AND Wallets.user_id = ? AND Category.user_id = ?",
                new String[]{category, type, wallet, String.valueOf(userID), String.valueOf(userID), String.valueOf(userID)}
        );
        float sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getFloat(0);
        }
        cursor.close();
        return sum;
    }
    // Метод для суммы по категории и месяцу
    public float getSumForCategoryMonth(String category, String month, String wallet, int userID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(Transactions.summ), 0) FROM Transactions " +
                        "JOIN Wallets ON Transactions.wallet_id = Wallets.id " +
                        "JOIN Category ON Transactions.category_id = Category.id " +
                        "WHERE Category.name = ? AND substr(Transactions.date, 4, 7) = ? AND Wallets.name = ? AND Transactions.user_id = ? AND Wallets.user_id = ? AND Category.user_id = ?",
                new String[]{category, month, wallet, String.valueOf(userID), String.valueOf(userID), String.valueOf(userID)}
        );
        float sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getFloat(0);
        }
        cursor.close();
        return sum;
    }
    // Метод для обновления progressAmount при транзакции
    public void updateBudgetProgress(int userId, String wallet_String, String categoryString, String amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        int categoryId = getIdByName(userId, "Category", categoryString);  // Получаем ID категории
        int wallet_id = getIdByName(userId,"Wallets", wallet_String);
        // Обновляем progressAmount только для бюджета с matching userId, categoryId и walletId
        String query = "UPDATE Budget SET progressAmount = CAST((CAST(progressAmount AS REAL) + ?) AS TEXT) WHERE user_id = ? AND category_id = ? AND wallet_id = ?";
        db.execSQL(query, new Object[]{amount, userId, categoryId, wallet_id});
        db.close();
    }

    public void insertRandomTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        Random random = new Random();
        int userId = 1;
        int walletCount = 5;
        int categoryIncomeCount = 3;  // 2 категории дохода
        int categoryExpenseCount = 8; // 9 категорий расходов

        db.beginTransaction();
        try {
            for (int i = 0; i < 10; i++) {
                int walletId = 1 + random.nextInt(walletCount); // случайный кошелек 1..5
                int categoryId;
                String summ;

                // Случайная дата с 01.07.2025 по 31.12.2025
                int month = 7 + random.nextInt(6); // месяцы 7..12
                int day;
                if (month == 10 || month == 12 || month == 7) {
                    day = 1 + random.nextInt(31); // июль (7), октябрь (10), декабрь (12) - 31 день
                } else {
                    day = 1 + random.nextInt(30); // август, сентябрь, ноябрь - 30 дней
                }
                String date = String.format(Locale.getDefault(), "%02d.%02d.2025", day, month);

                // Вероятность дохода 1 к 10
                if (random.nextInt(10) == 0) {  // 10% доходы
                    categoryId = 1 + random.nextInt(categoryIncomeCount); // категории 1..2
                    double amount = 500 + (1500 * random.nextDouble());
                    summ = String.format(Locale.US, "%.2f", amount);
                } else {
                    categoryId = categoryIncomeCount + 1 + random.nextInt(categoryExpenseCount); // расходы 3..11
                    double amount = 1 + (349 * random.nextDouble());
                    summ = String.format(Locale.US, "%.2f", amount);
                }

                String sql = "INSERT INTO Transactions (user_id, wallet_id, category_id, summ, date, description) VALUES (" +
                        userId + ", " +
                        walletId + ", " +
                        categoryId + ", '" +
                        summ + "', '" +
                        date + "', " +
                        "''" + // пустое описание
                        ")";
                db.execSQL(sql);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }



}