package com.github.mellamopablo.notifyfor3dj;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import cz.msebera.android.httpclient.Header;

public class Mention {

    public User user;
    public Message msg;
    public Time time;
    public int id;
    String delete_url = "http://www.3djuegos.com/modulos/comunidad/foro.php?get_info=elimina_" +
            "mencion&zona=info_foro_menciones&page=0&tipo_mencion=foro&id_mensaje=";

    public Mention(String username, String avatar_url, String profile_url, String msg_url,
                   String thread, String thatLongAgo, int timestamp, int id) {
        this.user = new User(username, avatar_url, profile_url);
        this.msg = new Message(msg_url, thread);
        this.time = new Time(thatLongAgo, timestamp);
        this.id = id;
    }

    /**
     * This is only used when we want do delete the mention and don't need to manipulate its data.
     * @param id The message id
     */
    public Mention(int id) {
        this.id = id;
    }

    /**
     * Sends a GET request to 3DJuegos to delete the mention
     *
     * @param context Android Context
     * @throws Exception
     */
    public void delete(Context context) throws Exception {
        CookieManager cm = new CookieManager(context.getSharedPreferences(
                MainActivity.shared_prefs_file, Context.MODE_PRIVATE));

        AsyncHttpClient client = new AsyncHttpClient();

        final PersistentCookieStore cookies = new PersistentCookieStore(context);
        client.setCookieStore(cookies);

        try {
            cookies.addCookie(cm.getCookie("recordar"));
            cookies.addCookie(cm.getCookie("recordar2"));
        } catch (Exception e) {
            //Remember me cookies aren't saved.
            //The user hasn't logged in yet or has deleted them.
            throw new Exception("User is not logged in (Can't find session cookies)");
        }

        client.get(delete_url + String.valueOf(this.id), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d("Mentions", "Mention deleted");
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d("Mentions", "Couldn't delete");
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });
    }

    public class User {
        public String name;
        public String avatar_url;
        public String profile_url;

        public User(String username, String avatar_url, String profile_url) {
            this.name = username;
            this.avatar_url = avatar_url;
            this.profile_url = profile_url;
        }
    }

    public class Message {
        public String url;
        public String thread;

        public Message(String url, String thread) {
            this.url = url;
            this.thread = thread;
        }
    }

    public class Time {
        public String thatLongAgo; //Example: "Hace 3 días".
        public int unixTimestamp;

        public Time(String thatLongAgo, int timestamp) {
            this.thatLongAgo = thatLongAgo;
            this.unixTimestamp = timestamp;
        }
    }
}
