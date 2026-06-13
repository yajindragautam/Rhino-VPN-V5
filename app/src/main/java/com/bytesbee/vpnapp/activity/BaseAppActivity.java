package com.bytesbee.vpnapp.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bytesbee.vpnapp.R;
import com.bytesbee.vpnapp.utils.Utils;

import java.util.Objects;


/**
 * Created by BytesBee.
 *
 * @author BytesBee
 * @link <a href="https://bytesbee.com">BytesBee</a>
 */

@SuppressLint("Registered")
public class BaseAppActivity extends AppCompatActivity {

    private ProgressDialog progress;
    public Activity mActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            EdgeToEdge.enable(this);
        }
    }

    public void setInsetMode(View view) {
//        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
//            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
//            // Apply the insets as a margin to the view. This solution sets only the
//            // bottom, left, and right dimensions, but you can apply whichever insets are
//            // appropriate to your layout. You can also update the view padding if that's
//            // more appropriate.
//            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
//            mlp.leftMargin = insets.left;
//            mlp.bottomMargin = insets.bottom;
//            mlp.rightMargin = insets.right;
//            v.setLayoutParams(mlp);
//
//            // Return CONSUMED if you don't want the window insets to keep passing
//            // down to descendant views.
//            return WindowInsetsCompat.CONSUMED;
//        });
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
//            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void initProgressDialog() {
        try {
            progress = new ProgressDialog(this);
            progress.setCancelable(false);
        } catch (Exception ignored) {

        }
    }

    public synchronized void showProgress() {
        if (progress == null) {
            initProgressDialog();
        }
        if (progress != null && !progress.isShowing()) {
            try {
                Objects.requireNonNull(progress.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                progress.show();
                progress.setContentView(R.layout.dialog_progress);
            } catch (Exception e) {
                Utils.getErrors(e);
            }
        }
    }

    synchronized void hideProgress() {
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }

    }
}
