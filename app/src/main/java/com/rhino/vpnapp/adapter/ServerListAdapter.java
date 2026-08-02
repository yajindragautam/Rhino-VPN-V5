package com.rhino.vpnapp.adapter;

import static com.rhino.vpnapp.constants.IConstants.MINUS;
import static com.rhino.vpnapp.constants.IConstants.ONE;
import static com.rhino.vpnapp.constants.IConstants.ZERO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.interfaces.onNewServerSelectedListener;
import com.rhino.vpnapp.managers.AdManager;
import com.rhino.vpnapp.managers.SessionManager;
import com.rhino.vpnapp.models.Server;
import com.rhino.vpnapp.utils.Utils;

import java.util.ArrayList;

public class ServerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final ArrayList<Server> items;
    private final Context mContext;
    private static final int ITEM_VIEW_TYPE = ZERO;  // it represents items
    private static final int BANNER_AD_VIEW_TYPE = ONE; // it represents advertise
    private int row_index = 0;
    private final onNewServerSelectedListener onNewServerSelectedListener;

    public ServerListAdapter(Context mContext, ArrayList<Server> mItems, onNewServerSelectedListener onNewServerSelectedListener) {
        if (mItems == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = mItems;
        }
        this.mContext = mContext;
        this.onNewServerSelectedListener = onNewServerSelectedListener;
        final Server mySer = SessionManager.get().getServer();
        if (mySer != null && mySer.getIndex() != MINUS) {
            row_index = mySer.getIndex() + 1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == ITEM_VIEW_TYPE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item_servers, parent, false);
            return new ViewHolder(view);
        } else {
            return new AdViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item_banner_ads, parent, false));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder.getItemViewType() == ITEM_VIEW_TYPE) {
                Server mServer = items.get(holder.getAdapterPosition());
                ((ViewHolder) holder).imgCountry.setImageResource(mServer.getFlagIcon());
                ((ViewHolder) holder).txtCountryName.setText(mServer.getCountry());
                ((ViewHolder) holder).layout.setOnClickListener(view -> {
                    row_index = holder.getAdapterPosition();
                    notifyDataSetChanged();
                    onNewServerSelectedListener.onNewServerSelected(items.get(position));
                });
                if (row_index == position) {
                    ((ViewHolder) holder).layout.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_background));
                    ((ViewHolder) holder).imgSelected.setVisibility(View.VISIBLE);
                } else {
                    ((ViewHolder) holder).layout.setBackground(ContextCompat.getDrawable(mContext, R.drawable.rounded_border));
                    ((ViewHolder) holder).imgSelected.setVisibility(View.GONE);
                }
            } else if (holder.getItemViewType() == BANNER_AD_VIEW_TYPE) {
                AdViewHolder adViewHolder = (AdViewHolder) holder;
                if (mContext instanceof Activity) {
                    AdManager.get().loadBanner((Activity) mContext, adViewHolder.bannerContainer);
                }
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items == null || items.get(position) == null) {
            return BANNER_AD_VIEW_TYPE;
        }
        return ITEM_VIEW_TYPE;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtCountryName;
        private final ImageView imgCountry;
        private final ImageView imgSelected;
        private final ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCountry = itemView.findViewById(R.id.imgCountry);
            imgSelected = itemView.findViewById(R.id.imgSelected);
            txtCountryName = itemView.findViewById(R.id.txtCountryName);
            layout = itemView.findViewById(R.id.layout);
        }
    }

    public static class AdViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout bannerContainer;

        AdViewHolder(View view) {
            super(view);
            bannerContainer = itemView.findViewById(R.id.bannerContainer);
        }
    }
}

