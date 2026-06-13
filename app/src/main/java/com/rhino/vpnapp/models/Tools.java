package com.rhino.vpnapp.models;

public class Tools {
    int Icon;
    String Title;

    public Tools(int Icon, String Title) {
        this.Icon = Icon;
        this.Title = Title;
    }

    public int getIcon() {
        return Icon;
    }

    public void setIcon(int icon) {
        Icon = icon;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }
}
