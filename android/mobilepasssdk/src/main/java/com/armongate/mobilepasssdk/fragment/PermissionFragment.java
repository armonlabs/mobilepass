package com.armongate.mobilepasssdk.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;

public class PermissionFragment extends Fragment  {
    private static final String ARG_TYPE = "type";

    private int mParamType;

    public PermissionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            mParamType = getArguments().getInt(ARG_TYPE);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_armon_permission, container, false);

        int resMessageId = R.string.text_permission_camera;

        if (mParamType == NeedPermissionType.NEED_PERMISSION_LOCATION) {
            resMessageId = R.string.text_permission_location;
        } else if (mParamType == NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES) {
            resMessageId = R.string.text_permission_location_service;
        } else if (mParamType == NeedPermissionType.NEED_PERMISSION_BLUETOOTH) {
            resMessageId = R.string.text_permission_ble_scan;
        }

        TextView txtMessage = view.findViewById(R.id.armon_mp_txtMessage);
        txtMessage.setText(resMessageId);

        Button button = view.findViewById(R.id.armon_mp_btnSettings);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DelegateManager.getInstance().goToSettings();

                Intent intent = new Intent(mParamType == NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES ? Settings.ACTION_LOCATION_SOURCE_SETTINGS : Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                if (mParamType != NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES) {
                    try {
                        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                        intent.setData(uri);
                    } catch (Exception ex) {
                        LogManager.getInstance().error("Exception occurred while adding intent data for package name, error: " + ex.getLocalizedMessage(), LogCodes.NEED_PERMISSION_DEFAULT + mParamType);
                    }
                }
                startActivity(intent);
            }
        });

        return view;
    }
}