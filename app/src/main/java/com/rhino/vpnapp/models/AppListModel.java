package com.rhino.vpnapp.models;

import android.graphics.drawable.Drawable;

public class AppListModel {
    private final String appName;
    private final Drawable appIcon;
    private final String appPackageName;
    private boolean isSelected;
    private boolean isSystemApp;

    public AppListModel(String appName, Drawable appIcon, String appPackageName, boolean isSelected) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.appPackageName = appPackageName;
        this.isSelected = isSelected;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }
}
