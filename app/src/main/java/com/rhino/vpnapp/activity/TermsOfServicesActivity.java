package com.rhino.vpnapp.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.constants.IConstants;
import com.rhino.vpnapp.utils.Utils;

public class TermsOfServicesActivity extends BaseAppActivity implements View.OnClickListener {
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_services);
        setInsetMode(findViewById(R.id.rootView));
        init();
        initData();
        goBack();
    }

    public void init() {
        webView = findViewById(R.id.webView);
        final ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(this);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initData() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        Utils.setThemeToWebView(this, webView);
        webView.loadUrl(IConstants.PATH_ASSET_FOLDER + IConstants.TERMS_OF_USE);
        webView.setBackgroundColor(0);
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgBack) {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }
}