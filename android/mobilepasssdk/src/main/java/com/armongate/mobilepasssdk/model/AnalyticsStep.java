package com.armongate.mobilepasssdk.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AnalyticsStep {
    @SerializedName("c")
    public Integer code;
    
    @SerializedName("m")
    public String message;
    
    @SerializedName("t")
    public String timestamp;

    public AnalyticsStep(Integer code, String message, Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedDate = sdf.format(timestamp);

        this.code = code;
        this.message = message;
        this.timestamp = formattedDate;
    }
}