package com.rhino.vpnapp.managers;

import android.app.Activity;
import android.content.Context;
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
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;

import java.util.ArrayList;
import java.util.List;

public class AdManager {

    private static final String TAG = "AdManager";
    private static final String NETWORK_ADMOB    = "admob";
    private static final String NETWORK_FACEBOOK = "facebook";

    // --- AdMob ---
    private InterstitialAd admobInterstitial;

    // --- Facebook ---
    private com.facebook.ads.InterstitialAd fbInterstitial;
    private com.facebook.ads.AdView fbBannerAd;
    private NativeAd fbNativeAd;

    private final String adNetwork;

    public interface InterstitialCallback {
        void onAdClosed();
        void onAdFailedToLoad(String error);
    }

    // --- Singleton ---
    private static AdManager mInstance;

    public static AdManager get() {
        return mInstance;
    }

    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new AdManager(context);
        }
    }

    private AdManager(Context context) {
        this.adNetwork = BuildConfig.AD_NETWORK;
        Log.d(TAG, "AdManager initialized with network: " + adNetwork);

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            AudienceNetworkAds.initialize(context);
        } else {
            MobileAds.initialize(context, status -> Log.d(TAG, "AdMob initialized"));
        }
    }

    // ==========================================
    //  BANNER ADS
    // ==========================================

    public void loadBanner(Activity activity, FrameLayout container) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            loadFacebookBanner(activity, container);
        } else {
            loadAdMobBanner(activity, container);
        }
    }

    private void loadAdMobBanner(Activity activity, FrameLayout container) {
        AdView adView = new AdView(activity);
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
        adView.setAdUnitId(activity.getString(R.string.banner_app_id));
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
        Log.d(TAG, "AdMob banner loaded");
    }

    private void loadFacebookBanner(Activity activity, FrameLayout container) {
        fbBannerAd = new com.facebook.ads.AdView(
                activity,
                activity.getString(R.string.fb_banner_id),
                com.facebook.ads.AdSize.BANNER_HEIGHT_50
        );
        com.facebook.ads.AdListener listener = new com.facebook.ads.AdListener() {
            @Override public void onError(Ad ad, com.facebook.ads.AdError error) {
                Log.e(TAG, "FB Banner error: " + error.getErrorMessage());
            }
            @Override public void onAdLoaded(Ad ad) {
                Log.d(TAG, "FB Banner loaded");
            }
            @Override public void onAdClicked(Ad ad) {}
            @Override public void onLoggingImpression(Ad ad) {}
        };
        container.removeAllViews();
        container.addView(fbBannerAd);
        fbBannerAd.loadAd(fbBannerAd.buildLoadAdConfig().withAdListener(listener).build());
    }

    // ==========================================
    //  INTERSTITIAL ADS
    // ==========================================

    public void loadInterstitial(Activity activity) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            loadFacebookInterstitial(activity);
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
                        Log.d(TAG, "AdMob interstitial loaded");
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
        fbInterstitial = new com.facebook.ads.InterstitialAd(
                activity,
                activity.getString(R.string.fb_interstitial_id)
        );
        fbInterstitial.loadAd(
                fbInterstitial.buildLoadAdConfig()
                        .withAdListener(new InterstitialAdListener() {
                            @Override public void onInterstitialDisplayed(Ad ad) {}
                            @Override public void onInterstitialDismissed(Ad ad) {
                                Log.d(TAG, "FB interstitial dismissed");
                                loadFacebookInterstitial(activity); // reload
                            }
                            @Override public void onError(Ad ad, com.facebook.ads.AdError error) {
                                Log.e(TAG, "FB interstitial error: " + error.getErrorMessage());
                            }
                            @Override public void onAdLoaded(Ad ad) {
                                Log.d(TAG, "FB interstitial loaded");
                            }
                            @Override public void onAdClicked(Ad ad) {}
                            @Override public void onLoggingImpression(Ad ad) {}
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
        } else {
            showAdMobInterstitial(activity, callback);
        }
    }

    private void showAdMobInterstitial(Activity activity, InterstitialCallback callback) {
        if (admobInterstitial != null) {
            admobInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    admobInterstitial = null;
                    loadAdMobInterstitial(activity); // reload
                    if (callback != null) callback.onAdClosed();
                }
                @Override
                public void onAdFailedToShowFullScreenContent(AdError error) {
                    if (callback != null) callback.onAdFailedToLoad(error.getMessage());
                }
            });
            admobInterstitial.show(activity);
        } else {
            Log.d(TAG, "AdMob interstitial not ready, loading now...");
            loadAdMobInterstitial(activity);
            if (callback != null) callback.onAdClosed(); // proceed without ad
        }
    }

    private void showFacebookInterstitial(Activity activity, final InterstitialCallback callback) {
        if (fbInterstitial != null && fbInterstitial.isAdLoaded()) {
            fbInterstitial.loadAd(
                    fbInterstitial.buildLoadAdConfig()
                            .withAdListener(new InterstitialAdListener() {
                                @Override public void onInterstitialDisplayed(Ad ad) {}
                                @Override public void onInterstitialDismissed(Ad ad) {
                                    Log.d(TAG, "FB interstitial dismissed");
                                    loadFacebookInterstitial(activity); // reload
                                    if (callback != null) callback.onAdClosed();
                                }
                                @Override public void onError(Ad ad, com.facebook.ads.AdError error) {
                                    if (callback != null) callback.onAdFailedToLoad(error.getErrorMessage());
                                }
                                @Override public void onAdLoaded(Ad ad) {}
                                @Override public void onAdClicked(Ad ad) {}
                                @Override public void onLoggingImpression(Ad ad) {}
                            })
                            .build()
            );
            fbInterstitial.show();
        } else {
            Log.d(TAG, "FB interstitial not ready, loading now...");
            loadFacebookInterstitial(activity);
            if (callback != null) callback.onAdClosed(); // proceed without ad
        }
    }

    // ==========================================
    //  NATIVE ADS
    // ==========================================

    public void loadNativeAd(Activity activity, TemplateView template, NativeAdLayout fbNativeLayout) {
        if (!BuildConfig.ADS_SHOWN) return;

        if (NETWORK_FACEBOOK.equals(adNetwork)) {
            if (fbNativeLayout != null) {
                loadFacebookNativeAd(activity, fbNativeLayout);
            } else {
                Log.e(TAG, "Facebook NativeAdLayout is null");
            }
        } else {
            if (template != null) {
                loadAdMobNativeAd(activity, template);
            } else {
                Log.e(TAG, "AdMob TemplateView is null");
            }
        }
    }

    private void loadAdMobNativeAd(Activity activity, TemplateView template) {
        AdLoader adLoader = new AdLoader.Builder(activity, activity.getString(R.string.native_app_id))
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
        fbNativeAd = new NativeAd(activity, activity.getString(R.string.fb_native_id));
        NativeAdListener nativeAdListener = new NativeAdListener() {
            @Override
            public void onMediaDownloaded(Ad ad) {}
            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                Log.e(TAG, "FB Native failed: " + adError.getErrorMessage());
            }
            @Override
            public void onAdLoaded(Ad ad) {
                if (fbNativeAd == null || fbNativeAd != ad) return;
                inflateFacebookNativeAd(activity, fbNativeAd, nativeAdLayout);
            }
            @Override
            public void onAdClicked(Ad ad) {}
            @Override
            public void onLoggingImpression(Ad ad) {}
        };
        fbNativeAd.loadAd(fbNativeAd.buildLoadAdConfig().withAdListener(nativeAdListener).build());
    }

    private void inflateFacebookNativeAd(Activity activity, NativeAd nativeAd, NativeAdLayout nativeAdLayout) {
        if (nativeAdLayout == null) return;
        nativeAd.unregisterView();
        nativeAdLayout.setVisibility(View.VISIBLE);

        // Add the AdOptionsView
        AdOptionsView adOptionsView = new AdOptionsView(activity, nativeAd, nativeAdLayout);
        nativeAdLayout.removeAllViews();
        nativeAdLayout.addView(adOptionsView, 0);

        // Create native UI using the FB SDK views
        com.facebook.ads.MediaView nativeAdIcon = new com.facebook.ads.MediaView(activity);
        com.facebook.ads.MediaView nativeAdMedia = new com.facebook.ads.MediaView(activity);
        android.widget.TextView nativeAdTitle = new android.widget.TextView(activity);
        android.widget.TextView nativeAdBody = new android.widget.TextView(activity);
        android.widget.Button nativeAdCallToAction = new android.widget.Button(activity);

        // Set the text and media
        nativeAdTitle.setText(nativeAd.getAdvertiserName());
        nativeAdBody.setText(nativeAd.getAdBodyText());
        nativeAdCallToAction.setText(nativeAd.getAdCallToAction());

        // Basic styling
        nativeAdTitle.setTextSize(16);
        nativeAdTitle.setPadding(10, 10, 10, 10);
        nativeAdTitle.setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.colorTitleText));
        nativeAdBody.setTextSize(14);
        nativeAdBody.setPadding(10, 0, 10, 10);
        nativeAdBody.setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.colorTitleText));

        // Create a layout to hold these
        android.widget.LinearLayout container = new android.widget.LinearLayout(activity);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);
        container.setBackground(androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.rounded_border));

        container.addView(nativeAdTitle);
        container.addView(nativeAdMedia);
        container.addView(nativeAdBody);
        container.addView(nativeAdCallToAction);

        nativeAdLayout.addView(container);

        // Register the Title and CTA button to listen for clicks.
        java.util.List<View> clickableViews = new java.util.ArrayList<>();
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdCallToAction);
        nativeAd.registerViewForInteraction(container, nativeAdMedia, nativeAdIcon, clickableViews);
    }

    public void destroy() {
        try {
            if (fbBannerAd != null) {
                fbBannerAd.destroy();
                fbBannerAd = null;
            }
            if (fbInterstitial != null) {
                fbInterstitial.destroy();
                fbInterstitial = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error destroying ads: " + e.getMessage());
        }
    }
}
