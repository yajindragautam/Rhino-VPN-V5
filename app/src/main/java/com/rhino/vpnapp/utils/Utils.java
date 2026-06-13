package com.rhino.vpnapp.utils;

import static com.rhino.vpnapp.constants.IConstants.MINUS;
import static com.rhino.vpnapp.constants.IConstants.ZERO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.rhino.vpnapp.BuildConfig;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.constants.IConstants;
import com.rhino.vpnapp.managers.Screens;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Random;

public class Utils {
    public static final boolean IS_TRIAL = false;

    public static void getErrors(final Exception e) {
        final String stackTrace = Log.getStackTraceString(e);
        Utils.sout(String.format("%s", stackTrace));
    }

    /**
     * to print message on console
     */
    public static void sout(final String msg) {
        if (IS_TRIAL) {
            System.out.println(IConstants.SOUT_MSG_PREFIX + msg);
        }
    }

    /**
     * shows ads
     *
     * @param mActivity context
     * @param template  which holds the advertisement
     */
    public static void initNativeAds(Activity mActivity, TemplateView template) {
        if (BuildConfig.ADS_SHOWN) {
            AdLoader adLoader = new AdLoader.Builder(mActivity, mActivity.getString(R.string.native_app_id))
                    .forNativeAd(template::setNativeAd).withAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
//                            Utils.sout("onAdClosed: ");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
//                            Utils.sout("onAdFailedToLoad: ");
                        }

                        @Override
                        public void onAdOpened() {
//                            Utils.sout("onAdOpened: ");
                            super.onAdOpened();
                        }

                        @Override
                        public void onAdLoaded() {
                            template.setVisibility(View.VISIBLE);
                            super.onAdLoaded();
                        }

                        @Override
                        public void onAdClicked() {
//                            Utils.sout("onAdClicked: ");
                            super.onAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                        }
                    })
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        }
    }

    private static InterstitialAd mInterstitialAd;

    public static void firstLoadAds(Activity mActivity) {
        if (BuildConfig.ADS_SHOWN) {
            InterstitialAd.load(mActivity, mActivity.getString(R.string.interstitial_app_id), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    mInterstitialAd = null;
                }
            });
        }
    }

    private static int LAUNCH_ADS = 1;
    private static final int COUNT_ADS = 3;

    public static void showIntAds(final Activity mActivity, final Handler handler) {
        showIntAds(mActivity, null, ZERO, handler);
    }

    public static void showIntAds(final Activity mActivity, final Class cls) {
        showIntAds(mActivity, cls, MINUS, null);
    }

    public static void showIntAds(final Activity mActivity, final Class cls, final int what, final Handler handler) {
        if (BuildConfig.ADS_SHOWN) {
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        openNextActivity(mActivity, cls, what, handler);
                        firstLoadAds(mActivity);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        openNextActivity(mActivity, cls, what, handler);
                    }
                });
                mInterstitialAd.show(mActivity);
            } else {
                openNextActivity(mActivity, cls, what, handler);
            }
        } else {
            openNextActivity(mActivity, cls, what, handler);
        }
    }

    public static void showIntAdsCount(final Activity mActivity, final Class cls) {
        showIntAdsCount(mActivity, cls, MINUS, null);
    }

    public static void showIntAdsCount(final Activity mActivity, final Class cls, final int what, final Handler handler) {
        if (BuildConfig.ADS_SHOWN) {
            if (mInterstitialAd != null && LAUNCH_ADS >= COUNT_ADS) {
                LAUNCH_ADS = 0;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        openNextActivity(mActivity, cls, what, handler);
                        firstLoadAds(mActivity);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        openNextActivity(mActivity, cls, what, handler);
                    }
                });
                mInterstitialAd.show(mActivity);
            } else {
                LAUNCH_ADS++;
                openNextActivity(mActivity, cls, what, handler);
            }
        } else {
            LAUNCH_ADS = 0;
            openNextActivity(mActivity, cls, what, handler);
        }
    }

    private static void openNextActivity(Activity mActivity, Class cls, final int what, final Handler handler) {
        if (handler != null) {
            final Message message = Message.obtain();
            handler.sendEmptyMessage(what);
            message.setTarget(handler);
            message.sendToTarget();
        } else {
            Screens.showCustomScreen(mActivity, cls);
        }
    }

    /**
     * check user is connected to internet or not
     *
     * @param mActivity context
     * @return true =>> user is connected to internet
     * false ==> user is not connected to internet
     */
    public static boolean checkInternetConnection(Activity mActivity) {
        final ConnectivityManager cm = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo nInfo = cm.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnectedOrConnecting();
    }

    /**
     * shows toast message
     *
     * @param mActivity context
     * @param message   message to be displayed
     */
    public static void showToast(Activity mActivity, String message) {
        Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * set webview theme as dark or light
     *
     * @param mActivity context
     * @param webView   webview which theme to be set
     */
    public static void setThemeToWebView(Activity mActivity, WebView webView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {

            int nightModeFlags = mActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                //Theme is switched to Night/Dark mode, turn on webview darkening
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            } else {
                //Theme is not switched to Night/Dark mode, turn off webview darkening
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            }
        }

    }

    public static String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static void setDarkMode(final boolean b) {
        try {
            if (b) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    public static Typeface getRegularFont(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins);
    }

    /**
     * min and max are to be understood inclusively
     */
    public static int getRandomNumber(int min, int max) {
        int random = (new Random()).nextInt((max - min) + 1) + min;
        if (BuildConfig.ADS_SHOWN && random % IConstants.ITEMS_PER_AD == ZERO) {
            getRandomNumber(min, max);
        }
        return random;
    }

    public static Intent getOpenUrlIntent(Context c, boolean external) {
        return new Intent(Intent.ACTION_VIEW);
    }
}
