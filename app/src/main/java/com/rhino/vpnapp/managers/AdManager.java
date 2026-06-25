package com.rhino.vpnapp.managers;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.rhino.vpnapp.BuildConfig;
import com.rhino.vpnapp.R;

// AdMob imports
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// Facebook imports
import com.facebook.ads.Ad;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;

// StartApp imports
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;

import java.util.ArrayList;
import java.util.List;

public class AdManager {

    private static final String TAG = "AdManager";
    private static final String NETWORK_ADMOB    = "admob";
    private static final String NETWORK_FACEBOOK = "facebook";
    private static final String NETWORK_STARTAPP = "startapp";

    // ✅ Minimum 30 seconds between reload attempts (Meta requirement)
    private static final long MIN_RELOAD_INTERVAL_MS = 30_000;

    // ─── AdMob ────────────────────────────────────────────
    private InterstitialAd admobInterstitial;

    // ─── Facebook ─────────────────────────────────────────
    private com.facebook.ads.InterstitialAd fbInterstitial;
    private com.facebook.ads.AdView         fbBannerAd;
    private NativeAd                         fbNativeAd;

    // ✅ Loading guards
    private boolean isFbBannerLoading       = false;
    private boolean isFbInterstitialLoading = false;
    private boolean isFbNativeLoading       = false;

    // ✅ Timestamps — enforce minimum reload interval
    private long lastBannerLoadTime       = 0;
    private long lastInterstitialLoadTime = 0;
    private long lastNativeLoadTime       = 0;

    // ✅ SDK init flag
    private boolean isFbSdkInitialized = false;

    // ✅ Single Handler instance — prevents stacking retry calls
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRetryRunnable       = null;
    private Runnable interstitialRetryRunnable = null;
    private Runnable nativeRetryRunnable       = null;

    // ✅ Pending callback for interstitial show
    private InterstitialCallback pendingInterstitialCallback = null;

    private final String  adNetwork;
    private final Context appContext;

    // ==========================================
    //  CALLBACK INTERFACE
    // ==========================================

    public interface InterstitialCallback {
        void onAdClosed();
        void onAdFailedToLoad(String error);
    }

    // ==========================================
    //  SINGLETON
    // ==========================================

    private static AdManager mInstance;

    public static AdManager get() { return mInstance; }

    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new AdManager(context.getApplicationContext());
        }
    }

    private AdManager(Context context) {
        this.appContext = context;
        this.adNetwork  = BuildConfig.AD_NETWORK;
        Log.d(TAG, "AdManager initializing with network: " + adNetwork);

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            initFacebookSdk(context);
        } else if (NETWORK_STARTAPP.equals(adNetwork)) {
            initStartAppSdk(context);
        } else {
            MobileAds.initialize(context,
                    status -> Log.d(TAG, "✅ AdMob initialized"));
        }
    }

    private void initStartAppSdk(Context context) {
        String appId = context.getString(R.string.startapp_app_id);
        StartAppSDK.init(context, appId, true);
//        StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG);
        Log.d(TAG, "✅ StartApp SDK initialized with ID: " + appId);
    }

    // ==========================================
    //  FACEBOOK SDK INIT
    // ==========================================

    private void initFacebookSdk(Context context) {
//        if (BuildConfig.DEBUG) {
//            AdSettings.setDebugBuild(true);
//            AdSettings.setIntegrationErrorMode(
//                    AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CRASH_DEBUG_MODE
//            );
//            // ✅ Replace with hash from Logcat filtered by "FBAdSDK"
//            AdSettings.addTestDevice("YOUR_DEVICE_HASH_FROM_LOGCAT");
//        }

        AudienceNetworkAds.buildInitSettings(context)
                .withInitListener(result -> {
                    if (result.isSuccess()) {
                        isFbSdkInitialized = true;
                        Log.d(TAG, "✅ Meta SDK initialized");
                    } else {
                        isFbSdkInitialized = false;
                        Log.e(TAG, "❌ Meta SDK init failed: " + result.getMessage());
                    }
                })
                .initialize();
    }

    // ==========================================
    //  HELPERS
    // ==========================================

    /**
     * ✅ Checks if enough time has passed since last load attempt.
     * Prevents error 1002 — Meta enforces minimum interval between requests.
     */
    private boolean isTooSoon(long lastLoadTime) {
        long elapsed = System.currentTimeMillis() - lastLoadTime;
        if (elapsed < MIN_RELOAD_INTERVAL_MS) {
            Log.w(TAG, "Too soon to reload — waited " + (elapsed / 1000)
                    + "s, need " + (MIN_RELOAD_INTERVAL_MS / 1000) + "s");
            return true;
        }
        return false;
    }

    // ==========================================
    //  BANNER ADS
    // ==========================================

    public void loadBanner(Activity activity, FrameLayout container) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            if (!isFbSdkInitialized) {
                // ✅ Cancel any existing retry before posting a new one
                if (bannerRetryRunnable != null) {
                    retryHandler.removeCallbacks(bannerRetryRunnable);
                }
                bannerRetryRunnable = () -> loadBanner(activity, container);
                retryHandler.postDelayed(bannerRetryRunnable, 2000);
                Log.w(TAG, "FB SDK not ready, banner retry in 2s");
                return;
            }
            loadFacebookBanner(activity, container);
        } else if (NETWORK_STARTAPP.equals(adNetwork)) {
            loadStartAppBanner(activity, container);
        } else {
            loadAdMobBanner(activity, container);
        }
    }

    private void loadStartAppBanner(Activity activity, FrameLayout container) {
        try {
            Banner startAppBanner = new Banner(activity);
            container.removeAllViews();
            container.addView(startAppBanner);
            Log.d(TAG, "✅ StartApp banner loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading StartApp banner: " + e.getMessage());
        }
    }

    private void loadAdMobBanner(Activity activity, FrameLayout container) {
        AdView adView = new AdView(activity);
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
        adView.setAdUnitId(activity.getString(R.string.banner_app_id));
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
        Log.d(TAG, "AdMob banner loading...");
    }

    private void loadFacebookBanner(Activity activity, FrameLayout container) {
        // ✅ Skip if already loading
        if (isFbBannerLoading) {
            Log.d(TAG, "FB Banner already loading, skipping");
            return;
        }
        // ✅ Reuse existing loaded banner
        if (fbBannerAd != null) {
            Log.d(TAG, "FB Banner already loaded, reusing");
            container.removeAllViews();
            container.addView(fbBannerAd);
            return;
        }
        // ✅ Enforce minimum reload interval
        if (isTooSoon(lastBannerLoadTime)) return;

        isFbBannerLoading = true;
        lastBannerLoadTime = System.currentTimeMillis();
        Log.d(TAG, "FB Banner loading...");

        fbBannerAd = new com.facebook.ads.AdView(
                activity,
                activity.getString(R.string.fb_banner_id),
                com.facebook.ads.AdSize.BANNER_HEIGHT_50
        );

        com.facebook.ads.AdListener listener = new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, com.facebook.ads.AdError error) {
                isFbBannerLoading = false;
                fbBannerAd = null;
                // ✅ Never retry inside onError — causes error 1002
                Log.e(TAG, "FB Banner error [" + error.getErrorCode()
                        + "]: " + error.getErrorMessage());
            }
            @Override
            public void onAdLoaded(Ad ad) {
                isFbBannerLoading = false;
                Log.d(TAG, "✅ FB Banner loaded");
            }
            @Override public void onAdClicked(Ad ad) {}
            @Override public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "FB Banner impression logged");
            }
        };

        container.removeAllViews();
        container.addView(fbBannerAd);
        fbBannerAd.loadAd(
                fbBannerAd.buildLoadAdConfig().withAdListener(listener).build()
        );
    }

    // ==========================================
    //  INTERSTITIAL ADS
    // ==========================================

    public void loadInterstitial(Activity activity) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            if (!isFbSdkInitialized) {
                // ✅ Cancel existing retry before posting new one
                if (interstitialRetryRunnable != null) {
                    retryHandler.removeCallbacks(interstitialRetryRunnable);
                }
                interstitialRetryRunnable = () -> loadInterstitial(activity);
                retryHandler.postDelayed(interstitialRetryRunnable, 2000);
                Log.w(TAG, "FB SDK not ready, interstitial retry in 2s");
                return;
            }
            loadFacebookInterstitial(activity);
        } else if (NETWORK_STARTAPP.equals(adNetwork)) {
            // StartApp Interstitials are often loaded and shown together, 
            // but we can pre-load if needed.
            Log.d(TAG, "StartApp interstitial ready for show (auto-managed)");
        } else {
            loadAdMobInterstitial(activity);
        }
    }

    private void loadAdMobInterstitial(Activity activity) {
        InterstitialAd.load(
                activity,
                activity.getString(R.string.interstitial_app_id),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        admobInterstitial = ad;
                        Log.d(TAG, "✅ AdMob interstitial loaded");
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        admobInterstitial = null;
                        Log.e(TAG, "AdMob interstitial failed: " + error.getMessage());
                    }
                }
        );
    }

    private void loadFacebookInterstitial(Activity activity) {
        // ✅ Skip if already loading
        if (isFbInterstitialLoading) {
            Log.d(TAG, "FB Interstitial already loading, skipping");
            return;
        }
        // ✅ Skip if already have a ready unshown ad
        if (fbInterstitial != null && fbInterstitial.isAdLoaded()) {
            Log.d(TAG, "FB Interstitial already loaded and ready, skipping");
            return;
        }
        // ✅ Enforce minimum reload interval — ROOT CAUSE of error 1002
        if (isTooSoon(lastInterstitialLoadTime)) return;

        isFbInterstitialLoading = true;
        lastInterstitialLoadTime = System.currentTimeMillis();

        // ✅ Destroy stale instance before creating new one
        if (fbInterstitial != null) {
            fbInterstitial.destroy();
            fbInterstitial = null;
        }

        Log.d(TAG, "FB Interstitial loading...");

        fbInterstitial = new com.facebook.ads.InterstitialAd(
                activity,
                activity.getString(R.string.fb_interstitial_id)
        );

        fbInterstitial.loadAd(
                fbInterstitial.buildLoadAdConfig()
                        .withAdListener(new InterstitialAdListener() {

                            @Override
                            public void onInterstitialDisplayed(Ad ad) {
                                Log.d(TAG, "FB Interstitial displayed");
                            }

                            @Override
                            public void onInterstitialDismissed(Ad ad) {
                                Log.d(TAG, "FB Interstitial dismissed");
                                fbInterstitial = null;

                                // ✅ Forward dismiss to caller
                                if (pendingInterstitialCallback != null) {
                                    pendingInterstitialCallback.onAdClosed();
                                    pendingInterstitialCallback = null;
                                }

                                // ✅ Reload ONLY after dismiss, never inside show()
                                // The 30s cooldown in loadFacebookInterstitial()
                                // will naturally prevent over-requesting
                                loadFacebookInterstitial(activity);
                            }

                            @Override
                            public void onError(Ad ad, com.facebook.ads.AdError error) {
                                isFbInterstitialLoading = false;
                                // ✅ Never retry inside onError — causes error 1002
                                Log.e(TAG, "FB Interstitial error ["
                                        + error.getErrorCode() + "]: "
                                        + error.getErrorMessage());

                                if (pendingInterstitialCallback != null) {
                                    pendingInterstitialCallback.onAdFailedToLoad(
                                            error.getErrorMessage());
                                    pendingInterstitialCallback = null;
                                }
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                                isFbInterstitialLoading = false;
                                Log.d(TAG, "✅ FB Interstitial loaded");
                            }

                            @Override public void onAdClicked(Ad ad) {}
                            @Override public void onLoggingImpression(Ad ad) {
                                Log.d(TAG, "FB Interstitial impression logged");
                            }
                        })
                        .build()
        );
    }

    public void showInterstitial(Activity activity, InterstitialCallback callback) {
        if (!BuildConfig.ADS_SHOWN) {
            if (callback != null) callback.onAdClosed();
            return;
        }
        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            showFacebookInterstitial(activity, callback);
        } else if (NETWORK_STARTAPP.equals(adNetwork)) {
            showStartAppInterstitial(activity, callback);
        } else {
            showAdMobInterstitial(activity, callback);
        }
    }

    private void showStartAppInterstitial(Activity activity, InterstitialCallback callback) {
        StartAppAd startAppAd = new StartAppAd(activity);
        startAppAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                startAppAd.showAd(new AdDisplayListener() {
                    @Override
                    public void adHidden(com.startapp.sdk.adsbase.Ad ad) {
                        if (callback != null) callback.onAdClosed();
                    }
                    @Override public void adDisplayed(com.startapp.sdk.adsbase.Ad ad) { Log.d(TAG, "StartApp interstitial displayed"); }
                    @Override public void adClicked(com.startapp.sdk.adsbase.Ad ad) {}
                    @Override public void adNotDisplayed(com.startapp.sdk.adsbase.Ad ad) {
                        if (callback != null) callback.onAdClosed();
                    }
                });
            }

            @Override
            public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                Log.e(TAG, "StartApp interstitial failed to load");
                if (callback != null) callback.onAdFailedToLoad("StartApp load failed");
            }
        });
    }

    private void showAdMobInterstitial(Activity activity, InterstitialCallback callback) {
        if (admobInterstitial != null) {
            admobInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    admobInterstitial = null;
                    loadAdMobInterstitial(activity);
                    if (callback != null) callback.onAdClosed();
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError error) {
                    if (callback != null) callback.onAdFailedToLoad(error.getMessage());
                }
            });
            admobInterstitial.show(activity);
        } else {
            Log.d(TAG, "AdMob interstitial not ready, loading...");
            loadAdMobInterstitial(activity);
            if (callback != null) callback.onAdClosed();
        }
    }

    private void showFacebookInterstitial(Activity activity, InterstitialCallback callback) {
        if (fbInterstitial != null && fbInterstitial.isAdLoaded()) {
            // ✅ KEY FIX: just show() — listener already attached at load time
            // Do NOT call loadAd() here
            Log.d(TAG, "Showing FB interstitial...");
            pendingInterstitialCallback = callback;
            fbInterstitial.show();
        } else {
            Log.d(TAG, "FB interstitial not ready, loading...");
            loadFacebookInterstitial(activity);
            if (callback != null) callback.onAdClosed();
        }
    }

    // ==========================================
    //  NATIVE ADS
    // ==========================================

    public void loadNativeAd(Activity activity, TemplateView template,
                             NativeAdLayout fbNativeLayout) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            if (!isFbSdkInitialized) {
                // ✅ Cancel existing retry before posting new one
                if (nativeRetryRunnable != null) {
                    retryHandler.removeCallbacks(nativeRetryRunnable);
                }
                nativeRetryRunnable = () -> loadNativeAd(activity, template, fbNativeLayout);
                retryHandler.postDelayed(nativeRetryRunnable, 2000);
                Log.w(TAG, "FB SDK not ready, native retry in 2s");
                return;
            }
            if (fbNativeLayout != null) {
                loadFacebookNativeAd(activity, fbNativeLayout);
            } else {
                Log.e(TAG, "Facebook NativeAdLayout is null");
            }
        } else if (NETWORK_STARTAPP.equals(adNetwork)) {
            if (template != null) {
                loadStartAppNativeAd(activity, template);
            }
        } else {
            if (template != null) {
                loadAdMobNativeAd(activity, template);
            } else {
                Log.e(TAG, "AdMob TemplateView is null");
            }
        }
    }

    private void loadStartAppNativeAd(Activity activity, TemplateView template) {
        StartAppNativeAd startAppNativeAd = new StartAppNativeAd(activity);
        NativeAdPreferences nativePrefs = new NativeAdPreferences();
        nativePrefs.setAdsNumber(1);
        nativePrefs.setAutoBitmapDownload(true);

        startAppNativeAd.loadAd(nativePrefs, new AdEventListener() {
            @Override
            public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                ArrayList<NativeAdDetails> ads = startAppNativeAd.getNativeAds();
                if (ads != null && ads.size() > 0) {
                    NativeAdDetails adDetails = ads.get(0);
                    // StartApp doesn't easily plug into AdMob's TemplateView without 
                    // a lot of custom work, so we just log it for now or 
                    // you might need a different view.
                    Log.d(TAG, "✅ StartApp native ad loaded: " + adDetails.getTitle());
                    // Note: Implementation for StartApp -> TemplateView would require 
                    // modifying TemplateView or creating a wrapper.
                }
            }

            @Override
            public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                Log.e(TAG, "StartApp native failed to load");
                template.setVisibility(View.GONE);
            }
        });
    }

    private void loadAdMobNativeAd(Activity activity, TemplateView template) {
        AdLoader adLoader = new AdLoader.Builder(
                activity, activity.getString(R.string.native_app_id))
                .forNativeAd(nativeAd -> {
                    template.setNativeAd(nativeAd);
                    template.setVisibility(View.VISIBLE);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "AdMob Native failed: " + adError.getMessage());
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void loadFacebookNativeAd(Activity activity, NativeAdLayout nativeAdLayout) {
        // ✅ Skip if already loading
        if (isFbNativeLoading) {
            Log.d(TAG, "FB Native already loading, skipping");
            return;
        }
        // ✅ Skip if already loaded
        if (fbNativeAd != null && fbNativeAd.isAdLoaded()) {
            Log.d(TAG, "FB Native already loaded, skipping");
            return;
        }
        // ✅ Enforce minimum reload interval
        if (isTooSoon(lastNativeLoadTime)) return;

        isFbNativeLoading = true;
        lastNativeLoadTime = System.currentTimeMillis();
        Log.d(TAG, "FB Native loading...");

        fbNativeAd = new NativeAd(activity, activity.getString(R.string.fb_native_id));
        NativeAdListener nativeAdListener = new NativeAdListener() {
            @Override public void onMediaDownloaded(Ad ad) {}

            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                isFbNativeLoading = false;
                // ✅ Never retry inside onError
                Log.e(TAG, "FB Native error [" + adError.getErrorCode()
                        + "]: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                isFbNativeLoading = false;
                if (fbNativeAd == null || fbNativeAd != ad) return;
                Log.d(TAG, "✅ FB Native loaded");
                inflateFacebookNativeAd(activity, fbNativeAd, nativeAdLayout);
            }

            @Override public void onAdClicked(Ad ad) {}
            @Override public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "FB Native impression logged");
            }
        };

        fbNativeAd.loadAd(
                fbNativeAd.buildLoadAdConfig().withAdListener(nativeAdListener).build()
        );
    }

    private void inflateFacebookNativeAd(Activity activity, NativeAd nativeAd,
                                         NativeAdLayout nativeAdLayout) {
        if (nativeAdLayout == null) return;
        nativeAd.unregisterView();
        nativeAdLayout.setVisibility(View.VISIBLE);

        AdOptionsView adOptionsView = new AdOptionsView(activity, nativeAd, nativeAdLayout);
        nativeAdLayout.removeAllViews();
        nativeAdLayout.addView(adOptionsView, 0);

        com.facebook.ads.MediaView nativeAdIcon  = new com.facebook.ads.MediaView(activity);
        com.facebook.ads.MediaView nativeAdMedia = new com.facebook.ads.MediaView(activity);
        android.widget.TextView    nativeAdTitle = new android.widget.TextView(activity);
        android.widget.TextView    nativeAdBody  = new android.widget.TextView(activity);
        android.widget.Button      nativeAdCta   = new android.widget.Button(activity);

        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdCta.setText(nativeAd.getAdCallToAction());

        nativeAdTitle.setTextSize(16);
        nativeAdTitle.setPadding(10, 10, 10, 10);
        nativeAdTitle.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        activity, R.color.colorTitleText));
        nativeAdBody.setTextSize(14);
        nativeAdBody.setPadding(10, 0, 10, 10);
        nativeAdBody.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        activity, R.color.colorTitleText));

        android.widget.LinearLayout container = new android.widget.LinearLayout(activity);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);
        container.setBackground(
                androidx.core.content.ContextCompat.getDrawable(
                        activity, R.drawable.rounded_border));

        container.addView(nativeAdTitle);
        container.addView(nativeAdMedia);
        container.addView(nativeAdBody);
        container.addView(nativeAdCta);
        nativeAdLayout.addView(container);

        java.util.List<View> clickableViews = new java.util.ArrayList<>();
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdCta);
        nativeAd.registerViewForInteraction(
                container, nativeAdMedia, nativeAdIcon, clickableViews);
    }

    // ==========================================
    //  DESTROY
    // ==========================================

    public void destroy() {
        try {
            // ✅ Cancel all pending retry handlers
            if (bannerRetryRunnable != null) {
                retryHandler.removeCallbacks(bannerRetryRunnable);
                bannerRetryRunnable = null;
            }
            if (interstitialRetryRunnable != null) {
                retryHandler.removeCallbacks(interstitialRetryRunnable);
                interstitialRetryRunnable = null;
            }
            if (nativeRetryRunnable != null) {
                retryHandler.removeCallbacks(nativeRetryRunnable);
                nativeRetryRunnable = null;
            }

            if (fbBannerAd != null) {
                fbBannerAd.destroy();
                fbBannerAd = null;
            }
            if (fbInterstitial != null) {
                fbInterstitial.destroy();
                fbInterstitial = null;
            }
            if (fbNativeAd != null) {
                fbNativeAd.destroy();
                fbNativeAd = null;
            }

            isFbBannerLoading       = false;
            isFbInterstitialLoading = false;
            isFbNativeLoading       = false;
            isFbSdkInitialized      = false;
            pendingInterstitialCallback = null;

        } catch (Exception e) {
            Log.e(TAG, "Error destroying ads: " + e.getMessage());
        }
    }
}