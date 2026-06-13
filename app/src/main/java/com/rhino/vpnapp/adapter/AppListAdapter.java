package com.rhino.vpnapp.adapter;

import static com.rhino.vpnapp.constants.IConstants.ONE;
import static com.rhino.vpnapp.constants.IConstants.ZERO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.models.AppListModel;
import com.rhino.vpnapp.utils.Utils;
import com.l4digital.fastscroll.FastScroller;

import java.util.ArrayList;
import java.util.List;


public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> implements FastScroller.SectionIndexer {

    private List<AppListModel> mApps;
    private final Context mContext;
    private final SwitchListener switchListener;

    public AppListAdapter(Context mContext, ArrayList<AppListModel> mApps, SwitchListener switchListener) {
        this.mApps = mApps;
        this.mContext = mContext;
        this.switchListener = switchListener;
    }

    @Override
    public CharSequence getSectionText(int i) {
        try {
            return mApps.get(i).getAppName().substring(ZERO, ONE).toUpperCase();
        } catch (Exception e) {
            return String.valueOf(i);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item_installed_apps, parent, false);
        return new ViewHolder(v);
    }

    Boolean isTouched = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mApps != null && !mApps.isEmpty()) {
            AppListModel appModel = mApps.get(position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.txtAppName.setText(Html.fromHtml(appModel.getAppName(), Html.FROM_HTML_MODE_LEGACY));
                holder.txtPackageName.setText(Html.fromHtml(appModel.getAppPackageName(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.txtAppName.setText(Html.fromHtml(appModel.getAppName()));
                holder.txtPackageName.setText(Html.fromHtml(appModel.getAppPackageName()));
            }
            holder.switchOnOff.setChecked(appModel.isSelected());
            Glide.with(mContext).load(appModel.getAppIcon()).into(holder.appIcon);

            holder.switchOnOff.setOnTouchListener((view, motionEvent) -> {
                isTouched = true;
                return false;
            });

            holder.switchOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isTouched) {
                    isTouched = false;

                    if (isChecked) {
                        switchListener.onListener(appModel.getAppPackageName());
                    } else {
                        switchListener.offListener(appModel.getAppPackageName());
                    }
                }
            });
            holder.cardView.setAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.anim_slide_scale));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListData(List<AppListModel> items) {
        try {
            this.mApps = items;
            notifyDataSetChanged();
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearListData() {
        try {
            this.mApps = new ArrayList<>();
            notifyDataSetChanged();
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    @Override
    public int getItemCount() {
        return mApps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtAppName;
        private final TextView txtPackageName;
        private final ImageView appIcon;
        private final CardView cardView;
        private final SwitchCompat switchOnOff;

        public ViewHolder(View itemView) {
            super(itemView);
            txtAppName = itemView.findViewById(R.id.txtapp_name);
            txtPackageName = itemView.findViewById(R.id.txtapp_package);
            appIcon = itemView.findViewById(R.id.app_icon);
            switchOnOff = itemView.findViewById(R.id.switchOnOff);
            cardView = itemView.findViewById(R.id.cardView);
        }

    }

    public interface SwitchListener {
        void onListener(String packageName);

        void offListener(String packageName);
    }
}
