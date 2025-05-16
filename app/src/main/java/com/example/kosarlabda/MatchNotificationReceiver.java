package com.example.kosarlabda;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MatchNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "match_notifications_channel";
    private static final String CHANNEL_NAME = "Mérkőzés értesítések";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String channelId = "match_channel_v2";  // új ID, új csatorna

        Log.d("Alarm", "Értesítés megérkezett: " + title + " - " + message);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Match Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Értesítések közelgő mérkőzésekről");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)  // vagy egy saját ikont használj
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

}
