package com.rhino.vpnapp.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.adapter.ServersPageAdapter;
import com.rhino.vpnapp.fragments.FreeServersFragment;

import java.util.ArrayList;
import java.util.List;

public class SelectServersActivity extends BaseAppActivity implements View.OnClickListener {
    private ViewPager2 viewPager;
    private ImageView imgBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_servers);
        setInsetMode(findViewById(R.id.rootView));
        init();
        initData();
        initListeners();
        goBack();
    }

    public void init() {
        viewPager = findViewById(R.id.viewPager);
        imgBack = findViewById(R.id.imgBack);
    }

    public void initData() {
        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(new FreeServersFragment());
        final FragmentStateAdapter pagerAdapter = new ServersPageAdapter(this, fragments);
        viewPager.setAdapter(pagerAdapter);
    }

    public void initListeners() {
        imgBack.setOnClickListener(this);
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
        if (view.getId() == R.id.imgBack) {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

}