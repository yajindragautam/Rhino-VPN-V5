package com.bytesbee.vpnapp.managers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bytesbee.vpnapp.utils.Utils;

public class UsageManager {

    private SharedPreferences pref = null;
    private static final String PREF_NAME = "_UsageHistory";
    public static final String KEY_TOTAL_TIME = "totalTime";
    public static final String KEY_TOTAL_CONNECTIONS = "totalConnections";

    public static final String STR_CONNECTIONS = "_connections";
    public static final String STR_TIME = "_time";

    //============== START
    private static UsageManager mInstance;

    public static UsageManager get() {
        return mInstance;
    }

    public static void init(Context ctx) {
        if (mInstance == null) mInstance = new UsageManager(ctx);
    }
    //============== END

    public UsageManager(final Context context) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            pref = EncryptedSharedPreferences.create(
                    context.getPackageName() + PREF_NAME,
                    masterKey,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Utils.getErrors(e);
            // FALLBACK: EncryptedSharedPreferences failed (common on API 37
            // due to Keystore changes). Use regular prefs so app doesn't crash.
            pref = context.getSharedPreferences(
                    context.getPackageName() + PREF_NAME + "_fallback",
                    Context.MODE_PRIVATE
            );
        }
    }

    public long getUsage(String key) {
        if (pref == null) return 0;
        return pref.getLong(key, 0);
    }

    public void setUsage(String key, long value) {
        if (pref == null) return;
        final SharedPreferences.Editor editor = pref.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void clearAll() {
        if (pref == null) return;
        final SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
    }
}