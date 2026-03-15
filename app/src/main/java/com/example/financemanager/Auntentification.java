package com.example.financemanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Auntentification extends AppCompatActivity {
    DBhelper dbHelper;              //объект для работы с БД
    Button buttonLogin;
    TextView textViewRegister;
    EditText editTextEmail, editTextPassword;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auntentification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        // Создание экземпляра DatabaseHelper
        dbHelper = new DBhelper(this);


    }


    public void LoginAccount(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        int userId = dbHelper.loginUser (email, password);
        // Вызов метода входа
        if (userId != 0) {
            UserSession.setCurrentUserId(userId);   // Сохраняем userid в глобальном классе
            Toast.makeText(this, "Вход успешен", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, startActivity.class);          //запускаем главное Activity
            startActivity(intent);
            finish(); // Закрываем текущую активность
        } else {
            Toast.makeText(this, "Ошибка входа", Toast.LENGTH_SHORT).show();
        }
    }

    public void showRegistrationDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Регистрация");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_registration, null);
        builder.setView(dialogView);

        final EditText editTextRegEmail = dialogView.findViewById(R.id.editTextRegEmail);
        final EditText editTextRegPassword = dialogView.findViewById(R.id.editTextRegPassword);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String email = editTextRegEmail.getText().toString().trim();
            String password = editTextRegPassword.getText().toString().trim();

            if (dbHelper.registerUser(email, password)) {
                Toast.makeText(Auntentification.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                dialog.dismiss(); // Закрываем диалог при успехе
            } else {
                Toast.makeText(Auntentification.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

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

}