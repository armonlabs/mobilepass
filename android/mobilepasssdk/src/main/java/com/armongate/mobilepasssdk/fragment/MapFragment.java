package com.armongate.mobilepasssdk.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.SettingsManager;
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

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Double mPointLatitude;
    private Double mPointLongitude;
    private int mPointRadius;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPointLatitude = getArguments().getDouble("latitude");
        mPointLongitude = getArguments().getDouble("longitude");
        mPointRadius = getArguments().getInt("radius");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        supportMapFragment.getMapAsync(this);

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

    private void initMap() {
        if (!SettingsManager.getInstance().checkLocationPermission(getContext())) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        LatLng accessPoint = new LatLng(mPointLatitude, mPointLongitude);
        mMap.addMarker(new MarkerOptions().position(accessPoint));
        mMap.addCircle(new CircleOptions().center(accessPoint).radius(mPointRadius).fillColor(0x20FF0000).strokeWidth(0));

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        initLocationTracking();
    }

    private void moveCamera(Location location) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f));
    }

    @SuppressLint("MissingPermission")
    private void initLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    LogManager.getInstance().debug("Location changed; Latitude: " + lastLocation.getLatitude() + ", Longitude: " + lastLocation.getLongitude());

                    Location dest = new Location(LocationManager.GPS_PROVIDER);
                    dest.setLatitude(mPointLatitude);
                    dest.setLongitude(mPointLongitude);

                    float distance = lastLocation.distanceTo(dest);

                    if (distance < mPointRadius) {
                        DelegateManager.getInstance().flowLocationValidated();
                    } else {
                        LogManager.getInstance().debug("Distance to point: " + distance);
                        moveCamera(lastLocation);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(new LocationRequest(), locationCallback, null);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) {
            initLocationTracking();
        }
    }

}
