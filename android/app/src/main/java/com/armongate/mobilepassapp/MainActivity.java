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
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassFlowResult;
import com.armongate.mobilepasssdk.model.PassFlowState;
import com.armongate.mobilepasssdk.model.PassResult;


public class MainActivity extends AppCompatActivity implements MobilePassDelegate {

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
            config.memberId = txtMemberId.getText().toString(); // "00988791";
            config.serverUrl = "https://macfit.armon.com.tr:3443"; // "https://dev3.armon.com.tr:4334"; // "https://10.10.10.243:4334"; // "https://qr.marsathletic.com";
            // config.barcode = "600542998112";
            config.language = "tr";
            config.waitBLEEnabled = false;
            config.closeWhenInvalidQRCode = true;
            config.connectionTimeout = 10;
            config.autoCloseTimeout = 5;
            config.listener = this;
            config.logLevel = LogLevel.DEBUG;

            MobilePass passer = new MobilePass(this, config);
            passer.setDelegate(this);

            passer.triggerQRCodeRead();
        }
    }

    /*
    @Override
    public void onPassCancelled(int reason) {
        Log.i("MobilePass", "Main - Pass Cancelled, Reason: " + reason);
    }

    @Override
    public void onPassCompleted(PassResult result) {
        Log.i("MobilePass", "Main - Pass Completed, Result: " + result.success + ", ClubId: " + result.clubId + ", ClubName: " + result.clubName + ", Direction: " + result.direction + ", FailCode: " + (result.failCode != null ? result.failCode : "-"));
    }

    @Override
    public void onQRCodeListStateChanged(int state) {
        Log.i("MobilePass", "Main - QR Code List Changed, State: " + state);
    }
     */

    @Override
    public void onLogReceived(LogItem log) {
        // Log.i("MobilePass", "Log Received >> " + log.level + " | " + log.message);
    }

    @Override
    public void onInvalidQRCode(String content) {
        Log.i("MobilePass", "MAIN - Invalid QR Code received: " + content);
    }

    @Override
    public void onMemberIdChanged() {
        Log.i("MobilePass", "MAIN - Member Id Changed");
    }

    @Override
    public void onSyncMemberIdCompleted() {
        Log.i("MobilePass", "MAIN - Sync MemberId Completed");
    }

    @Override
    public void onSyncMemberIdFailed(int statusCode) {
        Log.i("MobilePass", "MAIN - Sync MemberId Failed: " + statusCode);
    }

    @Override
    public void onQRCodesDataLoaded(int count) {
        Log.i("MobilePass", "MAIN - Stored QR Code list is loaded: " + count);
    }

    @Override
    public void onQRCodesSyncStarted() {
        Log.i("MobilePass", "MAIN - Sync QR Code list started");
    }

    @Override
    public void onQRCodesSyncFailed(int statusCode) {
        Log.i("MobilePass", "MAIN - Sync QR Code list failed: " + statusCode);
    }

    @Override
    public void onQRCodesReady(boolean synced, int count) {
        Log.i("MobilePass", "MAIN - QR Code is ready! Synced: " + synced + ", Count: " + count);
    }

    @Override
    public void onQRCodesEmpty() {
        Log.i("MobilePass", "MAIN - QR Code list is empty!");
    }

    @Override
    public void onScanFlowCompleted(PassFlowResult result) {
        Log.i("MobilePass", "MAIN - Scan Flow Completed! Status: " + result.result + ", ClubId: " + result.clubId + ", ClubName: " + result.clubName + ", Direction: " + result.direction);

        for (PassFlowState state :
                result.states) {
            Log.i("MobilePass", "MAIN - State: " + state.state + ", Data: " + state.data);
        }
    }
}
