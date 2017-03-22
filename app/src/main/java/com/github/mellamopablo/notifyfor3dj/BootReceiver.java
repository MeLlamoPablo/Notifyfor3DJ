package com.github.mellamopablo.notifyfor3dj;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * By default, Android alarms are cancelled when the device shuts down. Because of that, we need to
 * reactivate them with this BootReceiver when the device boots.
 * <p/>
 * Aditionally, we need to perform one initial mention check, because if we don't, the first check
 * will come after the first alarm is triggered.
 */
public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            final SharedPreferences prefs = context.getSharedPreferences(
                    MainActivity.SHARED_PREFS_FILE, Context.MODE_PRIVATE
            );

            try {
                Intent getMentions = new Intent(context, DisplayMentionsService.class);
                context.startService(getMentions);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MainActivity.restartAlarm(context, prefs.getLong(
                    "frequency", AlarmManager.INTERVAL_HOUR
            ));
        }
    }
}
