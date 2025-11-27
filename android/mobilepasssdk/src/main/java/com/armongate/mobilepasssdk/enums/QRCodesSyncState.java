package com.armongate.mobilepasssdk.enums;

import androidx.annotation.Nullable;

/**
 * State of QR codes synchronization process
 */
public class QRCodesSyncState {
    private final State state;
    private final Boolean synced;
    private final Integer count;
    private final Integer statusCode;

    public enum State {
        SYNC_STARTED,
        SYNC_COMPLETED,
        SYNC_FAILED,
        DATA_EMPTY
    }

    private QRCodesSyncState(State state, @Nullable Boolean synced, @Nullable Integer count, @Nullable Integer statusCode) {
        this.state = state;
        this.synced = synced;
        this.count = count;
        this.statusCode = statusCode;
    }

    public static QRCodesSyncState syncStarted() {
        return new QRCodesSyncState(State.SYNC_STARTED, null, null, null);
    }

    public static QRCodesSyncState syncCompleted(boolean synced, int count) {
        return new QRCodesSyncState(State.SYNC_COMPLETED, synced, count, null);
    }

    public static QRCodesSyncState syncFailed(int statusCode) {
        return new QRCodesSyncState(State.SYNC_FAILED, null, null, statusCode);
    }

    public static QRCodesSyncState dataEmpty() {
        return new QRCodesSyncState(State.DATA_EMPTY, null, null, null);
    }

    public State getState() {
        return state;
    }

    @Nullable
    public Boolean isSynced() {
        return synced;
    }

    @Nullable
    public Integer getCount() {
        return count;
    }

    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }
}

