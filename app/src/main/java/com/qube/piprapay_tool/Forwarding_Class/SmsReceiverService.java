package com.qube.piprapay_tool.Forwarding_Class;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.qube.piprapay_tool.Activity.MainActivity;
import com.qube.piprapay_tool.R;

public class SmsReceiverService extends Service {

    BroadcastReceiver receiver;
    private static final String CHANNEL_ID = "SmsDefault";
    public static final String ACTION_STOP_SERVICE = "STOP_SERVICE";
    private static final int NOTIFICATION_ID = 1;

    private static SmsReceiverService instance = null;

    public SmsReceiverService() {
        receiver = new SmsBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        } else {
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        startForeground(NOTIFICATION_ID, buildNotification("Service Running", "Ready to forward SMS"));
    }

    private Notification buildNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent stopIntent = new Intent(this, SmsReceiverService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent pStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent appIntent = new Intent(this, MainActivity.class);
        PendingIntent pAppIntent = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ant_round)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(pAppIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", pStopIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(getResources().getColor(R.color.main_color))
                .build();
    }

    public static void updateStatus(Context context, String title, String content) {
        if (instance != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, instance.buildNotification(title, content));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            // Ignore if not registered
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}