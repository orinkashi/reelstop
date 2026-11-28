package ma.waqf.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BedtimeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        Bedtime.channel(c);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 1, open, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, Bedtime.CH) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_moon)
         .setContentTitle("باقي 30 دقيقة على النعاس 🌙")
         .setContentText("حط التليفون يشارجي بعيد على الفراش، وبدا توجد.")
         .setContentIntent(pi).setAutoCancel(true);
        try { ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, b.build()); } catch (SecurityException e) { }
        Bedtime.schedule(c);
    }
}
