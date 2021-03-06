package com.github.mellamopablo.notifyfor3dj;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class Mention {

    public User user;
    public Message msg;
    public Time time;
    public int id;
    private static final String DELETE_URL = "http://www.3djuegos.com/modulos/comunidad/foro.php?" +
            "get_info=elimina_mencion&zona=info_foro_menciones&page=0&tipo_mencion=foro" +
            "&id_mensaje=";

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
     * This method fetches the logged user mentions from 3DJuegos.
     * Since the user needs to be logged in, isLoginNeeded() needs to be called and return false
     * for this method to work.
     *
     * @param context  the Android context
     * @param callback a GetMentionsCallback instance that overrides
     *                 onResponse(List<Mention> mentions), method wich will be called if getAll is
     *                 successful.
     * @throws Exception
     */
    public static void getAll(Context context, final GetMentionsCallback callback) throws Exception {

        final CookieManager cm = new CookieManager(context.getSharedPreferences(
                MainActivity.SHARED_PREFS_FILE,
                Context.MODE_PRIVATE));
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

        client.get(context, MainActivity.MENTION_PAGE_URL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String r = new String(responseBody);
                try {
                    callback.onResponse(new MentionParser(r).parse());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void saveToDb(Context context) {
        MentionDbHelper db = new MentionDbHelper(context);
        db.saveMention(this.user.name, this.time.unixTimestamp);
    }

    public boolean existsInDb(Context context) {
        MentionDbHelper db = new MentionDbHelper(context);
        return db.mentionExists(this.user.name, this.time.unixTimestamp);
    }

    /**
     * Sends a GET request to 3DJuegos to delete the mention
     *
     * @param context Android Context
     * @throws Exception
     */
    public void delete(Context context) throws Exception {
        CookieManager cm = new CookieManager(context.getSharedPreferences(
                MainActivity.SHARED_PREFS_FILE, Context.MODE_PRIVATE));

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

        client.get(DELETE_URL + String.valueOf(this.id), new AsyncHttpResponseHandler() {
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

    /**
     * The avatar supplied by 3DJuegos is in a low resolution (48x48 iirc). We could use it, but
     * it'd be ugly. We're better off first downloading the content from this.user.profile_url,
     * then retrieving the fill size avatar url.
     *
     * @param callback The callback that upon completion will be called.
     */
    public void getAvatarURl(final GetAvatarCallback callback) { // TODO this will always fail until we find a way to get the user profile url
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(this.user.profile_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Document doc = Jsoup.parse(new String(responseBody));
                Element avatar_container = doc.select(".photo").get(0);
                callback.onSuccess(avatar_container.attr("src"));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable e) {
                e.printStackTrace();
                callback.onFailure();
            }
        });
    }

    public interface GetMentionsCallback {
        void onResponse(List<Mention> mentions);
    }

    public interface GetAvatarCallback {
        void onSuccess(String avatar_url);

        void onFailure();
    }

    public class User {
        public String name;
        public String lowres_avatar_url;
        public String profile_url;

        public User(String username, String avatar_url, String profile_url) {
            this.name = username;
            this.lowres_avatar_url = avatar_url;
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
