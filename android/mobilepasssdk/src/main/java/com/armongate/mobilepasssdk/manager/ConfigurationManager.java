package com.armongate.mobilepasssdk.manager;

import android.content.Context;
import android.util.Log;

import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.constant.StorageKeys;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.CryptoKeyPair;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.armongate.mobilepasssdk.model.StorageDataUserDetails;
import com.armongate.mobilepasssdk.model.request.RequestPagination;
import com.armongate.mobilepasssdk.model.request.RequestSetUserData;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItem;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemQRCodeItem;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointList;
import com.armongate.mobilepasssdk.service.BaseService;
import com.armongate.mobilepasssdk.service.DataService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigurationManager {

    private static ConfigurationManager instance = null;
    private static Context mCurrentContext;
    private static Configuration mCurrentConfig;
    private static CryptoKeyPair mCurrentKeyPair;
    private static List<StorageDataUserDetails> mUserKeyDetails = new ArrayList<>();
    private static HashMap<String, QRCodeContent> mCurrentQRCodes = new HashMap<>();
    private static HashMap<String, QRCodeContent> mTempQRCodes = new HashMap<>();

    private int mReceivedItemCount = 0;
    private RequestPagination mPagination = null;

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
        return mCurrentQRCodes.get(qrCodeData);
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
        return mCurrentConfig != null && mCurrentConfig.language != null ? mCurrentConfig.language : "en";
    }

    public boolean allowMockLocation() {
        return mCurrentConfig != null && mCurrentConfig.allowMockLocation != null ? mCurrentConfig.allowMockLocation : false;
    }

    public Integer getBLEConnectionTimeout() {
        return mCurrentConfig != null && mCurrentConfig.connectionTimeout != null ? mCurrentConfig.connectionTimeout : 5;
    }

    private void getStoredQRCodes() {
        String storageQRCodes = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.QRCODES);

        Gson gson = new Gson();

        Type typeQRCodes = new TypeToken<HashMap<String, QRCodeContent>>(){}.getType();
        mCurrentQRCodes = gson.fromJson(storageQRCodes, typeQRCodes);

        if (mCurrentQRCodes == null) {
            mCurrentQRCodes = new HashMap<>();
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

    private void checkKeyPair() {
        String storedUserKeys = StorageManager.getInstance().getValue(mCurrentContext, StorageKeys.USER_DETAILS, new SecureAreaManager(mCurrentContext));

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
        }
    }

    private void sendUserData() {
        if (mCurrentContext != null) {
            if (mCurrentKeyPair == null) {
                checkKeyPair();
            }

            RequestSetUserData request = new RequestSetUserData();
            request.clubMemberId = getMemberId();
            request.publicKey = mCurrentKeyPair.getPublicKeyWithoutHead();

            new DataService().sendUserInfo(request, new BaseService.ServiceResultListener<Object>() {
                @Override
                public void onCompleted(Object response) {
                    getAccessPoints();
                }

                @Override
                public void onError(int statusCode) {
                    LogManager.getInstance().error("Send user info failed with status code " + statusCode);
                    getAccessPoints();
                }
            });
        }
    }

    private void processAccessPointsResponse(ResponseAccessPointList response) {
        mReceivedItemCount += response.items.length;

        for (ResponseAccessPointItem item:
                response.items) {
            for (ResponseAccessPointItemQRCodeItem qrCode :
                    item.qrCodeData) {
                QRCodeContent content = new QRCodeContent(qrCode.qrCodeData, item, qrCode);
                mTempQRCodes.put(qrCode.qrCodeData, content);

                // Open to show received access point definitions
                // LogManager.getInstance().debug(new Gson().toJson(content));
            }
        }

        if (response.pagination.total > mReceivedItemCount) {
            mPagination.skip = mReceivedItemCount;
            fetchAccessPoints();
        } else {
            mCurrentQRCodes = new HashMap<>();
            mCurrentQRCodes.putAll(mTempQRCodes);

            String valueQRCodesToStore = new Gson().toJson(mCurrentQRCodes);
            StorageManager.getInstance().setValue(mCurrentContext, StorageKeys.QRCODES, valueQRCodesToStore);

            DelegateManager.getInstance().onQRCodeListStateChanged(mCurrentQRCodes.size() > 0 ? QRCodeListState.USING_SYNCED_DATA : QRCodeListState.EMPTY);
        }
    }

    private void fetchAccessPoints() {
        new DataService().getAccessList(mPagination, new BaseService.ServiceResultListener<ResponseAccessPointList>() {
            @Override
            public void onCompleted(ResponseAccessPointList response) {
               processAccessPointsResponse(response);
            }

            @Override
            public void onError(int statusCode) {
                LogManager.getInstance().error("Get access list failed with status code " + statusCode);
                DelegateManager.getInstance().onQRCodeListStateChanged(mCurrentQRCodes.size() > 0 ? QRCodeListState.USING_STORED_DATA : QRCodeListState.EMPTY);
            }
        });
    }

    private void getAccessPoints() {
        mPagination = new RequestPagination();
        mPagination.take = 100;
        mPagination.skip = 0;

        mReceivedItemCount = 0;
        mTempQRCodes = new HashMap<>();

        this.fetchAccessPoints();
    }
}
