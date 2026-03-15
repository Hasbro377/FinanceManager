package com.example.financemanager;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {         //адаптер для RecyclerView

    private Context context;                                                    //объект для доступа к ресурсам приложения (строки, кнопки...)
    private ArrayList date_id, category_id, summ_id, description_id;            //объект для хранения данных
    private Spinner spinWallet;                                                 // Ссылка на внешний Spinner
    private DBhelper dbHelper;

    private int userID = UserSession.getCurrentUserId();    //получение ид пользователя
    public MyAdapter(Context context, ArrayList date_id, ArrayList category_id, ArrayList summ_id, ArrayList description_id, Spinner spinWallet) {
        this.context = context;                                                 //конструктор для определения данных
        this.date_id = date_id;
        this.category_id = category_id;
        this.summ_id = summ_id;
        this.description_id = description_id;
        this.spinWallet = spinWallet;                                            // Инициализация внешнего Spinner
        this.dbHelper = new DBhelper(context); // Инициализация DBHelper

    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {   //создает представление для вывода данных, само индивидуальное окно
        View v = LayoutInflater.from(context).inflate(R.layout.userentry,parent,false); //метод возвращает объект View из файла XML-разметки, макет для записей
        return new MyViewHolder(v);                                                     //отправляем объект в метод для связи данных и визуального жлемента
    }
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {          //установка текста в элементе
        holder.date_id.setText(String.valueOf(date_id.get(position)));
        holder.category_id.setText(String.valueOf(category_id.get(position)));
        holder.summ_id.setText(String.valueOf(summ_id.get(position)));
        holder.description_id.setText(String.valueOf(description_id.get(position)));

        String category = (String) category_id.get(position);
        String categoryType = dbHelper.getTypeCategory(userID, category);
        // Устанавливаем цвет фона в зависимости от типа категории
        if (categoryType.equals("Расход")) {
            holder.itemView.setBackgroundColor(Color.argb(255, 255, 119, 119));
        } else {
            holder.itemView.setBackgroundColor(Color.argb(144, 119, 255, 119));
        }



    }
    @Override
    public int getItemCount() {
        return date_id.size();                                                           //возврат количества объектов в списке
    }
    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView date_id, category_id, summ_id, description_id;                         //определение используемых элементов
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            date_id = itemView.findViewById(R.id.textDate);                             //инициализация переменных для ссылки на элементы
            category_id = itemView.findViewById(R.id.textCategory);
            summ_id = itemView.findViewById(R.id.textSumm);
            description_id = itemView.findViewById(R.id.textDescription);
            itemView.setOnClickListener(new View.OnClickListener() {                    //метод для обработки нажатия на элемент в RecyclerView
                @SuppressLint("ResourceType")
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();                                   //объявляем Контекст
                    Intent intent = new Intent(context, MainActivity.class);            //объявляем класс для запуска нового Activity
                    String [] editElements = new String[3];                             //массив для хранения данных для передачи
                    editElements[0] = date_id.getText().toString();                      // Дата
                    editElements[1] = category_id.getText().toString();                  // Категория
                    editElements[2] = summ_id.getText().toString();                      // Сумма

                    intent.putExtra("spinnerIndex", spinWallet.getSelectedItemPosition());  //передаем пункт активного Кошелка из раскрывающегося списка
                    intent.putExtra("allInfo", editElements);                  //передаем данне
                    context.startActivity(intent);                                          //открываем Activity
                }
            });
        }
    }
}
