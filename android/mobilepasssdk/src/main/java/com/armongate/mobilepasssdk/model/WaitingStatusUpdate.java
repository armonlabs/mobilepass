package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

public class WaitingStatusUpdate {

    public int messageId;
    public String messageText;
    public @Nullable Integer icon;

    public WaitingStatusUpdate(int messageId, String messageText, @Nullable Integer icon) {
        this.messageId = messageId;
        this.messageText = messageText;
        this.icon = icon;
    }
}
