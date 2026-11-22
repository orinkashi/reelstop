package com.omar.maw3id;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class Reminders {
    static final String PREF = "maw3id_rem";
    static final long DAY = 86400000L;

    static void setAll(Context c, String json) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        cancelAll(c, p.getString("list", "[]"));
        p.edit().putString("list", json).apply();
        scheduleAll(c, json);
    }

    static void rescheduleSaved(Context c) {
        scheduleAll(c, c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("list", "[]"));
    }

    static void cancelAll(Context c, String json) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                am.cancel(pending(c, a.getJSONObject(i)));
            }
        } catch (Exception e) {
            // ignore
        }
    }

    static void scheduleAll(Context c, String json) {
        try {
            JSONArray a = new JSONArray(json);
            long now = System.currentTimeMillis();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long at = o.getLong("at");
                if (o.optBoolean("daily")) {
                    while (at <= now) {
                        at += DAY;
                    }
                    o.put("at", at);
                }
                if (at > now) {
                    schedule(c, o, at);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    static PendingIntent pending(Context c, JSONObject o) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("com.omar.maw3id.REMINDER." + o.optString("id"));
        i.putExtra("json", o.toString());
        return PendingIntent.getBroadcast(c, o.optString("id").hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c, JSONObject o, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent p = pending(c, o);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    static void show(Context c, JSONObject o) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean alarm = o.optBoolean("alarm");
        String chId = alarm ? "dftar_alarm" : "dftar";
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, alarm ? "Maw3id alarme" : "Maw3id", NotificationManager.IMPORTANCE_HIGH);
            if (alarm) {
                ch.setSound(alarmSound, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ch.enableVibration(true);
                ch.setVibrationPattern(new long[]{0, 600, 400, 600, 400, 600});
            }
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent op = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, chId)
                : new Notification.Builder(c);
        if (alarm) {
            b.setCategory(Notification.CATEGORY_ALARM);
            if (Build.VERSION.SDK_INT < 26) {
                b.setSound(alarmSound);
                b.setVibrate(new long[]{0, 600, 400, 600, 400, 600});
                b.setPriority(Notification.PRIORITY_MAX);
            }
        }
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(o.optString("title", "Maw3id"))
                .setContentText(o.optString("body", ""))
                .setAutoCancel(true)
                .setContentIntent(op);
        Notification n = b.build();
        if (alarm) {
            n.flags |= Notification.FLAG_INSISTENT;
        }
        nm.notify(o.optString("id").hashCode(), n);
    }
}
