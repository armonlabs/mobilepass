package com.armongate.mobilepassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.armongate.mobilepasssdk.MobilePass;
import com.armongate.mobilepasssdk.constant.LogLevel;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.enums.PassFlowStateUpdate;
import com.armongate.mobilepasssdk.enums.QRCodeErrorType;
import com.armongate.mobilepasssdk.enums.QRCodesSyncState;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.LocationRequirement;
import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassFlowResult;
import com.armongate.mobilepasssdk.model.PassFlowState;
import com.armongate.mobilepasssdk.model.QRCodeProcessResult;


public class MainActivity extends AppCompatActivity implements MobilePassDelegate {

    private MobilePass sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonStartClicked(View v) {
        EditText txtMemberId = findViewById(R.id.armon_test_inputMemberId);

        if (txtMemberId.getText().toString().isEmpty()) {
            Toast.makeText(this, "Üye numaranızı giriniz", Toast.LENGTH_SHORT).show();
        } else {
            Configuration config = new Configuration();
            config.apiKey = "AB456C12998AA";
            config.memberId = txtMemberId.getText().toString();
            config.serverUrl = "https://macfit.armon.com.tr:3443";
            config.language = "tr";
            config.continueWithoutBLE = true;
            config.connectionTimeout = 10;
            config.listener = this;
            config.logLevel = LogLevel.DEBUG;

            sdk = new MobilePass(this, config);
            
            Toast.makeText(this, "✅ SDK hazır! Şimdi 'QR Kod Tara' butonuna basın.", Toast.LENGTH_SHORT).show();
        }
    }

    // Yeni metod: Mock QR kod tarama
    public void onButtonScanQRClicked(View v) {
        if (sdk == null) {
            Toast.makeText(this, "⚠️ Önce 'Başlat' butonuna basın!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Test için örnek QR kod verisi
        // Geçerli format: https://(app|sdk).armongate.com/(rq|bd|o|s)/[UUID](/[0-2])?
        // String mockQRCode = "https://app.armongate.com/o/2e9324d4-698a-44d7-8b37-ec862d924754";
        
        // Test için invalid format örnekleri:
        // String mockQRCode = "TEST_QR_CODE_DATA_123456"; // → INVALID_FORMAT
        // String mockQRCode = "http://app.armongate.com/o/uuid"; // → INVALID_FORMAT (http)
        // String mockQRCode = "https://app.armongate.com/rq/invalid-uuid"; // → INVALID_FORMAT (UUID format)
        String qrEntrance = "https://app.armongate.com/o/0c6fceca-7a86-486c-a7c2-4e9e45a96b25";
        String qrExit = "https://app.armongate.com/o/42261949-645f-4705-8ad7-dfaebea5c20f";

        String currentQR = qrExit;

        Log.i("MobilePass", "MAIN - Simulating QR scan: " + currentQR);
        Toast.makeText(this, "📷 QR Kod taranıyor...", Toast.LENGTH_SHORT).show();
        
        // SDK'ya QR kodu gönder
        QRCodeProcessResult result = sdk.processQRCode(currentQR);
        
        // Sonucu kontrol et
        if (result.isValid()) {
            Log.i("MobilePass", "MAIN - QR kod geçerli! SDK akışı başlatıldı.");
            Toast.makeText(this, "✅ QR kod geçerli! İşleniyor...", Toast.LENGTH_SHORT).show();
            // SDK otomatik olarak akışı başlatacak
            // Sonuç onPassFlowStateChanged() callback'inde gelecek
        } else {
            Log.i("MobilePass", "MAIN - QR kod geçersiz: " + result.getErrorType());
            handleInvalidQR(result.getErrorType());
        }
    }

    // Example: Handle invalid QR codes
    private void handleInvalidQR(QRCodeErrorType errorType) {
        String message;
        switch (errorType) {
            case INVALID_FORMAT:
                message = "QR kod formatı geçersiz. Tekrar deneyin.";
                break;
            case NOT_FOUND:
                message = "Yetkilendirilmemiş QR kod.";
                break;
            case EXPIRED:
                message = "QR kod süresi dolmuş.";
                break;
            case UNAUTHORIZED:
                message = "Bu QR koda erişim yetkiniz yok.";
                break;
            default:
                message = "Bilinmeyen hata.";
                break;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ========== NEW HEADLESS DELEGATE METHODS ==========

    @Override
    public void onLogReceived(LogItem log) {
        // Log.i("MobilePass", "Log: " + log.level + " | " + log.message);
    }

    @Override
    public void onMemberIdChanged() {
        Log.i("MobilePass", "MAIN - Member Id Changed");
    }

    @Override
    public void onSyncMemberIdCompleted(boolean success, Integer statusCode) {
        if (success) {
            Log.i("MobilePass", "MAIN - Sync MemberId Completed");
        } else {
            Log.i("MobilePass", "MAIN - Sync MemberId Failed: " + statusCode);
        }
    }

    @Override
    public void onQRCodesSyncStateChanged(QRCodesSyncState state) {
        switch (state.getState()) {
            case SYNC_STARTED:
                Log.i("MobilePass", "MAIN - QR Codes sync started");
                break;
            case SYNC_COMPLETED:
                Log.i("MobilePass", "MAIN - QR Codes ready! Synced: " + state.isSynced() + ", Count: " + state.getCount());
                break;
            case SYNC_FAILED:
                Log.i("MobilePass", "MAIN - QR Codes sync failed: " + state.getStatusCode());
                break;
            case DATA_EMPTY:
                Log.i("MobilePass", "MAIN - QR Codes list is empty!");
                break;
        }
    }

    @Override
    public void onPassFlowStateChanged(PassFlowStateUpdate update) {
        switch (update.getType()) {
            case STATE_CHANGED:
                Log.i("MobilePass", "MAIN - State: " + update.getState() + ", Message: " + update.getMessage());
                break;
            case COMPLETED:
                PassFlowResult result = update.getResult();
                Log.i("MobilePass", "MAIN - Flow Completed! " +
                        "Result: " + result.result + 
                        ", ClubId: " + result.clubId + 
                        ", ClubName: " + result.clubName + 
                        ", Direction: " + result.direction +
                        ", Message: " + result.message);

                for (PassFlowState state :
                        result.states) {
                    Log.i("MobilePass", "State: " + state.getState() + " | Timestamp: " + state.getDatetime().toString() + " | Data: " + state.getData());
                }
                
                // Show success/failure to user
                String resultMessage = result.result == 1 ? "✅ Geçiş başarılı!" : "❌ Geçiş başarısız!";
                Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onLocationVerificationRequired(LocationRequirement requirement) {
        Log.i("MobilePass", "MAIN - Location verification required: " +
                "lat=" + requirement.getLatitude() + 
                ", lon=" + requirement.getLongitude() + 
                ", radius=" + requirement.getRadius());
        
        // TODO: Implement your location verification UI
        // 1. Show map with requirement.latitude, requirement.longitude, requirement.radius
        // 2. Get user's current location
        // 3. Calculate distance
        // 4. If within radius, call: sdk.confirmLocationVerified()
        
        Toast.makeText(this, 
            "⚠️ Location verification needed!\n" +
            "Implement your own location UI\n" +
            "and call sdk.confirmLocationVerified()",
            Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionRequired(int type) {
        String permissionName;
        switch (type) {
            case 1: // NEED_PERMISSION_BLUETOOTH
                permissionName = "Bluetooth";
                break;
            case 2: // NEED_ENABLE_BLE
                permissionName = "Enable Bluetooth";
                Toast.makeText(this, "⚠️ Please enable Bluetooth in Settings", Toast.LENGTH_LONG).show();
                return;
            default:
                permissionName = "Unknown";
                break;
        }
        
        Log.i("MobilePass", "MAIN - Permission required: " + permissionName);
        Toast.makeText(this, "⚠️ Permission needed: " + permissionName, Toast.LENGTH_SHORT).show();
    }

    // Button: Confirm location verified
    public void onButtonConfirmLocationClicked(View v) {
        if (sdk == null) {
            Toast.makeText(this, "⚠️ Önce 'Başlat' butonuna basın!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.i("MobilePass", "MAIN - User confirmed location verification");
        sdk.confirmLocationVerified();
        Toast.makeText(this, "✅ Konum doğrulandı! SDK'ya bildirildi.", Toast.LENGTH_SHORT).show();
    }

    // Button: Trigger QR codes sync
    public void onButtonSyncQRCodesClicked(View v) {
        if (sdk == null) {
            Toast.makeText(this, "⚠️ Önce 'Başlat' butonuna basın!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.i("MobilePass", "MAIN - User triggered QR codes sync");
        sdk.sync();
        Toast.makeText(this, "🔄 QR kod listesi güncelleniyor...", Toast.LENGTH_SHORT).show();
    }

    // Button: Request permissions from user
    public void onButtonRequestPermissionsClicked(View v) {
        Log.i("MobilePass", "MAIN - Requesting all permissions");
        
        // Request Bluetooth permissions (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(
                new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                },
                100
            );
        } else {
            // Android 11 and below - only location needed (Bluetooth is auto-granted)
            requestPermissions(
                new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                },
                100
            );
        }
        
        Toast.makeText(this, "📋 İzin istekleri gönderildi", Toast.LENGTH_SHORT).show();
    }

    // Button: Cancel current flow
    public void onButtonCancelFlowClicked(View v) {
        if (sdk == null) {
            Toast.makeText(this, "⚠️ Önce 'Başlat' butonuna basın!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.i("MobilePass", "MAIN - User cancelled flow");
        sdk.cancelFlow();
        Toast.makeText(this, "❌ İşlem iptal edildi", Toast.LENGTH_SHORT).show();
    }
}
