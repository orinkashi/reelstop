package ma.waqf.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

// Daily reminder 30 minutes before the chosen bedtime (inexact alarm, no special permission needed).
final class Bedtime {
    static final String CH = "bedtime";

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("waqf", Context.MODE_PRIVATE); }

    static void save(Context c, String hhmm, boolean on) {
        SharedPreferences.Editor e = prefs(c).edit().putBoolean("on", on);
        if (hhmm != null) e.putString("bed", hhmm);
        e.apply();
    }

    static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 7, new Intent(c, BedtimeReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c) {
        if (!prefs(c).getBoolean("on", false)) return;
        String[] p = prefs(c).getString("bed", "23:00").split(":");
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        t.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        t.set(Calendar.SECOND, 0);
        t.add(Calendar.MINUTE, -30);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 60000) t.add(Calendar.DAY_OF_YEAR, 1);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi(c));
    }

    static void cancel(Context c) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi(c));
    }

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "وقت النعاس", NotificationManager.IMPORTANCE_DEFAULT);
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
