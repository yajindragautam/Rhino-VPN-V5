package com.rhino.vpnapp.activity;

import static com.rhino.vpnapp.managers.UsageManager.KEY_TOTAL_CONNECTIONS;
import static com.rhino.vpnapp.managers.UsageManager.KEY_TOTAL_TIME;
import static com.rhino.vpnapp.managers.UsageManager.STR_CONNECTIONS;
import static com.rhino.vpnapp.managers.UsageManager.STR_TIME;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.managers.SessionManager;
import com.rhino.vpnapp.managers.UsageManager;
import com.rhino.vpnapp.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;

public class UsageActivity extends BaseAppActivity implements View.OnClickListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);
        setInsetMode(findViewById(R.id.rootView));

        final TextView txtAppInstalled = findViewById(R.id.txtAppInstalled);
        final TextView txtAppVersion = findViewById(R.id.txtAppVersion);
        txtAppVersion.setText(getString(R.string.strAppVersion, Utils.getAppVersionName()));

        final ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(this);

        try {
            final String strDeviceCreated = SessionManager.get().getDeviceCreated();
            if (!strDeviceCreated.equals("null")) {
                final long deviceTime = Long.parseLong(strDeviceCreated);
                final long nowTime = System.currentTimeMillis();
                final long elapsedTime = nowTime - deviceTime;

                if (nowTime > deviceTime) {
                    if (elapsedTime >= 3_600_000 && elapsedTime < 7_200_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toHours(elapsedTime) + " Hour Ago"));
                    } else if (elapsedTime >= 7_200_000 && elapsedTime < 86_400_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toHours(elapsedTime) + " Hours Ago"));
                    } else if (elapsedTime >= 86_400_000 && elapsedTime < 172_800_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toDays(elapsedTime) + " Day Ago"));
                    } else if (elapsedTime >= 172_800_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toDays(elapsedTime) + " Days Ago"));
                    } else if (elapsedTime >= 120_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toMinutes(elapsedTime) + " Minutes Ago"));
                    } else if (elapsedTime >= 60_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toMinutes(elapsedTime) + " Minute ago"));
                    } else {//if (elapsedTime < 60_000) {
                        txtAppInstalled.setText(getString(R.string.strAppInstalled, TimeUnit.MILLISECONDS.toSeconds(elapsedTime) + " Seconds Ago"));
                    }
                }
            }
        } catch (Exception e) {
            txtAppInstalled.setText(R.string.strNA);
        }

        // today
        final Date Today = Calendar.getInstance().getTime();
        final SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        final String TODAY = df.format(Today);

        // yesterday
        final Calendar Cal1 = Calendar.getInstance();
        Cal1.add(Calendar.DATE, -1);
        final String YESTERDAY = df.format(new Date(Cal1.getTimeInMillis()));

        // three days
        final Calendar Cal2 = Calendar.getInstance();
        Cal2.add(Calendar.DATE, -2);
        final String THREEDAYS = df.format(new Date(Cal2.getTimeInMillis()));

        final String WEEK = String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        final String MONTH = String.valueOf(Calendar.getInstance().get(Calendar.MONTH));
        final String YEAR = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

        final long TODAY_USAGE = UsageManager.get().getUsage(TODAY);
        final long YESTERDAY_USAGE = UsageManager.get().getUsage(YESTERDAY);
        final long DAYTHREE_USAGE = UsageManager.get().getUsage(THREEDAYS);
        final long WEEK_USAGE = UsageManager.get().getUsage(WEEK + YEAR);
        final long MONTH_USAGE = UsageManager.get().getUsage(MONTH + YEAR);

        final TextView txtUsageDataToday = findViewById(R.id.txtUsageDataToday);
        if (TODAY_USAGE == 0) {
            txtUsageDataToday.setText(R.string.strNA);
        } else if (TODAY_USAGE <= 1000_000) {
            txtUsageDataToday.setText(getString(R.string.strFormatKB, (TODAY_USAGE / 1000)));
        } else {
            txtUsageDataToday.setText(getString(R.string.strFormatMB, (TODAY_USAGE / 1000_000)));
        }

        final TextView txtUsageDataYesterday = findViewById(R.id.txtUsageDataYesterday);
        if (YESTERDAY_USAGE == 0) {
            txtUsageDataYesterday.setText(R.string.strNA);
        } else if (YESTERDAY_USAGE < 1000) {
            txtUsageDataYesterday.setText(R.string.strOneKB);
        } else if (YESTERDAY_USAGE <= 1000_000) {
            txtUsageDataYesterday.setText(getString(R.string.strFormatKB, (YESTERDAY_USAGE / 1000)));
        } else {
            txtUsageDataYesterday.setText(getString(R.string.strFormatMB, (YESTERDAY_USAGE / 1000_000)));
        }

        final TextView txtUsageDataDayThreeDate = findViewById(R.id.txtUsageDataDayThreeDate);
        txtUsageDataDayThreeDate.setText(THREEDAYS);

        final TextView txtUsageDataDayThree = findViewById(R.id.txtUsageDataDayThree);
        if (DAYTHREE_USAGE == 0) {
            txtUsageDataDayThree.setText(R.string.strNA);
        } else if (DAYTHREE_USAGE < 1000) {
            txtUsageDataDayThree.setText(R.string.strOneKB);
        } else if (DAYTHREE_USAGE <= 1000_000) {
            txtUsageDataDayThree.setText(getString(R.string.strFormatKB, (DAYTHREE_USAGE / 1000)));
        } else {
            txtUsageDataDayThree.setText(getString(R.string.strFormatMB, (DAYTHREE_USAGE / 1000_000)));
        }

        final TextView txtUsageDataThisWeek = findViewById(R.id.txtUsageDataThisWeek);
        if (WEEK_USAGE == 0) {
            txtUsageDataThisWeek.setText(R.string.strNA);
        } else if (WEEK_USAGE < 1000) {
            txtUsageDataThisWeek.setText(R.string.strOneKB);
        } else if (WEEK_USAGE <= 1000_000) {
            txtUsageDataThisWeek.setText(getString(R.string.strFormatKB, (WEEK_USAGE / 1000)));
        } else {
            txtUsageDataThisWeek.setText(getString(R.string.strFormatMB, (WEEK_USAGE / 1000_000)));
        }

        final TextView txtUsageDataThisMonth = findViewById(R.id.txtUsageDataThisMonth);
        if (MONTH_USAGE == 0) {
            txtUsageDataThisMonth.setText(R.string.strNA);
        } else if (MONTH_USAGE < 1000) {
            txtUsageDataThisMonth.setText(R.string.strOneKB);
        } else if (MONTH_USAGE <= 1000_000) {
            txtUsageDataThisMonth.setText(getString(R.string.strFormatKB, (MONTH_USAGE / 1000)));
        } else {
            txtUsageDataThisMonth.setText(getString(R.string.strFormatMB, (MONTH_USAGE / 1000_000)));
        }

        final long timeToday = UsageManager.get().getUsage(TODAY + STR_TIME);
        final long timeTesterday = UsageManager.get().getUsage(YESTERDAY + STR_TIME);
        final long timeTotal = UsageManager.get().getUsage(KEY_TOTAL_TIME);

        final String TodayTime = String.format(getString(R.string.string_of_two_number), (timeToday / (1000 * 60 * 60)) % 24) + ":" +
                String.format(getString(R.string.string_of_two_number), TimeUnit.MILLISECONDS.toMinutes(timeToday) % 60) + ":" +
                String.format(getString(R.string.string_of_two_number), (TimeUnit.MILLISECONDS.toSeconds(timeToday) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeToday))));

        final String YesterdayTime = String.format(getString(R.string.string_of_two_number), (timeTesterday / (1000 * 60 * 60)) % 24) + ":" +
                String.format(getString(R.string.string_of_two_number), TimeUnit.MILLISECONDS.toMinutes(timeTesterday) % 60) + ":" +
                String.format(getString(R.string.string_of_two_number), (TimeUnit.MILLISECONDS.toSeconds(timeTesterday) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTesterday))));

        final String TotalTime = String.format(getString(R.string.string_of_two_number), (timeTotal / (1000 * 60 * 60)) % 24) + ":" +
                String.format(getString(R.string.string_of_two_number), TimeUnit.MILLISECONDS.toMinutes(timeTotal) % 60) + ":" +
                String.format(getString(R.string.string_of_two_number), (TimeUnit.MILLISECONDS.toSeconds(timeTotal) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTotal))));

        final TextView txtUsageTimeToday = findViewById(R.id.txtUsageTimeToday);
        final TextView txtUsageTimeYesterday = findViewById(R.id.txtUsageTimeYesterday);
        final TextView txtUsageTimeTotal = findViewById(R.id.txtUsageTimeTotal);

        txtUsageTimeToday.setText(TodayTime);
        txtUsageTimeYesterday.setText(YesterdayTime);
        txtUsageTimeTotal.setText(TotalTime);

        final long connectionsToday = UsageManager.get().getUsage(TODAY + STR_CONNECTIONS);
        final long connectionsYesterday = UsageManager.get().getUsage(YESTERDAY + STR_CONNECTIONS);
        final long connectionsTotal = UsageManager.get().getUsage(KEY_TOTAL_CONNECTIONS);

        final TextView txtUsageConnectionToday = findViewById(R.id.txtUsageConnectionToday);
        final TextView txtUsageConnectionYesterday = findViewById(R.id.txtUsageConnectionYesterday);
        final TextView txtUsageConnectionTotal = findViewById(R.id.txtUsageConnectionTotal);

        txtUsageConnectionToday.setText(String.valueOf(connectionsToday));
        txtUsageConnectionYesterday.setText(String.valueOf(connectionsYesterday));
        txtUsageConnectionTotal.setText(String.valueOf(connectionsTotal));

        final LinearLayout imgGoForward = findViewById(R.id.imgGoForward);
        imgGoForward.setOnClickListener(v -> {
            try {
                finish();
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            } catch (Exception ignored) {
            }
        });

        findViewById(R.id.imgDeleteUsage).setOnClickListener(v -> {
            BottomSheetMaterialDialog mBottomSheetDialog = new BottomSheetMaterialDialog.Builder(this)
                    .setTitle(getString(R.string.strDeleteTitle))
                    .setMessage(getString(R.string.strDeleteMsg))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.lblYes), R.drawable.ic_delete_usage, (dialogInterface, which) -> {
                        UsageManager.get().clearAll();
                        dialogInterface.dismiss();
                        recreate();
                    })
                    .setNegativeButton(getString(R.string.lblNo), R.drawable.ic_close, (dialogInterface, which) -> dialogInterface.dismiss())
                    .build();

            mBottomSheetDialog.show();
        });

        goBack();
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
