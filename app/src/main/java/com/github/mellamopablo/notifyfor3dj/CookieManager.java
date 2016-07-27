package com.github.mellamopablo.notifyfor3dj;

import android.content.SharedPreferences;

import java.util.List;

import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

public class CookieManager {

    private SharedPreferences prefs;

    public CookieManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public BasicClientCookie getCookie(String key) throws Exception {
        String val = prefs.getString(key, "");
        if (val.equals(""))
            throw new Exception("Cookie with value " + val + "isn't saved to the shared prefs");

        BasicClientCookie c = new BasicClientCookie(key, val);
        c.setVersion(1);
        c.setDomain("www.3djuegos.com");
        c.setPath("/");

        return c;
    }

    public void saveToSharedPrefs(List<Cookie> cookies) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Cookie c : cookies) {
            editor.putString(c.getName(), c.getValue());
        }
        editor.apply();
    }

    public void deleteSharedPrefs() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
