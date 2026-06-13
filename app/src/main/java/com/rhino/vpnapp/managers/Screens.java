package com.rhino.vpnapp.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.rhino.vpnapp.R;

/**
 * @author Prashant
 * {@code @company} BytesBee
 * @link <a href="http://bytesbee.com/">BytesBee</a>
 */
public class Screens {

    public static void showClearTopScreen(final Context context, final Class<?> cls) {
        final Intent intent = new Intent(context, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public static void showCustomScreen(final Context context, final Class<?> cls) {
        final Intent intent = new Intent(context, cls);
        context.startActivity(intent);
        try {
            ((Activity) context).overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        } catch (Exception ignored) {
        }
    }

}
