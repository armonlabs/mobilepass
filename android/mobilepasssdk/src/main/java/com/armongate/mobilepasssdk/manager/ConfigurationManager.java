package com.armongate.mobilepasssdk.manager;

import android.content.Context;

import com.armongate.mobilepasssdk.constant.ConfigurationDefaults;
import com.armongate.mobilepasssdk.constant.LogLevel;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
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
import java.util.Map;

public class ConfigurationManager {

    private static ConfigurationManager instance = null;

    private static Context          mCurrentContext;
    private static Configuration    mCurrentConfig;
    private static CryptoKeyPair    mCurrentKeyPair;

    private static List<StorageDataUserDetails>                 mUserKeyDetails = new ArrayList<>();
    private static HashMap<String, QRCodeMatch>                 mQRCodes = new HashMap<>();
    private static HashMap<String, ResponseAccessPointListItem> mAccessPoints = new HashMap<>();
    private static List<ResponseAccessPointListItem>            mTempList = new ArrayList<>();

    private int mReceivedItemCount = 0;
    private RequestPagination mPagination = null;
    private Long mListSyncDate = null;
    private boolean mListClearFlag = false;

    private ConfigurationManager() { }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }

    public void setConfig(Context context, Configuration data) {
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
                return new QRCodeContent(details.i, match.qrCode, details.t, details.g);
            }
        }

        return null;
    }

    public String getMemberId() {
        return mCurrentConfig != null ? mCurrentConfig.memberId : "";
    }

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

    public int getLogLevel() {
        return mCurrentConfig != null && mCurrentConfig.logLevel != null ? mCurrentConfig.logLevel : LogLevel.INFO;
    }

    public String getConfigurationLog() {
        return mCurrentConfig != null ? mCurrentConfig.getLog() : "";
    }

    public void refreshList() {
        if (DelegateManager.getInstance().isQRCodeListRefreshable()) {
            this.getAccessPoints(true);
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
    }

    private void validateConfig() {
        if (mCurrentConfig == null) {
            throw new Error("Configuration is required for MobilePass");
        }

        if (mCurrentConfig.memberId == null || mCurrentConfig.memberId.isEmpty()) {
            throw new Error("Provide valid Member Id to continue, received data is empty!");
        }

        if (mCurrentConfig.serverUrl == null || mCurrentConfig.serverUrl.isEmpty()) {
            throw new Error("Provide valid Server URL to continue, received data is empty!");
        }
    }

    private boolean checkKeyPair() {
        mCurrentKeyPair = null;

        String storedUserKeys = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.USER_DETAILS, new SecureAreaManager(mCurrentContext));

        boolean newlyCreated = false;
        Gson gson = new Gson();

        if (storedUserKeys != null && !storedUserKeys.isEmpty()) {
            Type typeUserKeys = new TypeToken<List<StorageDataUserDetails>>(){}.getType();
            mUserKeyDetails = gson.fromJson(storedUserKeys, typeUserKeys);

            for (StorageDataUserDetails user : mUserKeyDetails) {
                if (user.userId.equals(getMemberId())) {
                    mCurrentKeyPair = new CryptoKeyPair(user.publicKey, user.privateKey);
                    break;
                }
            }
        }

        if (mCurrentKeyPair == null) {
            mCurrentKeyPair = CryptoManager.getInstance().generateKeyPair();
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
                        LogManager.getInstance().info("Member id is stored successfully");
                        getAccessPoints(false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        LogManager.getInstance().error("Send user info failed with status code " + statusCode, null);
                        getAccessPoints(false);
                    }
                });
            } else {
                LogManager.getInstance().info("User info is already sent to server");
                getAccessPoints(false);
            }
        }
    }

    private void processQRCodesResponse(ResponseAccessPointList response) {
        mReceivedItemCount += response.i.length;

        for (ResponseAccessPointListItem item: response.i) {
            mTempList.add(item);

            // Open to show received access point definitions
            LogManager.getInstance().debug(new Gson().toJson(item));
        }

        if (response.p.c > mReceivedItemCount) {
            mPagination.s = mReceivedItemCount;
            fetchAccessPoints();
        } else {
            if (mListClearFlag) {
                mQRCodes = new HashMap<>();
                mAccessPoints = new HashMap<>();
            }

            for (ResponseAccessPointListItem item :
                    mTempList) {
                ResponseAccessPointListItem storedAccessPoint = mAccessPoints.get(item.i);

                List<String> storedQRCodeData = storedAccessPoint != null ? new ArrayList<>(Arrays.asList(storedAccessPoint.d)) : new ArrayList<String>();
                List<String> newQrCodeData = new ArrayList<>();

                for (ResponseAccessPointListQRCode qrCode :
                        item.q) {
                    storedQRCodeData.remove(qrCode.q);
                    newQrCodeData.add(qrCode.q);

                    mQRCodes.put(qrCode.q, new QRCodeMatch(item.i, qrCode));
                }

                for (String qrCodeData :
                            storedQRCodeData) {
                    mQRCodes.remove(qrCodeData);
                }

                item.q = new ResponseAccessPointListQRCode[0];
                item.d = newQrCodeData.toArray(new String[0]);

                mAccessPoints.put(item.i, item);
            }

            String valueQRCodesToStore      = new Gson().toJson(mQRCodes);
            String valueAccessPointsToStore = new Gson().toJson(mAccessPoints);

            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_QRCODES, valueQRCodesToStore);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_ACCESSPOINTS, valueAccessPointsToStore);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.LIST_SYNC_DATE, new Date().getTime() + "");

            DelegateManager.getInstance().onQRCodeListStateChanged(mQRCodes.size() > 0 ? QRCodeListState.USING_SYNCED_DATA : QRCodeListState.EMPTY);
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
                if (statusCode == 409) {
                    LogManager.getInstance().error("Sync error received, get list again", null);
                    getAccessPoints(true);
                } else {
                    LogManager.getInstance().error("Get access list failed with status code " + statusCode, null);
                    DelegateManager.getInstance().onQRCodeListStateChanged(mQRCodes.size() > 0 ? QRCodeListState.USING_STORED_DATA : QRCodeListState.EMPTY);
                }
            }
        });
    }

    private void getAccessPoints(boolean clear) {
        DelegateManager.getInstance().onQRCodeListStateChanged(QRCodeListState.SYNCING);

        mListClearFlag = clear;

        if (clear) {
            mListSyncDate = null;
        } else {
            String storedSyncDate = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.LIST_SYNC_DATE);

            if (storedSyncDate != null && !storedSyncDate.isEmpty()) {
                try {
                    mListSyncDate = Long.parseLong(storedSyncDate);
                } catch (Exception ex) {
                    LogManager.getInstance().error("Invalid stored sync date: " + storedSyncDate, null);
                }
            }
        }

        mPagination = new RequestPagination();
        mPagination.t = 100;
        mPagination.s = 0;

        mReceivedItemCount = 0;
        mTempList = new ArrayList<>();

        this.fetchAccessPoints();
    }
}
