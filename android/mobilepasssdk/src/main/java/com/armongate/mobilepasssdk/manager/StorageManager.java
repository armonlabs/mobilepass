package com.armongate.mobilepasssdk.manager;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.armongate.mobilepasssdk.constant.StorageKeys;
import com.armongate.mobilepasssdk.model.StorageDataDevice;
import com.armongate.mobilepasssdk.model.StorageDataUserDetails;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

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

    private static final int    MAX_LOG_COUNT           = 250;
    private static final String DEFAULT_STRING_VALUE    = "";
    private static final Object LOCK                    = new Object();

    // Public Functions

    public boolean setValue(Context context, String key, String value, SecureAreaManager secureStoreInstance) {
        String storeValue = value;

        if (!TextUtils.isEmpty(storeValue)) {
            try {
                storeValue = secureStoreInstance.encryptData(storeValue);
            } catch (Exception ex) {
                LogManager.getInstance().error("Add item to secure storage failed with error: " + ex.getLocalizedMessage());
                return false;
            }
        }

        return sharedPreferencesSetValue(context, key, storeValue);
    }

    public boolean setValue(Context context, String key, String value) {
        String storeValue = value;
        return sharedPreferencesSetValue(context, key, storeValue);
    }

    public String getValue(Context context, String key, SecureAreaManager secureStoreInstance) {
        String storedValue = sharedPreferencesGetValue(context, key);

        if (!TextUtils.isEmpty(storedValue)) {
            try {
                storedValue = secureStoreInstance.decryptData(storedValue);
            } catch (Exception ex) {
                LogManager.getInstance().error("Get item from secure storage failed with error: " + ex.getLocalizedMessage());
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

    /*
    public boolean storeConfiguration(Context context, ReadableMap data, SecureAreaManager secureAreInstance) {
        Configuration configuration = data != null ? new Configuration(data) : null;

        Gson gson = new Gson();

        String valueLanguage        = configuration != null ? configuration.language                    : "";
        String valuePassDevices     = configuration != null ? gson.toJson(configuration.passDevices)    : "";
        String valueManageDevices   = configuration != null ? gson.toJson(configuration.manageDevices)  : "";
        String valueUserDetails     = configuration != null ? gson.toJson(configuration.userDetails)    : "";

        boolean setLanguage     = sharedPreferencesSetValue(context, StorageKeys.LANGUAGE, valueLanguage);
        boolean setPassDevices  = sharedPreferencesSetValue(context, StorageKeys.DEVICES_PASS, valuePassDevices);
        boolean setManageDevies = sharedPreferencesSetValue(context, StorageKeys.DEVICES_MANAGE, valueManageDevices);

        return setValue(context, StorageKeys.USER_DETAILS, valueUserDetails, secureAreInstance) && setLanguage && setPassDevices && setManageDevies;
    }

    public void addLog(Context context, LogItem log) {
        ArrayList<LogItem> storedLogs = getLogs(context);
        storedLogs.add(log);

        int firstIndex = 0;
        if (storedLogs.size() > MAX_LOG_COUNT) {
            firstIndex = storedLogs.size() - MAX_LOG_COUNT;
        }

        List<LogItem> newLogList = storedLogs.subList(firstIndex, storedLogs.size());

        Gson gson = new Gson();
        sharedPreferencesSetValue(context, StorageKeys.LIBRARY_LOGS, gson.toJson(newLogList));
    }

    public ArrayList<LogItem> getLogs(Context context) {
        String logsJSON = sharedPreferencesGetValue(context, StorageKeys.LIBRARY_LOGS);

        Gson gson = new Gson();
        Type typeLogs = new TypeToken<List<LogItem>>(){}.getType();
        List<LogItem> logs = gson.fromJson(logsJSON, typeLogs);

        return logs == null ? new ArrayList<LogItem>() : (ArrayList<LogItem>) logs;
    }

    public boolean clearLogs(Context context) {
        return sharedPreferencesDeleteValue(context, StorageKeys.LIBRARY_LOGS);
    }

     */

    public StorageDataDevice findDevice(Context context, String deviceId) throws Exception {
        String storageDevices = getValue(context, StorageKeys.QRCODES);

        Gson gson = new Gson();

        Type typeDevices = new TypeToken<List<StorageDataDevice>>(){}.getType();
        List<StorageDataDevice> devices = gson.fromJson(storageDevices, typeDevices);

        for (StorageDataDevice device : devices) {
            if (device.deviceId.replace("-", "").equals(deviceId)) {
                return device;
            }
        }

        // TODO Change
        //  NotificationManager.getInstance().show("Cihaz Bluetooth Erişim listesinde bulunamadı", "Device not found in list of Bluetooth Access devices");
        throw new Exception("Device not found in storage list");
    }

    public StorageDataUserDetails findUser(Context context, String userId) throws Exception {
        String storageUsers = getValue(context, StorageKeys.USER_DETAILS, new SecureAreaManager(context));

        Gson gson = new Gson();

        Type typeUsers = new TypeToken<List<StorageDataUserDetails>>(){}.getType();
        List<StorageDataUserDetails> users = gson.fromJson(storageUsers, typeUsers);

        for (StorageDataUserDetails user : users) {
            if (user.userId.equals(userId)) {
                return user;
            }
        }

        // TODO Change
        //  NotificationManager.getInstance().show("Kullanıcı bilgilerinize erişilemedi, sorun devam ederse yeniden giriş yapınız", "User information could not be accessed, if the problem continues, login again");
        throw new Exception("User not found in storage list");
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
