package com.armongate.mobilepassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.armongate.mobilepasssdk.MobilePass;
import com.armongate.mobilepasssdk.constant.LogLevel;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.LogItem;


public class MainActivity extends AppCompatActivity implements MobilePassDelegate {
    private MobilePass passer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView txtLogs = findViewById(R.id.armon_test_txtLogs);
        txtLogs.setMovementMethod(new ScrollingMovementMethod());
    }

    public void onButtonStartClicked(View v) {
        EditText txtMemberId = findViewById(R.id.armon_test_inputMemberId);

        if (txtMemberId.getText().toString().isEmpty()) {
            Toast.makeText(this, "Üye numaranızı giriniz", Toast.LENGTH_SHORT).show();
        } else {
            Configuration config = new Configuration();
            config.memberId = txtMemberId.getText().toString(); // "00988791";
            config.serverUrl = "https://qr.marsathletic.com";
            config.language = "tr";
            config.waitBLEEnabled = true;
            config.connectionTimeout = 10;
            config.listener = this;
            config.logLevel = LogLevel.INFO;

            passer = new MobilePass(this, config);
            // passer.setDelegate(this);

            passer.triggerQRCodeRead();
        }
    }

    public void onButtonGetLogsClicked(View v) {
        Button btnShare = findViewById(R.id.armon_test_btnShare);

        if(passer != null) {
            TextView txtLogs = findViewById(R.id.armon_test_txtLogs);

            String logs = getLogs();
            txtLogs.setText(logs);

            btnShare.setVisibility(logs.isEmpty() ? Button.INVISIBLE : Button.VISIBLE);
        } else {
            Toast.makeText(this, "Kayıtları almak için öncelikle geçiş akışı başlatınız", Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonShareClicked(View v) {
        Intent myIntent = new Intent(Intent.ACTION_SEND);
        myIntent.setType("text/plain");
        String body = getLogs();
        String sub = "SDK Kayıtları";
        myIntent.putExtra(Intent.EXTRA_SUBJECT,sub);
        myIntent.putExtra(Intent.EXTRA_TEXT,body);
        startActivity(Intent.createChooser(myIntent, "Paylaş"));
    }

    private String getLogs() {
        return "";
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

    @Override
    public void onLogReceived(LogItem log) {
        // Log.i("MobilePass", "Log Received >> " + log.level + " | " + log.message);
    }
}
