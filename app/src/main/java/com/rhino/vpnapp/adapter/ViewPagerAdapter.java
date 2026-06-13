package com.rhino.vpnapp.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.models.Page;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Activity activity;
    private final List<Page> message;

    public ViewPagerAdapter(Activity activity, List<Page> message) {
        this.activity = activity;
        this.message = message;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.row_item_tour_page_adapter, parent, false);
        return new PagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        ((PagesViewHolder) holder).animationView.setAnimation(message.get(position).getIcon());
        ((PagesViewHolder) holder).txtTitle.setText(message.get(position).getTitle());
        ((PagesViewHolder) holder).txtMessage.setText(message.get(position).getMessage());
    }

    @Override
    public int getItemCount() {
        return message.size();
    }

    public static class PagesViewHolder extends RecyclerView.ViewHolder {
        final LottieAnimationView animationView;
        final TextView txtMessage;
        final TextView txtTitle;

        public PagesViewHolder(@NonNull View itemView) {
            super(itemView);
            animationView = itemView.findViewById(R.id.animationView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }
    }
}
