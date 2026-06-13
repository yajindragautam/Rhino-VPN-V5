package com.bytesbee.vpnapp.managers;

import static com.bytesbee.vpnapp.constants.IConstants.ALL_APPS;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bytesbee.vpnapp.models.Server;
import com.bytesbee.vpnapp.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class SessionManager {

    private SharedPreferences pref = null;
    private static final String PREF_NAME = "_BytesBeeV1";
    private static final String KEY_DEVICE_CREATED = "deviceCreated";
    private static final String KEY_ENTRY_DONE = "statusDone";
    private static final String KEY_SERVER_OBJ = "ServerObj";
    private static final String KEY_ON_OFF_DARK_MODE = "onOffDarkMode";
    private static final String KEY_CONNECT_TUNNEL = "connectToVPN";
    public static final String KEY_SELECT_NOT = "selectAppsNot";
    public static final String KEY_ONLY_SELECT = "onlySelectApps";

    //============== START
    private static SessionManager mInstance;

    public static SessionManager get() {
        return mInstance;
    }

    public static void init(Context ctx) {
        if (mInstance == null) mInstance = new SessionManager(ctx);
    }
    //============== END

    public SessionManager(final Context context) {
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
            // FALLBACK: if encrypted prefs fail (e.g. API 37 keystore issue),
            // use regular SharedPreferences so app doesn't crash
            pref = context.getSharedPreferences(
                    context.getPackageName() + PREF_NAME + "_fallback",
                    Context.MODE_PRIVATE
            );
        }
    }

    public void setDeviceCreated(String device) {
        final SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_DEVICE_CREATED, device);
        editor.apply();
    }

    public String getDeviceCreated() {
        if (pref == null) return "null";
        return pref.getString(KEY_DEVICE_CREATED, "null");
    }

    public void setEntryDone() {
        final SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_ENTRY_DONE, true);
        editor.apply();
    }

    public boolean isEntryDone() {
        if (pref == null) return false;
        return pref.getBoolean(KEY_ENTRY_DONE, false);
    }

    /**
     * return instance of session manager
     */
    public static SessionManager getInstance(final Context context) {
        return new SessionManager(context);
    }

    /**
     * save server object to session
     *
     * @param server server object to be saved to session
     */
    public void saveServer(Server server) {
        final SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_SERVER_OBJ, new Gson().toJson(server));
        editor.apply();
    }

    /**
     * returns the object of  server saved in session
     */
    public Server getServer() {
        if (pref == null) return null;
        try {
            return new Gson().fromJson(pref.getString(KEY_SERVER_OBJ, null), Server.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void setOnOffDarkMode(final boolean value) {
        final SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_ON_OFF_DARK_MODE, value);
        editor.apply();
    }

    public boolean isDarkModeOn() {
        if (pref == null) return false;
        return pref.getBoolean(KEY_ON_OFF_DARK_MODE, false);
    }

    public void setConnectTunnel(final int value) {
        final SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_CONNECT_TUNNEL, value);
        editor.apply();
    }

    public int getConnectTunnel() {
        if (pref == null) return ALL_APPS;
        return pref.getInt(KEY_CONNECT_TUNNEL, ALL_APPS);
    }

    public void saveHashMap(String key, Object obj) {
        SharedPreferences.Editor editor = pref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        editor.putString(key, json);
        editor.apply();
    }

    public HashMap<String, String> getHashMap(String key) {
        Gson gson = new Gson();
        String json = pref.getString(key, "");
        if (TextUtils.isEmpty(json)) {
            return new HashMap<>();
        } else {
            java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();
            return gson.fromJson(json, type);
        }
    }

    public ArrayList<String> getPackageList(String key) {
        try {
            HashMap<String, String> map = getHashMap(key);
            Set<String> keySet = map.keySet();
            return new ArrayList<>(keySet);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
