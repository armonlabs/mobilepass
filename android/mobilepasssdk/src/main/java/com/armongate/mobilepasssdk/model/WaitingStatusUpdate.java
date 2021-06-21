package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

public class WaitingStatusUpdate {

    public int background;
    public int message;
    public boolean showSpinner;
    public @Nullable Integer icon;

    public WaitingStatusUpdate(int background, int message, boolean showSpinner, @Nullable Integer icon) {
        this.background = background;
        this.message = message;
        this.showSpinner = showSpinner;
        this.icon = icon;
    }
}
