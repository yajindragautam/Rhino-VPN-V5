package com.rhino.vpnapp.managers;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VPNCredentialManager {

    private static final String TAG = "VPNCredentialManager";
    private static final String API_URL = "https://node-vpn-scraping.up.railway.app/api/vpnbook/credentials";
    private static final String API_KEY = "your_api_key_here"; // or read from BuildConfig

    public interface CredentialCallback {
        void onSuccess(String username, String password);
        void onFailure(String error);
    }

    public static void fetchCredentials(CredentialCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", API_KEY);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONObject data = json.getJSONObject("data");
                    String username = data.getString("username");
                    String password = data.getString("password");

                    callback.onSuccess(username, password);
                } else {
                    callback.onFailure("Server error: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch credentials", e);
                callback.onFailure(e.getMessage());
            }
        });
    }
}


