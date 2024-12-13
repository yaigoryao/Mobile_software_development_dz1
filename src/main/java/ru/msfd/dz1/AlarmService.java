package ru.msfd.dz1;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.msfd.common.AlarmData;

public class AlarmService extends Service {
    private final CharSequence ChannelName = "Alarm Service Channel";
    private final String ChannelDescription = "Channel for alarm service";
    private final String ChannelId = "ALARM_CHANNEL_ID";
    public final static String BroadcastResultStatus = "alarm_status";

    private HashMap<UUID, AlarmTask> alarmsExetutors;
    private NotificationChannel notificationChannel;
    private NotificationManager notificationManager;

    @Override
    public void onCreate()
    {
        super.onCreate();
        alarmsExetutors = new HashMap<>();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(ChannelId, ChannelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(ChannelDescription);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                //Log.d("Alarm Service", "Канал уведомлений создан");
            } else {
                //Log.e("Alarm Service", "Ошибка: notificationManager равен null при создании канала");
            }
        }
    }

    public AlarmService() { }

    private Notification createAlarmStartNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannel.getId())
                .setContentTitle("Будильник")
                .setContentText("Сервис активен")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //Log.d("Alarm Service", "запущен сервис");
        //Log.d("Alarm Service", intent.getStringExtra(AlarmManager.ALARM_DATA_EXTRAS));
        if (startId == 1) startForeground(UUID.randomUUID().hashCode(), createAlarmStartNotification());

        AlarmData alarmData = AlarmData.fromString(intent.getStringExtra(AlarmManager.ALARM_DATA_EXTRAS));
        //int alarmArrayPos = intent.getIntExtra(AlarmManager.ALARM_ARRAY_POS, -1);
        boolean removeAlarm = intent.getBooleanExtra(AlarmManager.ALARM_REMOVE_FLAG, false);
        if(alarmData != null) Log.d("AlarmService", "Получены данные: " + alarmData.toString() + " " + removeAlarm);
        else Log.d("AlarmService", "Ошибка получения данных!!!");
        //Log.d("Alarm Service", String.valueOf(alarmArrayPos) + " позиция в списке ПОЛУЧЕНА");
        if(alarmData == null)
        {
            Toast.makeText(this, "Ошибка включения будильника, проверьте данные!", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }
        if (removeAlarm) RemoveAlarm(alarmData);
        else if (alarmData.getIsActivated()) SetAlarm(alarmData);
        else StopAlarm(alarmData);
        return START_REDELIVER_INTENT;
    }

    private void SetAlarm(AlarmData alarmData) //, int alarmArrayPos
    {
        Log.d("AlarmService", "Установка будильника " + alarmData.toString());

        if(alarmData == null) return;
        if(alarmsExetutors == null) return;
        if(alarmsExetutors.containsKey(alarmData.getAlarmUUID())) return;
        else
        {
            alarmsExetutors.put(alarmData.getAlarmUUID(), new AlarmTask(getApplicationContext()));
            alarmsExetutors.get(alarmData.getAlarmUUID()).start( GetAlarmStopTime(alarmData), () ->
                    {
                        final String resultStatus = "Будильник сработал";
                        Log.d("AlarmService", resultStatus + alarmData.toString());

                        AlarmTask task = alarmsExetutors.get(alarmData.getAlarmUUID());
                        if(task != null) task.stop();
                        if(alarmsExetutors.containsKey(alarmData.getAlarmUUID())) alarmsExetutors.remove(alarmData.getAlarmUUID());

                        if (notificationManager != null)
                        {
                            boolean canSend = true;
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            {
                                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) canSend = false;
                            }
                            if(canSend)
                            {
                                notificationManager.notify(UUID.randomUUID().hashCode(), CreateNotificationsBuilder(resultStatus, alarmData.getSoundId())
                                        .setDeleteIntent(PendingIntent.getService(getApplicationContext(), alarmData.getAlarmUUID().hashCode(),
                                                new Intent(getApplicationContext(), MediaPlayerService.class).setAction(MediaPlayerService.STOP_PLAYING_ACTION),
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                                        .build());
                                Intent startIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
                                startIntent.putExtra(MediaPlayerService.MEDIA_ID, alarmData.getSoundId());
                                startIntent.setAction(MediaPlayerService.START_PLAYING_ACTION);
                                startService(startIntent);
                                Log.d("AlarmService", "Уведомление отправлено");
                            }
                            else Log.e("AlarmService", "Разрешение на уведомления не предоставлено");
                        }
                        else  Log.e("AlarmService", "notificationManager не инициализирован");
                        SharedPreferences preferences = getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        alarmData.setIsActivated(false);
                        editor.remove(alarmData.getAlarmUUID().toString());
                        //editor.remove(AlarmManager.ALARM_TAG + alarmArrayPos);
                        editor.commit();
                        Log.d("AlarmService", "Осталось записей ПОСЛЕ СРАБАТЫВАНИЯ: " + String.valueOf(preferences.getAll().size()));
                        Intent broadcastIntent = new Intent(AlarmManager.ALARM_INTENT_FILTER);
                        broadcastIntent.putExtra(BroadcastResultStatus, resultStatus);
                        try
                        {
                            sendBroadcast(broadcastIntent);
                            Log.d("AlarmService", "бродкаст отправлен");
                        } catch (Exception e) {
                            Log.e("Alarm Service", "Ошибка при отправке бродкаста", e);
                        }
                        TryStopForegroundService();
                    }
            );
            PrintPrefs(getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE), "УСТАНОВКА");
        }
    }

    private NotificationCompat.Builder CreateNotificationsBuilder(String message, int mediaId)
    {
        return new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Оповещение")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDeleteIntent(createStopPlaybackIntent());
    }

    private PendingIntent createStopPlaybackIntent()
    {
        Intent stopIntent = new Intent(this, MediaPlayerService.class);
        stopIntent.setAction(MediaPlayerService.STOP_PLAYING_ACTION);
        return PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void TryStopForegroundService()
    {
        Log.d("AlarmService", "попытка остановки сервиса");
        if(alarmsExetutors.isEmpty())
        {
            Log.d("AlarmService", "Сервис остановлен");
            stopForeground(true);
            stopSelf();
        }
    }

    private void StopAlarm(AlarmData alarmData) // int alarmArrayPos,
    {
        Log.d("AlarmService", "Остановка будильника " + alarmData.toString() );
        if(alarmData == null) return;
        if(alarmsExetutors == null) return;
        if(!alarmsExetutors.containsKey(alarmData.getAlarmUUID())) return;
        else
        {
            Log.d("AlarmService", "Будильник ОТКЛЮЧЕН");

            AlarmTask task = alarmsExetutors.get(alarmData.getAlarmUUID());
            if(task != null) task.stop();
            if(alarmsExetutors.containsKey(alarmData.getAlarmUUID())) alarmsExetutors.remove(alarmData.getAlarmUUID());

            SharedPreferences preferences = getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            alarmData.setIsActivated(false);
            editor.putString(alarmData.getAlarmUUID().toString(), alarmData.toString());
            //editor.putString(AlarmManager.ALARM_TAG + alarmArrayPos, alarmData.toString());
            editor.commit();
            Log.d("AlarmService", "Осталось записей ПОСЛЕ ОТКЛЮЧЕНИЯ: " + String.valueOf(preferences.getAll().size()));

            PrintPrefs(getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE), "УСТАНОВКА");

            Intent broadcastIntent = new Intent(AlarmManager.ALARM_INTENT_FILTER);
            broadcastIntent.putExtra("alarm_status", "Будильник выключен");
            sendBroadcast(broadcastIntent);
            TryStopForegroundService();
        }
    }

    private void PrintPrefs(SharedPreferences preferences, String message)
    {
        for (Map.Entry<String, ?> alarmMapData : preferences.getAll().entrySet())
        {
            try { Log.d("AlarmService", message + " Данные будильника: " + alarmMapData.getValue().toString()); }
            catch (Exception e) { Log.d("Alarm Manager", "ошибка полученяи данных будильника"); }
        }
    }

    private void RemoveAlarm(AlarmData alarmData) // int alarmArrayPos,
    {
        Log.d("AlarmService", "Удаление будильника " + alarmData.toString());

        if(alarmData == null) return;
        if(alarmsExetutors != null)
        {
            if(alarmsExetutors.containsKey(alarmData.getAlarmUUID()))
            {
                AlarmTask task = alarmsExetutors.get(alarmData.getAlarmUUID());
                if(task != null) task.stop();
                alarmsExetutors.remove(alarmData.getAlarmUUID());
            }
        }
        SharedPreferences preferences = getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(alarmData.getAlarmUUID().toString());
        editor.commit();
        Log.d("AlarmService", "Осталось записей ПОСЛЕ УДАЛЕНИЯ: " + String.valueOf(preferences.getAll().size()));
        PrintPrefs(getSharedPreferences(AlarmManager.ALARM_PREFERENCES_TAG, MODE_PRIVATE), "УДАЛЕНИЕ");
        Intent broadcastIntent = new Intent(AlarmManager.ALARM_INTENT_FILTER);
        broadcastIntent.putExtra("alarm_status", "Будильник удален");
        sendBroadcast(broadcastIntent);
        TryStopForegroundService();
    }

    private long GetAlarmStopTime(AlarmData alarmData)
    {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime alarmTime = now.withHour(alarmData.getAlarmTime().getHour())
                .withMinute(alarmData.getAlarmTime().getMinute())
                .withSecond(0)
                .withNano(0);

        if (now.isAfter(alarmTime))  alarmTime = alarmTime.plusDays(1);
        return ChronoUnit.MILLIS.between(now, alarmTime);
    }

    @Override
    public void onDestroy()
    {
        Log.d("AlarmService", "Разрушение сервиса");
        for (AlarmTask task : alarmsExetutors.values()) task.stop();
        alarmsExetutors.clear();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public class AlarmTask
    {
        private ScheduledExecutorService executorService;
        private boolean isRunning;

        private PowerManager.WakeLock wakeLock;
        //private Handler handler;

        public AlarmTask(Context context)
        {
            this.executorService = Executors.newSingleThreadScheduledExecutor();
            this.isRunning = false;
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "AlarmsManager::WakeLockTag" + System.currentTimeMillis());
        }

        public void start(long delayInMillis, Runnable onFinish)
        {
            if (isRunning) return;
            isRunning = true;
            wakeLock.acquire();
//            handler = new Handler();
//            handler.postDelayed(() ->
//            {
//                isRunning = false;
//                onFinish.run();
//                if (wakeLock.isHeld()) wakeLock.release();
//            }, delayInMillis);
            executorService.schedule(() ->
            {
                isRunning = false;
                onFinish.run();
                if (wakeLock.isHeld())  wakeLock.release();

            }, delayInMillis, TimeUnit.MILLISECONDS);
        }

        public void stop()
        {
            if (!isRunning) return;
            isRunning = false;

            //handler.postDelayed( () -> {}, 0);
            //handler.removeCallbacksAndMessages(null);

            executorService.shutdownNow();
            if (wakeLock.isHeld())  wakeLock.release();
        }

        public boolean isRunning() { return isRunning; }

        public void releaseResources()
        {
            executorService.shutdown();
            if (wakeLock.isHeld())  wakeLock.release();
        }
    }
}