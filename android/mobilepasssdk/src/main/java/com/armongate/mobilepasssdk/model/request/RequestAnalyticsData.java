package com.armongate.mobilepasssdk.model.request;

import com.armongate.mobilepasssdk.enums.AnalyticsResult;
import com.armongate.mobilepasssdk.enums.AnalyticsOS;
import com.armongate.mobilepasssdk.enums.PassMethod;
import com.armongate.mobilepasssdk.model.AnalyticsStep;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RequestAnalyticsData {
    @SerializedName("accessTime")
    public String accessTime;
    
    @SerializedName("duration")
    public long duration;
    
    @SerializedName("result")
    public String result;
    
    @SerializedName("method")
    public String method;
    
    @SerializedName("clubId")
    public String clubId;

    @SerializedName("qrCodeId")
    public String qrCodeId;
    
    @SerializedName("direction")
    public Integer direction;
    
    @SerializedName("os")
    public String os;
    
    @SerializedName("steps")
    public List<AnalyticsStep> steps;

    public RequestAnalyticsData(String accessTime, long duration, AnalyticsResult result,
                              PassMethod method, String clubId, String qrCodeId, Integer direction,
                              List<AnalyticsStep> steps) {
        this.accessTime = accessTime;
        this.duration = duration;
        this.result = result.getValue();
        this.method = method != null ? method.getValue() : null;
        this.clubId = clubId;
        this.qrCodeId = qrCodeId;
        this.direction = direction;
        this.os = AnalyticsOS.ANDROID.getValue();
        this.steps = steps;
    }
}