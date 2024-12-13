package ru.msfd.dz1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopPlayingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(MediaPlayerService.STOP_PLAYING_ACTION.equalsIgnoreCase(intent.getAction()))
        {
            context.startService(new Intent(context, MediaPlayerService.class)
                                     .setAction(MediaPlayerService.STOP_PLAYING_ACTION));
        }
    }
}