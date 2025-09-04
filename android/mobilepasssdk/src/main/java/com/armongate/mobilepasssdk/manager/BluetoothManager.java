package com.armongate.mobilepasssdk.manager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.core.app.ActivityCompat;

import com.armongate.mobilepasssdk.constant.DataTypes;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.PacketHeaders;
import com.armongate.mobilepasssdk.constant.UUIDs;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.enums.Language;
import com.armongate.mobilepasssdk.model.BLEDataContent;
import com.armongate.mobilepasssdk.model.BLEScanConfiguration;
import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnection;
import com.armongate.mobilepasssdk.model.DeviceConnectionInfo;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.DeviceInRange;
import com.armongate.mobilepasssdk.model.DeviceWriteItem;
import com.armongate.mobilepasssdk.util.ArrayUtil;
import com.armongate.mobilepasssdk.util.ConverterUtil;
import com.armongate.mobilepasssdk.util.DataParserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class BluetoothManager {

    // Singleton

    private static BluetoothManager instance = null;
    private BluetoothManager() {
        String manufacturer = Build.MANUFACTURER;
        LogManager.getInstance().debug("Device manufacturer: " + manufacturer.toUpperCase());
    }

    public static BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }

        return instance;
    }

    // Fields

    public BluetoothManagerDelegate delegate;

    private Context                         activeContext               = null;
    private DeviceCapability                bluetoothState              = null;

    private List<UUID>                      filterServiceUUIDs          = new ArrayList<>();
    private List<UUID>                      filterCharacteristicUUIDs   = new ArrayList<>();

    private Map<String, DeviceInRange>      currentDevicesInRange       = new HashMap<>();
    private Map<String, DeviceConnection>   currentConnectedDevices     = new HashMap<>();
    private BLEScanConfiguration            currentConfiguration        = null;
    private BluetoothAdapter                currentBluetoothAdapter     = null;
    private BluetoothLeScanner              currentBluetoothScanner     = null;

    private boolean                         isConnectionActive          = false;
    private Map<String, Long>               lastConnectionTime          = new HashMap<>();

    private boolean                         isScanningActive            = false;
    private boolean                         isReceiverRegistered        = false;
    
    private final Queue<DeviceWriteItem>    mWriteQueue     = new LinkedList<>();
    private boolean                         mWriteActive    = false;
    private int                             mReconnectCount = 0;

    // Public Functions

    public void setContext(Context context) {
        this.activeContext = context;

        if (this.currentBluetoothAdapter == null) {
            readyBluetoothAdapter();
        }
    }

    public DeviceCapability getCurrentState() {
        if (this.bluetoothState == null) {
            this.bluetoothState = new DeviceCapability(false, false, false);
        }

        return this.bluetoothState;
    }

    public void registerStateReceiver() {
        if (this.currentBluetoothScanner != null && this.activeContext != null && !this.isReceiverRegistered) {
            this.registerReceiver();
        }
    }

    public void unregisterStateReceiver() {
        if (this.currentBluetoothScanner != null && this.activeContext != null && this.isReceiverRegistered) {
            this.activeContext.unregisterReceiver(mBLEStateReceiver);

            LogManager.getInstance().info("Bluetooth state listener is unregistered");
            this.isReceiverRegistered = false;
        }
    }

    public void setReady() {
        if (currentBluetoothAdapter == null) {
            readyBluetoothAdapter();
        }
    }

    public void startScan(BLEScanConfiguration configuration) {
        LogManager.getInstance().info("Bluetooth scanner is starting...");

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in startScan. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        // Check current Bluetooth Adapter instance
        setReady();

        // Check current Bluetooth Scanner instance
        if (currentBluetoothScanner == null) {
            if (currentBluetoothAdapter != null) {
                currentBluetoothScanner = currentBluetoothAdapter.getBluetoothLeScanner();

                if (currentBluetoothScanner == null) {
                    LogManager.getInstance().error("Bluetooth scanner has been received as null from adapter, so starting scan has been ignored", LogCodes.BLUETOOTH_SCANNER_FAILED);
                    return;
                }
            } else {
                LogManager.getInstance().warn("Bluetooth adapter has not been initialized, so starting scan has been ignored", LogCodes.BLUETOOTH_SCANNING_FLOW);
                return;
            }

            LogManager.getInstance().info("Bluetooth scanner has been initialized");
        } else {
            LogManager.getInstance().info("Bluetooth scanner has already been initialized");
        }

        LogManager.getInstance().debug("Flush pending scan results for Bluetooth scanner to ready for next scanning");
        currentBluetoothScanner.flushPendingScanResults(mPeripheralScanCallback);

        // Ready for new scanning
        clearFieldsForNewScan(configuration);

        // Prepare service and characteristic UUID lists for new scan
        List<ScanFilter>    scanFilters     = prepareUUIDLists();
        ScanSettings        scanSettings    = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        LogManager.getInstance().info("Bluetooth scanner is ready for scanning");

        // Check active scanning status
        if (isScanningActive) {
            LogManager.getInstance().info("Bluetooth scanner is already scanning, new session has been ignored!");
            return;
        }

        isScanningActive = true;

        LogManager.getInstance().info("Scanning nearby BLE devices now");
        currentBluetoothScanner.startScan(scanFilters, scanSettings, mPeripheralScanCallback);
    }

    public void stopScan(boolean disconnect) {
        LogManager.getInstance().info("Bluetooth scanner is stopping...");

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in stopScan. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        if(currentBluetoothAdapter == null || currentBluetoothScanner == null) {
            LogManager.getInstance().info("Bluetooth scanner has not been initialized yet, so stop request has been ignored");
            return;
        }

        isScanningActive = false;

        try {
            currentBluetoothScanner.stopScan(mPeripheralScanCallback);
            currentBluetoothScanner.flushPendingScanResults(mPeripheralScanCallback);

            LogManager.getInstance().info("Bluetooth scanner has been stopped successfully");
        } catch (Exception ex) {
            LogManager.getInstance().warn("Stop Bluetooth scanner request has been failed, error: " + ex.getLocalizedMessage(), LogCodes.BLUETOOTH_SCANNING_FLOW);
            LogManager.getInstance().warn("Bluetooth scanning may be in progress, but incoming results are not being processed.", LogCodes.BLUETOOTH_SCANNING_FLOW);
        }

        if (disconnect) {
            disconnect();
        }
    }

    public void connectToDevice(String deviceIdentifier) {
        LogManager.getInstance().debug("Connect to device is requested for identifier: " + deviceIdentifier);

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in connectToDevice. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        if(isConnectionActive) {
            LogManager.getInstance().warn("Another Bluetooth connection is active, so new one has been ignored", LogCodes.BLUETOOTH_CONNECTION_DUPLICATE);
            return;
        }

        DeviceInRange deviceInRange = currentDevicesInRange.get(deviceIdentifier);

        if (deviceInRange != null) {
            isConnectionActive = true;
            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.CONNECTING);

            LogManager.getInstance().info("Connecting to device with service UUID: " + deviceInRange.serviceUUID);

            BluetoothGatt connection = deviceInRange.device.connectGatt(activeContext, false, mBluetoothGattCallback);
            currentConnectedDevices.put(deviceIdentifier, new DeviceConnection(deviceInRange.device, deviceInRange.serviceUUID, connection));
        } else {
            LogManager.getInstance().warn("Connecting to device has been ignored, because selected device has not been found in range now", LogCodes.BLUETOOTH_CONNECTION_NOT_FOUND);
            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.NOT_FOUND);
        }
    }

    public void disconnectFromDevice() {
        disconnect();
    }

    // Private Functions

    private void readyBluetoothAdapter() {
        LogManager.getInstance().info("Bluetooth adapter is initializing...");

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in readyBluetoothAdapter. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        android.bluetooth.BluetoothManager manager = (android.bluetooth.BluetoothManager) this.activeContext.getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            this.currentBluetoothAdapter = manager.getAdapter();

            if (this.currentBluetoothAdapter != null) {
                LogManager.getInstance().info("Bluetooth adapter is ready for scanner");
                this.registerReceiver();
            } else {
                LogManager.getInstance().error("Bluetooth adapter has been received as null from BluetoothManager instance of Android system", LogCodes.BLUETOOTH_ADAPTER_ERROR);
            }
        } else {
            LogManager.getInstance().error("Bluetooth adapter could not be accessed, because empty manager instance has been received from Android system", LogCodes.BLUETOOTH_ADAPTER_ERROR);
        }
    }

    private void registerReceiver() {
        if (this.currentBluetoothAdapter != null && this.activeContext != null) {
            this.updateBLECapability(this.currentBluetoothAdapter.getState());

            IntentFilter bleStateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            this.activeContext.registerReceiver(mBLEStateReceiver, bleStateFilter);

            LogManager.getInstance().info("Bluetooth state listener has been registered");
            this.isReceiverRegistered = true;
        } else {
            LogManager.getInstance().debug("Register receiver has been cancelled because of empty adapter or context reference");
        }
    }

    private void updateBLECapability(int state) {
        LogManager.getInstance().info("Bluetooth state changed. Supported: " + (this.currentBluetoothAdapter != null) + ", State: " + (state == BluetoothAdapter.STATE_ON ? "Enabled" : "Disabled"));
        this.bluetoothState = new DeviceCapability(this.currentBluetoothAdapter != null, state == BluetoothAdapter.STATE_ON, false);

        if (delegate != null) {
            delegate.onBLEStateChanged(this.bluetoothState);
        }
    }

    private void clearFieldsForNewScan(BLEScanConfiguration configuration) {
        // Set fields for new scanning
        currentConfiguration = configuration;

        // Clear devices that found before
        currentDevicesInRange.clear();

        // Clear devices that stay in connected devices list
        currentConnectedDevices.clear();

        // Clear connection limit flags
        isConnectionActive = false;

        LogManager.getInstance().debug("Related fields are cleared for new scanning session");
    }

    private List<ScanFilter> prepareUUIDLists() {
        if (currentConfiguration == null) {
            return new ArrayList<>();
        }

        filterServiceUUIDs.clear();
        filterCharacteristicUUIDs.clear();

        for (String uuid :
                currentConfiguration.deviceList.keySet()) {
            LogManager.getInstance().debug("Filter services with uuid: " + uuid);
            try {
                filterServiceUUIDs.add(UUID.fromString(uuid));
            }  catch (IllegalArgumentException e) {
                LogManager.getInstance().error("Invalid UUID format for service: " + uuid, LogCodes.BLUETOOTH_SCANNING_FLOW);
            }

            try {
                filterCharacteristicUUIDs.add(UUID.fromString(UUIDs.CHARACTERISTIC));
            }  catch (IllegalArgumentException e) {
                LogManager.getInstance().error("Invalid UUID format for characteristic: " + UUIDs.CHARACTERISTIC, LogCodes.BLUETOOTH_SCANNING_FLOW);
            }
        }

        // Dynamic filtering is active for SDK, so ScanFilter builder has not been used
        return new ArrayList<>();
    }

    private void disconnect() {
        LogManager.getInstance().info("Disconnect from connected device is requested");

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in disconnect. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        boolean waitingDisconnect = false;

        if (currentConnectedDevices != null && currentConnectedDevices.size() > 0) {
            for (DeviceConnection connectedDevice :
                    currentConnectedDevices.values()) {
                if (connectedDevice.connection == null) {
                    LogManager.getInstance().info("Peripheral exists in connected devices list, but not connected now.");
                    break;
                }

                LogManager.getInstance().info("Disconnecting from peripheral");
                waitingDisconnect = true;
                connectedDevice.connection.disconnect();
            }
        }

        if (!waitingDisconnect) {
            LogManager.getInstance().info("There is no connected device now, disconnection flow is not required");
            onDisconnectedCompleted();
        }
    }

    private void onDisconnectedCompleted() {
        LogManager.getInstance().info("Disconnecting from device is completed");
        isConnectionActive = false;
        mReconnectCount = 0;
    }

    private void onConnectionStateChanged(String identifier, DeviceConnectionStatus.ConnectionState connectionState) {
        onConnectionStateChanged(identifier, connectionState, null, null);
    }

    private void onConnectionStateChanged(String identifier, DeviceConnectionStatus.ConnectionState connectionState, Integer failReason, String failMessage) {
        LogManager.getInstance().info("Bluetooth connection state changed for " + (identifier != null ? identifier : "-") + " > " + connectionState.toString());

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in onConnectionStateChanged. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        if (delegate != null) {
            if (identifier != null) {
                delegate.onConnectionStateChanged(new DeviceConnectionStatus(identifier, connectionState, failReason, failMessage));
            } else {
                LogManager.getInstance().debug("Identifier is not provided to generate callback for listener about connection state");
            }
        } else {
            LogManager.getInstance().debug("There is no listener exists to inform about connection state");
        }

        if (connectionState == DeviceConnectionStatus.ConnectionState.FAILED) {
            LogManager.getInstance().warn("Bluetooth connection to device has failed", LogCodes.BLUETOOTH_CONNECTION_FAILED);
            disconnect();
        } else if (connectionState == DeviceConnectionStatus.ConnectionState.DISCONNECTED) {
            if (identifier != null) {
                lastConnectionTime.put(identifier, System.currentTimeMillis());

                if (currentConnectedDevices.containsKey(identifier)) {
                    LogManager.getInstance().info("Device (" + identifier + ") is removed from connected devices' list!");
                    DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

                    // Close connection before remove
                    if (connectedDevice != null && connectedDevice.connection != null) {
                        connectedDevice.connection.close();
                        connectedDevice.connection = null;
                    }

                    currentConnectedDevices.remove(identifier);
                }

                if (currentDevicesInRange.containsKey(identifier)) {
                    LogManager.getInstance().info("Device (" + identifier + ") is removed from list of devices in range");
                    currentDevicesInRange.remove(identifier);
                }
            } else {
                LogManager.getInstance().warn("Identifier is empty, so related operations cannot be done for disconnect!", LogCodes.BLUETOOTH_CONNECTION_FLOW);
            }

            onDisconnectedCompleted();
        }
    }

    private void changeSubscription(boolean enable, String identifier, BluetoothGattCharacteristic forCharacteristic) {
        LogManager.getInstance().debug("Change subscription state for " + forCharacteristic.getUuid().toString() + " with value: " + enable);
        LogManager.getInstance().debug("Descriptor count for characteristic: " + forCharacteristic.getDescriptors().size());

        BluetoothGattDescriptor descriptor = null;

        for (BluetoothGattDescriptor descriptorIterator: forCharacteristic.getDescriptors()) {
            descriptor = descriptorIterator;
        }

        if (descriptor != null) {
            // Check notify property
            if ((forCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            } else {
                LogManager.getInstance().warn("Bluetooth characteristic does not have NOTIFY property!", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
            }

            LogManager.getInstance().debug("Adding write descriptor request for " + forCharacteristic.getUuid().toString());
            DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

            if (connectedDevice != null && connectedDevice.connection != null) {
                writeToDevice(enable, forCharacteristic, descriptor, connectedDevice.connection );
            } else {
                onWriteForDescriptorFailed("Write descriptor is ignored, current connection is null!", null);
            }
        } else {
            onWriteForDescriptorFailed("Change subscription failed, descriptor not found for characteristic", null);
        }
    }

    private void writeToDevice(byte[] data, BluetoothGattCharacteristic characteristic, BluetoothGatt connection) {
        this.writeToDevice(new DeviceWriteItem(data, characteristic, connection));
    }

    private void writeToDevice(boolean notifyEnable, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, BluetoothGatt connection) {
        this.writeToDevice(new DeviceWriteItem(notifyEnable, characteristic, descriptor, connection));
    }

    private void writeToDevice(DeviceWriteItem item) {
        if (item.connection != null) {
            LogManager.getInstance().debug("Write to device > " + (item.useDescriptor ? item.notifyEnable : ConverterUtil.bytesToHexString(item.message)));

            this.mWriteQueue.add(item);
            checkWriteQueue();
        }
    }

    private void onWriteCompleted() {
        this.mWriteActive = false;
        checkWriteQueue();
    }

    private void checkWriteQueue() {
        LogManager.getInstance().debug("Checking write queue for Bluetooth communication. Queue Size: " + mWriteQueue.size() + ", IsWriteActive: " + mWriteActive);

        if (this.activeContext == null) {
            LogManager.getInstance().warn("BluetoothManager: activeContext is null in checkWriteQueue. Initialization may be missing.", LogCodes.BLUETOOTH_MISSING_CONTEXT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this.activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().info("Missing BluetoothScan permission!");
            return;
        }

        if (this.mWriteQueue.size() > 0 && !this.mWriteActive) {
            // Get next item from queue to send
            DeviceWriteItem currentItem = this.mWriteQueue.remove();

            // Set active flag true to prevent sending another data packet before complete current one
            this.mWriteActive = true;

            if (currentItem.useDescriptor) {
                LogManager.getInstance().debug("Writing data to device for descriptor");

                if (currentItem.connection.setCharacteristicNotification(currentItem.characteristic, currentItem.notifyEnable)) {
                    try {
                        if (currentItem.connection.writeDescriptor(currentItem.descriptor)) {
                            LogManager.getInstance().debug("Set notify " + currentItem.notifyEnable + " is completed | Characteristic UUID: " + currentItem.characteristic.getUuid().toString());
                        } else {
                            onWriteForDescriptorFailed("Failed to set notification subscription! - WriteDescriptor return false", currentItem.connection);
                        }
                    } catch (Exception ex) {
                        onWriteForDescriptorFailed("Failed to set notification subscription with error: " + ex.getLocalizedMessage(), currentItem.connection);
                    }

                } else {
                    onWriteForDescriptorFailed("Failed to set notification subscription! - SetCharacteristicNotification return false", currentItem.connection);
                }
            } else {
                LogManager.getInstance().debug("Writing data to device for characteristic");

                currentItem.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                currentItem.characteristic.setValue(currentItem.message);

                currentItem.connection.writeCharacteristic(currentItem.characteristic);
            }
        }
    }

    private void onWriteForDescriptorFailed(String logMessage, BluetoothGatt connection) {
        LogManager.getInstance().warn(logMessage, LogCodes.BLUETOOTH_COMMUNICATION_FLOW);

        if (connection != null && connection.getDevice() != null) {
            onConnectionStateChanged(connection.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
        } else {
            onConnectionStateChanged(null, DeviceConnectionStatus.ConnectionState.FAILED);
        }

        onWriteCompleted();
    }

    private void processChallengeResult(String deviceIdentifier, BLEDataContent result) {
        if (result.result == DataTypes.RESULT.Succeed) {
            onConnectionStateChanged(deviceIdentifier, DeviceConnectionStatus.ConnectionState.CONNECTED);
            LogManager.getInstance().info("Disconnecting from device after successful process of passing!");
            disconnect();
        } else {
            onConnectionStateChanged(
                    deviceIdentifier,
                    DeviceConnectionStatus.ConnectionState.FAILED,
                    result.data.containsKey("reason") ? (Integer) result.data.get("reason") : null,
                    result.data.containsKey("message") ? (String) result.data.get("message") : null);
        }
    }

    private byte[] generateChallengeResponse(byte[] challenge, byte[] iv, DeviceConnectionInfo deviceInfo) throws Exception {
        if (currentConfiguration.dataUserBarcode == null || currentConfiguration.dataUserBarcode.isEmpty()) {
            return this.generateDirectionChallengeResponse(challenge, iv, deviceInfo);
        } else {
            return this.generateMacfitChallengeResponse(challenge, iv, deviceInfo);
        }
    }

    private byte[] generateDirectionChallengeResponse(byte[] challenge, byte[] iv, DeviceConnectionInfo deviceInfo) throws Exception {
        byte[] resultData = new byte[] {
                PacketHeaders.PROTOCOLV2.GROUP.AUTH,
                PacketHeaders.PROTOCOLV2.AUTH.DIRECTION_CHALLENGE,
                PacketHeaders.PLATFORM_ANDROID
        };

        resultData = ArrayUtil.concat(resultData, ConverterUtil.stringToData(currentConfiguration.dataUserId, 16, (byte)0, false));
        resultData = ArrayUtil.concat(resultData, ConverterUtil.stringToData(currentConfiguration.hardwareId, 16, (byte)0, false));
        resultData = ArrayUtil.add(resultData, (byte)ConverterUtil.mergeToData(currentConfiguration.deviceNumber, currentConfiguration.direction, currentConfiguration.relayNumber));

        byte[] encryptedResponse = CryptoManager.getInstance().encryptBytesWithIV(ConfigurationManager.getInstance().getPrivateKey(), deviceInfo.publicKey, challenge, iv);
        resultData = ArrayUtil.concat(resultData, encryptedResponse);

        return resultData;
    }

    private byte[] generateMacfitChallengeResponse(byte[] challenge, byte[] iv, DeviceConnectionInfo deviceInfo) throws Exception {
        byte[] resultData = new byte[] {
                PacketHeaders.PROTOCOLV2.GROUP.AUTH,
                PacketHeaders.PROTOCOLV2.AUTH.MACFIT_CHALLENGE,
                PacketHeaders.PLATFORM_ANDROID
        };

        resultData = ArrayUtil.concat(resultData, ConverterUtil.stringToData(currentConfiguration.dataUserId, 16, (byte)0, false));
        resultData = ArrayUtil.concat(resultData, ConverterUtil.stringToData(currentConfiguration.dataUserBarcode, 16, (byte)0, false));
        resultData = ArrayUtil.concat(resultData, ConverterUtil.hexStringToBytes(currentConfiguration.qrCodeId.replace("-", "")));
        resultData = ArrayUtil.add(resultData, currentConfiguration.language == Language.EN ? (byte)0x01 : (byte)0x00);

        byte[] encryptedResponse = CryptoManager.getInstance().encryptBytesWithIV(ConfigurationManager.getInstance().getPrivateKey(), deviceInfo.publicKey, challenge, iv);
        resultData = ArrayUtil.concat(resultData, encryptedResponse);

        return resultData;
    }

    private void changeCommunicationState(String identifier, boolean start, int rssiValue) {
        if (currentConnectedDevices.containsKey(identifier)) {
            DeviceConnection connectedDevice = currentConnectedDevices.get(identifier);

            if (connectedDevice != null) {
                String communicationState = start ? "Starting" : "Ending";
                String subscriptionState = start ? "Subscribe" : "Unsubscribe";

                LogManager.getInstance().debug(communicationState + " communication with " + identifier + " at RSSI: " + rssiValue);

                for (BluetoothGattCharacteristic characteristic :
                        connectedDevice.characteristics.values()) {
                    LogManager.getInstance().debug(subscriptionState + " for characteristic: " + characteristic.getUuid().toString());
                    changeSubscription(start, identifier, characteristic);
                }
            } else {
                LogManager.getInstance().warn("Device identifier could not be found in connected devices list", LogCodes.BLUETOOTH_CONNECTION_FLOW);
                onConnectionStateChanged(identifier, DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }
    }


    // Broadcast Receivers

    private final BroadcastReceiver mBLEStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateBLECapability(state);
            }
        }
    };

    // Callbacks

    private ScanCallback mPeripheralScanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            LogManager.getInstance().error("Bluetooth scanning failed! Error Code: " + errorCode, LogCodes.BLUETOOTH_SCANNING_FLOW);
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (isScanningActive) {
                if (!isConnectionActive) {
                    if (filterServiceUUIDs == null || filterServiceUUIDs.size() == 0) {
                        LogManager.getInstance().debug("There is no service UUID filter, ignore scan result!");
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    ScanRecord scanRecord = result.getScanRecord();

                    if (scanRecord == null) {
                        LogManager.getInstance().debug("Scan result is empty, ignore this one!");
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    String  armonDeviceName     = scanRecord.getDeviceName();
                    String  foundServiceUUID    = "";
                    boolean hasValidServiceUUID = false;

                    List<ParcelUuid> recordServiceUUIDs = scanRecord.getServiceUuids();

                    if (recordServiceUUIDs != null && recordServiceUUIDs.size() > 0) {
                        for (ParcelUuid uuid : recordServiceUUIDs) {
                            if (uuid.getUuid() != null && filterServiceUUIDs.contains(uuid.getUuid())) {
                                hasValidServiceUUID = true;
                                foundServiceUUID    = uuid.getUuid().toString();
                            }
                        }
                    } else {
                        LogManager.getInstance().debug("Found device has no service uuid, ignore this one! | " + armonDeviceName);
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    if (!hasValidServiceUUID) {
                        LogManager.getInstance().debug("Peripheral found with unknown service UUID, ignore scan result! | " + armonDeviceName);
                        super.onScanResult(callbackType, result);
                        return;
                    }

                    LogManager.getInstance().debug("Discovered peripheral (" + armonDeviceName + ") at " + result.getRssi());

                    if (currentConfiguration == null) {
                        LogManager.getInstance().debug("Connection configuration is not defined yet!");
                    } else {
                        if (result.getRssi() < 0 && result.getRssi() > -95) {
                            // Device in range for specified RSSI limit

                            // Check device added to list before
                            if (!currentDevicesInRange.containsKey(result.getDevice().getAddress())) {
                                if (lastConnectionTime.containsKey(result.getDevice().getAddress())) {
                                    Long lastConnection = lastConnectionTime.get(result.getDevice().getAddress());
                                    long currentTime = System.currentTimeMillis();

                                    if (lastConnection != null) {
                                        double elapsedTime = (currentTime - lastConnection) / 1000.0;

                                        if (elapsedTime < 5) {
                                            LogManager.getInstance().debug("Device is ignored because of reconnection timeout, elapsed time: " + elapsedTime);
                                            super.onScanResult(callbackType, result);
                                            return;
                                        }
                                    }
                                }

                                currentDevicesInRange.put(result.getDevice().getAddress(), new DeviceInRange(foundServiceUUID, result.getDevice()));
                                LogManager.getInstance().debug("Peripheral is added to device list, current device count: " + currentDevicesInRange.size());

                                connectToDevice(result.getDevice().getAddress());
                            }

                        } else if (result.getRssi() != 127) {
                            if (currentDevicesInRange.containsKey(result.getDevice().getAddress())) {
                                currentDevicesInRange.remove(result.getDevice().getAddress());

                                LogManager.getInstance().debug("Peripheral is removed from device list, current device count: " + currentDevicesInRange.size());
                            }
                        }
                    }
                }
            } else {
                LogManager.getInstance().debug("Scanning active flag is false now, scan result is ignored");
            }

            super.onScanResult(callbackType, result);
        }
    };

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LogManager.getInstance().info("Missing BluetoothScan permission!");
                super.onConnectionStateChange(gatt, status, newState);
                return;
            }

            String state;

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    state = "Connected";
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    state = "Connecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    state = "Disconnecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    state = "Disconnected";
                    break;
                default:
                    state = "Unknown";
            }

            LogManager.getInstance().info("Connection state changed, new state: " + state + ", status: " + (status == BluetoothGatt.GATT_SUCCESS ? "Succeed" : "Failed"));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                LogManager.getInstance().info("Connected to " + gatt.getDevice().getName());

                // Change MTU size for large packets
                gatt.requestMtu(185);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                LogManager.getInstance().info("Disconnected from " + gatt.getDevice().getName());

                mWriteActive = false;
                mWriteQueue.clear();

                if (status == 133 && mReconnectCount < 2) {
                    LogManager.getInstance().info("Try reconnection to " + gatt.getDevice().getName());

                    isConnectionActive = false;
                    mReconnectCount++;
                    connectToDevice(gatt.getDevice().getAddress());
                } else {
                    onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.DISCONNECTED);
                }
            }

            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(activeContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LogManager.getInstance().info("Missing BluetoothScan permission!");
                return;
            }

            if(status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.getInstance().debug("MTU size changed successfully: " + mtu);
                LogManager.getInstance().debug("Starting discover services...");

                gatt.discoverServices();
            }
            else {
                LogManager.getInstance().error("Changing MTU size failed!", LogCodes.BLUETOOTH_CONNECTION_FLOW);
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            LogManager.getInstance().debug("Service discovered with status: " + status);

            if (gatt.getServices() == null || gatt.getServices().isEmpty()) {
                LogManager.getInstance().warn("List of peripheral services is empty for " + gatt.getDevice().getAddress(), LogCodes.BLUETOOTH_CONNECTION_FLOW);
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                return;
            }

            for (BluetoothGattService service: gatt.getServices()) {
                if (filterServiceUUIDs.contains(service.getUuid())) {
                    LogManager.getInstance().debug("Service found for peripheral (" + gatt.getDevice().getAddress() + "), UUID: " + service.getUuid().toString());

                    DeviceConnection device = currentConnectedDevices.get(gatt.getDevice().getAddress());

                    if (device != null) {
                        for (BluetoothGattCharacteristic characteristicIterator : service.getCharacteristics()) {
                            if (filterCharacteristicUUIDs.contains(characteristicIterator.getUuid())) {
                                LogManager.getInstance().debug("Characteristic found for peripheral (" + gatt.getDevice().getAddress() + "), UUID: " + characteristicIterator.getUuid().toString());

                                LogManager.getInstance().debug("Store characteristic with UUID: " + characteristicIterator.getUuid().toString());
                                device.characteristics.put(characteristicIterator.getUuid().toString(), characteristicIterator);
                            }
                        }

                        if (currentConfiguration != null) {
                            LogManager.getInstance().debug("Auto subscribe for modes other than passing with close phone");
                            changeCommunicationState(gatt.getDevice().getAddress(), true, 0);
                        }
                    } else {
                        LogManager.getInstance().warn("Device cannot be found in connected devices list, stop process!", LogCodes.BLUETOOTH_CONNECTION_FLOW);
                        onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.getInstance().debug("Write to device for characteristic completed successfully!");
            } else {
                LogManager.getInstance().error("Write to device for characteristic failed!", LogCodes.BLUETOOTH_CONNECTION_FLOW);
            }

            onWriteCompleted();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            byte[] dataValue = descriptor.getValue();
            if(dataValue == null) {
                LogManager.getInstance().debug("Descriptor value is empty on this go-round");
                onWriteCompleted();
                return;
            }

            // 0x01 - Subscribe
            // 0x00 - Unsubscribe

            if (dataValue[0] == 0x01) {
                LogManager.getInstance().debug("Subscribed to characteristic: " + descriptor.getCharacteristic().getUuid().toString());
            } else if (dataValue[0] == 0x00) {
                LogManager.getInstance().debug("Unsubscribed from characteristic: " + descriptor.getCharacteristic().getUuid().toString());
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.getInstance().debug("Write to device for descriptor completed successfully!");
            } else {
                LogManager.getInstance().error("Write to device for descriptor failed!", LogCodes.BLUETOOTH_CONNECTION_FLOW);
            }

            onWriteCompleted();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            try {
                byte[] receivedData = characteristic.getValue();

                if (receivedData == null || receivedData.length == 0) {
                    LogManager.getInstance().error("Value that received from characteristic is empty, disconnect now", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                    onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    return;
                } else {
                    LogManager.getInstance().debug("New value received from characteristic");
                    LogManager.getInstance().debug("Received Data: " + ConverterUtil.bytesToHexString(receivedData));
                }

                if (!currentConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
                    LogManager.getInstance().warn("Value received but peripheral is not in connected devices' list, ignore data!", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                    return;
                }


                BLEDataContent result = DataParserUtil.getInstance().parse(receivedData);

                if (result != null) {
                    if (result.type == DataTypes.TYPE.AuthChallengeForPublicKey) {
                        try {
                            String deviceId = result.data.containsKey("deviceId") ? result.data.get("deviceId").toString() : "";

                            DeviceConnection        connectedDevice         = currentConnectedDevices.get(gatt.getDevice().getAddress());
                            DeviceConnectionInfo    deviceConnectionInfo    = currentConfiguration.deviceList.get(connectedDevice.serviceUUID.toLowerCase());

                            if (connectedDevice != null && deviceConnectionInfo != null) {
                                LogManager.getInstance().debug("Auth challenge received, device id: " + deviceId);

                                byte[] resultData = generateChallengeResponse(
                                        result.data.containsKey("challenge") ? ConverterUtil.hexStringToBytes(result.data.get("challenge").toString()) : new byte[0],
                                        result.data.containsKey("iv") ? ConverterUtil.hexStringToBytes(result.data.get("iv").toString()) : new byte[0],
                                        deviceConnectionInfo
                                );

                                writeToDevice(resultData, characteristic, connectedDevice.connection);
                            } else {
                                LogManager.getInstance().error("Generate challenge response failed because of empty stored device definition", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                            }
                        } catch (Exception ex) {
                            LogManager.getInstance().error("Generate challenge response failed with error: " + ex.getLocalizedMessage(), LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                            onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                        }
                    } else if (result.type == DataTypes.TYPE.AuthChallengeResult) {
                        processChallengeResult(gatt.getDevice().getAddress(), result);
                    } else {
                        LogManager.getInstance().warn("Unknown data type received from device, disconnect now!", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                        onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                    }

                } else {
                    LogManager.getInstance().warn("Unknown data received from device, disconnect now!", LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                    onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
                }
            } catch (Exception ex) {
                LogManager.getInstance().error("Handle data that received from device failed! " + ex.getLocalizedMessage(), LogCodes.BLUETOOTH_COMMUNICATION_FLOW);
                onConnectionStateChanged(gatt.getDevice().getAddress(), DeviceConnectionStatus.ConnectionState.FAILED);
            }
        }

    };
}
