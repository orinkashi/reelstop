package com.omar.nghma;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/** Service sghir kaykhalli l'app 7ya f l'arrière-plan bach lmusic ma t7bsch */
public class PlayService extends Service {
    static final String CH = "nghma_play";
    private PowerManager.WakeLock lock;
    private boolean started = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        if (started) return START_STICKY;
        started = true;

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Lecture", NotificationManager.IMPORTANCE_LOW));

        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Nghma")
                .setContentText("Lmusic khddama")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }

        PowerManager pm = getSystemService(PowerManager.class);
        lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nghma:play");
        lock.acquire(6 * 60 * 60 * 1000L);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (lock != null && lock.isHeld()) lock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
