package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.model.request.RequestAnalyticsData;
import com.google.gson.annotations.SerializedName;

public class AnalyticsQueueItem {
    @SerializedName("request")
    public RequestAnalyticsData request;
    
    @SerializedName("timestamp")
    public long timestamp;

    public AnalyticsQueueItem(RequestAnalyticsData request) {
        this.request = request;
        this.timestamp = System.currentTimeMillis();
    }
}