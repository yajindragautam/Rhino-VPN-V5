package com.rhino.vpnapp.activity;

import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import static com.rhino.vpnapp.constants.IConstants.MINUS;
import static com.rhino.vpnapp.constants.IConstants.MY_IP;
import static com.rhino.vpnapp.constants.IConstants.ONE;
import static com.rhino.vpnapp.constants.IConstants.ONLY_SELECT;
import static com.rhino.vpnapp.constants.IConstants.SELECT_NOT;
import static com.rhino.vpnapp.constants.IConstants.VPN_IP;
import static com.rhino.vpnapp.constants.IConstants.ZERO;
import static com.rhino.vpnapp.managers.SessionManager.KEY_ONLY_SELECT;
import static com.rhino.vpnapp.managers.SessionManager.KEY_SELECT_NOT;
import static com.rhino.vpnapp.managers.UsageManager.KEY_TOTAL_CONNECTIONS;
import static com.rhino.vpnapp.managers.UsageManager.STR_CONNECTIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.rhino.vpnapp.BuildConfig;
import com.rhino.vpnapp.R;
import com.rhino.vpnapp.adapter.NavigationAdapter;
import com.rhino.vpnapp.constants.IConstants;
import com.rhino.vpnapp.managers.SessionManager;
import com.rhino.vpnapp.managers.UsageManager;
import com.rhino.vpnapp.managers.VPNCredentialManager;
import com.rhino.vpnapp.models.IPModel;
import com.rhino.vpnapp.models.Navigation;
import com.rhino.vpnapp.models.Server;
import com.rhino.vpnapp.utils.Utils;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;

public class MainActivity extends BaseAppActivity implements View.OnClickListener {
    private DrawerLayout drawerLayout;
    private Activity mActivity;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    public static ArrayList<Server> items;
    private TextView txtStatus, txtCountryName, txtTime, txtUploadSpeed, txtDownloadSpeed;
    private TextView txtYourIPAddress, txtVPNIPAddress;
    private ImageView imgCountryFlag, imgInstalledApp;
    private TemplateView templateView;
    private ConstraintLayout layoutServers;
    private LottieAnimationView animationView, imgConnect, imgDisconnect, imgDownload, imgUpload, imgTime;
    private SwitchCompat darkModeOnOff;
    private boolean vpnStart = false;
    private boolean isBegin = false;
    private Server server;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setInsetMode(findViewById(R.id.mainRootLayout));
        mActivity = this;
//        Utils.initializeMobileAds(mActivity);
        init();
        initData();
        initListeners();

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.strNavOpen, R.string.strNavClose);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.colorTitleText));
        drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
        setRecyclerView();

        Utils.initNativeAds(mActivity, templateView);
        Utils.firstLoadAds(mActivity);

        askNotification13();
        goBack();
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            mHandler = new Handler(message -> {
                try {
                    Gson gson = new Gson();
                    IPModel ipModel = gson.fromJson((String) message.obj, IPModel.class);
                    String ip = ipModel.getIp();
//                    String ip = ipModel.getQuery();
                    Utils.sout("IP Value: " + ip + " = " + ipModel);
                    if (message.what == MY_IP) {
                        txtYourIPAddress.setText(ip);
                    } else if (message.what == VPN_IP) {
                        txtVPNIPAddress.setText(ip);
                    }
                } catch (Exception e) {
                    if (message.what == MY_IP) {
                        txtYourIPAddress.setText(getString(R.string.strDash));
                    } else if (message.what == VPN_IP) {
                        txtVPNIPAddress.setText(getString(R.string.strDash));
                    }
                }
                return true;
            });
        } catch (Exception ignored) {

        }
    }

    private void askNotification13() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGrant -> Utils.sout("PermissionLauncher: " + isGrant));

    public void init() {
        final Date Today = Calendar.getInstance().getTime();
        final SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        TODAY = df.format(Today);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        recyclerView = findViewById(R.id.recyclerView);
        txtStatus = findViewById(R.id.txtStatus);
        layoutServers = findViewById(R.id.layoutServers);
        templateView = findViewById(R.id.templateView);
        imgConnect = findViewById(R.id.imgConnect);
        imgDisconnect = findViewById(R.id.imgDisconnect);
        imgCountryFlag = findViewById(R.id.imgCountryFlag);
        imgInstalledApp = findViewById(R.id.imgInstalledApp);
        txtCountryName = findViewById(R.id.txtCountryName);
        txtTime = findViewById(R.id.txtTime);
        animationView = findViewById(R.id.animationView);
        imgDownload = findViewById(R.id.imgDownload);
        imgUpload = findViewById(R.id.imgUpload);
        imgTime = findViewById(R.id.imgTime);
        txtDownloadSpeed = findViewById(R.id.txtDownloadSpeed);
        txtUploadSpeed = findViewById(R.id.txtUploadSpeed);
        txtYourIPAddress = findViewById(R.id.txtYourIPAddress);
        txtVPNIPAddress = findViewById(R.id.txtVPNIPAddress);
        darkModeOnOff = findViewById(R.id.darkModeOnOff);

        final TextView mTxtVersionName = findViewById(R.id.txtVersionName);
        mTxtVersionName.setText(getString(R.string.version, Utils.getAppVersionName()));
        initializeServerList();
    }

    public void initData() {
        try {
            //set server based on condition if it is null set data from shared preference
            //if its not null means data is received  from ServerListActivity

            if (getIntent().getStringExtra(IConstants.BUNDLE_KEY_SERVER) == null) {
                server = SessionManager.get().getServer();
            } else {
                server = new Gson().fromJson(getIntent().getStringExtra(IConstants.BUNDLE_KEY_SERVER), Server.class);
                if (vpnStart) {
                    stopVpn();
                }

                prepareVpn();
            }
            txtCountryName.setText(server.getCountry());
            imgCountryFlag.setImageResource(server.getFlagIcon());
        } catch (Exception e) {
            Utils.getErrors(e);
        }

        VpnStatus.initLogCache(mActivity.getCacheDir());

        darkModeOnOff.setChecked(SessionManager.get().isDarkModeOn());

        darkModeOnOff.setOnCheckedChangeListener((compoundButton, b) -> {
            SessionManager.get().setOnOffDarkMode(b);
            Utils.setDarkMode(b);
        });

        callIPAddress(MY_IP);
    }

    private void initializeServerList() {
        List<Server> temp = addList();
        items = new ArrayList<>();
        addBannerAds(temp);
        try {
            if (SessionManager.get().getServer() == null) {
                SessionManager.get().saveServer(items.get(ZERO));
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    public List<Server> addList() {
        final List<Server> temp = new ArrayList<>();
        temp.add(new Server(getString(R.string.strAutoSelect), R.drawable.ic_auto_select, "vpnbook-us16-udp53.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));

//        temp.add(new Server(getString(R.string.strGermany), R.drawable.flag_germany, "germany1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        // ✅ VPNBook servers — pass empty credentials, will be fetched dynamically
        temp.add(new Server(getString(R.string.strUSA1), R.drawable.flag_usa, "vpnbook-us16-udp53.ovpn", "", "", Server.TYPE_VPNBOOK));
        temp.add(new Server(getString(R.string.strUSA2), R.drawable.flag_usa, "vpnbook-us178-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));

        temp.add(new Server(getString(R.string.strUK1), R.drawable.flag_uk, "vpnbook-uk68-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));
        temp.add(new Server(getString(R.string.strUK2), R.drawable.flag_uk, "vpnbook-uk205-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));

        temp.add(new Server(getString(R.string.strCanada1), R.drawable.flag_canada, "vpnbook-ca149-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));
        temp.add(new Server(getString(R.string.strCanada2), R.drawable.flag_canada, "vpnbook-ca196-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));

        temp.add(new Server(getString(R.string.strGermany1), R.drawable.flag_germany, "vpnbook-de20-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));
        temp.add(new Server(getString(R.string.strGermany1), R.drawable.flag_germany, "vpnbook-de220-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));

        temp.add(new Server(getString(R.string.strFrance1), R.drawable.flag_france, "vpnbook-fr200-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));
        temp.add(new Server(getString(R.string.strFrance2), R.drawable.flag_france, "vpnbook-fr2311-tcp443.ovpn", "", "", Server.TYPE_VPNBOOK));


        // ✅ VPNGate servers — use static credentials from strings.xml, no type needed (default)
        temp.add(new Server(getString(R.string.strJapan1), R.drawable.flag_japan, "japan1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strJapan2), R.drawable.flag_japan, "japan2.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strJapan3), R.drawable.flag_japan, "japan3.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strKorea1), R.drawable.flag_korea, "korea1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strKorea2), R.drawable.flag_korea, "korea2.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strKorea3), R.drawable.flag_korea, "korea3.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strRussia), R.drawable.flag_russia, "russia1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strRussia), R.drawable.flag_russia, "russia2.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strThailand1), R.drawable.flag_thailand, "thailand1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));
        temp.add(new Server(getString(R.string.strVietnam), R.drawable.flag_vietnam, "vietnam1.ovpn", getString(R.string.strOvpUserName), getString(R.string.strOvpPassword)));


        return temp;
    }

    /**
     * Adds banner ads to the items list.
     */
    private void addBannerAds(List<Server> temp) {
        // Loop through the items array and place a new banner ad in every ith position in the items List.
        int pos = 0;
        for (int i = ZERO; i < temp.size(); i++) {
            if (BuildConfig.ADS_SHOWN) {
                if (i % IConstants.ITEMS_PER_AD == ZERO && i > ZERO) {
                    items.add(null);
                    pos++;
                }
            }
            Server s = temp.get(i);
            if (s.getCountry().equalsIgnoreCase(getString(R.string.strAutoSelect))) {
                s.setIndex(MINUS);
            } else {
                s.setIndex(pos);
                pos++;
            }
            items.add(s);
        }
    }

    public void initListeners() {
        layoutServers.setOnClickListener(this);
        imgConnect.setOnClickListener(this);
        imgDisconnect.setOnClickListener(this);
        imgInstalledApp.setOnClickListener(this);
    }

    /**
     * set items in recyclerview
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));

        final ArrayList<Navigation> items = new ArrayList<>();
        items.add(new Navigation(getString(R.string.strSelectServer), R.drawable.ic_nb_server));
        items.add(new Navigation(getString(R.string.strUsageHistory), R.drawable.ic_nb_usage));
        items.add(new Navigation(getString(R.string.strTools), R.drawable.ic_nb_network_check));
        items.add(new Navigation(getString(R.string.strLogs), R.drawable.ic_nb_logs));
        items.add(new Navigation(getString(R.string.strPrivacyPolicy), R.drawable.ic_nb_policy));
        items.add(new Navigation(getString(R.string.strTermsOfService), R.drawable.ic_nb_termofservice));
        items.add(new Navigation(getString(R.string.strRate), R.drawable.ic_nb_star));
        items.add(new Navigation(getString(R.string.strFeedback), R.drawable.ic_nb_feedback));
        NavigationAdapter adapter = new NavigationAdapter(mActivity, getSupportFragmentManager(), items, () -> {
            if (drawerLayout != null) {
                if (drawerLayout.isOpen())
                    drawerLayout.close();
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.layoutServers) {
            Utils.showIntAdsCount(mActivity, SelectServersActivity.class);
//            Screens.showCustomScreen(mActivity, SelectServersActivity.class);
        } else if (id == R.id.imgConnect || id == R.id.imgDisconnect) {// if user is connected to any VPN server disconnect from currently connected server
            if (vpnStart) {
                DisconnectFromConnectedServer();
            } else {
                if (animationView.getVisibility() == View.GONE) {
                    Utils.showIntAds(mActivity, myHandler);
//                    prepareVpn();
                } else {
                    stopVpn();
                }
//                prepareVpn();
            }
        } else if (id == R.id.imgInstalledApp) {
            if (vpnStart) {
                Toast.makeText(mActivity, "Kindly disconnect and update this options to reflect new changes", Toast.LENGTH_SHORT).show();
            }
            final Intent intent = new Intent(mActivity, InstalledAppActivity.class);
            appLauncher.launch(intent);
        }
    }

    final ActivityResultLauncher<Intent> appLauncher = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
            });

    /**
     * Show show disconnect confirm dialog
     */
    public void DisconnectFromConnectedServer() {
        BottomSheetMaterialDialog mBottomSheetDialog = new BottomSheetMaterialDialog.Builder(this)
                .setTitle(getString(R.string.strDisConnect))
                .setMessage(getString(R.string.strDisconnectMsg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.lblDisconnect), R.drawable.ic_disconnect, (dialogInterface, which) -> {
                    stopVpn();
                    dialogInterface.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), R.drawable.ic_close, (dialogInterface, which) -> dialogInterface.dismiss())
                .build();

        mBottomSheetDialog.show();
    }

    public final Handler myHandler = new Handler(message -> {
        prepareVpn();
        return true;
    });

    public void stopVpn() {
        try {
            isBegin = false;
            hideShowLoading(false);
            OpenVPNService.abortConnectionVPN = true;
            ProfileManager.setConntectedVpnProfileDisconnected(this);
            OpenVPNThread.stop();
            txtStatus.setText(getString(R.string.strTapStart));
        } catch (Exception e) {
            Utils.getErrors(e);
        }

    }

    private VpnProfile vpnProfile;

    private void startVpn() {
        try {
            //===================================================
            final int selectedItem = SessionManager.get().getConnectTunnel();
            ArrayList<String> bypassPackages = new ArrayList<>();
            try {
                if (selectedItem == SELECT_NOT) {
                    bypassPackages = SessionManager.get().getPackageList(KEY_SELECT_NOT);
                    vpnProfile.mAllowedAppsVpnAreDisallowed = true;
                } else if (selectedItem == ONLY_SELECT) {
                    bypassPackages = SessionManager.get().getPackageList(KEY_ONLY_SELECT);
                    vpnProfile.mAllowedAppsVpnAreDisallowed = false;
                }
                // ✅ Add this check — if no apps selected, route ALL traffic
                if (bypassPackages == null || bypassPackages.isEmpty()) {
                    vpnProfile.mAllowedAppsVpn.clear();
                    vpnProfile.mAllowAppVpnBypass = false;  // ✅ No bypass
                    vpnProfile.mAllowedAppsVpnAreDisallowed = true;
                }
            } catch (Exception e) {
                Utils.getErrors(e);
            }
            if (bypassPackages != null && !bypassPackages.isEmpty()) {
                vpnProfile.mAllowedAppsVpn.addAll(bypassPackages);
                vpnProfile.mAllowAppVpnBypass = true;
            }

            ProfileManager.setTemporaryProfile(mActivity, vpnProfile);
            VPNLaunchHelper.startOpenVpn(vpnProfile, mActivity, "New country selection", false);

            isBegin = true;
            start_vpn();
            //===========

            txtStatus.setText(getString(R.string.strWaitingMsg, txtCountryName.getText()));
            txtStatus.setTextColor(getResources().getColor(R.color.colorTitleText));
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    private void prepareVpn() {
        if (!vpnStart && !isBegin) {
            if (Utils.checkInternetConnection(mActivity)) {
                Server autoSelect = null;
                try {
                    if (server.getCountry().equalsIgnoreCase(getString(R.string.strAutoSelect))) {
                        final int totalSize = MainActivity.items.size() - ONE;
                        final int index = totalSize - (BuildConfig.ADS_SHOWN ? (totalSize / IConstants.ITEMS_PER_AD) : ZERO);
                        autoSelect = MainActivity.items.get(Utils.getRandomNumber(ONE, index));
                    } else {
                        autoSelect = server;
                    }

                    Utils.sout("Server Name:: " + autoSelect.getCountry() + " >> " + autoSelect.getIndex());
                    InputStream conf = getAssets().open(autoSelect.getOvpn());
                    InputStreamReader isr = new InputStreamReader(conf);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder config = new StringBuilder();
                    String line;
                    while (true) {
                        line = br.readLine();
                        if (line == null) break;
                        config.append(line).append("\n");

                    }
                    // ✅ Add these lines after reading the file
                    config.append("redirect-gateway def1 bypass-dhcp").append("\n");
                    config.append("dhcp-option DNS 8.8.8.8").append("\n");
                    config.append("dhcp-option DNS 8.8.4.4").append("\n");
                    config.append("--data-ciphers DEFAULT:AES-128-CBC").append("\n");
//                    config.append("--data-ciphers DEFAULT:AES-128-CBC").append("\n");
                    br.readLine();

                    ConfigParser configParser = new ConfigParser();
                    configParser.parseConfig(new StringReader(config.toString()));
                    vpnProfile = configParser.convertProfile();

                    if (vpnProfile.checkProfile(mActivity) != R.string.no_error_found) {
                        throw new RemoteException(getString(vpnProfile.checkProfile(mActivity)));
                    }

                    vpnProfile.mName = autoSelect.getCountry();
                    vpnProfile.mProfileCreator = mActivity.getPackageName();
                    vpnProfile.mDNS1 = VpnProfile.DEFAULT_DNS1;
                    vpnProfile.mDNS2 = VpnProfile.DEFAULT_DNS2;
                    vpnProfile.mOverrideDNS = true;

                } catch (Exception e) {
                    Utils.getErrors(e);
                }

                final Server finalAutoSelect = autoSelect;

                // ✅ KEY LOGIC: Branch based on server type
                if (finalAutoSelect != null &&
                        Server.TYPE_VPNBOOK.equals(finalAutoSelect.getServerType())) {

                    // 🔄 VPNBook: fetch dynamic credentials from your backend
                    hideShowLoading(true);
                    VPNCredentialManager.fetchCredentials(new VPNCredentialManager.CredentialCallback() {
                        @Override
                        public void onSuccess(String username, String password) {
                            // Cache for offline fallback
                            SessionManager.get().setVpnCredentials(username, password);

                            runOnUiThread(() -> {
                                try {
                                    vpnProfile.mUsername = username;
                                    vpnProfile.mPassword = password;
                                    launchVpn(finalAutoSelect);
                                } catch (Exception e) {
                                    Utils.getErrors(e);
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> {
                                try {
                                    // Try cached credentials
                                    String cachedUser = SessionManager.get().getVpnUsername();
                                    String cachedPass = SessionManager.get().getVpnPassword();

                                    if (cachedPass != null) {
                                        vpnProfile.mUsername = cachedUser;
                                        vpnProfile.mPassword = cachedPass;
                                        launchVpn(finalAutoSelect);
                                    } else {
                                        hideShowLoading(false);
                                        Utils.showToast(mActivity, "Failed to fetch VPNBook credentials. Try again.");
                                    }
                                } catch (Exception e) {
                                    Utils.getErrors(e);
                                }
                            });
                        }
                    });

                } else {
                    // ✅ VPNGate: use static credentials directly — no API call needed
                    try {
                        vpnProfile.mUsername = finalAutoSelect.getOvpnUserName();
                        vpnProfile.mPassword = finalAutoSelect.getOvpnUserPassword();
                        launchVpn(finalAutoSelect);
                    } catch (Exception e) {
                        Utils.getErrors(e);
                    }
                }

            } else {
                Utils.showToast(mActivity, getString(R.string.strNoInternetConnectionMsg));
            }
        } else {
            stopVpn();
        }
    }

    // ✅ Extracted helper to avoid duplicate launch code
    private void launchVpn(Server autoSelect) {
        try {
            Intent intent = VpnService.prepare(mActivity);
            if (intent != null)
                resultLauncher.launch(intent);
            else
                startVpn();

            if (autoSelect != null) {
                txtCountryName.setText(autoSelect.getCountry());
            }
            txtStatus.setText(getString(R.string.strWaitingMsg, txtCountryName.getText()));
            txtStatus.setTextColor(getResources().getColor(R.color.colorTitleText));
            hideShowLoading(true);
        } catch (Exception e) {
            Utils.getErrors(e);
        }
    }

    private String TODAY;

    private void start_vpn() {
        long connection_today = UsageManager.get().getUsage(TODAY + STR_CONNECTIONS);
        long connection_total = UsageManager.get().getUsage(KEY_TOTAL_CONNECTIONS);
        UsageManager.get().setUsage(TODAY + STR_CONNECTIONS, connection_today + 1);
        UsageManager.get().setUsage(KEY_TOTAL_CONNECTIONS, connection_total + 1);
    }

    private String conn = "";

    /**
     * Status change with corresponding vpn connection status
     *
     * @param connectionState states which we get onRecieve() of broadcast
     */
    public void setStatus(String connectionState) {
        if (connectionState != null) {
            conn = connectionState;
            Utils.sout("ConnectionState:: " + connectionState);
            switch (connectionState) {
                case IConstants.AUTH_FAILED:
                    txtStatus.setText(getString(R.string.strAuthFailedMsg));
                    break;

                case IConstants.CONNECTRETRY:
                case IConstants.WAIT:
                    txtStatus.setText(getString(R.string.strWaitingMsg, txtCountryName.getText()));
                    txtStatus.setTextColor(getResources().getColor(R.color.colorTitleText));
                    hideShowLoading(true);
                    break;

                case IConstants.AUTH:
                    txtStatus.setText(getString(R.string.strAuthMsg));
                    txtStatus.setTextColor(getResources().getColor(R.color.colorTitleText));
                    hideShowLoading(true);
                    break;

                case IConstants.NO_NETWORK:
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        if (conn.equalsIgnoreCase(IConstants.NO_NETWORK)) {
                            txtStatus.setText(getString(R.string.strNoIntentConnection));
                            txtStatus.setTextColor(getResources().getColor(R.color.colorTitleText));
                            hideShowLoading(true);
                            stopTimer();
                        }
                    }, 1000 * 10);
                    break;

                case IConstants.CONNECTED:
                    vpnStart = true;// it will use after restart this activity
                    isBegin = true;
                    txtStatus.setText(getString(R.string.strConnected));
                    imgConnect.setVisibility(View.GONE);
                    imgDisconnect.setVisibility(View.VISIBLE);
                    hideShowLoading(false);
                    imgDownload.playAnimation();
                    imgUpload.playAnimation();
                    imgTime.playAnimation();
                    startTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    callIPAddress(VPN_IP);

                    break;

                case IConstants.USERPAUSE:
                    break;

                case IConstants.ASSIGN_IP:
//                    callIPAddress(VPN_IP);
                    break;

                case IConstants.RECONNECTING:
                case IConstants.DISCONNECTED:
                    isBegin = false;
                    if (vpnStart) {
                        vpnStart = false;
                        OpenVPNService.setDefaultStatus();
                        txtStatus.setText(getString(R.string.strTapStart));
                        stopTimer();
                    }
                    txtVPNIPAddress.setText(getString(R.string.strDash));
                    imgConnect.setVisibility(View.VISIBLE);
                    imgDisconnect.setVisibility(View.GONE);
                    hideShowLoading(false);
                    break;

            }
        }
    }

    private Handler backgroundHandler;

    private void initHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    private void callIPAddress(int ipAddr) {
        initHandlerThread();
        backgroundHandler.postDelayed(() -> {
            try {
                String myOwnIP = getMyOwnIP();
                Message msg = Message.obtain(mHandler, ipAddr, myOwnIP);
                msg.sendToTarget();
            } catch (Exception e) {
                Utils.getErrors(e);
            }
        }, 200);
    }

    private Handler mHandler;
    private HttpURLConnection urlConnection;

    private String getMyOwnIP() throws IllegalArgumentException {
        StringBuilder resp = new StringBuilder();
        try {
//            final URL url = new URL("http://www.ip-api.com/json");

//            final URL url = new URL ("https://api.ipify.org/?format=json");

            final URL url = new URL("https://ipinfo.io/json");//ip
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while (true) {
                String line = in.readLine();
                if (line == null)
                    return resp.toString();
                resp.append(line);
            }
        } catch (Exception e) {
            Utils.getErrors(e);
            return "";
        } finally {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
                Utils.getErrors(e);
            }
        }
    }

    private long startTimer;
    private final Handler timerHandler = new Handler();

    private final Runnable timerRunnable = new Runnable() {
        public void run() {
            try {
                long millis = System.currentTimeMillis() - startTimer;
                Date date = new Date(millis);
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formatted = formatter.format(date);
                txtTime.setText(formatted);
                int interval = 500;
                timerHandler.postDelayed(this, interval);
            } catch (Exception ignored) {
            }
        }
    };

    private void stopTimer() {
        try {
            imgDownload.pauseAnimation();
            imgUpload.pauseAnimation();
            imgTime.pauseAnimation();
            imgDownload.setFrame(0);
            imgUpload.setFrame(0);
            imgTime.setFrame(0);
            if (timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
                txtTime.setText(R.string.strStaticTime);
            }
        } catch (Exception e) {
            Utils.sout(e.getMessage());
        }
    }

    /**
     * update textview with  latest duration user is connected to VPN
     *
     * @param bytesOut upload speed
     * @param byteIn   download speed
     */
    public void updateConnectionStatus(String byteIn, String bytesOut) {
        txtDownloadSpeed.setText(byteIn);
        txtUploadSpeed.setText(bytesOut);
    }

    /**
     * Receives broadcast message
     */
    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                Utils.getErrors(e);
            }
            try {
                String strDownloadSpeed, strUploadSpeed;
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (byteIn == null)
                    strDownloadSpeed = getString(R.string.strStaticSpeed);
                else
                    strDownloadSpeed = byteIn.split("-")[0];
                strDownloadSpeed = strDownloadSpeed.substring(1);

                if (byteOut == null)
                    strUploadSpeed = getString(R.string.strStaticSpeed);
                else
                    strUploadSpeed = byteOut.split("-")[0];
                strUploadSpeed = strUploadSpeed.substring(1);


                updateConnectionStatus(strDownloadSpeed, strUploadSpeed);
            } catch (Exception e) {
                Utils.getErrors(e);
            }
        }
    };

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        try {
            LocalBroadcastManager.getInstance(mActivity).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));

            if (server == null) {
                server = SessionManager.get().getServer();
            }
        } catch (Exception e) {
            Utils.getErrors(e);
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private long exitTime = 0;

    private void goBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitApp();
            }
        });
    }

    public void exitApp() {
        int DEFAULT_DELAY = 2000;
        if (drawerLayout != null) {
            if (drawerLayout.isOpen()) {
                drawerLayout.close();
                return;
            }
        }
        if ((System.currentTimeMillis() - exitTime) > DEFAULT_DELAY) {
            try {

                final Snackbar snackbar = Snackbar.make(findViewById(R.id.mainRootLayout), getString(R.string.press_again_to_exit), Snackbar.LENGTH_LONG);
                final View sbView = snackbar.getView();
                final TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setTypeface(Utils.getRegularFont(mActivity));
                textView.setTextColor(ContextCompat.getColor(mActivity, R.color.colorSnackBar));
                snackbar.show();
            } catch (Exception e) {
                Utils.getErrors(e);
                Utils.showToast(mActivity, getString(R.string.press_again_to_exit));
            }
            exitTime = System.currentTimeMillis();
        } else {
            this.moveTaskToBack(true);
        }
    }

    /**
     * shows lottie animation  when user is about to connect to VPN
     *
     * @param value true ==> shows animation
     *              value false ==> hides animation
     */
    public void hideShowLoading(boolean value) {
        if (value) {
            animationView.setVisibility(View.VISIBLE);
        } else {
            animationView.setVisibility(View.GONE);
            imgConnect.setVisibility(View.VISIBLE);
        }
    }

    /**
     * when permission for VPN is given by user onActivityResult
     * will be called and VPN starts
     */
    final ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        startVpn();
                    } catch (Exception e) {
                        Utils.getErrors(e);
                    }
                } else {
                    Utils.sout(getString(R.string.strPermissionDeniedMsg));
                }
            });
}