package com.example.financemanager;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class Settings extends BaseActivity  {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // ✅ Вызываем super.onCreate()
        EdgeToEdge.enable(this);
        setContentViewInFrame(R.layout.activity_settings);  // ✅ Как в примере

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        // Загружаем состояние уведомлений при запуске
        loadNotificationState();
    }
    private void loadNotificationState() {
        SwitchMaterial switchNotifications = findViewById(R.id.switch_notifications);
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);
    }

    // Метод для кнопки "Изменить пароль" (из XML android:onClick="buttonChangePass")
    public void buttonChangePass(View view) {
        showChangePasswordDialog();
    }

    // Метод для кнопки "Добавить кошелек" (из XML android:onClick="addWallet")
    public void addWallet(View view) {
        showQuickAddDialog("wallet");
    }

    // Метод для кнопки "Добавить категорию" (из XML android:onClick="addCategory")
    public void addCategory(View view) {
        showQuickAddDialog("category");
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить пароль");

        // TODO: Создайте layout dialog_change_password.xml с полями:
        // editTextOldPassword, editTextNewPassword, editTextConfirmPassword
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        final EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
        final EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        final EditText editTextConfirmPassword = dialogView.findViewById(R.id.editTextConfirmPassword);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String oldPassword = editTextOldPassword.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();
            Toast.makeText(Settings.this, "Пароль изменен", Toast.LENGTH_SHORT).show();

        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Стилизация кнопок как в вашем коде
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


    private void showQuickAddDialog(String type) {
        String title = type.equals("wallet") ? "Добавить кошелек" : "Добавить категорию";
        String hint = type.equals("wallet") ? "Название кошелька" : "Название категории";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setTitle(title)
                .setView(input)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        // TODO: Добавить в базу данных
                        Toast.makeText(this, type.equals("wallet") ?
                                "Кошелек '" + name + "' добавлен" :
                                "Категория '" + name + "' добавлена", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private View createPasswordInputView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        EditText oldPassword = new EditText(this);
        oldPassword.setHint("Старый пароль");
        oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText newPassword = new EditText(this);
        newPassword.setHint("Новый пароль");
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("Подтвердите пароль");
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(oldPassword);
        layout.addView(newPassword);
        layout.addView(confirmPassword);

        return layout;
    }

    // Обработка кнопки "Назад"
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
