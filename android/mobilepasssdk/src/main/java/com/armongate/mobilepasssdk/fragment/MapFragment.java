package com.armongate.mobilepasssdk.fragment;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Double mPointLatitude;
    private Double mPointLongitude;
    private Integer mPointRadius;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mPointLatitude = getArguments() != null && getArguments().containsKey("latitude") ? getArguments().getDouble("latitude") : null;
        mPointLongitude = getArguments() != null && getArguments().containsKey("longitude") ? getArguments().getDouble("longitude") : null;
        mPointRadius = getArguments() != null && getArguments().containsKey("radius") ? getArguments().getInt("radius") : null;

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_armon_map, container, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.armon_mp_fragment_google_maps);

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            assert supportMapFragment != null;
            supportMapFragment.getMapAsync(this);
        } catch (Exception ex) {
            LogManager.getInstance().error("Error occurred while initializing map for location validation, error: " + ex.getLocalizedMessage(), LogCodes.PASSFLOW_MAP_ERROR);
        }

        return view;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initMap();
    }

    @SuppressLint("MissingPermission")
    private void initMap() {
        if (mPointLatitude != null && mPointLongitude != null && mPointRadius != null) {
            LatLng accessPoint = new LatLng(mPointLatitude, mPointLongitude);
            mMap.addMarker(new MarkerOptions().position(accessPoint));
            mMap.addCircle(new CircleOptions().center(accessPoint).radius(mPointRadius).fillColor(0x20FF0000).strokeWidth(0));
        }

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        initLocationTracking();
        mMap.setMyLocationEnabled(true);
    }

    private void moveCamera(Location location) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15.0f));
    }

    @SuppressLint("MissingPermission")
    private void initLocationTracking() {
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    // Task completed successfully
                    processLocation(task.getResult());
                }

                LocationRequest locationRequest = new LocationRequest();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(3000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void processLocation(Location location) {
        if (location != null) {
            LogManager.getInstance().debug("User location changed; Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());

            if (location.isFromMockProvider() && !ConfigurationManager.getInstance().allowMockLocation()) {
                LogManager.getInstance().info("Mock location detected, so pass flow will be cancelled");
                DelegateManager.getInstance().onMockLocationDetected();
            } else {
                if (mPointLatitude != null && mPointLongitude != null && mPointRadius != null) {
                    Location dest = new Location(LocationManager.GPS_PROVIDER);
                    dest.setLatitude(mPointLatitude);
                    dest.setLongitude(mPointLongitude);

                    float distance = location.distanceTo(dest);

                    if (distance < mPointRadius) {
                        LogManager.getInstance().info("User is in validation area now, distance to center point: " + distance);
                        DelegateManager.getInstance().flowLocationValidated();
                    } else {
                        LogManager.getInstance().debug("User is " + distance + "m away from validation point");
                        moveCamera(location);
                    }
                }
            }
        }
    }

    private final LocationCallback locationCallback = new LocationCallback(){

        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            super.onLocationResult(locationResult);

            Location lastLocation = locationResult.getLastLocation();
            processLocation(lastLocation);
        }
    };
}
