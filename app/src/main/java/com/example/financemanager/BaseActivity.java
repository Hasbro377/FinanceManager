package com.example.financemanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;


public class BaseActivity extends AppCompatActivity {
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);  // Устанавливаем общий layout

        // Настройка Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Настройка Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Toggle для открытия/закрытия Drawer (гамбургер-иконка)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Обработчик кликов по пунктам меню
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Переход к экрану кошельков
                startActivity(new Intent(this, startActivity.class));
            } else if (id == R.id.nav_report) {
                // Переход к отчетам
                startActivity(new Intent(this, reportActivity.class));
            } else if (id == R.id.nav_budget) {
                // Переход к бюджетированию
                startActivity(new Intent(this, BudgetListActivity.class));
            }else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, Settings.class));
            }
            drawerLayout.closeDrawers();  // Закрываем Drawer после выбора
            return true;
        });
    }

    // Метод для установки контента в FrameLayout (вызывайте в дочерних Activity)
    protected void setContentViewInFrame(int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
    }
}
