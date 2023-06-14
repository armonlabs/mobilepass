package com.armongate.mobilepasssdk.activity;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.delegate.QRCodeListStateDelegate;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.PassFlowManager;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.ml.scan.HmsScan;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuaweiQRCodeReaderActivity extends Activity implements QRCodeListStateDelegate {
    private RemoteView remoteView;
    private boolean isQRFound = false;
    private String validQRCode = null;

    final Handler handler = new Handler();

    private final Map<String, Long> foundQRCodes = new HashMap<>();

    int mScreenWidth;
    int mScreenHeight;

    final int SCAN_FRAME_SIZE = 240;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        HuaweiQRCodeReaderActivity.this.finish();
        DelegateManager.getInstance().onCancelled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isQRFound = false;
        validQRCode = null;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_armon_hms_qrcode_reader);

        DelegateManager.getInstance().setCurrentQRCodeListStateDelegate(this);

        // Bind the camera preview screen.
        FrameLayout frameLayout = findViewById(R.id.armon_mp_hms_rim);

        //1. Obtain the screen density to calculate the viewfinder's rectangle.
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        //2. Obtain the screen size.
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        int scanFrameSize = (int) (SCAN_FRAME_SIZE * density);

        //3. Calculate the viewfinder's rectangle, which in the middle of the layout.
        //Set the scanning area. (Optional. Rect can be null. If no settings are specified, it will be located in the middle of the layout.)
        Rect rect = new Rect();
        rect.left = mScreenWidth / 2 - scanFrameSize / 2;
        rect.right = mScreenWidth / 2 + scanFrameSize / 2;
        rect.top = mScreenHeight / 2 - scanFrameSize / 2;
        rect.bottom = mScreenHeight / 2 + scanFrameSize / 2;

        //Initialize the RemoteView instance, and set callback for the scanning result.
        remoteView = new RemoteView.Builder().setContext(this).setBoundingBox(rect).setFormat(HmsScan.ALL_SCAN_TYPE).build();

        remoteView.setOnErrorCallback(i -> {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Huawei remote view error code: " + i);
            LogManager.getInstance().error("QRCodeReader | Barcode process failure > " + i, LogCodes.PASSFLOW_QRCODE_READER_PROCESS_EXCEPTION, this);
        });

        // Subscribe to the scanning result callback event.
        remoteView.setOnResultCallback(result -> {
            //Check the result.
            if (result != null && result.length > 0) {
                HmsScan barcodeObj = result[0];

                if (barcodeObj != null) {
                    String qrcodeContent = barcodeObj.getOriginalValue();

                    if (qrcodeContent != null && !TextUtils.isEmpty(qrcodeContent)) {
                        Long foundTime = foundQRCodes.containsKey(qrcodeContent) ? foundQRCodes.get(qrcodeContent) : null;

                        if (foundTime != null && new Date().getTime() - foundTime < 2500) {
                            return;
                        }

                        Pattern sPattern = Pattern.compile("https://(app|sdk).armongate.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2])?$");

                        if (!isQRFound) {
                            foundQRCodes.put(qrcodeContent, new Date().getTime());

                            Matcher matcher = sPattern.matcher(qrcodeContent);
                            if (matcher.matches()) {
                                isQRFound = true;

                                String prefix = matcher.group(2);
                                String uuid = matcher.group(3);
                                String direction = matcher.group(4);

                                String parsedContent = (prefix != null ? prefix : "") + "/" + (uuid != null ? uuid : "");

                                if (prefix != null && prefix.equals("rq")) {
                                    parsedContent += (direction != null ? direction : "");
                                }

                                QRCodeContent activeQRCodeContent = ConfigurationManager.getInstance().getQRCodeContent(parsedContent);

                                if (activeQRCodeContent == null) {
                                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_NO_MATCH, qrcodeContent);
                                    LogManager.getInstance().warn("QR code reader could not find matching content for " + parsedContent, LogCodes.PASSFLOW_QRCODE_READER_NO_MATCHING);

                                    isQRFound = false;
                                    setInvalid(parsedContent, false, false);
                                } else {
                                    if (activeQRCodeContent.valid) {
                                        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_FOUND, qrcodeContent);

                                        HuaweiQRCodeReaderActivity.this.finish();
                                        validQRCode = parsedContent;
                                    } else {
                                        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_INVALID_CONTENT, qrcodeContent);
                                        LogManager.getInstance().warn("QR code reader found content for " + parsedContent + " but it is invalid", LogCodes.PASSFLOW_QRCODE_READER_INVALID_CONTENT);

                                        isQRFound = false;
                                        setInvalid(parsedContent, true, false);
                                    }
                                }
                            } else {
                                PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_INVALID_FORMAT, qrcodeContent);
                                LogManager.getInstance().warn("QR code reader found unknown format > " + qrcodeContent, LogCodes.PASSFLOW_QRCODE_READER_INVALID_FORMAT);

                                isQRFound = false;
                                setInvalid(qrcodeContent, false, true);
                            }
                        }
                    } else {
                        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Received QR Code display value is empty");
                        LogManager.getInstance().error("Barcode display value is empty", LogCodes.PASSFLOW_QRCODE_READER_EMPTY_RESULT, this);
                    }
                } else {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Received QR Code result object is empty");
                    LogManager.getInstance().error("Received barcode result object is empty", LogCodes.PASSFLOW_QRCODE_READER_EMPTY_RESULT, this);
                }
            }
        });

        // Load the customized view to the activity.
        remoteView.onCreate(savedInstanceState);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        frameLayout.addView(remoteView, params);

        if (ConfigurationManager.getInstance().getMessageQRCode().length() > 0) {
            TextView txtQRCodeMessage = findViewById(R.id.armon_mp_hms_txtQRInfoMessage);
            txtQRCodeMessage.setText(ConfigurationManager.getInstance().getMessageQRCode());
        }

        TextView txtListStateMessage = findViewById(R.id.armon_mp_hms_txtListStateInfo);
        txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));

        TextView txtListRefreshMessage = findViewById(R.id.armon_mp_hms_txtListRefreshInfo);
        txtListRefreshMessage.setVisibility(DelegateManager.getInstance().isQRCodeListRefreshable() ? View.VISIBLE : View.GONE);

        txtListStateMessage.setOnClickListener(view -> {
            if (DelegateManager.getInstance().isQRCodeListRefreshable()) {
                ConfigurationManager.getInstance().refreshList();
            }
        });

        txtListRefreshMessage.setOnClickListener(view -> {
            if (DelegateManager.getInstance().isQRCodeListRefreshable()) {
                ConfigurationManager.getInstance().refreshList();
            }
        });
    }

    /**
     * Call the lifecycle management method of the remoteView activity.
     */
    @Override
    protected void onStart() {
        super.onStart();
        remoteView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        remoteView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        remoteView.onDestroy();

        if (validQRCode != null) {
            DelegateManager.getInstance().flowQRCodeFound(validQRCode);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        remoteView.onStop();
    }

    @Override
    public void onStateChanged(int state) {
        if (!this.isFinishing() && !this.isDestroyed()) {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_hms_txtListStateInfo);
            TextView txtListRefreshMessage = findViewById(R.id.armon_mp_hms_txtListRefreshInfo);

            if (txtListRefreshMessage != null && txtListStateMessage != null) {
                txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));
                txtListRefreshMessage.setVisibility(DelegateManager.getInstance().isQRCodeListRefreshable() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private int getQRCodeListStateMessage(int state) {
        switch (state) {
            case QRCodeListState.EMPTY:
                return R.string.text_qrcode_list_state_empty;
            case QRCodeListState.SYNCING:
                return R.string.text_qrcode_list_state_syncing;
            case QRCodeListState.USING_STORED_DATA:
                return R.string.text_qrcode_list_state_stored_data;
            case QRCodeListState.USING_SYNCED_DATA:
                return R.string.text_qrcode_list_state_synced_data;
            default:
                return R.string.text_qrcode_list_state_initializing;
        }
    }


    private void setInvalid(String code, boolean isInvalidContent, boolean isInvalidFormat) {
        if (ConfigurationManager.getInstance().closeWhenInvalidQRCode() && isInvalidFormat) {
            HuaweiQRCodeReaderActivity.this.finish();
            DelegateManager.getInstance().flowCloseWithInvalidQRCode(code);
        } else {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_hms_txtListStateInfo);
            TextView txtQRCodeContent = findViewById(R.id.armon_mp_hms_txtQRCodeContent);

            handler.post(() -> {
                if (isInvalidContent) {
                    txtListStateMessage.setText(R.string.text_qrcode_invalid);
                } else if (isInvalidFormat) {
                    txtListStateMessage.setText(R.string.text_qrcode_unknown);
                } else {
                    txtListStateMessage.setText(R.string.text_qrcode_not_found);
                }

                String qrCodeContent = code + " [" + ConfigurationManager.getInstance().getQRCodesCount() + "]";

                txtQRCodeContent.setText(qrCodeContent);
                txtQRCodeContent.setVisibility(View.VISIBLE);
                setMaskColor(isInvalidContent ? R.color.qrcode_mask_content_failure : R.color.qrcode_mask_invalid);
            });

            handler.postDelayed(() -> {
                txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));
                txtQRCodeContent.setText("");
                txtQRCodeContent.setVisibility(View.GONE);
                setMaskColor(R.color.qrcode_mask);
            }, TimeUnit.SECONDS.toMillis(4));
        }
    }

    private void setMaskColor(int colorId) {
        try {
            View maskLeft = findViewById(R.id.armon_mp_hms_qrMaskLeft);
            View maskRight = findViewById(R.id.armon_mp_hms_qrMaskRight);
            View maskTop = findViewById(R.id.armon_mp_hms_qrMaskTop);
            View maskBottom = findViewById(R.id.armon_mp_hms_qrMaskBottom);

            if (maskBottom != null && maskLeft != null && maskRight != null && maskTop != null) {
                maskLeft.setBackgroundResource(colorId);
                maskRight.setBackgroundResource(colorId);
                maskTop.setBackgroundResource(colorId);
                maskBottom.setBackgroundResource(colorId);
            }
        } catch (Exception ex) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Set mask color failed! Error: " +  ex.getLocalizedMessage());
            LogManager.getInstance().error("Set mask color for invalid qr code has been failed!, error: " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_CHANGE_MASK_FAILED);
        }
    }
}