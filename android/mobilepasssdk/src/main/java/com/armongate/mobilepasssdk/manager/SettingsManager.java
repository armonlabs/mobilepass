package com.armongate.mobilepasssdk.manager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class SettingsManager {

    public static int REQUEST_CODE_CAMERA = 1001;
    public static int REQUEST_CODE_LOCATION = 1002;
    public static int REQUEST_CODE_BLE_SCAN = 1003;

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
        return this.checkPermission(context, new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION } , REQUEST_CODE_LOCATION, activity);
    }

    public boolean checkCameraPermission(Context context, Activity activity) {
        return this.checkPermission(context, new String[] { Manifest.permission.CAMERA }, REQUEST_CODE_CAMERA, activity);
    }

    public boolean checkBluetoothScanPermission(Context context, Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return this.checkPermission(context, new String[] { Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT }, REQUEST_CODE_BLE_SCAN, activity);
        } else {
            return true;
        }
    }

    // Private Functions

    private boolean checkPermission(Context context, String[] permissions, int requestCode, Activity activity) {
        boolean result = true;

        for (String permission :
                permissions) {
            result = result && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }

        if (!result) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }

        return result;
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