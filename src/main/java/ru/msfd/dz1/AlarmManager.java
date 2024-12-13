package ru.msfd.dz1;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import ru.msfd.common.AlarmData;
import ru.msfd.common.AlarmListItemAdapter;

public class AlarmManager extends AppCompatActivity {

    public final static String ALARM_DATA_EXTRAS = "ALARM_DATA_EXTRAS";
    public final static String ALARM_ARRAY_POS = "ALARM_ARRAY_POS_EXTRAS";
    public final static String ALARM_REMOVE_FLAG = "ALARM_REMOVE_FLAG";

    public final static String ALARM_INTENT_FILTER = "igor.alarm.ALARM_UPDATE";

    public final static String ALARM_TAG = "ALARM";
    public final static String ALARM_PREFERENCES_TAG = "ALARM_PREFERENCES";
    private ArrayList<AlarmData> alarmsData;
    private int selectedItemPosition = -1;
    private AlarmListItemAdapter adapter;
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(!isGranted)
                    Toast.makeText(this, "Разрешение было отклонено пользователем", Toast.LENGTH_LONG).show();
            });

    private BroadcastReceiver alarmUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if(intent != null && intent.getAction() != null)
//            {
//                Log.d("Alarm Service", "Получен бродкаст с действием: " + intent.getAction());
//            }

            if (intent != null && intent.getAction() != null && intent.getAction().equals(ALARM_INTENT_FILTER))
            {
                String alarmStatus = intent.getStringExtra(AlarmService.BroadcastResultStatus);
                Log.d("AlarmManager", "Обратная связь от сервиса: " + alarmStatus);
                Toast.makeText(AlarmManager.this, alarmStatus, Toast.LENGTH_SHORT).show();
                alarmsData = LoadAlarmsData();
                SetupAlarmList();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alarm_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Log.d("Alarm Manager", "Бродкаст зарегистрирован");
        IntentFilter filter = new IntentFilter(ALARM_INTENT_FILTER);
        registerReceiver(alarmUpdateReceiver, filter);
        Setup();
    }

    private void CheckSelfPermissions()
    {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void Setup()
    {
        //Log.d("Alarm Manager", "сетуп");
        CheckSelfPermissions();
        alarmsData = LoadAlarmsData();
        SetupAlarmList();
        SetupButtons();
        SaveAlarmsData();
    }

    @Override
    public void onDestroy()
    {
        //Log.d("Alarm Manager", "он дестрой");
        this.SaveAlarmsData();
        unregisterReceiver(alarmUpdateReceiver);
        super.onDestroy();
    }

    private void SetupAlarmList() {
        //Log.d("Alarm Manager", "сетуп аларм лист");
        Log.d("AlarmManager", "Установка данных списка");

        RecyclerView recyclerView = findViewById(R.id.alarms_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlarmListItemAdapter(this.alarmsData, position -> selectedItemPosition = position, this);
        recyclerView.setAdapter(adapter);
    }

    private void SaveAlarmsData()
    {
        //Log.d("Alarm Manager", "сейв алармс дата");
        Log.d("AlarmManager", "Сохранение данных будильников");

        SharedPreferences preferences = getSharedPreferences(ALARM_PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        for (AlarmData alarmData : alarmsData) editor.putString(alarmData.getAlarmUUID().toString(), alarmData.toString());
//        int i = 0;
//        for (AlarmData alarmData : alarmsData)
//            editor.putString(ALARM_TAG + i++, alarmData.toString());
        editor.commit();
    }

    private ArrayList<AlarmData> LoadAlarmsData()
    {
        Log.d("AlarmManager", "Загрузка данных будильников");

        ArrayList<AlarmData> data = new ArrayList<>();
        try
        {
            SharedPreferences preferences = getSharedPreferences(ALARM_PREFERENCES_TAG, MODE_PRIVATE);
            Map<String, ?> alarmsMapData = preferences.getAll();
            AlarmData alarmData;
            for (Map.Entry<String, ?> alarmMapData : alarmsMapData.entrySet())
            {
                try { Log.d("Alarm Manager", "Данные будильника: " + alarmMapData.getKey() + alarmMapData.getValue().toString()); }
                catch (Exception e) { Log.d("Alarm Manager", "ошибка полученяи данных будильника"); }
                alarmData = AlarmData.fromString( (alarmMapData.getValue() instanceof String) ? (String) alarmMapData.getValue() : null);
                if(alarmData != null) data.add(alarmData);
            }
//            SharedPreferences preferences = getSharedPreferences(ALARM_PREFERENCES_TAG, MODE_PRIVATE);
//            int i = 0;
//            AlarmData currentData;
//            while (preferences.contains(ALARM_TAG + i))
//            {
//                currentData = AlarmData.fromString(preferences.getString(ALARM_TAG + i++, ""));
//                if (currentData != null) data.add(currentData);
//            }
//            while (preferences.contains(ALARM_TAG + i))
//            {
//                String alarmString = preferences.getString(ALARM_TAG + i++, "");
//                currentData = AlarmData.fromString(alarmString);
//                if (currentData != null) data.add(currentData);
//            }
            Log.d("AlarmManager", "Всего получено данных будильников: " + String.valueOf(data.size()));
            for (AlarmData a: data)  Log.d("AlarmManager", a.toString());
            return data;
        }
        catch(Exception e)
        {
            return data;
        }
    }

    private void SetupButtons()
    {
        //Log.d("Alarm Manager", "сетуп баттонс");

        Button addAlarmButton = (Button) findViewById(R.id.add_alarm_button);
        Button removeAlarmButton = (Button) findViewById(R.id.remove_alarm_button);

        addAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlarmData newAlarm = new AlarmData();
                Log.d("AlarmManager", "Добавление будильника " + (selectedItemPosition + 1) + " " + newAlarm.toString());

                alarmsData.add(newAlarm);
                adapter.notifyItemInserted(alarmsData.size() - 1);
                SaveAlarmsData();
            }
        });

        removeAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedItemPosition > -1 && selectedItemPosition < alarmsData.size())
                {
                    AlarmData alarmData = alarmsData.get(selectedItemPosition);
                    Log.d("AlarmManager", "Удаление будильника " + selectedItemPosition + " " + alarmData.toString());
                    alarmsData.remove(selectedItemPosition);
                    adapter.notifyItemRemoved(selectedItemPosition);
                    adapter.notifyItemRangeChanged(selectedItemPosition, alarmsData.size());

//                    SharedPreferences.Editor editor = getSharedPreferences(ALARM_PREFERENCES_TAG, MODE_PRIVATE).edit();
//                    alarmData.setIsActivated(false);
//                    editor.remove(AlarmManager.ALARM_TAG + selectedItemPosition);
//                    editor.commit();
                    Intent alarmIntent = new Intent(AlarmManager.this, AlarmService.class);
                    //alarmData.setIsActivated(false);
                    alarmIntent.putExtra(AlarmManager.ALARM_DATA_EXTRAS, alarmData.toString());
                    //alarmIntent.putExtra(AlarmManager.ALARM_ARRAY_POS, selectedItemPosition);
                    alarmIntent.putExtra(AlarmManager.ALARM_REMOVE_FLAG, true);
                    //startService(alarmIntent);
                    startForegroundService(alarmIntent);
                }
            }
        });
    }
}