package com.bytesbee.vpnapp.activity;


import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.airbnb.lottie.LottieAnimationView;
import com.bytesbee.vpnapp.R;

import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;

/** @noinspection StringConcatenationInLoop*/
public class LogsActivity extends BaseAppActivity implements View.OnClickListener {
    private TextView txtClear, txtLogs;
    private String strLog = "";
    private LottieAnimationView animationView;
    private ImageView imgBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        setInsetMode(findViewById(R.id.rootView));
        init();
        initListeners();
        initData();
        goBack();
    }

    public void init() {
        txtClear = findViewById(R.id.txtClear);
        txtLogs = findViewById(R.id.txtLogs);
        animationView = findViewById(R.id.animationView);
        imgBack = findViewById(R.id.imgBack);
    }

    public void initListeners() {
        txtClear.setOnClickListener(this);
        imgBack.setOnClickListener(this);
    }

    public void initData() {
        if (VpnStatus.getlogbuffer().length > 0) {
            animationView.setVisibility(View.GONE);
            txtLogs.setVisibility(View.VISIBLE);

            LogItem[] logItems = VpnStatus.getlogbuffer();
            for (int i = logItems.length; i >= 1; i--) {
                strLog += logItems[i - 1] + "\n";
            }
            txtLogs.setText(strLog);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.txtClear) {
            /* clear all log data and set text empty */
            VpnStatus.clearLog();
            txtLogs.setText("");
            animationView.setVisibility(View.VISIBLE);
            txtLogs.setVisibility(View.GONE);
        }
    }

    private void goBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            }
        });
    }

}