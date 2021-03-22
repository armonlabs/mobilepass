package com.armongate.mobilepassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.armongate.mobilepasssdk.MobilePass;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.model.Configuration;

public class MainActivity extends AppCompatActivity implements MobilePassDelegate {
    private MobilePass passer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration config = new Configuration();
        config.memberId = "customid";
        config.serverUrl = "https://dev3.armon.com.tr:3443";

        passer = new MobilePass(this, config);
        passer.setDelegate(this);
    }

    public void onTest(View v) {
        if (passer != null) {
            passer.triggerQRCodeRead();
        }
    }

    @Override
    public void onPassCancelled(int reason) {
        Log.i("MobilePass", "Main - Pass Cancelled, Reason: " + reason);
    }

    @Override
    public void onPassCompleted(boolean succeed) {
        Log.i("MobilePass", "Main - Pass Completed, Result: " + succeed);
    }

    @Override
    public void onQRCodeListStateChanged(int state) {
        Log.i("MobilePass", "Main - QR Code List Changed, State: " + state);
    }
}
