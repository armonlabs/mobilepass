package com.armongate.mobilepasssdk.manager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsManager {

    public static int REQUEST_CODE_CAMERA = 1001;
    public static int REQUEST_CODE_LOCATION = 1002;

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

    public boolean checkLocationEnabled(Context context) {
        return this.checkLocationServicesOn(context);
    }

    public boolean checkLocationPermission(Context context, Activity activity) {
        return this.checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_CODE_LOCATION, activity);
    }

    public boolean checkCameraPermission(Context context, Activity activity) {
        return this.checkPermission(context, Manifest.permission.CAMERA, REQUEST_CODE_CAMERA, activity);
    }

    // Private Functions

    private boolean checkPermission(Context context, String permission, int requestCode, Activity activity) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ActivityCompat.requestPermissions(activity, new String[] { permission }, requestCode);
        return false;
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