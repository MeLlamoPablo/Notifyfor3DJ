package com.github.mellamopablo.notifyfor3dj;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import java.util.List;

public class Notif {

    public int id;
    String mention_page_url = "http://www.3djuegos.com/comunidad.php?zona=info_foro_menciones";
    private NotificationCompat.Builder nBuilder;

    /**
     * Builds an individual notfication
     *
     * @param context the Android context
     * @param id      the id of the notification
     * @param m       the mention from wich the data will be taken
     */
    public Notif(Context context, int id, Mention m) {
        this.id = id;
        NotificationCompat.Builder notif = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_placeholder) //TODO replace with logo
                .setContentTitle(context.getString(R.string.notification_title, m.user.name))
                .setContentText(context.getString(R.string.notification_desc_text, m.msg.thread))
                .setGroup("3DJ_Mentions") //This will group different mentions in Nougat
                .setAutoCancel(true);

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.shared_prefs_file,
                Context.MODE_PRIVATE);
        if(!prefs.getBoolean("silent", false))
            notif.setDefaults(Notification.DEFAULT_SOUND);

        //Load thread on notification tap and deletes it from 3DJuegos
        Intent resultIntent = new Intent(context, OpenURLService.class);
        resultIntent.putExtra("url", m.msg.url);
        resultIntent.putExtra("id", m.id);

        PendingIntent pending = PendingIntent.getService(context, 0,
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.setContentIntent(pending);

        this.nBuilder = notif;
    }

    /**
     * Builds a summary notification
     *
     * @param context  the Android context
     * @param id       the id of the notification
     * @param mentions a list with all the mentions from wich the data will be taken
     */
    public Notif(Context context, int id, List<Mention> mentions) {
        this.id = id;
        NotificationCompat.Builder summaryNotif = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_placeholder) //TODO replace with logo
                .setContentTitle(context.getString(R.string.notification_summary, mentions.size()))
                .setGroup("3DJ_Mentions")
                .setGroupSummary(true)
                .setAutoCancel(true);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        int i = 0;
        for (Mention m : mentions) {
            if (i < 5) {
                style.addLine(context.getString(R.string.notification_inboxstyle_line,
                        m.user.name, m.msg.thread));
                i++;
            }
        }

        summaryNotif.setStyle(style);

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.shared_prefs_file,
                Context.MODE_PRIVATE);
        if(!prefs.getBoolean("silent", false))
            summaryNotif.setDefaults(Notification.DEFAULT_SOUND);

        //Load 3DJuegos' mention page on tap
        Intent summaryResultIntent = new Intent(Intent.ACTION_VIEW);
        summaryResultIntent.setData(Uri.parse(mention_page_url));

        PendingIntent summaryPending = PendingIntent.getService(context, 0,
                summaryResultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        summaryNotif.setContentIntent(summaryPending);

        this.nBuilder = summaryNotif;
    }

    public void send(NotificationManager nMgr) {
        nMgr.notify(id, nBuilder.build());
    }
}