package com.omar.dftar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        try {
            JSONObject o = new JSONObject(intent.getStringExtra("json"));
            Reminders.show(c, o);
            if (o.optBoolean("daily")) {
                long at = o.getLong("at");
                long now = System.currentTimeMillis();
                while (at <= now) {
                    at += Reminders.DAY;
                }
                o.put("at", at);
                Reminders.schedule(c, o, at);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
