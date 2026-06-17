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

    // ✅ Make sure this is correct — no trailing slash, correct port
    private static final String API_URL = "https://node-vpn-scraping.up.railway.app/api/vpnbook/credentials";
    private static final String API_KEY = "5keOCNiowsOKAgAREeXNN1wxmmZkt1iM";

    public interface CredentialCallback {
        void onSuccess(String username, String password);
        void onFailure(String error);
    }

    public static void fetchCredentials(CredentialCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                Log.d(TAG, "📡 Fetching credentials from: " + API_URL);

                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", API_KEY);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "📶 Response code: " + responseCode);

                // ✅ Read response body regardless of success/failure
                BufferedReader reader;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    // Read error stream to see what server returned
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "📥 Raw response: " + response.toString());

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject json = new JSONObject(response.toString());
                    boolean success = json.getBoolean("success");

                    if (success) {
                        JSONObject data = json.getJSONObject("data");
                        String username = data.getString("username");
                        String password = data.getString("password");
                        String lastUpdated = data.optString("lastUpdated", "unknown");

                        Log.d(TAG, "✅ Credentials fetched successfully");
                        Log.d(TAG, "👤 Username: " + username);
                        Log.d(TAG, "🕒 Last updated: " + lastUpdated);

                        callback.onSuccess(username, password);
                    } else {
                        String msg = json.optString("message", "Unknown error");
                        Log.e(TAG, "❌ Server returned success=false: " + msg);
                        callback.onFailure("Server error: " + msg);
                    }
                } else if (responseCode == 401) {
                    Log.e(TAG, "🔐 Unauthorized — check your API key");
                    callback.onFailure("Unauthorized: Invalid API key");
                } else if (responseCode == 503) {
                    Log.e(TAG, "⏳ Credentials not yet scraped by backend");
                    callback.onFailure("Credentials not ready yet, try again");
                } else {
                    Log.e(TAG, "❌ HTTP error: " + responseCode);
                    callback.onFailure("HTTP error: " + responseCode);
                }

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "🌐 Unknown host — check API_URL or internet connection: " + e.getMessage());
                callback.onFailure("Unknown host: " + e.getMessage());
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "⏱️ Connection timed out: " + e.getMessage());
                callback.onFailure("Timeout: " + e.getMessage());
            } catch (java.net.ConnectException e) {
                Log.e(TAG, "🔌 Connection refused — is your server running? " + e.getMessage());
                callback.onFailure("Connection refused: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "💥 Unexpected error: " + e.getMessage(), e);
                callback.onFailure("Error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}