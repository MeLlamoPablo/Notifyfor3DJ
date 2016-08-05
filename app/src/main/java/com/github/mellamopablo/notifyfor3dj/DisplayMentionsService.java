package com.github.mellamopablo.notifyfor3dj;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class DisplayMentionsService extends Service {
    public DisplayMentionsService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Check for updates
            final Context context = this;
            SharedPreferences prefs = getSharedPreferences(MainActivity.shared_prefs_file,
                    Context.MODE_PRIVATE);
            if (prefs.getBoolean("update", true)) {
                Version.getLatest(new Version.VersionCallback() {
                    @Override
                    public void onResponse(final Version latest) {
                        if (Version.current.compareTo(latest) == Version.LESS) {
                            Version.getApkUrl(latest, new Version.ApkCallback() {
                                @Override
                                public void onResponse(String apk) {
                                    Notif notif = new Notif(context, 0, latest, apk);
                                    NotificationManager nMgr = (NotificationManager)
                                            getSystemService(NOTIFICATION_SERVICE);
                                    notif.send(nMgr);
                                }
                            });
                        }
                    }
                });
            }

            // We display mentions after checking for updates just in case a change in 3DJuegos' end
            // makes our app crash, so that the users know when a fix is released.
            displayMentions();

            /*//TODO remove
            Version.getChangelog(new Version.ChangelogCallback() {
                @Override
                public void onResponse(String changelog_html) {
                    Log.d("CHANGELOG GOT", changelog_html);
                }
            });*/

        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_NOT_STICKY;
    }

    /**
     * This method uses Mention.getAll() to get all user mentions and issues notifications if the
     * user has got new ones.
     *
     * @throws Exception
     */
    public void displayMentions() throws Exception {
        final Context context = this;

        Mention.getAll(this, new Mention.GetMentionsCallback() {
            @Override
            public void onResponse(List<Mention> allMentions) {
                /*
                Users might not remove read mentions from 3DJuegos' website, but we can't notify
                them with their already read mentions. Therefore we need to save every mention
                that goes to a notification in our DB.

                Then, when we check for mentions, we compare every mention to the DB's mentions.
                If there is any that wasn't there, it must be new. So we send the user a
                notification and save that one as well.

                Users can opt out from this behaviour (checkbox_db)
                 */
                List<Mention> newMentions = new ArrayList<>();

                SharedPreferences prefs = getSharedPreferences(MainActivity.shared_prefs_file,
                        Context.MODE_PRIVATE);
                boolean useDb = prefs.getBoolean("db", true);

                if (useDb) {
                    for (Mention m : allMentions) {
                        if (!m.existsInDb(context))
                            newMentions.add(m);
                    }
                } else {
                    newMentions = allMentions;
                }

                /*
                If we have only one new mention, we display it. However, if we have more than
                one we set a summary notification with InboxStyle that displays the first 5

                This notification will be the only displayed in Marshmallow and below. In
                Nougat, the user will be able to expand the summary notification to discover
                all the individual ones.
                */

                final NotificationManager nMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                /*
                If the user choses not to use the DB option and hasn't cancelled their
                notifications, they will duplicate, as the user will still have mentions.
                Cancel every notification and issue them again for that not to happen.
                */
                if (!useDb) nMgr.cancelAll();

                if (newMentions.size() == 1) {
                    Mention m = newMentions.get(0);
                    new Notif(context, MainActivity.lastNotifId + 1, m, new Notif.NotifReadyCallback() {
                        @Override
                        public void onReady(Notif notif) {
                            notif.send(nMgr);
                        }
                    });

                    MainActivity.lastNotifId++;
                    m.saveToDb(context);
                } else if (newMentions.size() > 1) {
                    Notif summaryNotif = new Notif(context, MainActivity.lastNotifId + 1, newMentions);
                    MainActivity.lastNotifId++;

                    summaryNotif.send(nMgr);

                    for (Mention m : newMentions) {
                        new Notif(
                                context,
                                MainActivity.lastNotifId + 1,
                                m,
                                new Notif.NotifReadyCallback() {
                                    @Override
                                    public void onReady(Notif notif) {
                                        notif.send(nMgr);
                                    }
                                }
                        );

                        MainActivity.lastNotifId++;
                        m.saveToDb(context);
                    }
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
