package com.github.mellamopablo.notifyfor3dj;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED_PREFS_FILE = "com.github.mellamopablo.notifyfor3dj_PREFS";
    public static final String MENTION_PAGE_URL = "http://www.3djuegos.com/modulos/comunidad/foro.php?get_info=comu_info_foro_main&zona=perfil_foro_menciones";
    public static int lastNotifId = 1; //So that ID 0 is the "New Updates Available" notification.
    final String logout_url = "http://www.3djuegos.com/foros/index.php?zona=desconectar_sesion";
    Context context;

    public static void restartAlarm(Context context, long freq) {
        Intent alarmIntent = new Intent(context, DisplayMentionsService.class);
        PendingIntent pending = PendingIntent.getService(context, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //Cancel the alarm, then set it again
        alarmMgr.cancel(pending);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                freq, freq, pending);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        boolean isLoginNeeded = isLoginNeeded();
        hideViews(isLoginNeeded);

        final SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);

        long freq = prefs.getLong("frequency", AlarmManager.INTERVAL_HOUR);

        if (!isLoginNeeded) {
            try {
                Intent getMentions = new Intent(context, DisplayMentionsService.class);
                startService(getMentions);
            } catch (Exception e) {
                e.printStackTrace();
            }

            restartAlarm(this, freq);
        }

        final Button button_login = (Button) findViewById(R.id.button_login);
        assert button_login != null;

        button_login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText email = (EditText) findViewById(R.id.editText_email);
                final EditText pass = (EditText) findViewById(R.id.editText_pass);
                assert email != null && pass != null;

                login(email.getText().toString(), pass.getText().toString());
            }
        });

        final Button button_logout = (Button) findViewById(R.id.button_logout);
        assert button_logout != null;

        button_logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    logout();

                    CookieManager cm = new CookieManager(context.getSharedPreferences(SHARED_PREFS_FILE,
                            Context.MODE_PRIVATE));

                    cm.deleteSharedPrefs();

                    hideViews(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final Button button_check = (Button) findViewById(R.id.button_check);
        assert button_check != null;

        button_check.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent getMentions = new Intent(context, DisplayMentionsService.class);
                    startService(getMentions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Check the radio button with the current preference
        int radio_id;
        // You can switch with long types. Nice memes, java.
        if (freq == AlarmManager.INTERVAL_HALF_HOUR) {
            radio_id = R.id.radio_freq_30;

        } else if (freq == AlarmManager.INTERVAL_HOUR) {
            radio_id = R.id.radio_freq_60;

        } else if (freq == AlarmManager.INTERVAL_HALF_DAY) {
            radio_id = R.id.radio_freq_halfday;

        } else if (freq == AlarmManager.INTERVAL_DAY) {
            radio_id = R.id.radio_freq_day;

        } else {
            radio_id = R.id.radio_freq_60;
        }

        RadioButton radioButton = (RadioButton) findViewById(radio_id);
        radioButton.toggle();

        final RadioGroup freq_select = (RadioGroup) findViewById(R.id.freq_select);
        assert freq_select != null;

        freq_select.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                SharedPreferences.Editor editor = prefs.edit();

                long freq;

                switch (i) {
                    case R.id.radio_freq_30:
                        freq = AlarmManager.INTERVAL_HALF_HOUR;
                        break;
                    case R.id.radio_freq_60:
                        freq = AlarmManager.INTERVAL_HOUR;
                        break;
                    case R.id.radio_freq_halfday:
                        freq = AlarmManager.INTERVAL_HALF_DAY;
                        break;
                    case R.id.radio_freq_day:
                        freq = AlarmManager.INTERVAL_DAY;
                        break;
                    default:
                        freq = AlarmManager.INTERVAL_HALF_HOUR;
                }

                restartAlarm(context, freq);
                editor.putLong("frequency", freq);
                editor.apply();
                settingsChangedSnack();
            }
        });

        CheckBox checkbox_silent = (CheckBox) findViewById(R.id.checkbox_silent);
        boolean silent = prefs.getBoolean("silent", false);
        checkbox_silent.setChecked(silent);

        checkbox_silent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateBoolPref(prefs, "silent", b);
            }
        });

        CheckBox checkbox_avatar = (CheckBox) findViewById(R.id.checkbox_avatar);
        boolean avatar = prefs.getBoolean("avatar", true);
        checkbox_avatar.setChecked(/*avatar*/ false); // TODO revert once the avatar thing is fixed

        checkbox_avatar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateBoolPref(prefs, "avatar", b);
            }
        });

        CheckBox checkbox_delete = (CheckBox) findViewById(R.id.checkbox_delete);
        boolean delete = prefs.getBoolean("delete", false);
        checkbox_delete.setChecked(delete);

        checkbox_delete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateBoolPref(prefs, "delete", b);
            }
        });

        CheckBox checkbox_db = (CheckBox) findViewById(R.id.checkbox_db);
        boolean db = prefs.getBoolean("db", true);
        checkbox_db.setChecked(db);

        checkbox_db.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateBoolPref(prefs, "db", b);
            }
        });

        final CheckBox checkbox_update = (CheckBox) findViewById(R.id.checkbox_update);
        boolean update = prefs.getBoolean("update", true);
        checkbox_update.setChecked(update);

        checkbox_update.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                if (!b) { //If the user is disabling updates
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.alert_disable_updating_title))
                            .setMessage(R.string.alert_disable_updating_msg)
                            .setNegativeButton(
                                    R.string.alert_disable_updating_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //User cancelled
                                            checkbox_update.setChecked(true);
                                        }
                                    }
                            )
                            .setPositiveButton(
                                    R.string.alert_disable_updating_accept,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //User accepted
                                            updateBoolPref(prefs, "update", false);
                                        }
                                    }
                            )
                            .show();
                } else {
                    /*
                    When the user attempts to disable this preference, is prompted with the
                    AlertDialog, and then decides to do nothing instead,
                    checkbox_update.setChecked(true) will be called to restore the original
                    value. That means that onCheckedChanged is called again, and we end up being
                    in this block of code, meaning that a snackbar would be shown even though
                    the user changed nothing.

                    The way to work around this is check the preference's current value. If it's
                    false, that means that this option was disabled and the user wants to
                    re-enable it. If it's true, that means that the user didn't intend to disable
                    this option, so we don't call updateBoolPref();
                    */
                    if (!prefs.getBoolean("update", true))
                        updateBoolPref(prefs, "update", true);
                }
            }
        });

        TextView version = (TextView) findViewById(R.id.text_version);
        version.setText(getString(R.string.main_app_version, Version.current.string));
    }

    private void updateBoolPref(SharedPreferences prefs, String preference, boolean newVal) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(preference, newVal);
        editor.apply();
        settingsChangedSnack();
    }

    private void settingsChangedSnack() {
        Snackbar.make(findViewById(R.id.layout_main),
                R.string.snackbar_settings_changed, Snackbar.LENGTH_SHORT)
                .show();
    }

    public void hideViews(boolean isLoginNeeded) {
        final RelativeLayout layout_login = (RelativeLayout) findViewById(R.id.layout_login);
        final RelativeLayout layout_main = (RelativeLayout) findViewById(R.id.layout_loggedin);

        if (isLoginNeeded) {
            layout_login.setVisibility(View.VISIBLE);
            layout_main.setVisibility(View.GONE);
        } else {
            layout_login.setVisibility(View.GONE);
            layout_main.setVisibility(View.VISIBLE);
        }
    }

    private void login(String email, String pass) {
        if (email.equals("")) {
            Snackbar.make(findViewById(R.id.layout_main),
                    R.string.error_no_email, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        if (pass.equals("")) {
            Snackbar.make(findViewById(R.id.layout_main),
                    R.string.error_no_pass, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        final CookieManager cm = new CookieManager(this.getSharedPreferences(SHARED_PREFS_FILE,
                Context.MODE_PRIVATE));

        final String url = "http://www.3djuegos.com/foros/index.php?zona=iniciar_sesion";

        HashMap<String, String> params = new HashMap<>();
        params.put("login_email", email);
        params.put("login_password", pass);
        params.put("login_recordar", "true");
        //Don't know why is this needed, but it's on 3DJuegos' page
        params.put("referer", "http://www.3djuegos.com/");

        AsyncHttpClient client = new AsyncHttpClient();
        client.setEnableRedirects(true); //Upon login, 3DJuegos sends HTTP 302 at first redirecting
        //to the correct login page.

        final PersistentCookieStore cookies = new PersistentCookieStore(this);
        client.setCookieStore(cookies);

        client.post(/*context,*/ url, new RequestParams(params), new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                if (cookies.getCookies().size() < 4) {
                    //Not enough cookies were sent. That means that log in was unsuccessful.
                    Document doc = Jsoup.parse(new String(response));
                    String error = doc.select(".lh19").first().text(); // .lh19 contains the error
                    // information

                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.error))
                            .setMessage(error)
                            .show();
                } else {
                    Snackbar.make(findViewById(R.id.layout_main),
                            R.string.snackbar_logged_in, Snackbar.LENGTH_LONG)
                            .show();

                    cm.saveToSharedPrefs(cookies.getCookies());
                    hideViews(false);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Snackbar.make(findViewById(R.id.layout_main),
                        R.string.error_at_login, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void logout() throws Exception {
        final CookieManager cm = new CookieManager(this.getSharedPreferences(SHARED_PREFS_FILE,
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

        MentionDbHelper db = new MentionDbHelper(this);
        db.deleteAllRecords();

        client.get(logout_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Snackbar.make(findViewById(R.id.layout_main),
                        R.string.snackbar_logged_out, Snackbar.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });

    }

    private boolean isLoginNeeded() {
        final CookieManager cm = new CookieManager(this.getSharedPreferences(SHARED_PREFS_FILE,
                Context.MODE_PRIVATE));

        try {
            cm.getCookie("recordar");
        } catch (Exception e) {
            //Remember me cookies aren't saved.
            //The user hasn't logged in yet or has deleted them.
            return true;
        }

        return false;
    }

}
