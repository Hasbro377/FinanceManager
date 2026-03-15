package com.example.financemanager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class BudgetListActivity extends BaseActivity {

    private RecyclerView recyclerViewBudgets;
    private Button buttonAddBudget;
    private TabLayout tabLayoutWallets;
    private BudgetAdapter budgetAdapter;
    private int userId;
    private List<Integer> walletIds = new ArrayList<>();  // Список ID кошельков для табов
    private int selectedWalletId = -1;  // Текущий выбранный ID кошелька (-1 для всех)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewInFrame(R.layout.activity_budget_list);

        userId = UserSession.getCurrentUserId();

        recyclerViewBudgets = findViewById(R.id.recyclerViewBudgets);
        buttonAddBudget = findViewById(R.id.buttonAddBudget);
        tabLayoutWallets = findViewById(R.id.tab_layout_wallets);

        recyclerViewBudgets.setLayoutManager(new LinearLayoutManager(this));
        budgetAdapter = new BudgetAdapter(this, userId, selectedWalletId);
        recyclerViewBudgets.setAdapter(budgetAdapter);

        loadWalletsIntoTabs();  // Загружаем кошельки в TabLayout

        // Обработчик выбора таба
        tabLayoutWallets.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                selectedWalletId = (position == 0) ? -1 : walletIds.get(position - 1);  // 0 - "Все", остальные - ID
                budgetAdapter.updateWalletFilter(selectedWalletId);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        buttonAddBudget.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditBudgetActivity.class);
            intent.putExtra("budget_id", -1);
            startActivity(intent);
        });
    }

    private void loadWalletsIntoTabs() {
        SQLiteDatabase db = new DBhelper(this).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM Wallets WHERE user_id = ?", new String[]{String.valueOf(userId)});
        tabLayoutWallets.addTab(tabLayoutWallets.newTab().setText("Все"));  // Первый таб - все кошельки
        while (cursor.moveToNext()) {
            walletIds.add(cursor.getInt(0));
            tabLayoutWallets.addTab(tabLayoutWallets.newTab().setText(cursor.getString(1)));
        }
        cursor.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        budgetAdapter.updateWalletFilter(selectedWalletId);  // Обновляем список с текущим фильтром
    }
}
