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
import com.facebook.ads.NativeAdLayout;
import com.rhino.vpnapp.managers.AdManager;
import com.rhino.vpnapp.managers.Screens;
import com.google.android.ads.nativetemplates.TemplateView;

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
        AdManager.get().loadNativeAd(mActivity, template, null);
    }

    public static void initNativeAds(Activity mActivity, TemplateView template, NativeAdLayout fbNativeLayout) {
        AdManager.get().loadNativeAd(mActivity, template, fbNativeLayout);
    }

    public static void firstLoadAds(Activity mActivity) {
        AdManager.get().loadInterstitial(mActivity);
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
        AdManager.get().showInterstitial(mActivity, new AdManager.InterstitialCallback() {
            @Override
            public void onAdClosed() {
                openNextActivity(mActivity, cls, what, handler);
            }

            @Override
            public void onAdFailedToLoad(String error) {
                openNextActivity(mActivity, cls, what, handler);
            }
        });
    }

    public static void showIntAdsCount(final Activity mActivity, final Class cls) {
        showIntAdsCount(mActivity, cls, MINUS, null);
    }

    public static void showIntAdsCount(final Activity mActivity, final Class cls, final int what, final Handler handler) {
        if (LAUNCH_ADS >= COUNT_ADS) {
            LAUNCH_ADS = 0;
            showIntAds(mActivity, cls, what, handler);
        } else {
            LAUNCH_ADS++;
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
