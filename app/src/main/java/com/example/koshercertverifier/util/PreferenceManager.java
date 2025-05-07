package com.example.koshercertverifier.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private final SharedPreferences prefs;

    private static final String KEY_IS_CERTIFIED = "is_certified";
    private static final String KEY_LAST_CERTIFIED_TIME = "last_certified_time";

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences("kosher_cert_prefs", Context.MODE_PRIVATE);
    }

    public void setCertified(boolean certified) {
        prefs.edit().putBoolean(KEY_IS_CERTIFIED, certified).apply();
    }

    public boolean isCertified() {
        return prefs.getBoolean(KEY_IS_CERTIFIED, false);
    }

    public void setLastCertifiedTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_CERTIFIED_TIME, timestamp).apply();
    }

    public long getLastCertifiedTime() {
        return prefs.getLong(KEY_LAST_CERTIFIED_TIME, 0);
    }
}
