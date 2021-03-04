package com.armongate.mobilepasssdk.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

public class SettingsManager {
    // Singleton

    private static SettingsManager instance = null;
    private SettingsManager() { }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }

    // Public Functions

    public boolean checkLocationPermission(Context context) {
        boolean hasPermission = this.checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean servicesOn = this.checkLocationServicesOn(context);

        if (!servicesOn) {
            DelegateManager.getInstance().flowNeedLocationSettingsChange();
        }

        if (!hasPermission) {
            DelegateManager.getInstance().flowNeedPermissionLocation();
        }

        return hasPermission && servicesOn;
    }

    public boolean checkCameraPermission(Context context) {
        boolean hasPermission = this.checkPermission(context, Manifest.permission.CAMERA);

        if (!hasPermission) {
            DelegateManager.getInstance().flowNeedPermissionCamera();
        }

        return hasPermission;
    }

    // Private Functions

    private boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkLocationServicesOn(Context context) {
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        return gpsEnabled || networkEnabled;
    }
}