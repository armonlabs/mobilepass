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
            config.serverUrl = "https://dev3.armon.com.tr:4334"; // "https://qr.marsathletic.com";
            config.language = "tr";
            config.waitBLEEnabled = true;
            config.closeWhenInvalidQRCode = false;
            config.connectionTimeout = 10;
            config.autoCloseTimeout = 5;
            config.listener = this;
            config.logLevel = LogLevel.INFO;

            MobilePass passer = new MobilePass(this, config);
            passer.setDelegate(this);

            passer.triggerQRCodeRead();
        }
    }

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

    @Override
    public void onLogReceived(LogItem log) {
        // Log.i("MobilePass", "Log Received >> " + log.level + " | " + log.message);
    }

    @Override
    public void onInvalidQRCode(String content) {
        Log.i("MobilePass", "Main - Invalid QR Code received: " + content);
    }
}
