package com.github.mellamopablo.notifyfor3dj;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class Version {

    public static Version current = new Version("0.2");
    public static int GREATER = 1;
    public static int EQUAL = 0;
    public static int LESS = -1;

    public String string;

    public Version(String string) {
        this.string = string;
    }

    /**
     * Checks the GitHub repo and checks if there's any new version.
     *
     * @param callback a VersionCallback that overrides the onResponse(Version latest) method
     *                 to handle the response.
     */
    public static void getLatest(final VersionCallback callback) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.setUserAgent("MeLlamoPablo/Notifyfor3DJ"); //Required by GitHub
        String github_api_url = "https://api.github.com/repos/MeLlamoPablo/Notifyfor3DJ/releases/latest";
        client.get(github_api_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject json = new JSONObject(new String(responseBody));
                    Version latest = new Version(json.getString("tag_name"));
                    callback.onResponse(latest);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String r = new String(responseBody);
                Log.e("Update check", "Couldn't get latest version");
            }
        });
    }

    public static void getApkUrl(Version version, final ApkCallback callback) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.setUserAgent("MeLlamoPablo/Notifyfor3DJ"); //Required by GitHub
        String github_api_url = "https://api.github.com/repos/MeLlamoPablo/Notifyfor3DJ/releases/tags/";
        client.get(github_api_url + version.string, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject json = new JSONObject(new String(responseBody));
                    String apk = json.getJSONArray("assets")
                            .getJSONObject(0)
                            .getString("browser_download_url");
                    callback.onResponse(apk);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("Update check", "Couldn't get apk");
            }
        });
    }

    /**
     * Compares two version strings.
     * <p/>
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     * <p/>
     * <p>
     * Note: it does not work if "1.10" is supposed to be equal to "1.10.0".
     * </p>
     * <p>
     * Author: Alex Gitelman @ http://stackoverflow.com/a/6702029/1932096
     * </p>
     *
     * @param version The version object to compare.
     * @return Version.LESS, Version.EQUAL or Version.GREATER
     */
    public int compareTo(Version version) {
        String[] vals1 = this.string.split("\\.");
        String[] vals2 = version.string.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        int result = Integer.signum(vals1.length - vals2.length);
        if (result < 0) {
            return Version.LESS;
        } else if (result > 0) {
            return Version.GREATER;
        } else {
            return Version.EQUAL;
        }
    }

    public interface VersionCallback {
        void onResponse(Version latest);

    }

    public interface ApkCallback {
        void onResponse(String apk);
    }
}
