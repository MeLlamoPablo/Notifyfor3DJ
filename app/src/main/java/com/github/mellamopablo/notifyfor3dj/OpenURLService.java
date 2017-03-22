package com.github.mellamopablo.notifyfor3dj;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

public class OpenURLService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();

        String url = (String) extras.get("url");
        int id = (int) extras.get("id");

        //Open the message in browser
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

        //Delete the mention
        SharedPreferences prefs = getSharedPreferences(MainActivity.SHARED_PREFS_FILE,
                Context.MODE_PRIVATE);
        if(prefs.getBoolean("delete", true)){
            Mention m = new Mention(id);
            try {
                m.delete(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
