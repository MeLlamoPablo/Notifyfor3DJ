package com.github.mellamopablo.notifyfor3dj;

import android.provider.BaseColumns;

public class MentionContract {
    public MentionContract() {
    }

    public static abstract class MentionEntry implements BaseColumns {
        public static final String TABLE_NAME = "mentions";
        public static final String COLUMN_NAME_USER = "user";
        public static final String COLUMN_NAME_UNIX = "unix";
    }
}
