package com.github.mellamopablo.notifyfor3dj;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class GetMentionsService extends Service {
    public GetMentionsService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            getMentions(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_NOT_STICKY;
    }

    /**
     * This method fetchs the logged user mentions from 3DJuegos and notifies the user about them.
     * Since the user needs to be logged in, isLoginNeeded() needs to be called and return false
     * for this method to work.
     *
     * @param alertOnNoMentions Whether the user should receive a snackbar letting them know if they
     *                          have no mentions. Should be used only when the user explicitly asks
     *                          for mention checking.
     *                          DOES NOT CURRENTLY WORK
     * @throws Exception
     */
    public void getMentions(final boolean alertOnNoMentions) throws Exception {
        final Context context = this;

        final CookieManager cm = new CookieManager(getSharedPreferences(MainActivity.shared_prefs_file,
                Context.MODE_PRIVATE));
        AsyncHttpClient client = new AsyncHttpClient();

        final PersistentCookieStore cookies = new PersistentCookieStore(this);
        client.setCookieStore(cookies);

        try {
            cookies.addCookie(cm.getCookie("recordar"));
            cookies.addCookie(cm.getCookie("recordar2"));
        } catch (Exception e) {
            //Remember me cookies aren't saved.
            //The user hasn't logged in yet or has deleted them.
            throw new Exception("User is not logged in (Can't find session cookies)");
        }

        client.get(this, MainActivity.mention_page_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String r = new String(responseBody);
                try {
                    List<Mention> mentions = new MentionParser(r).parse();

                    /*
                    If we have only one mention, we display it. However, if we have more than one
                    we set a summary notification with InboxStyle that displays the first 5

                    This notification will be the only displayed in Marshmallow and below. In
                    Nougat, the user will be able to expand the summary notification to discover
                    all the individual ones.
                    */

                    NotificationManager nMgr =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    // If the user hasn't cancelled their notifications, they will duplicate, as the
                    // user will still have mentions. Cancell every notification and issue them
                    // again for that not to happen.
                    nMgr.cancelAll();

                    /*if (mentions.size() == 0 && alertOnNoMentions) {
                        Snackbar.make(findViewById(R.id.layout_main),
                                R.string.snackbar_no_new_mentions, Snackbar.LENGTH_LONG)
                                .show();
                    } else */ //TODO make this somehow work
                    if (mentions.size() == 1) {
                        Mention m = mentions.get(0);
                        Notif notif = new Notif(context, MainActivity.lastNotifId + 1, m);
                        MainActivity.lastNotifId++;

                        notif.send(nMgr);
                    } else if (mentions.size() > 1) {
                        Notif summaryNotif = new Notif(context, MainActivity.lastNotifId + 1, mentions);
                        MainActivity.lastNotifId++;

                        summaryNotif.send(nMgr);

                        for (Mention m : mentions) {
                            Notif notif = new Notif(context, MainActivity.lastNotifId + 1, m);
                            MainActivity.lastNotifId++;

                            notif.send(nMgr);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
