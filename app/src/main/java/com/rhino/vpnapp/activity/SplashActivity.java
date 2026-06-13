package com.rhino.vpnapp.activity;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.rhino.vpnapp.BuildConfig;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.managers.Screens;
import com.rhino.vpnapp.managers.SessionManager;

public class SplashActivity extends AppCompatActivity {
    /**
     * Number of seconds to count down before showing the app open ad. This simulates the time needed
     * to load the app.
     */
    private long DELAY = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //make translucent statusBar on kitkat devices
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        //make fully Android Transparent Status bar
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_splash);

        if (BuildConfig.ADS_SHOWN) {
            DELAY = 5;
            createTimer(DELAY);
        } else {
            DELAY = 3000;
            startMainActivity();
        }
    }

    private void createTimer(long seconds) {
        CountDownTimer countDownTimer =
                new CountDownTimer(seconds * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        Application application = getApplication();

                        // If the application is not an instance of MyApplication, log an error message and
                        // start the MainActivity without showing the app open ad.
                        if (!(application instanceof UIApplication)) {
                            startMainActivity();
                            return;
                        }

                        // Show the app open ad.
                        ((UIApplication) application).showAdIfAvailable(SplashActivity.this, () -> startMainActivity());
                    }
                };
        countDownTimer.start();
    }

    /**
     * Start the MainActivity.
     */
    public void startMainActivity() {
        final Handler handler = new Handler();

        handler.postDelayed(() -> {
            Class cls;
            if (SessionManager.getInstance(this).isEntryDone()) {
                cls = MainActivity.class;
            } else {
                cls = TakeToTourActivity.class;
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            Screens.showClearTopScreen(getApplicationContext(), cls);
        }, DELAY);
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }
}