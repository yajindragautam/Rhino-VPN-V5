package com.bytesbee.vpnapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bytesbee.vpnapp.R;
import com.bytesbee.vpnapp.activity.InternetSpeedActivity;
import com.bytesbee.vpnapp.activity.LogsActivity;
import com.bytesbee.vpnapp.activity.PrivacyPolicyActivity;
import com.bytesbee.vpnapp.activity.SelectServersActivity;
import com.bytesbee.vpnapp.activity.TermsOfServicesActivity;
import com.bytesbee.vpnapp.activity.UsageActivity;
import com.bytesbee.vpnapp.managers.Screens;
import com.bytesbee.vpnapp.models.Navigation;
import com.bytesbee.vpnapp.utils.Utils;
import com.vimalcvs.materialrating.MaterialFeedback;
import com.vimalcvs.materialrating.MaterialRating;

import java.util.ArrayList;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.ViewHolder> {
    private final ArrayList<Navigation> items;
    private final Context mContext;
    private final IClickListener iClickListener;
    private final FragmentManager fragmentManager;

    public NavigationAdapter(Context mContext, FragmentManager fragmentManager, ArrayList<Navigation> mItems, IClickListener cListener) {
        this.items = mItems;
        this.mContext = mContext;
        this.fragmentManager = fragmentManager;
        this.iClickListener = cListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, final int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item_navigation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.imgIcon.setImageResource(items.get(holder.getAdapterPosition()).getIcon());
        holder.txtTitle.setText(items.get(holder.getAdapterPosition()).getTitle());
        holder.layout.setOnClickListener(view -> {
            if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strSelectServer))) {
                Screens.showCustomScreen(mContext, SelectServersActivity.class);
//                Utils.showIntAds((Activity) mContext, SelectServersActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strUsageHistory))) {
                Screens.showCustomScreen(mContext, UsageActivity.class);
//                Utils.showIntAds((Activity) mContext, UsageActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strTools))) {
                /*
                 * If you need to hide the ads on this particular item click, uncomment below code and comment Utils.showIntAds.... code for stop the ads to showing.
                 * Same way for other click item, but make sure, you must have to use proper activity name.
                 * Check the above Select Server item. Do the same for it with particular Activity name.
                 * */
                Screens.showCustomScreen(mContext, InternetSpeedActivity.class);
//                Utils.showIntAds((Activity) mContext, InternetSpeedActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strLogs))) {
                Screens.showCustomScreen(mContext, LogsActivity.class);
//                Utils.showIntAds((Activity) mContext, LogsActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strPrivacyPolicy))) {
//                Screens.showCustomScreen(mContext, PrivacyPolicyActivity.class);
                Utils.showIntAds((Activity) mContext, PrivacyPolicyActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strTermsOfService))) {
//                Screens.showCustomScreen(mContext, TermsOfServicesActivity.class);
                Utils.showIntAds((Activity) mContext, TermsOfServicesActivity.class);
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strRate))) {
//                Utils.rateApp((Activity) mContext);
                MaterialRating feedBackDialog = new MaterialRating(mContext.getString(R.string.feedback_email));
                feedBackDialog.show(fragmentManager, "rating");
            } else if (items.get(holder.getAdapterPosition()).getTitle().equalsIgnoreCase(mContext.getString(R.string.strFeedback))) {
                MaterialFeedback materialFeedback = new MaterialFeedback(mContext.getString(R.string.feedback_email));
                materialFeedback.show(fragmentManager, "feedback");
            }
            iClickListener.closeDrawer();
            try {
                ((Activity) mContext).overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
            } catch (Exception e) {
                Utils.getErrors(e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitle;
        final ImageView imgIcon;
        final ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            layout = itemView.findViewById(R.id.layout);
        }
    }

    public interface IClickListener {
        void closeDrawer();
    }
}
