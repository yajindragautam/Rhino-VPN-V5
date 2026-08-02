package com.rhino.vpnapp.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.activity.MainActivity;
import com.rhino.vpnapp.adapter.ServerListAdapter;
import com.rhino.vpnapp.constants.IConstants;
import com.rhino.vpnapp.managers.SessionManager;
import com.rhino.vpnapp.models.Server;
import com.rhino.vpnapp.utils.Utils;
import com.google.gson.Gson;

public class FreeServersFragment extends Fragment {
    private RecyclerView recyclerView;

    public FreeServersFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_free_servers, container, false);
        init(view);
        initData();
        return view;
    }

    public void init(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    public void initData() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        setRecyclerView();
    }


    /**
     * sets items
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setRecyclerView() {
        ServerListAdapter serverListAdapter = new ServerListAdapter(getActivity(), MainActivity.items, server -> {
            Utils.sout(server.getCountry());
            SessionManager.get().saveServer(server);
            Utils.showIntAds(getActivity(), myHandler);
        });
        recyclerView.setAdapter(serverListAdapter);
        serverListAdapter.notifyDataSetChanged();
    }

    private final Handler myHandler = new Handler(message -> {
        Server server = SessionManager.get().getServer();
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(IConstants.BUNDLE_KEY_SERVER, new Gson().toJson(server));
            startActivity(intent);
        }
        return true;
    });
}