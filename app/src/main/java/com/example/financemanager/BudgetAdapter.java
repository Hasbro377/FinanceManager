package com.example.financemanager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private Context context;
    private List<Budget> budgetList;
    private DBhelper dbHelper;
    private int userId;
    private int walletFilterId;  // -1 для всех, иначе ID кошелька

    public BudgetAdapter(Context context, int userId, int walletFilterId) {
        this.context = context;
        this.userId = userId;
        this.walletFilterId = walletFilterId;
        this.dbHelper = new DBhelper(context);
        this.budgetList = new ArrayList<>();
        loadBudgets();
    }

    public void updateWalletFilter(int walletId) {
        this.walletFilterId = walletId;
        loadBudgets();
    }

    public void loadBudgets() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT b.id, c.name AS category_name, w.name AS wallet_name, b.limitAmount, b.endDate, b.startDate, b.progressAmount " +
                "FROM Budget b " +
                "JOIN Category c ON b.category_id = c.id " +
                "JOIN Wallets w ON b.wallet_id = w.id " +
                "WHERE b.user_id = ?";
        String[] args;
        if (walletFilterId == -1) {
            args = new String[]{String.valueOf(userId)};
        } else {
            query += " AND b.wallet_id = ?";
            args = new String[]{String.valueOf(userId), String.valueOf(walletFilterId)};
        }
        Cursor cursor = db.rawQuery(query, args);
        budgetList.clear();
        while (cursor.moveToNext()) {
            Budget budget = new Budget();
            budget.setId(cursor.getInt(0));
            budget.setCategoryName(cursor.getString(1));
            budget.setWalletName(cursor.getString(2));
            budget.setLimitAmount(cursor.getString(3));
            budget.setEndDate(cursor.getString(4));
            budget.setStartDate(cursor.getString(5));
            budget.setProgressAmount(cursor.getString(6));
            budgetList.add(budget);
        }
        cursor.close();
        db.close();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);
        holder.textCategory.setText("Категория: " + budget.getCategoryName());
        holder.textPeriod.setText("До: " + budget.getEndDate());
        holder.textStartDate.setText("От: " + budget.getStartDate());

        int progress = calculateProgress(budget);
        holder.progressBarStatus.setProgress(progress);

        // Изменено: добавлен процент в скобках после суммы
        holder.textProgressAmount.setText("Потрачено: " + budget.getProgressAmount() + " (" + progress + "%)");
        holder.textLimitAmount.setText("Лимит: " + budget.getLimitAmount());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditBudgetActivity.class);
            intent.putExtra("budget_id", budget.getId());
            context.startActivity(intent);
        });
    }

    private int calculateProgress(Budget budget) {
        try {
            double spent = Double.parseDouble(budget.getProgressAmount());
            double limit = Double.parseDouble(budget.getLimitAmount());
            if (limit == 0) return 0;
            int progress = (int) ((spent / limit) * 100);
            return progress;
        } catch (NumberFormatException e) {
            Log.e("BudgetAdapter", "Error parsing amounts: " + e.getMessage());
            return 0;
        }
    }

    private int getCategoryIdByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM Category WHERE name = ?", new String[]{name});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    private int getWalletIdByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM Wallets WHERE name = ? AND user_id = ?", new String[]{name, String.valueOf(userId)});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView textCategory, textPeriod, textStartDate, textProgressAmount, textLimitAmount;
        ProgressBar progressBarStatus;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textCategory);
            textPeriod = itemView.findViewById(R.id.textPeriod);
            textStartDate = itemView.findViewById(R.id.textStartDate);
            progressBarStatus = itemView.findViewById(R.id.progressBarStatus);
            textProgressAmount = itemView.findViewById(R.id.textProgressAmount);
            textLimitAmount = itemView.findViewById(R.id.textLimitAmount);
        }
    }

    // Внутренний класс для модели бюджета
    public static class Budget {
        private int id;
        private String categoryName;
        private String walletName;
        private String limitAmount;
        private String endDate;
        private String startDate;
        private String progressAmount;

        // Геттеры и сеттеры
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getWalletName() { return walletName; }
        public void setWalletName(String walletName) { this.walletName = walletName; }

        public String getLimitAmount() { return limitAmount; }
        public void setLimitAmount(String limitAmount) { this.limitAmount = limitAmount; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getProgressAmount() { return progressAmount; }
        public void setProgressAmount(String progressAmount) { this.progressAmount = progressAmount; }
    }
}
