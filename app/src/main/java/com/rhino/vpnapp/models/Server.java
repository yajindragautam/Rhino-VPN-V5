package com.rhino.vpnapp.models;

public class Server {
    private int index;
    private String country;
    private int flagIcon;
    private String ovpn;
    private String ovpnUserName;
    private String ovpnUserPassword;

    // ✅ Add this
    private String serverType;
    public static final String TYPE_VPNGATE = "vpngate";
    public static final String TYPE_VPNBOOK = "vpnbook";

    public Server() {
    }

    public Server(String country, int flagIcon) {
        this.country = country;
        this.flagIcon = flagIcon;
    }

    public Server(String country, int flagIcon, String ovpn) {
        this.country = country;
        this.flagIcon = flagIcon;
        this.ovpn = ovpn;
    }

    public Server(String country, int flagIcon, String ovpn, String ovpnUserName, String ovpnUserPassword) {
        this.country = country;
        this.flagIcon = flagIcon;
        this.ovpn = ovpn;
        this.ovpnUserName = ovpnUserName;
        this.ovpnUserPassword = ovpnUserPassword;
        this.serverType = TYPE_VPNGATE; // ✅ default is VPNGate
    }

    // ✅ New constructor with serverType
    public Server(String country, int flagIcon, String ovpn, String ovpnUserName, String ovpnUserPassword, String serverType) {
        this.country = country;
        this.flagIcon = flagIcon;
        this.ovpn = ovpn;
        this.ovpnUserName = ovpnUserName;
        this.ovpnUserPassword = ovpnUserPassword;
        this.serverType = serverType;
    }

    // ✅ Add getter/setter
    public String getServerType() { return serverType; }
    public void setServerType(String serverType) { this.serverType = serverType; }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getFlagIcon() {
        return flagIcon;
    }

    public void setFlagIcon(int flagIcon) {
        this.flagIcon = flagIcon;
    }

    public String getOvpn() {
        return ovpn;
    }

    public void setOvpn(String ovpn) {
        this.ovpn = ovpn;
    }

    public String getOvpnUserName() {
        return ovpnUserName;
    }

    public void setOvpnUserName(String ovpnUserName) {
        this.ovpnUserName = ovpnUserName;
    }

    public String getOvpnUserPassword() {
        return ovpnUserPassword;
    }

    public void setOvpnUserPassword(String ovpnUserPassword) {
        this.ovpnUserPassword = ovpnUserPassword;
    }
}
