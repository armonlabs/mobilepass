package com.armongate.mobilepasssdk.manager;

import android.content.Context;
import android.os.Build;

import com.armongate.mobilepasssdk.constant.ConfigurationDefaults;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.LogLevel;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.constant.ServiceProviders;
import com.armongate.mobilepasssdk.constant.StorageKeys;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.CryptoKeyPair;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.armongate.mobilepasssdk.model.QRCodeMatch;
import com.armongate.mobilepasssdk.model.StorageDataUserDetails;
import com.armongate.mobilepasssdk.model.request.RequestPagination;
import com.armongate.mobilepasssdk.model.request.RequestSetUserData;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointList;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListItem;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.service.BaseService;
import com.armongate.mobilepasssdk.service.DataService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ConfigurationManager {

    private static ConfigurationManager instance = null;

    private static Context          mCurrentContext;
    private static Configuration    mCurrentConfig;
    private static CryptoKeyPair    mCurrentKeyPair;
    private static String           mCurrentServiceProvider;

    private static List<StorageDataUserDetails>                 mUserKeyDetails = new ArrayList<>();
    private static HashMap<String, QRCodeMatch>                 mQRCodes = new HashMap<>();
    private static HashMap<String, ResponseAccessPointListItem> mAccessPoints = new HashMap<>();
    private static List<ResponseAccessPointListItem>            mTempList = new ArrayList<>();

    private int                 mReceivedItemCount  = 0;
    private RequestPagination   mPagination         = null;
    private Long                mListSyncDate       = null;
    private boolean             mListClearFlag      = false;

    private ConfigurationManager() { }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }

    public void setConfig(Context context, Configuration data) {
        DeviceManager deviceInfo = new DeviceManager();

        mCurrentServiceProvider = deviceInfo.getServiceProvider(context);
        mCurrentContext = context;
        mCurrentConfig = data;
    }

    public void setReady() {
        getStoredQRCodes();
        validateConfig();
        sendUserData();
    }

    public void setToken(String token, String language) {
        if (mCurrentConfig != null) {
            mCurrentConfig.token = token;
            mCurrentConfig.language = language;

            sendUserData();
        }
    }

    public QRCodeContent getQRCodeContent(String qrCodeData) {
        QRCodeMatch match = mQRCodes.get(qrCodeData);

        if (match != null) {
            ResponseAccessPointListItem details = mAccessPoints.get(match.accessPointId);

            if (details != null) {
                return new QRCodeContent(details.i, match.qrCode, details.t, details.g, details.c);
            }
        }

        return null;
    }

    public Context getCurrentContext() {
        return mCurrentContext;
    }

    public String getMemberId() {
        return mCurrentConfig != null ? mCurrentConfig.memberId : "";
    }

    public String getBarcodeId() { return mCurrentConfig != null ? mCurrentConfig.barcode : ""; }

    public String getPrivateKey() {
        return mCurrentKeyPair != null ? mCurrentKeyPair.privateKey : "";
    }

    public String getServerURL() {
        String serverUrl = mCurrentConfig != null ? mCurrentConfig.serverUrl : "";

        if (!serverUrl.isEmpty() && !serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        return serverUrl;
    }

    public String getMessageQRCode() {
        return mCurrentConfig != null && mCurrentConfig.qrCodeMessage != null ? mCurrentConfig.qrCodeMessage : "";
    }

    public String getToken() {
        return mCurrentConfig != null && mCurrentConfig.token != null ? mCurrentConfig.token : "unknown";
    }

    public String getLanguage() {
        return mCurrentConfig != null && mCurrentConfig.language != null ? mCurrentConfig.language : ConfigurationDefaults.Language;
    }

    public boolean allowMockLocation() {
        return mCurrentConfig != null && mCurrentConfig.allowMockLocation != null ? mCurrentConfig.allowMockLocation : ConfigurationDefaults.AllowMockLocation;
    }

    public Integer getBLEConnectionTimeout() {
        return mCurrentConfig != null && mCurrentConfig.connectionTimeout != null ? mCurrentConfig.connectionTimeout : ConfigurationDefaults.BLEConnectionTimeout;
    }

    public Integer autoCloseTimeout() {
        return mCurrentConfig != null ? mCurrentConfig.autoCloseTimeout : null;
    }

    public Boolean waitForBLEEnabled() {
        return mCurrentConfig != null && mCurrentConfig.waitBLEEnabled != null ? mCurrentConfig.waitBLEEnabled : ConfigurationDefaults.WaitBleEnabled;
    }

    public Boolean closeWhenInvalidQRCode() {
        return mCurrentConfig != null && mCurrentConfig.closeWhenInvalidQRCode != null ? mCurrentConfig.closeWhenInvalidQRCode : ConfigurationDefaults.CloseWhenInvalidQRCode;
    }

    public boolean usingHMS() {
        return mCurrentServiceProvider != null && mCurrentServiceProvider.equals(ServiceProviders.Huawei);
    }

    public String getServiceProvider() {
        return mCurrentServiceProvider;
    }

    public int getLogLevel() {
        return mCurrentConfig != null && mCurrentConfig.logLevel != null ? mCurrentConfig.logLevel : LogLevel.INFO;
    }

    public String getConfigurationLog() {
        return mCurrentConfig != null ? mCurrentConfig.getLog() : "";
    }

    public int getQRCodesCount() {
        return mQRCodes.size();
    }

    public void refreshList() {
        if (DelegateManager.getInstance().isQRCodeListRefreshable()) {
            LogManager.getInstance().info("QR Code definition list will be retrieved again with user request");
            this.getAccessPoints(true, false);
        }
    }

    private void getStoredQRCodes() {
        StorageManager.getInstance().deleteValue(mCurrentContext, StorageKeys.QRCODES);

        String storageQRCodes       = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.LIST_QRCODES);
        String storageAccessPoints  = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.LIST_ACCESSPOINTS);

        Gson gson = new Gson();

        Type typeQRCodes        = new TypeToken<HashMap<String, QRCodeMatch>>(){}.getType();
        Type typeAccessPoints   = new TypeToken<HashMap<String, ResponseAccessPointListItem>>(){}.getType();

        mQRCodes        = gson.fromJson(storageQRCodes, typeQRCodes);
        mAccessPoints   = gson.fromJson(storageAccessPoints, typeAccessPoints);

        if (mQRCodes == null) {
            mQRCodes = new HashMap<>();
        }

        if (mAccessPoints == null) {
            mAccessPoints = new HashMap<>();
        }

        DelegateManager.getInstance().onQRCodesDataLoaded(mQRCodes.size());

        LogManager.getInstance().info("Stored QR Code list is ready to use, total: " + mQRCodes.size());
        LogManager.getInstance().info("Stored Access Point list is ready to use, total: " + mAccessPoints.size());
    }

    private void validateConfig() {
        if (mCurrentConfig == null) {
            String message = "Configuration is required for MobilePass";
            LogManager.getInstance().error(message, LogCodes.CONFIGURATION_VALIDATION);
            throw new Error(message);
        }

        if (mCurrentConfig.memberId == null || mCurrentConfig.memberId.isEmpty()) {
            String message = "Provide valid Member Id to continue, received data is empty!";
            LogManager.getInstance().error(message, LogCodes.CONFIGURATION_VALIDATION);
            throw new Error(message);
        }

        if (mCurrentConfig.serverUrl == null || mCurrentConfig.serverUrl.isEmpty()) {
            String message = "Provide valid Server URL to continue, received data is empty!";
            LogManager.getInstance().error(message, LogCodes.CONFIGURATION_VALIDATION);
            throw new Error(message);
        }
    }

    private boolean checkKeyPair() {
        mCurrentKeyPair = null;

        String firstRun = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.FIRST_RUN);

        if (firstRun.isEmpty()) {
            LogManager.getInstance().info("First run of application");
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.FIRST_RUN, "yes");
            StorageManager.getInstance().deleteValue(mCurrentContext, StorageKeys.USER_DETAILS);
        }

        String storedUserKeys = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.USER_DETAILS, new SecureAreaManager(mCurrentContext));

        boolean newlyCreated = false;
        Gson gson = new Gson();

        if (storedUserKeys != null && !storedUserKeys.isEmpty()) {
            Type typeUserKeys = new TypeToken<List<StorageDataUserDetails>>(){}.getType();
            mUserKeyDetails = gson.fromJson(storedUserKeys, typeUserKeys);

            for (StorageDataUserDetails user : mUserKeyDetails) {
                if (user.userId.equals(getMemberId())) {
                    LogManager.getInstance().info("Key pair has already been generated for member id: " + getMemberId());
                    mCurrentKeyPair = new CryptoKeyPair(user.publicKey, user.privateKey);
                    break;
                }
            }
        }

        if (mCurrentKeyPair == null) {
            mCurrentKeyPair = CryptoManager.getInstance().generateKeyPair();
            LogManager.getInstance().info("New key pair is generated for member id: " + getMemberId());
            
            mUserKeyDetails.add(new StorageDataUserDetails(getMemberId(), mCurrentKeyPair.publicKey, mCurrentKeyPair.privateKey));

            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.USER_DETAILS, gson.toJson(mUserKeyDetails), new SecureAreaManager(mCurrentContext));

            newlyCreated = true;
        }

        return newlyCreated;
    }

    private void sendUserData() {
        if (mCurrentContext != null) {
            boolean needUpdate = checkKeyPair();

            String storedMemberId = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.MEMBERID);

            if (storedMemberId == null || storedMemberId.isEmpty() || !storedMemberId.equals(getMemberId())) {
                DelegateManager.getInstance().onMemberIdChanged();
                needUpdate = true;
            }

            if (needUpdate) {
                RequestSetUserData request = new RequestSetUserData();
                request.clubMemberId = getMemberId();
                request.publicKey = mCurrentKeyPair.getPublicKeyWithoutHead();

                new DataService().sendUserInfo(request, new BaseService.ServiceResultListener<Object>() {
                    @Override
                    public void onCompleted(Object response) {
                        StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.MEMBERID, getMemberId());

                        DelegateManager.getInstance().onMemberIdSyncCompleted(true, null);
                        LogManager.getInstance().info("User info has been shared with server successfully and member id is ready now");

                        getAccessPoints(false, false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        DelegateManager.getInstance().onMemberIdSyncCompleted(false, statusCode);
                        LogManager.getInstance().error("Sending user info to server has failed with status code " + statusCode, LogCodes.CONFIGURATION_SERVER_SYNC_INFO);

                        getAccessPoints(false, false);
                    }
                });
            } else {
                LogManager.getInstance().info("User info has already been sent to server for member id: " + getMemberId());
                getAccessPoints(false, false);
            }
        }
    }

    private void processQRCodesResponse(ResponseAccessPointList response) {
        if (response == null) {
            LogManager.getInstance().error("Empty data received for access points list", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
            return;
        }

        if (response.i == null || response.p == null) {
            LogManager.getInstance().error("Synchronization list response has invalid fields", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
            return;
        }

        mReceivedItemCount += response.i.length;

        mTempList.addAll(Arrays.asList(response.i));
        // Open to show received access point definitions with for loop
        // LogManager.getInstance().debug(new Gson().toJson(item));

        if (response.p.c > mReceivedItemCount) {
            mPagination.s = mReceivedItemCount;
            fetchAccessPoints();
        } else {
            if (mListClearFlag) {
                mQRCodes = new HashMap<>();
                mAccessPoints = new HashMap<>();

                LogManager.getInstance().info("Stored qr code and access points lists are cleared");
            }

            LogManager.getInstance().info("Total " + mTempList.size() + " item(s) received from server for definition list of access points");

            for (ResponseAccessPointListItem item :
                    mTempList) {
                if (item.i != null && !item.i.isEmpty()) {
                    ResponseAccessPointListItem storedAccessPoint = mAccessPoints.get(item.i);

                    List<String> storedQRCodeData = storedAccessPoint != null ? new ArrayList<>(Arrays.asList(storedAccessPoint.d)) : new ArrayList<String>();
                    List<String> newQrCodeData = new ArrayList<>();

                    if (item.q != null) {
                        for (ResponseAccessPointListQRCode qrCode :
                                item.q) {
                            if (qrCode.q != null && !qrCode.q.isEmpty()) {
                                storedQRCodeData.remove(qrCode.q);
                                newQrCodeData.add(qrCode.q);

                                mQRCodes.put(qrCode.q, new QRCodeMatch(item.i, qrCode));
                            } else {
                                LogManager.getInstance().warn("QR code data is empty that received in synchronization for access point id " + item.i, LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                            }
                        }
                    } else {
                        LogManager.getInstance().warn("QR code list received empty while synchronization for access point id " + item.i, LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                    }

                    for (String qrCodeData :
                            storedQRCodeData) {
                        LogManager.getInstance().info(qrCodeData + " is removed from definitions");
                        mQRCodes.remove(qrCodeData);
                    }

                    item.q = new ResponseAccessPointListQRCode[0];
                    item.d = newQrCodeData.toArray(new String[0]);

                    mAccessPoints.put(item.i, item);
                } else {
                    LogManager.getInstance().warn("Updated or added item received from server but id is empty!", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                }
            }

            String valueQRCodesToStore      = new Gson().toJson(mQRCodes);
            String valueAccessPointsToStore = new Gson().toJson(mAccessPoints);

            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_VERSION, ConfigurationDefaults.CurrentListVersion);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_QRCODES, valueQRCodesToStore);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_ACCESSPOINTS, valueAccessPointsToStore);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_SYNC_DATE, new Date().getTime() + "");

            LogManager.getInstance().info("Updated QR Code list is ready to use, total: " + mQRCodes.size() + ", version: " + ConfigurationDefaults.CurrentListVersion);
            LogManager.getInstance().info("Updated Access Point list is ready to use, total: " + mAccessPoints.size() + ", version: " + ConfigurationDefaults.CurrentListVersion);

            if (mQRCodes.size() > 0) {
                LogManager.getInstance().info("Up to date qr code list will be used for passing flow");
            } else {
                LogManager.getInstance().warn("There is no qr code that retrieved from server to continue passing flow", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
            }

            DelegateManager.getInstance().onQRCodeListStateChanged(QRCodeListState.USING_SYNCED_DATA, mQRCodes.size());

            clearConfigActiveDate();
        }
    }

    private void fetchAccessPoints() {
        new DataService().getAccessList(mPagination, mListSyncDate, new BaseService.ServiceResultListener<ResponseAccessPointList>() {
            @Override
            public void onCompleted(ResponseAccessPointList response) {
                processQRCodesResponse(response);
            }

            @Override
            public void onError(int statusCode, String message) {
                clearConfigActiveDate();

                if (statusCode == 409) {
                    LogManager.getInstance().warn("Sync error received for qr codes and access points, definition list will be retrieved again", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                    getAccessPoints(true, true);
                } else {
                    DelegateManager.getInstance().onQRCodesSyncFailed(statusCode);

                    LogManager.getInstance().error("Getting definition list of qr codes and access points has failed with status code " + statusCode, LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                    LogManager.getInstance().warn(mQRCodes.size() > 0 ? "Stored qr code list will be used for passing flow" : "There is no qr code that stored before to continue passing flow", LogCodes.CONFIGURATION_SERVER_SYNC_LIST);
                    DelegateManager.getInstance().onQRCodeListStateChanged(QRCodeListState.USING_STORED_DATA, mQRCodes.size());
                }
            }
        });
    }

    private void clearConfigActiveDate() {
        StorageManager.getInstance().deleteValue(mCurrentContext, StorageKeys.FLAG_CONFIGURATION_ACTIVE);
    }

    private boolean isGetAccessPointsAvailable() {
        String storedConfigActiveDate = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.FLAG_CONFIGURATION_ACTIVE);
        Long lastActiveDate = null;

        if (storedConfigActiveDate != null && !storedConfigActiveDate.isEmpty()) {
            try {
                lastActiveDate = Long.parseLong(storedConfigActiveDate);
            } catch (Exception ex) {
                LogManager.getInstance().info("Found invalid configuration activation date at storage: " + storedConfigActiveDate);
            }
        }

        return lastActiveDate == null || (Math.abs(new Date().getTime() - lastActiveDate) > 3000);
    }

    private void getAccessPoints(boolean clear, boolean force) {
        if (isGetAccessPointsAvailable() || force) {
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.FLAG_CONFIGURATION_ACTIVE, new Date().getTime() + "");

            LogManager.getInstance().info("Syncing definition list with server has been started");
            DelegateManager.getInstance().onQRCodeListStateChanged(QRCodeListState.SYNCING, mQRCodes.size());

            boolean forceFlag = force;

            String storedListVersion = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.LIST_VERSION);

            if (storedListVersion == null || !storedListVersion.equals(ConfigurationDefaults.CurrentListVersion)) {
                LogManager.getInstance().info("Force to clear definition list before fetch because of difference on version");
                forceFlag = true;
            }

            mListClearFlag = clear || forceFlag;

            if (mListClearFlag) {
                mListSyncDate = null;
            } else {
                String storedSyncDate = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.LIST_SYNC_DATE);

                if (storedSyncDate != null && !storedSyncDate.isEmpty()) {
                    try {
                        mListSyncDate = Long.parseLong(storedSyncDate);
                    } catch (Exception ex) {
                        LogManager.getInstance().info("Found invalid sync date at storage: " + storedSyncDate);
                    }
                }
            }

            mPagination = new RequestPagination();
            mPagination.t = 100;
            mPagination.s = 0;

            mReceivedItemCount = 0;
            mTempList = new ArrayList<>();

            this.fetchAccessPoints();
        } else {
            LogManager.getInstance().info("Get access list from server is cancelled because of another active progress");
        }
    }
}
