package com.armongate.mobilepasssdk.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.delegate.QRCodeListStateDelegate;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeReaderFragment extends Fragment implements SurfaceHolder.Callback, Detector.Processor<Barcode>, QRCodeListStateDelegate {

    private CameraSource    cameraSource;
    private BarcodeDetector detector;
    private Context         mContext;
    private View            mCurrentView;
    private boolean         isQRFound = false;
    private boolean         needSetupControls = false;

    private Map<String, Long> foundQRCodes = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mCurrentView = inflater.inflate(R.layout.fragment_armon_qrcode_reader, container, false);

        if (ConfigurationManager.getInstance().getMessageQRCode().length() > 0) {
            TextView txtQRCodeMessage = mCurrentView.findViewById(R.id.armon_mp_txtQRInfoMessage);
            txtQRCodeMessage.setText(ConfigurationManager.getInstance().getMessageQRCode());
        }

        TextView txtListStateMessage = mCurrentView.findViewById(R.id.armon_mp_txtListStateInfo);
        txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));

        setupControls((SurfaceView)mCurrentView.findViewById(R.id.armon_mp_qrSurfaceView));

        DelegateManager.getInstance().setCurrentQRCodeListStateDelegate(this);

        return mCurrentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

        if (needSetupControls && mCurrentView != null) {
            setupControls((SurfaceView)mCurrentView.findViewById(R.id.armon_mp_qrSurfaceView));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        foundQRCodes.clear();
    }

    private void setupControls(SurfaceView cameraSV) {
        if (mContext != null) {
            detector = new BarcodeDetector.Builder(mContext).setBarcodeFormats(Barcode.QR_CODE).build();
            cameraSource = new CameraSource.Builder(mContext, detector).setAutoFocusEnabled(true).build();

            cameraSV.getHolder().addCallback(this);

            detector.setProcessor(this);
        } else {
            needSetupControls = true;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            cameraSource.start(surfaceHolder);
        } catch (Exception exception) {
            DelegateManager.getInstance().onErrorOccurred(exception);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop();
    }

    @Override
    public void release() { }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        if (detections != null && detections.getDetectedItems().size() > 0) {
            SparseArray<Barcode> qrCodes = detections.getDetectedItems();
            Barcode code = qrCodes.valueAt(0);

            Pattern sPattern = Pattern.compile("https://(app|sdk).armongate.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2]{1})?()?$");

            if (foundQRCodes.containsKey(code.displayValue) && new Date().getTime() - foundQRCodes.get(code.displayValue) < 2500) {
                return;
            }

            if (!isQRFound) {
                foundQRCodes.put(code.displayValue, new Date().getTime());

                Matcher matcher = sPattern.matcher(code.displayValue);
                if (matcher.matches()) {
                    isQRFound = true;

                    String parsedContent = matcher.group(2) + "/" + matcher.group(3);

                    if (matcher.group(2).equals("rq")) {
                        parsedContent += matcher.group(4);
                    }

                    if (ConfigurationManager.getInstance().getQRCodeContent(parsedContent) == null) {
                        isQRFound = false;
                        setInvalid();
                    } else  {
                        DelegateManager.getInstance().flowQRCodeFound(parsedContent);
                    }
                } else {
                    setInvalid();
                    LogManager.getInstance().warn("Unknown QR code format! > " + code.displayValue);
                }
            }
        }
    }

    @Override
    public void onStateChanged(int state) {
        if (mCurrentView != null) {
            TextView txtListStateMessage = mCurrentView.findViewById(R.id.armon_mp_txtListStateInfo);
            txtListStateMessage.setText(getQRCodeListStateMessage(DelegateManager.getInstance().getQRCodeListState()));
        }
    }

    private int getQRCodeListStateMessage(int state) {
        switch (state) {
            case QRCodeListState.INITIALIZING:
                return R.string.text_qrcode_list_state_initializing;
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

    private void setInvalid() {
        setMaskColor(R.color.qrcode_mask_invalid);

        mCurrentView.postDelayed(new Runnable() {
            public void run() {
                setMaskColor(R.color.qrcode_mask);
            }
        }, TimeUnit.SECONDS.toMillis(2));
    }

    private void setMaskColor(int colorId) {
        if (mCurrentView != null) {
            View maskLeft = mCurrentView.findViewById(R.id.armon_mp_qrMaskLeft);
            View maskRight = mCurrentView.findViewById(R.id.armon_mp_qrMaskRight);
            View maskTop = mCurrentView.findViewById(R.id.armon_mp_qrMaskTop);
            View maskBottom = mCurrentView.findViewById(R.id.armon_mp_qrMaskBottom);

            maskLeft.setBackgroundResource(colorId);
            maskRight.setBackgroundResource(colorId);
            maskTop.setBackgroundResource(colorId);
            maskBottom.setBackgroundResource(colorId);
        }
    }
}
