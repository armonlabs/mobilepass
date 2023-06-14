package com.armongate.mobilepasssdk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleQRCodeReaderActivity extends AppCompatActivity implements QRCodeListStateDelegate {
    private boolean isQRFound = false;
    private String validQRCode = null;

    private PreviewView previewView = null;
    private ProcessCameraProvider cameraProvider = null;
    private CameraSelector cameraSelector = null;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase = null;
    private ImageAnalysis analysisUseCase = null;

    final Handler handler = new Handler();

    private final Map<String, Long> foundQRCodes = new HashMap<>();

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DelegateManager.getInstance().onCancelled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_STARTED);

        isQRFound = false;
        validQRCode = null;

        setContentView(R.layout.activity_armon_gms_qrcode_reader);
        previewView = findViewById(R.id.armon_mp_gms_preview_view);

        setupCamera();

        DelegateManager.getInstance().setCurrentQRCodeListStateDelegate(this);

        if (ConfigurationManager.getInstance().getMessageQRCode().length() > 0) {
            TextView txtQRCodeMessage = findViewById(R.id.armon_mp_gms_txtQRInfoMessage);
            txtQRCodeMessage.setText(ConfigurationManager.getInstance().getMessageQRCode());
        }

        TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
        txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));

        TextView txtListRefreshMessage = findViewById(R.id.armon_mp_gms_txtListRefreshInfo);
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

        ImageView imgSwitchCamera = findViewById(R.id.armon_mp_gms_btnSwitchCamera);

        imgSwitchCamera.setOnClickListener(view -> {
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
                setupCamera();
            } catch (Exception ex){
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
                LogManager.getInstance().error("Switch camera failed! Exception: " + ex.getLocalizedMessage(), LogCodes.UI_SWITCH_CAMERA_FAILED, getApplicationContext());
                DelegateManager.getInstance().onErrorOccurred(ex);
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (validQRCode != null) {
            DelegateManager.getInstance().flowQRCodeFound(validQRCode);
        }
    }

    @Override
    public void onStateChanged(int state) {
        if (!this.isFinishing() && !this.isDestroyed()) {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
            TextView txtListRefreshMessage = findViewById(R.id.armon_mp_gms_txtListRefreshInfo);

            if (txtListRefreshMessage != null && txtListStateMessage != null) {
                txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));
                txtListRefreshMessage.setVisibility(DelegateManager.getInstance().isQRCodeListRefreshable() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, e.getLocalizedMessage());
                LogManager.getInstance().error("QRCodeReader | Add listener to camera provider failed! > " + e.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        try {
            if (cameraProvider == null) {
                return;
            }

            if (previewUseCase != null) {
                cameraProvider.unbind(previewUseCase);
            }

            Preview.Builder builder = new Preview.Builder();
            builder.setTargetRotation(Surface.ROTATION_0);

            previewUseCase = builder.build();
            previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

            cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception ex) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
            LogManager.getInstance().error("QRCodeReader | Error while bind preview use case > " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
        }
    }

    private void bindAnalysisUseCase() {
        try {
            if (cameraProvider == null) {
                return;
            }

            if (analysisUseCase != null) {
                cameraProvider.unbind(analysisUseCase);
            }

            Executor cameraExecutor = Executors.newSingleThreadExecutor();

            ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
            builder.setTargetRotation(Surface.ROTATION_0);

            analysisUseCase = builder.build();
            analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception ex) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
            LogManager.getInstance().error("QRCodeReader | Error while bind analysis use case > " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        try {
            if (image.getImage() == null) return;

            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(this::onSuccessListener)
                    .addOnFailureListener(e -> {
                            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, e.getLocalizedMessage());
                            LogManager.getInstance().error("QRCodeReader | Barcode process failure > " + e.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_PROCESS_EXCEPTION, this);
                    })
                    .addOnCompleteListener(task -> image.close());
        } catch (Exception ex) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
            LogManager.getInstance().error("QRCodeReader | Barcode analyze failure > " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_ANALYZE_EXCEPTION, this);
        }
    }

    private void onSuccessListener(List<Barcode> barcodes) {
        //Check the result
        if (barcodes != null && barcodes.size() > 0) {
            Barcode barcodeObj = barcodes.get(0);

            if (barcodeObj != null) {
                String qrcodeContent = barcodeObj.getDisplayValue();

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

                                    GoogleQRCodeReaderActivity.this.finish();
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
            GoogleQRCodeReaderActivity.this.finish();
            DelegateManager.getInstance().flowCloseWithInvalidQRCode(code);
        } else {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
            TextView txtQRCodeContent = findViewById(R.id.armon_mp_gms_txtQRCodeContent);

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
            View maskLeft = findViewById(R.id.armon_mp_gms_qrMaskLeft);
            View maskRight = findViewById(R.id.armon_mp_gms_qrMaskRight);
            View maskTop = findViewById(R.id.armon_mp_gms_qrMaskTop);
            View maskBottom = findViewById(R.id.armon_mp_gms_qrMaskBottom);

            if (maskBottom != null && maskLeft != null && maskRight != null && maskTop != null) {
                maskLeft.setBackgroundResource(colorId);
                maskRight.setBackgroundResource(colorId);
                maskTop.setBackgroundResource(colorId);
                maskBottom.setBackgroundResource(colorId);
            }
        } catch (Exception ex) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Set mask color failed! Error: " + ex.getLocalizedMessage());
            LogManager.getInstance().error("Set mask color for invalid qr code has been failed!, error: " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_QRCODE_READER_CHANGE_MASK_FAILED, this);
        }
    }

}
