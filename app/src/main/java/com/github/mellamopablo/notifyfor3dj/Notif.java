package com.github.mellamopablo.notifyfor3dj;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
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
    public Notif(Context context, int id, Mention m, final NotifReadyCallback callback) {
        this.id = id;
        final NotificationCompat.Builder notif = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_placeholder) //TODO replace with logo
                .setContentTitle(context.getString(R.string.notification_title, m.user.name))
                .setContentText(context.getString(R.string.notification_desc_text, m.msg.thread))
                //.setLargeIcon(MentionParser.getBitmapFromURL(m.user.lowres_avatar_url))
                .setColor(Color.argb(255, 255, 87, 34)) //App primary color
                .setGroup("3DJ_Mentions") //This will group different mentions in Nougat
                .setAutoCancel(true);

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_FILE,
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

        // Get the user avatar and add it to the notificaiton.
        // We can't use m.user.lowres_avatar_url because it's low resolution.
        // If, for some reason, this fails, that's ok, launch the noitification
        // without large iconanyways.
        if (/*prefs.getBoolean("avatar", true)*/ false) { // TODO implement this option back
            m.getAvatarURl(new Mention.GetAvatarCallback() {
                @Override
                public void onSuccess(String avatar_url) {
                    MentionParser.getBitmapFromURL(avatar_url,
                            new MentionParser.GetBitmapCallback() {
                                @Override
                                public void onSuccess(Bitmap avatar) {
                                    notif.setLargeIcon(avatar);
                                    Notif.this.nBuilder = notif;
                                    callback.onReady(Notif.this);
                                }

                                @Override
                                public void onFailure() {
                                    Notif.this.nBuilder = notif;
                                    callback.onReady(Notif.this);
                                }
                            }
                    );
                }

                @Override
                public void onFailure() {
                    Notif.this.nBuilder = notif;
                    callback.onReady(Notif.this);
                }
            });
        } else {
            this.nBuilder = notif;
            callback.onReady(this);
        }

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
                .setColor(Color.argb(255, 255, 87, 34)) //App primary color
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

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_FILE,
                Context.MODE_PRIVATE);
        if(!prefs.getBoolean("silent", false))
            summaryNotif.setDefaults(Notification.DEFAULT_SOUND);

        //Load 3DJuegos' mention page on tap
        Intent summaryResultIntent = new Intent(Intent.ACTION_VIEW);
        summaryResultIntent.setData(Uri.parse(mention_page_url));

        PendingIntent summaryPending = PendingIntent.getActivity(context, 0,
                summaryResultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        summaryNotif.setContentIntent(summaryPending);

        this.nBuilder = summaryNotif;
    }

    /**
     * Builds an "Update available" mention
     *
     * @param context the Android context
     * @param id      the id of the notification
     * @param latest  the latest version
     */
    public Notif(Context context, int id, Version latest, String apk) {
        this.id = id;
        NotificationCompat.Builder updateNotif = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_placeholder) //TODO replace with logo
                .setContentTitle(context.getString(R.string.notification_update_available))
                .setContentText(context.getString(R.string.notification_update_desc, latest.string))
                .setColor(Color.argb(255, 255, 87, 34)) //App primary color
                .setAutoCancel(true);

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.SHARED_PREFS_FILE,
                Context.MODE_PRIVATE);
        if (!prefs.getBoolean("silent", false))
            updateNotif.setDefaults(Notification.DEFAULT_SOUND);

        //Download new apk on tap
        Intent summaryResultIntent = new Intent(Intent.ACTION_VIEW);
        summaryResultIntent.setData(Uri.parse(apk));

        PendingIntent summaryPending = PendingIntent.getActivity(context, 0,
                summaryResultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        updateNotif.setContentIntent(summaryPending);

        this.nBuilder = updateNotif;
    }

    public void send(NotificationManager nMgr) {
        nMgr.notify(id, nBuilder.build());
    }

    public interface NotifReadyCallback {
        void onReady(Notif notif);
    }
}