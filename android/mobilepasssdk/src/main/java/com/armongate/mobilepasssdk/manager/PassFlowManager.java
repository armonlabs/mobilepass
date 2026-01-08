package com.armongate.mobilepasssdk.manager;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.constant.PassFlowResultCode;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.constant.QRTriggerType;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.enums.Language;
import com.armongate.mobilepasssdk.enums.QRCodeErrorType;
import com.armongate.mobilepasssdk.model.BLEScanConfiguration;
import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.LocationRequirement;
import com.armongate.mobilepasssdk.model.PassFlowState;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.armongate.mobilepasssdk.model.QRCodeProcessResult;
import com.armongate.mobilepasssdk.model.request.RequestAccess;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.service.AccessPointService;
import com.armongate.mobilepasssdk.service.BaseService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Headless pass flow manager
 * Manages the entire pass flow logic without UI
 */
public class PassFlowManager {

    // Singleton
    private static PassFlowManager instance = null;

    private PassFlowManager() {
        this.ignore = new ArrayList<>();

        // All state codes are now user-facing - no ignore list needed

        setupBluetoothStateListener();
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

    // State machine fields
    private QRCodeContent activeQRCodeContent = null;
    private List<String> actionList = new ArrayList<>();
    private String actionCurrent = "";
    private Handler bleTimeoutHandler = null;
    private Handler locationTimeoutHandler = null;
    private String bleScanSessionId = null;
    private boolean isWaitingForBLEEnabled = false;
    private boolean isWaitingForLocationVerification = false;
    private boolean lastBleEnabledState = false;
    private String lastFailureMessage = null; // Store failure message from BLE or remote access

    // Setup
    private void setupBluetoothStateListener() {
        // Initialize with current state
        lastBleEnabledState = BluetoothManager.getInstance().getCurrentState().enabled;
    }

    // Public Functions
    public void clearStates() {
        states.clear();
        logStates.clear();

        this.lastClubId = null;
        this.lastQRCodeId = null;
        this.activeQRCodeContent = null;
        this.actionList.clear();
        this.actionCurrent = "";

        cancelBLETimeout();
        cancelLocationTimeout();
        bleScanSessionId = null;
        isWaitingForBLEEnabled = false;
        isWaitingForLocationVerification = false;
    }

    public void cancelFlow() {
        // Stop any active Bluetooth scan/connection
        BluetoothManager.getInstance().stopScan(true);
        
        LogManager.getInstance().info("Pass flow cancelled by app");
        
        // Capture current state info before clearing
        boolean isRemote = actionCurrent != null && actionCurrent.equals("remoteAccess");
        Integer direction = activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null;
        String clubId = activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null;
        String clubName = activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null;
        
        // Clear all states and reset flow (also cancels timeouts)
        clearStates();
        
        // Notify delegate that flow was cancelled (with preserved info)
        DelegateManager.getInstance().onCompleted(
            PassFlowResultCode.CANCEL,
            isRemote,
            direction,
            clubId,
            clubName
        );
    }    private void cancelBLETimeout() {
        if (bleTimeoutHandler != null) {
            bleTimeoutHandler.removeCallbacksAndMessages(null);
            bleTimeoutHandler = null;
        }
    }

    private void cancelLocationTimeout() {
        if (locationTimeoutHandler != null) {
            locationTimeoutHandler.removeCallbacksAndMessages(null);
            locationTimeoutHandler = null;
        }
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
        // Prevent duplicate SCAN_QRCODE_NO_MATCH states
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

        this.logStates.add(new PassFlowState(state, data, new Date()));
        
        // Notify delegate of state change
        DelegateManager.getInstance().notifyStateChanged(state, data);
    }

    public void setQRData(String qrId, String clubId) {
        this.lastQRCodeId = qrId;
        this.lastClubId = clubId;
    }

    /**
     * Process QR code data and start flow
     *
     * @param data QR code string
     * @return QRCodeProcessResult with validation status
     */
    public QRCodeProcessResult processQRCode(String data) {
        LogManager.getInstance().info("Processing QR code: " + data);

        // 1. Check format - basic validation
        if (data == null || data.isEmpty()) {
            addToStates(PassFlowStateCode.SCAN_QRCODE_INVALID_FORMAT, "Empty QR code");
            LogManager.getInstance().warn("QR code data is empty", null);
            
            // Complete flow with failure
            DelegateManager.getInstance().onCompleted(PassFlowResultCode.FAIL, false, null, null, null);
            
            return new QRCodeProcessResult(false, QRCodeErrorType.INVALID_FORMAT);
        }

        // 2. Validate QR code format with regex
        String pattern = "https://(app|sdk)\\.armongate\\.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2])?$";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = regex.matcher(data);

        if (!matcher.matches()) {
            addToStates(PassFlowStateCode.SCAN_QRCODE_INVALID_FORMAT, data);
            LogManager.getInstance().warn("QR code has invalid format: " + data, null);
            
            // Complete flow with failure
            DelegateManager.getInstance().onCompleted(PassFlowResultCode.FAIL, false, null, null, null);
            
            return new QRCodeProcessResult(false, QRCodeErrorType.INVALID_FORMAT);
        }

        // 3. Extract QR code content parts from URL
        String prefix = matcher.group(2);
        String uuid = matcher.group(3);
        String direction = matcher.group(4);

        String qrCodeContent = prefix + "/" + uuid + (direction != null ? direction : "");
        LogManager.getInstance().debug("Extracted QR code content: " + qrCodeContent);

        // 4. Check if exists in authorized list
        QRCodeContent content = ConfigurationManager.getInstance().getQRCodeContent(qrCodeContent);

        if (content == null) {
            addToStates(PassFlowStateCode.SCAN_QRCODE_NO_MATCH, qrCodeContent);
            LogManager.getInstance().warn("QR code not found in authorized list", PassFlowStateCode.SCAN_QRCODE_NO_MATCH);
            
            // Complete flow with failure
            DelegateManager.getInstance().onCompleted(PassFlowResultCode.FAIL, false, null, null, null);
            
            return new QRCodeProcessResult(false, QRCodeErrorType.NOT_FOUND);
        }
        
        // 5. Validate QR code content structure
        if (!content.valid) {
            LogManager.getInstance().warn("QR code configuration is invalid or incomplete", null);
            
            // Complete flow with failure
            DelegateManager.getInstance().onCompleted(
                PassFlowResultCode.FAIL, 
                false,
                content.qrCode != null ? content.qrCode.d : null,
                content.clubInfo != null ? content.clubInfo.i : null,
                content.clubInfo != null ? content.clubInfo.n : null
            );
            
            return new QRCodeProcessResult(false, QRCodeErrorType.INVALID_FORMAT);
        }

        LogManager.getInstance().info("QR code content validated successfully");
        addToStates(PassFlowStateCode.SCAN_QRCODE_FOUND);

        setQRData(content.qrCode != null ? content.qrCode.i : null,
                  content.clubInfo != null ? content.clubInfo.i : null);

        buildActionList(content);

        this.activeQRCodeContent = content;

        executeNextAction();

        return new QRCodeProcessResult(true, null);
    }

    /**
     * Confirm location has been verified by app
     */
    public void confirmLocationVerified() {
        if (!isWaitingForLocationVerification) {
            LogManager.getInstance().warn("confirmLocationVerified called but not awaiting location verification - ignoring", null);
            return;
        }

        // Cancel timeout since location was verified in time
        cancelLocationTimeout();
        
        isWaitingForLocationVerification = false;
        addToStates(PassFlowStateCode.RUN_ACTION_LOCATION_VALIDATED);
        LogManager.getInstance().info("Location verified by app, proceeding with remote access");

        executeRemoteAccess();
    }

    /**
     * Build action list based on QR trigger type
     */
    private void buildActionList(QRCodeContent content) {
        actionList.clear();
        actionCurrent = "";

        if (content.qrCode == null || content.qrCode.t == null) {
            LogManager.getInstance().error("QR code trigger type is empty", null);
            return;
        }

        boolean needLocation = content.qrCode.v != null && content.qrCode.v
                && content.geoLocation != null
                && content.geoLocation.la != null
                && content.geoLocation.lo != null
                && content.geoLocation.r != null;

        LogManager.getInstance().info("QR code trigger type: " + content.qrCode.t + ", needs location: " + needLocation);

        switch (content.qrCode.t) {
            case QRTriggerType.Bluetooth:
                actionList.add("bluetooth");
                break;

            case QRTriggerType.BluetoothThenRemote:
                actionList.add("bluetooth");
                if (needLocation) {
                    actionList.add("location");
                }
                actionList.add("remoteAccess");
                break;

            case QRTriggerType.Remote:
            case QRTriggerType.RemoteThenBluetooth:
                if (needLocation) {
                    actionList.add("location");
                }
                actionList.add("remoteAccess");
                if (content.qrCode.t == QRTriggerType.RemoteThenBluetooth) {
                    actionList.add("bluetooth");
                }
                break;

            default:
                LogManager.getInstance().error("Unknown trigger type", null);
                break;
        }

        LogManager.getInstance().info("Action list built: " + actionList.toString());
    }

    /**
     * Execute next action in queue
     */
    private void executeNextAction() {
        LogManager.getInstance().debug("Executing next action");

        if (actionList.isEmpty()) {
            LogManager.getInstance().error("No actions in queue", null);
            return;
        }

        actionCurrent = actionList.remove(0);

        LogManager.getInstance().info("Current action: " + actionCurrent + ", Remaining: " + actionList);

        processAction(actionCurrent);
    }

    /**
     * Process current action
     */
    private void processAction(String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        switch (action) {
            case "bluetooth":
                addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH);
                executeBluetooth();
                break;

            case "remoteAccess":
                addToStates(PassFlowStateCode.PROCESS_ACTION_REMOTE_ACCESS);
                // Check if location verification required for remote access
                if (activeQRCodeContent != null
                        && activeQRCodeContent.qrCode != null
                        && activeQRCodeContent.qrCode.v != null
                        && activeQRCodeContent.qrCode.v
                        && activeQRCodeContent.geoLocation != null
                        && activeQRCodeContent.geoLocation.la != null
                        && activeQRCodeContent.geoLocation.lo != null
                        && activeQRCodeContent.geoLocation.r != null) {
                    // Location required - notify app
                    LocationRequirement requirement = new LocationRequirement(
                            activeQRCodeContent.geoLocation.la,
                            activeQRCodeContent.geoLocation.lo,
                            activeQRCodeContent.geoLocation.r
                    );
                    addToStates(PassFlowStateCode.RUN_ACTION_LOCATION_WAITING);
                    isWaitingForLocationVerification = true;
                    
                    // Start location verification timeout (configurable, default 30 seconds)
                    final int timeout = ConfigurationManager.getInstance().getLocationVerificationTimeout();
                    LogManager.getInstance().info("Waiting for location verification (timeout: " + timeout + " seconds)");
                    
                    locationTimeoutHandler = new Handler(Looper.getMainLooper());
                    locationTimeoutHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isWaitingForLocationVerification) {
                                LogManager.getInstance().warn("Location verification timeout (" + timeout + "s) - user did not verify location", null);
                                isWaitingForLocationVerification = false;
                                
                                // Clear delegate to stop receiving BLE callbacks (scan may not be active)
                                BluetoothManager.getInstance().delegate = null;
                                bleScanSessionId = null;
                                
                                // Complete flow as failed
                                DelegateManager.getInstance().onCompleted(
                                    PassFlowResultCode.FAIL_LOCATION_TIMEOUT,
                                    true,
                                    activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                                    activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                                    activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
                                );
                            }
                        }
                    }, timeout * 1000L);
                    
                    DelegateManager.getInstance().notifyLocationRequired(requirement);
                } else {
                    // No location required - proceed directly
                    executeRemoteAccess();
                }
                break;

            case "location":
                // Location handled as part of remoteAccess
                executeNextAction();
                break;

            default:
                LogManager.getInstance().error("Unknown action type: " + action, null);
                break;
        }
    }

    /**
     * Execute Bluetooth action
     */
    private void executeBluetooth() {
        if (activeQRCodeContent == null
                || activeQRCodeContent.terminals == null
                || activeQRCodeContent.qrCode == null) {
            LogManager.getInstance().error("Missing required data for Bluetooth execution", null);
            // Critical error: Complete flow as failed
            DelegateManager.getInstance().onCompleted(
                    PassFlowResultCode.FAIL,
                    false,
                    activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
            );
            return;
        }

        ResponseAccessPointListQRCode qrCode = activeQRCodeContent.qrCode;

        if (qrCode.i == null || qrCode.i.isEmpty()) {
            LogManager.getInstance().warn("QR code ID is empty", null);
            fallbackOrFail();
            return;
        }

        if (qrCode.d == null) {
            LogManager.getInstance().warn("Direction is empty", null);
            fallbackOrFail();
            return;
        }

        if (qrCode.h == null || qrCode.h.isEmpty()) {
            LogManager.getInstance().warn("Hardware ID is empty", null);
            fallbackOrFail();
            return;
        }

        if (qrCode.r == null) {
            LogManager.getInstance().warn("Relay number is empty", null);
            fallbackOrFail();
            return;
        }

        // Check Bluetooth authorization and enabled state
        DeviceCapability bleState = BluetoothManager.getInstance().getCurrentState();

        // Check authorization first (includes BLE permissions and location permission for Android 11-)
        if (bleState.needAuthorize) {
            LogManager.getInstance().warn("Bluetooth or location permission not granted", null);
            
            // Check configuration: should we continue without BLE, or wait for permission?
            boolean shouldContinue = ConfigurationManager.getInstance().continueWithoutBLE();
            boolean hasNoFallback = actionList.isEmpty();
            
            if (shouldContinue && !hasNoFallback) {
                // Continue to next action (e.g., remote access) without waiting
                // No callback needed - user chose to continue without BLE
                LogManager.getInstance().info("BLE permission missing and continueWithoutBLE=true - skipping to next action silently");
                fallbackOrFail();
            } else {
                // Wait for user to grant permission - but flow cannot auto-resume
                // User must grant permission and scan QR code again
                addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING);
                LogManager.getInstance().warn("BLE permission missing and continueWithoutBLE=false - completing flow, user must grant permission and retry", PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING);
                DelegateManager.getInstance().needPermission(NeedPermissionType.NEED_PERMISSION_BLUETOOTH);
                
                // Complete flow as failed - user needs to grant permission and scan QR again
                DelegateManager.getInstance().onCompleted(
                    PassFlowResultCode.FAIL_PERMISSION,
                    false,
                    activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
                );
            }
            return;
        }

        // Check if Bluetooth is powered on
        if (!bleState.enabled) {
            LogManager.getInstance().warn("Bluetooth is not enabled", null);

            // Check configuration: should we continue without BLE, or wait?
            boolean shouldContinue = ConfigurationManager.getInstance().continueWithoutBLE();
            boolean hasNoFallback = actionList.isEmpty();

            if (shouldContinue && !hasNoFallback) {
                // Continue to next action - skip Bluetooth
                // No callback needed - user chose to continue without BLE
                LogManager.getInstance().info("Bluetooth disabled and continueWithoutBLE=true - skipping to next action silently");
                fallbackOrFail();
            } else {
                // Wait for user to enable Bluetooth
                addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING);
                LogManager.getInstance().warn("Waiting for Bluetooth to be enabled (continueWithoutBLE=" + shouldContinue + ", hasNoFallback=" + hasNoFallback + ")", PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING);
                isWaitingForBLEEnabled = true;
                DelegateManager.getInstance().needPermission(NeedPermissionType.NEED_ENABLE_BLE);
                // App should show "Please enable Bluetooth" UI
            }
            return;
        }

        // Bluetooth is ready - start scan
        String memberId = ConfigurationManager.getInstance().getMemberId();
        String barcodeId = ConfigurationManager.getInstance().getBarcodeId();
        Language language = Objects.equals(ConfigurationManager.getInstance().getLanguage(), "en")
                ? Language.EN : Language.TR;

        BLEScanConfiguration config = new BLEScanConfiguration(
                Arrays.asList(activeQRCodeContent.terminals),
                memberId,
                barcodeId,
                qrCode.i,
                qrCode.h,
                qrCode.d,
                qrCode.r,
                language
        );

        // Create unique session ID for this scan to prevent race conditions
        final String sessionId = UUID.randomUUID().toString();
        this.bleScanSessionId = sessionId;

        // Set up callback to handle Bluetooth results
        BluetoothManager.getInstance().delegate = new BluetoothManagerDelegate() {
            @Override
            public void onConnectionStateChanged(DeviceConnectionStatus status) {
                // Guard: Check if this callback is for the current scan session
                if (!sessionId.equals(bleScanSessionId)) {
                    LogManager.getInstance().warn("Ignoring BLE event from old scan session (session mismatch)", null);
                    return;
                }

                switch (status.state) {
                    case CONNECTED:
                        // Invalidate session FIRST to prevent duplicate terminal events
                        bleScanSessionId = null;
                        BluetoothManager.getInstance().delegate = null;
                        
                        // Bluetooth success! Cancel timeout and stop scan
                        cancelBLETimeout();
                        BluetoothManager.getInstance().stopScan(false); // Don't disconnect - already connected
                        
                        LogManager.getInstance().info("Bluetooth connection successful");
                        addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_PASS_SUCCEED);
                        
                        DelegateManager.getInstance().onCompleted(
                                PassFlowResultCode.SUCCESS,
                                false,
                                activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                                activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                                activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
                        );
                        break;

                    case FAILED:
                    case NOT_FOUND:
                        // Invalidate session FIRST to prevent duplicate terminal events
                        bleScanSessionId = null;
                        BluetoothManager.getInstance().delegate = null;
                        
                        // Bluetooth failed - cancel timeout and stop scan immediately
                        cancelBLETimeout();
                        BluetoothManager.getInstance().stopScan(true);
                        
                        // Store failure message for potential use in onCompleted
                        lastFailureMessage = status.failMessage;
                        
                        LogManager.getInstance().warn("Bluetooth connection failed: " + status.failMessage, PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTION_FAILED);
                        addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTION_FAILED, status.failMessage);
                        
                        fallbackOrFail();
                        break;

                    case CONNECTING:
                        LogManager.getInstance().debug("Bluetooth connecting...");
                        addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTING);
                        break;

                    case DISCONNECTED:
                        LogManager.getInstance().debug("Bluetooth disconnected");
                        break;
                }
            }

            @Override
            public void onBLEStateChanged(DeviceCapability state) {
                // Handle BLE state changes while waiting
                handleBLEStateChange(state);
            }
        };

        // Start scanning timeout timer
        int timeout = ConfigurationManager.getInstance().getBLEConnectionTimeout();
        LogManager.getInstance().info("Starting Bluetooth scan (timeout: " + timeout + " seconds)");

        bleTimeoutHandler = new Handler(Looper.getMainLooper());
        bleTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Guard: Check if timeout is for current scan session
                if (!sessionId.equals(bleScanSessionId)) {
                    LogManager.getInstance().warn("Ignoring BLE timeout from old scan session", PassFlowStateCode.RUN_ACTION_BLUETOOTH_TIMEOUT);
                    return;
                }

                // Timeout reached - no device found
                LogManager.getInstance().warn("Bluetooth scan timeout (" + timeout + "s) - no device found", PassFlowStateCode.RUN_ACTION_BLUETOOTH_TIMEOUT);
                addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_TIMEOUT);

                // Stop scanning
                BluetoothManager.getInstance().stopScan(true);

                // Try next action if available
                fallbackOrFail();
            }
        }, timeout * 1000L);

        BluetoothManager.getInstance().startScan(config);
    }

    /**
     * Handle Bluetooth state changes
     */
    private void handleBLEStateChange(DeviceCapability state) {
        // Only trigger if state changed from OFF to ON while waiting
        boolean stateChangedToEnabled = !lastBleEnabledState && state.enabled;

        if (isWaitingForBLEEnabled && stateChangedToEnabled) {
            LogManager.getInstance().info("Bluetooth state changed from OFF to ON - retrying execution");
            isWaitingForBLEEnabled = false;
            executeBluetooth();
        }

        // Always update last state
        lastBleEnabledState = state.enabled;
    }

    /**
     * Execute remote access action
     */
    private void executeRemoteAccess() {
        if (activeQRCodeContent == null
                || activeQRCodeContent.qrCode == null
                || activeQRCodeContent.qrCode.i == null
                || activeQRCodeContent.qrCode.i.isEmpty()) {
            LogManager.getInstance().error("Missing required data for remote access", null);
            // Critical error: Complete flow as failed
            DelegateManager.getInstance().onCompleted(
                    PassFlowResultCode.FAIL,
                    true,
                    activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
            );
            return;
        }

        RequestAccess request = new RequestAccess();
        request.q = activeQRCodeContent.qrCode.i;

        if (ConfigurationManager.getInstance().getInstallationId() != null && !ConfigurationManager.getInstance().getInstallationId().isEmpty()) {
            request.i = ConfigurationManager.getInstance().getInstallationId();
        }

        new AccessPointService().remoteOpen(request, new BaseService.ServiceResultListener() {
            @Override
            public void onCompleted(Object result) {
                LogManager.getInstance().info("Remote access successful");
                addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED);
                
                // Clear delegate to stop receiving BLE callbacks (scan may not be active)
                BluetoothManager.getInstance().delegate = null;
                bleScanSessionId = null;
                
                DelegateManager.getInstance().onCompleted(
                        PassFlowResultCode.SUCCESS,
                        true,
                        activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                        activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                        activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null
                );
            }

            @Override
            public void onError(int errorCode, String message) {
                LogManager.getInstance().warn("Remote access failed: " + errorCode + " - " + message, PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED);

                // Store error message for potential use in onCompleted
                lastFailureMessage = message;

                // Handle specific error codes
                switch (errorCode) {
                    case 401:
                        addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED, message);
                        break;
                    case 404:
                        addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED, message);
                        break;
                    case 408:
                        addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT, message);
                        break;
                    case 0:
                        addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_NO_NETWORK, message);
                        break;
                    default:
                        addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED, message);
                        break;
                }

                // Important: Don't fallback to Bluetooth if unauthorized (401)
                // User is not authorized - Bluetooth won't help either!
                boolean shouldTryFallback = errorCode != 401 && !actionList.isEmpty();

                if (shouldTryFallback) {
                    LogManager.getInstance().info("Remote access failed (code: " + errorCode + "), trying next action (Bluetooth fallback)");
                    executeNextAction();
                } else {
                    if (errorCode == 401) {
                        LogManager.getInstance().info("Remote access failed: Unauthorized (401) - no fallback");
                    } else {
                        LogManager.getInstance().info("Remote access failed - no fallback action available");
                    }

                    DelegateManager.getInstance().onCompleted(
                            PassFlowResultCode.FAIL,
                            true,
                            activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                            activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                            activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null,
                            message
                    );
                }
            }
        });
    }

    /**
     * Fallback to next action or fail
     */
    private void fallbackOrFail() {
        if (!actionList.isEmpty()) {
            LogManager.getInstance().info("Trying next action");
            executeNextAction();
        } else {
            LogManager.getInstance().info("No fallback action available");
            
            // Clear delegate to stop receiving BLE callbacks
            // (scan already stopped by timeout or FAILED case)
            BluetoothManager.getInstance().delegate = null;
            bleScanSessionId = null;
            
            DelegateManager.getInstance().onCompleted(
                    PassFlowResultCode.FAIL,
                    actionCurrent.equals("remoteAccess"),
                    activeQRCodeContent != null && activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.d : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null,
                    activeQRCodeContent != null && activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.n : null,
                    lastFailureMessage
            );
        }
    }
}
