package com.armongate.mobilepasssdk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.armongate.mobilepasssdk.util.QRCodeValidator;

public class GoogleQRCodeReaderActivity extends AppCompatActivity implements QRCodeListStateDelegate {
    private final AtomicBoolean isQRFound = new AtomicBoolean(false);
    private String validQRCode = null;
    private final Object validQRCodeLock = new Object();

    private PreviewView previewView = null;
    private ProcessCameraProvider cameraProvider = null;
    private CameraSelector cameraSelector = null;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase = null;
    private ImageAnalysis analysisUseCase = null;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    final Handler handler = new Handler();

    private final Map<String, Long> foundQRCodes = new HashMap<>();

    private static long SCAN_INTERVAL_MS = 300; // Scan every 500ms
    private static long DEBOUNCE_DELAY_MS = 2500; // Same QR code delay
    private long lastScanTime = 0;

    private static final int REQUEST_CAMERA_PERMISSION = 1923;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DelegateManager.getInstance().onCancelled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_STARTED);
        LogManager.getInstance().info("Scan QR Code started for GMS");

        isQRFound.set(false);
        setValidQRCode(null);

        setContentView(R.layout.activity_armon_gms_qrcode_reader);
        previewView = findViewById(R.id.armon_mp_gms_preview_view);

        if (previewView == null) {
            LogManager.getInstance().error("QRCodeReader | Preview view is null", LogCodes.PASSFLOW_QRCODE_READER_PREVIEW_VIEW_NULL, this);
            return;
        }

        // Add device-specific configuration before setting up camera
        setupDeviceSpecificConfig();
        
        setupAccessibility();
        setupUI();
        setupCamera();
        scheduleQRCodeCleanup();

        DelegateManager.getInstance().setCurrentQRCodeListStateDelegate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Only pause camera, don't cleanup resources yet
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                LogManager.getInstance().error("Failed to unbind camera: " + e.getMessage(), 
                    LogCodes.PASSFLOW_QRCODE_READER_CLEANUP_FAILED, this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No cleanup needed here
    }

    @Override
    protected void onDestroy() {
        cleanupResources();
        
        if (validQRCode != null) {
            DelegateManager.getInstance().flowQRCodeFound(validQRCode);
        }
        
        super.onDestroy();
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

    private void setValidQRCode(String qrCode) {
        synchronized (validQRCodeLock) {
            validQRCode = qrCode;
        }
    }

    private String getValidQRCode() {
        synchronized (validQRCodeLock) {
            return validQRCode;
        }
    }

    private void setupCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().error("Camera permission not granted", 
                LogCodes.PASSFLOW_QRCODE_READER_PERMISSION_DENIED, this);
            showPermissionError();
            requestCameraPermission();
            return;
        }

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null) {
                    throw new IllegalStateException("Camera provider is null");
                }
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException | IllegalStateException e) {
                LogManager.getInstance().error("Camera setup failed: " + e.getMessage(), 
                    LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
                showCameraError();
                handleCameraSetupError(e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.CAMERA},
            REQUEST_CAMERA_PERMISSION);
    }

    private void handleCameraSetupError(Exception e) {
        PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, e.getLocalizedMessage());
        if (e instanceof SecurityException) {
            showPermissionError();
        } else {
            showCameraError();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                showPermissionError();
            }
        }
    }

    private void showPermissionError() {
        runOnUiThread(() -> {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
            if (txtListStateMessage != null) {
                txtListStateMessage.setText(R.string.error_camera_permission);
            }
        });
    }

    private void showCameraError() {
        runOnUiThread(() -> {
            TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
            if (txtListStateMessage != null) {
                txtListStateMessage.setText(R.string.error_camera_setup);
            }
        });
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider == null) {
            LogManager.getInstance().error("Camera provider is null", 
                LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
            return;
        }

        try {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        } catch (Exception e) {
            LogManager.getInstance().error("Failed to bind camera use cases: " + e.getMessage(), 
                LogCodes.PASSFLOW_QRCODE_READER_SETUP_EXCEPTION, this);
            showCameraError();
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

            previewUseCase = new Preview.Builder().build();
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

            ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build();

            ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
            builder.setResolutionSelector(resolutionSelector);
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

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
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime < SCAN_INTERVAL_MS || isProcessing.get()) {
                image.close();
                return;
            }
            
            if (!isProcessing.compareAndSet(false, true)) {
                image.close();
                return;
            }
            
            lastScanTime = currentTime;

            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );

            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();

            BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        try {
                            onSuccessListener(barcodes);
                        } finally {
                            isProcessing.set(false);
                            image.close();
                        }
                    })
                    .addOnFailureListener(e -> {
                        try {
                            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, e.getLocalizedMessage());
                            LogManager.getInstance().error("QRCodeReader | Barcode process failure > " + e.getLocalizedMessage(), 
                                LogCodes.PASSFLOW_QRCODE_READER_PROCESS_EXCEPTION, this);
                        } finally {
                            isProcessing.set(false);
                            image.close();
                        }
                    });
        } catch (Exception ex) {
            try {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
                LogManager.getInstance().error("QRCodeReader | Barcode analyze failure > " + ex.getLocalizedMessage(), 
                    LogCodes.PASSFLOW_QRCODE_READER_ANALYZE_EXCEPTION, this);
            } finally {
                isProcessing.set(false);
                image.close();
            }
        }
    }

    private void onSuccessListener(List<Barcode> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "No barcodes detected");
            LogManager.getInstance().error("No barcodes detected", LogCodes.PASSFLOW_QRCODE_READER_EMPTY_RESULT, this);
            return;
        }

        Barcode barcodeObj = barcodes.get(0);
        if (barcodeObj == null) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Received QR Code result object is empty");
            LogManager.getInstance().error("Received barcode result object is empty", LogCodes.PASSFLOW_QRCODE_READER_EMPTY_RESULT, this);
            return;
        }

        String qrcodeContent = barcodeObj.getRawValue();
        QRCodeValidator.ValidationResult validationResult = QRCodeValidator.validate(qrcodeContent);

        if (validationResult.isEmpty) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, "Received QR Code raw value is empty");
            LogManager.getInstance().error("Barcode raw value is empty", LogCodes.PASSFLOW_QRCODE_READER_EMPTY_RESULT, this);
            return;
        }

        // Check for duplicate QR codes
        Long foundTime = foundQRCodes.get(qrcodeContent);
        if (foundTime != null && System.currentTimeMillis() - foundTime < DEBOUNCE_DELAY_MS) {
            return;
        }

        foundQRCodes.put(qrcodeContent, System.currentTimeMillis());

        if (validationResult.isValid) {
            processValidQRCode(validationResult.parsedContent, qrcodeContent);
        } else {
            processInvalidQRCode(qrcodeContent, false, true);
        }
    }

    private void processValidQRCode(String parsedContent, String qrcodeContent) {
        QRCodeContent activeQRCodeContent = ConfigurationManager.getInstance().getQRCodeContent(parsedContent);

        if (activeQRCodeContent == null) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_NO_MATCH, qrcodeContent);
            LogManager.getInstance().warn("QR code reader could not find matching content for " + parsedContent, LogCodes.PASSFLOW_QRCODE_READER_NO_MATCHING);
            processInvalidQRCode(parsedContent, false, false);
            return;
        }

        if (activeQRCodeContent.valid) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_FOUND, qrcodeContent);
            PassFlowManager.getInstance().setQRData(
                activeQRCodeContent.qrCode != null ? activeQRCodeContent.qrCode.i : null,
                activeQRCodeContent.clubInfo != null ? activeQRCodeContent.clubInfo.i : null
            );

            LogManager.getInstance().info("QR code reader found content: " + qrcodeContent);
            finish();
            setValidQRCode(parsedContent);
        } else {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_INVALID_CONTENT, qrcodeContent);
            LogManager.getInstance().warn("QR code reader found content for " + parsedContent + " but it is invalid", LogCodes.PASSFLOW_QRCODE_READER_INVALID_CONTENT);
            processInvalidQRCode(parsedContent, true, false);
        }
    }

    private void processInvalidQRCode(String code, boolean isInvalidContent, boolean isInvalidFormat) {
        runOnUiThread(() -> {
            try {
                if (ConfigurationManager.getInstance().closeWhenInvalidQRCode() && isInvalidFormat) {
                    finish();
                    DelegateManager.getInstance().flowCloseWithInvalidQRCode(code);
                    return;
                }

                TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
                TextView txtQRCodeContent = findViewById(R.id.armon_mp_gms_txtQRCodeContent);

                if (txtListStateMessage != null && txtQRCodeContent != null) {
                    updateInvalidStateUI(txtListStateMessage, txtQRCodeContent, isInvalidContent, isInvalidFormat);
                }
            } catch (Exception ex) {
                LogManager.getInstance().error("Set invalid state failed: " + ex.getMessage(), 
                    LogCodes.PASSFLOW_QRCODE_READER_CHANGE_MASK_FAILED, this);
            }
        });
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

    private void updateInvalidStateUI(TextView txtListStateMessage, TextView txtQRCodeContent, 
            boolean isInvalidContent, boolean isInvalidFormat) {
        if (isInvalidContent) {
            txtListStateMessage.setText(R.string.text_qrcode_invalid);
        } else if (isInvalidFormat) {
            txtListStateMessage.setText(R.string.text_qrcode_unknown);
        } else {
            txtListStateMessage.setText(R.string.text_qrcode_not_found);
        }

        String qrCodeContent = "[" + ConfigurationManager.getInstance().getQRCodesCount() + "] / " + 
            ConfigurationManager.getInstance().getMemberId();
        txtQRCodeContent.setText(qrCodeContent);
        txtQRCodeContent.setVisibility(View.VISIBLE);
        setMaskColor(isInvalidContent ? R.color.qrcode_mask_content_failure : R.color.qrcode_mask_invalid);

        handler.postDelayed(() -> {
            txtListStateMessage.setText(getQRCodeListStateMessage(
                DelegateManager.getInstance().getQRCodeListState()));
            txtQRCodeContent.setText("");
            txtQRCodeContent.setVisibility(View.GONE);
            setMaskColor(R.color.qrcode_mask);
        }, TimeUnit.SECONDS.toMillis(4));
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

    private void cleanupResources() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                LogManager.getInstance().error("Failed to unbind camera: " + e.getMessage(), 
                    LogCodes.PASSFLOW_QRCODE_READER_CLEANUP_FAILED, this);
            } finally {
                cameraProvider = null;
            }
        }
        
        previewUseCase = null;
        analysisUseCase = null;
        previewView = null;
    }

    private void scheduleQRCodeCleanup() {
        if (handler != null) {
            handler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    scheduleQRCodeCleanup();
                }
            }, DEBOUNCE_DELAY_MS);
        }
    }

    private void setupUI() {
        runOnUiThread(() -> {
            try {
                TextView txtQRCodeMessage = findViewById(R.id.armon_mp_gms_txtQRInfoMessage);
                if (txtQRCodeMessage != null && !ConfigurationManager.getInstance().getMessageQRCode().isEmpty()) {
                    txtQRCodeMessage.setText(ConfigurationManager.getInstance().getMessageQRCode());
                }

                TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
                if (txtListStateMessage != null) {
                    txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));
                }

                TextView txtListRefreshMessage = findViewById(R.id.armon_mp_gms_txtListRefreshInfo);
                if (txtListRefreshMessage != null) {
                    txtListRefreshMessage.setVisibility(DelegateManager.getInstance().isQRCodeListRefreshable() ? View.VISIBLE : View.GONE);
                }

                setupClickListeners();
            } catch (Exception ex) {
                LogManager.getInstance().error("Setup UI failed: " + ex.getMessage(), 
                    LogCodes.UI_SETUP_FAILED, this);
            }
        });
    }

    private void setupClickListeners() {
        TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
        TextView txtListRefreshMessage = findViewById(R.id.armon_mp_gms_txtListRefreshInfo);
        ImageView imgSwitchCamera = findViewById(R.id.armon_mp_gms_btnSwitchCamera);

        View.OnClickListener refreshClickListener = view -> {
            if (DelegateManager.getInstance().isQRCodeListRefreshable()) {
                ConfigurationManager.getInstance().refreshList();
            }
        };

        if (txtListStateMessage != null) {
            txtListStateMessage.setOnClickListener(refreshClickListener);
        }

        if (txtListRefreshMessage != null) {
            txtListRefreshMessage.setOnClickListener(refreshClickListener);
        }

        if (imgSwitchCamera != null) {
            imgSwitchCamera.setOnClickListener(view -> {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
                    setupCamera();
                } catch (Exception ex) {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_ERROR, ex.getLocalizedMessage());
                    LogManager.getInstance().error("Switch camera failed! Exception: " + ex.getLocalizedMessage(), LogCodes.UI_SWITCH_CAMERA_FAILED, getApplicationContext());
                    DelegateManager.getInstance().onErrorOccurred(ex);
                }
            });
        }
    }

    private void setupAccessibility() {
        try {
            ImageView imgSwitchCamera = findViewById(R.id.armon_mp_gms_btnSwitchCamera);
            if (imgSwitchCamera != null) {
                imgSwitchCamera.setContentDescription(getString(R.string.accessibility_switch_camera));
                imgSwitchCamera.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            TextView txtQRCodeMessage = findViewById(R.id.armon_mp_gms_txtQRInfoMessage);
            if (txtQRCodeMessage != null) {
                txtQRCodeMessage.setContentDescription(getString(R.string.accessibility_qr_code_message));
                txtQRCodeMessage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            TextView txtListStateMessage = findViewById(R.id.armon_mp_gms_txtListStateInfo);
            if (txtListStateMessage != null) {
                txtListStateMessage.setContentDescription(getString(R.string.accessibility_list_state));
                txtListStateMessage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            TextView txtListRefreshMessage = findViewById(R.id.armon_mp_gms_txtListRefreshInfo);
            if (txtListRefreshMessage != null) {
                txtListRefreshMessage.setContentDescription(getString(R.string.accessibility_refresh_list));
                txtListRefreshMessage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            PreviewView previewView = findViewById(R.id.armon_mp_gms_preview_view);
            if (previewView != null) {
                previewView.setContentDescription(getString(R.string.accessibility_camera_preview));
                previewView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        } catch (Exception ex) {
            LogManager.getInstance().error("Setup accessibility failed: " + ex.getMessage(), 
                LogCodes.UI_SETUP_ACCESSIBILITY_FAILED, this);
        }
    }

    private boolean isRealmeDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("realme");
    }

    private void setupDeviceSpecificConfig() {
        if (isRealmeDevice()) {
            // Adjust scan interval for Realme devices
            SCAN_INTERVAL_MS = 1000; // Increase interval for Realme
            DEBOUNCE_DELAY_MS = 3000; // Increase debounce time

            LogManager.getInstance().info("Realme device detected. Adjusted scan interval and debounce delay.");
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_STARTED, "Realme device detected. Adjusted scan interval and debounce delay.");
        }
    }

}
