package com.example.financemanager;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class reportActivity extends BaseActivity {

    EditText textFromDate, textBeforeDate;
    PieChart pieChartExpense, pieChartIncome;
    BarChart barChartStacked;  // Новый график
    String selectedWallet;
    TabLayout tabLayoutWallets;

    DBhelper dbHelper;  // объект для работы с БД
    int userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentViewInFrame(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Инициализация элементов
        textFromDate = findViewById(R.id.textFromDate);
        textBeforeDate = findViewById(R.id.textBeforeDate);
        tabLayoutWallets = findViewById(R.id.tab_layout_wallets);
        pieChartExpense = findViewById(R.id.pieChartExpense);
        pieChartIncome = findViewById(R.id.pieChartIncome);
        barChartStacked = findViewById(R.id.barChartStacked);  // Инициализация BarChart
        dbHelper = new DBhelper(this);


        userID = UserSession.getCurrentUserId();    //получение ид пользователя
        // Получаем список кошельков (предполагаю, userId - поле из BaseActivity или другого места)
        List<String> walletNames = dbHelper.getAllWallet(userID);  // Ваш метод, возвращает List<String>

        if (walletNames.isEmpty()) {
            Toast.makeText(this, "Нет доступных кошельков", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем вкладки для каждого кошелька
        for (String wallet : walletNames) {
            TabLayout.Tab tab = tabLayoutWallets.newTab().setText(wallet);
            tabLayoutWallets.addTab(tab);
        }

        // Обработчик выбора вкладки
        tabLayoutWallets.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedWallet = tab.getText().toString();
                updateReportData();  // Обновляем данные при выборе
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Инициализация: выбираем первую вкладку
        selectedWallet = walletNames.get(0);
        tabLayoutWallets.getTabAt(0).select();  // Выбираем первую
        updateReportData();

        setTextDate();
    }

    // Метод для обновления данных отчета
    private void updateReportData() {
        showExpense();
        showIncome();
        showStackedBarChart();  // Добавьте вызов для BarChart
        String balance = dbHelper.getWalletBalanceAndCurrency(userID, selectedWallet);
        getSupportActionBar().setTitle(balance);

    }

    // Новый метод для Stacked Bar Chart
    private void showStackedBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Получаем данные по категориям
        List<String> categories = dbHelper.getCategoriesForWallet(selectedWallet, userID);
        if (categories.isEmpty()) {
            barChartStacked.clear();  // Очищаем график, если нет данных
            barChartStacked.setNoDataText("Нет данных для отображения");
            barChartStacked.invalidate();
            return;
        }

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            float income = dbHelper.getSumForCategoryAndType("Доход", category, selectedWallet, userID);
            float expense = dbHelper.getSumForCategoryAndType("Расход", category, selectedWallet, userID);
            entries.add(new BarEntry(i, new float[]{expense, income}));
            labels.add(category);  // Добавляем название категории
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(new int[]{Color.RED, Color.GREEN});
        dataSet.setStackLabels(new String[]{"Расходы", "Доходы"});

        BarData barData = new BarData(dataSet);
        barChartStacked.setData(barData);

        // Настройка горизонтальной оси (XAxis)
        XAxis xAxis = barChartStacked.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));  // Названия категорий
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);  // Шаг между метками
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(labels.size());  // Количество меток = количеству категорий
        xAxis.setTextSize(12f);  // Размер текста
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);  // Убираем сетку
        xAxis.setLabelRotationAngle(-45f);  // Поворот текста для длинных названий


        // Настройка вертикальной оси (YAxis)
        YAxis leftAxis = barChartStacked.getAxisLeft();
        leftAxis.setTextSize(12f);
        leftAxis.setTextColor(Color.BLACK);
        barChartStacked.getAxisRight().setEnabled(false);  // Отключаем правую ось

        barChartStacked.getDescription().setEnabled(false);
        barChartStacked.setFitBars(true);  // Подгонка столбцов
        barChartStacked.invalidate();  // Обновление графика
    }


    private void showExpense() {
        String walletName = selectedWallet;
        ArrayList<PieEntry> entries = dbHelper.getDataForPieChart("Расход", walletName);

        PieDataSet dataSet = new PieDataSet(entries, "Расход");
        dataSet.setColors(new int[]{
                Color.parseColor("#FF8A65"), // Темный оранжевый
                Color.parseColor("#D32F2F"), // Темный красный
                Color.parseColor("#7B1FA2"), // Темный фиолетовый
                Color.parseColor("#1976D2"), // Темный синий
                Color.parseColor("#388E3C"), // Темный зеленый
                Color.parseColor("#FBC02D"), // Темный желтый
                Color.parseColor("#8D6E63"), // Темный коричневый
                Color.parseColor("#455A64")  // Темный серо-голубой
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(16f);

        PieData pieData = new PieData(dataSet);
        pieChartExpense.setData(pieData);
        pieChartExpense.invalidate();
        pieChartExpense.getDescription().setEnabled(false);
    }

    private void showIncome() {
        String walletName = selectedWallet;  // Используем selectedWallet
        ArrayList<PieEntry> entries = dbHelper.getDataForPieChart("Доход", walletName);

        PieDataSet dataSet = new PieDataSet(entries, "Доход");
        dataSet.setColors(new int[]{
                Color.parseColor("#6BCB77"), // Темный мятный
                Color.parseColor("#A3C9A8"), // Темный светло-зеленый
                Color.parseColor("#FFB74D"), // Темный персиковый
                Color.parseColor("#FF6F61"), // Темный коралловый
                Color.parseColor("#D50032"), // Темный красный
                Color.parseColor("#FF8A65"), // Темный оранжевый
                Color.parseColor("#B0BEC5"), // Темный серо-голубой
                Color.parseColor("#A0522D"), // Темный коричневый
                Color.parseColor("#7B1FA2"), // Темный фиолетовый
                Color.parseColor("#FF7043")  // Темный розовато-красный
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(16f);

        PieData pieData = new PieData(dataSet);
        pieChartIncome.setData(pieData);
        pieChartIncome.invalidate();
        pieChartIncome.getDescription().setEnabled(false);
    }

    private void setTextDate() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String selectedDate = day + "." + (month + 1) + "." + year;
        textBeforeDate.setText(selectedDate);
        day = 1;
        selectedDate = day + "." + (month + 1) + "." + year;
        textFromDate.setText(selectedDate);
    }

    public void setFromDate(View view) {
        String dateString = textFromDate.getText().toString();
        String[] dateParts = dateString.split("\\.");

        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int year = Integer.parseInt(dateParts[2]);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        String selectedDate = selectedDay + "." + (selectedMonth + 1) + "." + selectedYear;
                        textFromDate.setText(selectedDate);
                        updateReportData();  // Обновляем отчет при изменении даты
                    }
                }, year, (month - 1), day);
        datePickerDialog.show();
    }

    public void setBeforeDate(View view) {
        String dateString = textBeforeDate.getText().toString();
        String[] dateParts = dateString.split("\\.");
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int year = Integer.parseInt(dateParts[2]);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        String selectedDate = selectedDay + "." + (selectedMonth + 1) + "." + selectedYear;
                        textBeforeDate.setText(selectedDate);
                        updateReportData();  // Обновляем отчет при изменении даты
                    }
                }, year, (month - 1), day);
        datePickerDialog.show();
    }


}