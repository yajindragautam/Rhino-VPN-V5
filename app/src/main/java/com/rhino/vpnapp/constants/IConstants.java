package com.rhino.vpnapp.constants;

public interface IConstants {
    String SOUT_MSG_PREFIX = "Prashant :: ";

    int ITEMS_PER_AD = 3; // Show ads after each 3rd items like: 3, 6, 9, 12 .... etc.
    int MINUS = -1;
    int ZERO = 0;
    int ONE = 1;

    int USER = 1;
    int SYSTEM = 2;
    int ALL = 3;

    int ALL_APPS = 1;
    int SELECT_NOT = 2;
    int ONLY_SELECT = 3;

    String PATH_ASSET_FOLDER = "file:///android_asset/";

    //Go to /assets folder and replace below files.
    String PRIVACY_POLICY = "h_privacy_policy.html";
    String TERMS_OF_USE = "h_terms_of_service.html";

    String BUNDLE_KEY_SERVER = "key_server";

    /**
     * Do not change anything from the below text.
     */
    String CONNECTED = "CONNECTED";
    String DISCONNECTED = "DISCONNECTED";
    String WAIT = "WAIT";
    String AUTH = "AUTH";
    String RECONNECTING = "RECONNECTING";
    String NO_NETWORK = "NONETWORK";
    String AUTH_FAILED = "AUTH_FAILED";
    String USERPAUSE = "USERPAUSE";
    String CONNECTRETRY = "CONNECTRETRY";
    String ASSIGN_IP = "ASSIGN_IP";
    //    String EXITING = "EXITING";

    int MY_IP = 1;
    int VPN_IP = 2;
}
