package com.armongate.mobilepasssdk.manager;

import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.model.PassFlowState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PassFlowManager {

    // Singleton

    private static PassFlowManager instance = null;
    private PassFlowManager() {
        this.ignore = new ArrayList<>();

        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_NEED_PERMISSION);
        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_PERMISSION_GRANTED);
        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_STARTED);
        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_INVALID_CONTENT);
        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_INVALID_FORMAT);
        this.ignore.add(PassFlowStateCode.SCAN_QRCODE_ERROR);
        this.ignore.add(PassFlowStateCode.INVALID_QRCODE_TRIGGER_TYPE);
        this.ignore.add(PassFlowStateCode.INVALID_QRCODE_MISSING_CONTENT);
        this.ignore.add(PassFlowStateCode.INVALID_ACTION_LIST_EMPTY);
        this.ignore.add(PassFlowStateCode.INVALID_ACTION_TYPE);
        this.ignore.add(PassFlowStateCode.INVALID_BLUETOOTH_QRCODE_DATA);
        this.ignore.add(PassFlowStateCode.INVALID_BLUETOOTH_DIRECTION);
        this.ignore.add(PassFlowStateCode.INVALID_BLUETOOTH_HARDWARE_ID);
        this.ignore.add(PassFlowStateCode.INVALID_BLUETOOTH_RELAY_NUMBER);
        this.ignore.add(PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_DATA);
        this.ignore.add(PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_ID);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_STARTED);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_PERMISSION);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_ENABLED);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_LOCATION_PERMISSION_GRANTED);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_ENABLED);
        this.ignore.add(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_PERMISSION_GRANTED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_BLUETOOTH_STARTED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_NO_WAIT);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_BLUETOOTH_START_SCAN);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTING);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_LOCATION_WAITING);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_LOCATION_VALIDATED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST);
        this.ignore.add(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED);
    }

    public static PassFlowManager getInstance() {
        if (instance == null) {
            instance = new PassFlowManager();
        }

        return instance;
    }

    // Fields

    private List<PassFlowState> states = new ArrayList<>();
    private List<PassFlowState> logStates = new ArrayList<>();
    private String lastQRCodeId = null;
    private String lastClubId = null;
    private List<Integer> ignore = new ArrayList<>();

    // Public Functions

    public void clearStates() {
        states.clear();
        logStates.clear();

        this.lastClubId = null;
        this.lastQRCodeId = null;
    }

    public List<PassFlowState> getStates() {
        return this.states;
    }
    
    public List<PassFlowState> getLogStates() {
        return this.logStates;
    }

    public String getLastClubId() {
        return lastClubId;
    }

    public String getLastQRCodeId() {
        return lastQRCodeId;
    }

    public void addToStates(Integer state) {
        this.addToStates(state, null);
    }

    public void addToStates(Integer state, String data) {
        if (this.ignore.contains(state)) {
            LogManager.getInstance().debug("State has been ignored for code: " + state.toString());
        } else {
            if (state == PassFlowStateCode.SCAN_QRCODE_NO_MATCH) {
                boolean alreadyAdded = false;

                for (PassFlowState stateItem : this.states) {
                    if (stateItem.getState() == PassFlowStateCode.SCAN_QRCODE_NO_MATCH) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (alreadyAdded) {
                    LogManager.getInstance().info("No match QR Code state has been added before");
                } else {
                    this.states.add(new PassFlowState(state, data));
                }
            } else {
                this.states.add(new PassFlowState(state, data));
            }
        }

        this.logStates.add(new PassFlowState(state, data, new Date()));
    }

    public void setQRData(String qrId, String clubId) {
        this.lastQRCodeId = qrId;
        this.lastClubId = clubId;
    }
}
