package ru.msfd.dz1;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService extends Service {

    public final static String STOP_PLAYING_ACTION = "STOP_PLAYBACK";
    public final static String START_PLAYING_ACTION = "START_PLAYBACK";
    public final static String MEDIA_ID = "mediaId";
    public MediaPlayerService() {
    }

    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("MediaPlayerService", "Пришел запрос");

        if (intent != null)
        {
            String action = intent.getAction();
            if (STOP_PLAYING_ACTION.equals(action)) stopPlayback();
            else if (START_PLAYING_ACTION.equals(action))
            {
                int mediaId = intent.getIntExtra(MEDIA_ID, -1);
                startPlayback(mediaId);
            }
        }
        return START_NOT_STICKY;
    }

    private void startPlayback(int mediaId)
    {
        Log.d("MediaPlayerService", "Запускаем музыку");

        if(mediaId == -1) return;
        if (mediaPlayer == null)
        {
            try {
                Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, audioUri);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                //mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
                Log.d("MediaPlayerService", "Включение");

                mediaPlayer.start();
            } catch (IOException e) {
                Log.e("MediaPlayerService", "Ошибка при воспроизведении аудио", e);
            }
        }
    }

    private void stopPlayback()
    {
        Log.d("MediaPlayerService", "Выключаем музыку");

        if (mediaPlayer != null)
        {
            Log.d("MediaPlayerService", "Выключение");

            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}