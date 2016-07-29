package com.github.mellamopablo.notifyfor3dj;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MentionDbHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "Mentions.db";

    public MentionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void saveMention(String user, int timestamp) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MentionContract.MentionEntry.COLUMN_NAME_USER, user);
        values.put(MentionContract.MentionEntry.COLUMN_NAME_UNIX, timestamp);

        db.insert(MentionContract.MentionEntry.TABLE_NAME, null, values);
    }

    public boolean mentionExists(String user, int timestamp) {
        SQLiteDatabase db = getReadableDatabase();

        String[] values = new String[]{
                user,
                String.valueOf(timestamp)
        };

        Cursor c = db.query(
                MentionContract.MentionEntry.TABLE_NAME,
                null,
                MentionContract.MentionEntry.COLUMN_NAME_USER + " = ? AND " + MentionContract.MentionEntry.COLUMN_NAME_UNIX + " = ?",
                values,
                null,
                null,
                null
        );

        boolean r = c.moveToNext();
        c.close();

        return r;
    }

    public void deleteAllRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + MentionContract.MentionEntry.TABLE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + MentionContract.MentionEntry.TABLE_NAME + " ("
                + MentionContract.MentionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + MentionContract.MentionEntry.COLUMN_NAME_UNIX + " TEXT NOT NULL,"
                + MentionContract.MentionEntry.COLUMN_NAME_USER + " TEXT NOT NULL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
