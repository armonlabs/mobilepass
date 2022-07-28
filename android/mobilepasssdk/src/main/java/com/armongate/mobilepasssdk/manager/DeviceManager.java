package com.armongate.mobilepasssdk.manager;

import android.content.Context;

import com.armongate.mobilepasssdk.constant.ServiceProviders;
import com.google.android.gms.common.GoogleApiAvailability;
import com.huawei.hms.api.HuaweiApiAvailability;

public class DeviceManager {

    public String getServiceProvider(Context context) {
        if(isGmsAvailable(context)) {
            return ServiceProviders.Google;
        } else if (isHmsAvailable(context)) {
            return ServiceProviders.Huawei;
        } else {
            return "";
        }
    }

    private boolean isHmsAvailable(Context context) {
        boolean isAvailable = false;

        if (null != context) {
            int result = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context);
            isAvailable = (com.huawei.hms.api.ConnectionResult.SUCCESS == result);
        }

        return isAvailable;
    }

    private boolean isGmsAvailable(Context context) {
        boolean isAvailable = false;

        if (null != context) {
            int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            isAvailable = (com.google.android.gms.common.ConnectionResult.SUCCESS == result);
        }

        return isAvailable;
    }
}
