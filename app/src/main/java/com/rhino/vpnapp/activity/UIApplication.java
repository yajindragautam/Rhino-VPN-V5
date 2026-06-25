package com.rhino.vpnapp.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.rhino.vpnapp.BuildConfig;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.managers.AdManager;
import com.rhino.vpnapp.managers.SessionManager;
import com.rhino.vpnapp.managers.UsageManager;
import com.rhino.vpnapp.utils.Utils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.StatusListener;

public class UIApplication extends Application
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static final String TAG = "UIApplication";
    private AppOpenAdManager appOpenAdManager;
    private Activity currentActivity;

    // ✅ Tracks if app is coming from background (not cold start)
    private boolean isAppResumedFromBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        this.registerActivityLifecycleCallbacks(this);

        // ✅ Step 1: Session managers first
        SessionManager.init(getApplicationContext());
        UsageManager.init(getApplicationContext());

        // ✅ Step 2: Dark mode
        try {
            SessionManager session = SessionManager.get();
            if (session != null) {
                Utils.setDarkMode(session.isDarkModeOn());
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }

        // ✅ Step 3: Device created timestamp
        try {
            SessionManager session = SessionManager.get();
            if (session != null) {
                String deviceCreated = session.getDeviceCreated();
                if (deviceCreated == null || deviceCreated.equalsIgnoreCase("null")) {
                    session.setDeviceCreated(String.valueOf(System.currentTimeMillis()));
                }
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }

        // ✅ Step 4: Initialize AdManager
        AdManager.init(getApplicationContext());

        // ✅ Step 5: App Open Ads only for AdMob
        if (BuildConfig.ADS_SHOWN && "admob".equals(BuildConfig.AD_NETWORK)) {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
            appOpenAdManager = new AppOpenAdManager(getString(R.string.app_open_id));
        }

        // ✅ Step 6: VPN init
        PRNGFixes.apply();
        StatusListener mStatus = new StatusListener();
        mStatus.init(getApplicationContext());
    }

    // ✅ KEY FIX: Only show App Open ad when returning FROM background
    // NOT on cold start — this was freezing the splash screen
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);

        if (!BuildConfig.ADS_SHOWN || !"admob".equals(BuildConfig.AD_NETWORK)) return;
        if (appOpenAdManager == null) return;
        if (currentActivity == null) return;

        // ✅ Only show when returning from background, not on first launch
        if (isAppResumedFromBackground) {
            appOpenAdManager.showAdIfAvailable(currentActivity);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        // ✅ Mark that app went to background
        isAppResumedFromBackground = true;
    }

    // ==========================================
    //  ACTIVITY LIFECYCLE
    // ==========================================

    @Override
    public void onActivityCreated(@NonNull Activity activity,
                                  @Nullable Bundle savedInstanceState) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (BuildConfig.ADS_SHOWN
                && "admob".equals(BuildConfig.AD_NETWORK)
                && appOpenAdManager != null
                && !appOpenAdManager.isShowingAd) {
            currentActivity = activity;
        } else {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                      @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    // ==========================================
    //  PUBLIC SHOW METHOD
    // ==========================================

    public void showAdIfAvailable(
            @NonNull Activity activity,
            @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        if (BuildConfig.ADS_SHOWN
                && "admob".equals(BuildConfig.AD_NETWORK)
                && appOpenAdManager != null) {
            appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
        } else {
            // ✅ Always call complete so caller is never stuck waiting
            onShowAdCompleteListener.onShowAdComplete();
        }
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    // ==========================================
    //  APP OPEN AD MANAGER (AdMob only)
    // ==========================================

    private static class AppOpenAdManager {

        private static final String TAG = "AppOpenAdManager";

        private final String adUnitId;
        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        boolean isShowingAd = false;
        private long loadTime = 0;

        public AppOpenAdManager(String adUnitId) {
            this.adUnitId = adUnitId;
        }

        private void loadAd(Context context) {
            // ✅ Don't load if already loading or already have a fresh ad
            if (isLoadingAd || isAdAvailable()) {
                Log.d(TAG, "App open ad already loading or available, skipping");
                return;
            }

            isLoadingAd = true;
            AdRequest request = new AdRequest.Builder().build();

            // ✅ FIXED: Only ONE AppOpenAd.load() call (was duplicated before)
            AppOpenAd.load(
                    context,
                    adUnitId,
                    request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            appOpenAd = ad;
                            isLoadingAd = false;
                            loadTime = new Date().getTime();
                            Log.d(TAG, "✅ App open ad loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoadingAd = false;
                            Log.e(TAG, "App open ad failed: " + loadAdError.getMessage());
                        }
                    });
        }

        private boolean wasLoadTimeLessThanNHoursAgo() {
            long dateDifference = new Date().getTime() - loadTime;
            long numMilliSecondsPerHour = 3600000;
            return dateDifference < (numMilliSecondsPerHour * 4L);
        }

        private boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo();
        }

        private void showAdIfAvailable(@NonNull final Activity activity) {
            showAdIfAvailable(activity, () -> {});
        }

        private void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {

            // ✅ Already showing — don't stack ads
            if (isShowingAd) {
                Log.d(TAG, "Ad already showing, skipping");
                return;
            }

            // ✅ Only show on SplashActivity
            String activityName = activity.getClass().getSimpleName();
            if (!activityName.equalsIgnoreCase("SplashActivity")) {
                Log.d(TAG, "Not SplashActivity (" + activityName + "), skipping app open ad");
                // ✅ Load for next time but don't block the caller
                loadAd(activity);
                return;
            }

            // ✅ No ad available — proceed without blocking
            if (!isAdAvailable()) {
                Log.d(TAG, "App open ad not available, proceeding without ad");
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(activity);
                return;
            }

            // ✅ Show the ad
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAd = false;
                    Log.d(TAG, "App open ad dismissed");
                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;
                    Log.e(TAG, "App open ad failed to show: " + adError.getMessage());
                    // ✅ Always call complete so app never gets stuck
                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                    Log.d(TAG, "App open ad showed");
                }
            });

            Log.d(TAG, "Showing app open ad...");
            appOpenAd.show(activity);
        }
    }
}