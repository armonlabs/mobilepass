package com.armongate.mobilepassapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.model.Configuration;

import java.util.Collections;
import java.util.List;

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
            config.memberId = "00988791"; // txtMemberId.getText().toString();
            config.serverUrl = "https://10.10.10.118:3443"; // "https://qr.marsathletic.com";
            config.language = "tr";
            config.waitBLEEnabled = true;

            passer = new MobilePass(this, config);
            passer.setDelegate(this);

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
        StringBuilder builder = new StringBuilder();

        List<String> logItems = passer.getLogs();
        Collections.reverse(logItems);

        for (String log :
                logItems) {
            builder.append(log + "\n");
        }

        return builder.toString();
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
    public void onNeedPermission(int type) {
        Log.i("MobilePass", "Main - Need Permission, Type: " + type);
    }

    @Override
    public void onQRCodeListStateChanged(int state) {
        Log.i("MobilePass", "Main - QR Code List Changed, State: " + state);
    }
}
