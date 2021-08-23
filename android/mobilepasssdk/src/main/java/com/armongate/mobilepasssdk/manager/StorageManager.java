package com.armongate.mobilepasssdk.manager;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.armongate.mobilepasssdk.constant.LogCodes;

public class StorageManager {

    // Singleton

    private static StorageManager  instance = null;
    private StorageManager () { }

    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager ();
        }

        return instance;
    }

    // Constants

    private static final String DEFAULT_STRING_VALUE    = "";
    private static final Object LOCK                    = new Object();

    // Public Functions

    public boolean setValue(Context context, String key, String value, SecureAreaManager secureStoreInstance) {
        String storeValue = value;

        if (!TextUtils.isEmpty(storeValue)) {
            try {
                storeValue = secureStoreInstance.encryptData(storeValue);
            } catch (Exception ex) {
                LogManager.getInstance().error("Add item to secure storage failed with error: " + ex.getLocalizedMessage(), LogCodes.CONFIGURATION_STORAGE);
                return false;
            }
        }

        return sharedPreferencesSetValue(context, key, storeValue);
    }

    public boolean setValue(Context context, String key, String value) {
        return sharedPreferencesSetValue(context, key, value);
    }

    public String getValue(Context context, String key, SecureAreaManager secureStoreInstance) {
        String storedValue = sharedPreferencesGetValue(context, key);

        if (!TextUtils.isEmpty(storedValue)) {
            try {
                storedValue = secureStoreInstance.decryptData(storedValue);
            } catch (Exception ex) {
                LogManager.getInstance().error("Get item from secure storage failed with error: " + ex.getLocalizedMessage(), LogCodes.CONFIGURATION_STORAGE);
                return "";
            }
        }

        return storedValue;
    }

    public String getValue(Context context, String key) {
        return sharedPreferencesGetValue(context, key);
    }

    public boolean deleteValue(Context context, String key) {
        return sharedPreferencesDeleteValue(context, key);
    }

    // Private Functions

    private boolean sharedPreferencesSetValue(Context context, String key, String value) {
        synchronized (LOCK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().putString(key, value).commit();
        }
    }

    private String sharedPreferencesGetValue(Context context, String key) {
        synchronized (LOCK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString(key, DEFAULT_STRING_VALUE);
        }
    }

    private boolean sharedPreferencesDeleteValue(Context context, String key) {
        synchronized (LOCK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.edit().remove(key).commit();
        }
    }

}
