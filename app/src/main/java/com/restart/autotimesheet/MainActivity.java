package com.restart.autotimesheet;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = ".MainActivity";
    private static final String GEOFENCE_ID = "MyGeofenceId";
    private static final int PERMISSION_REQUEST = 1234;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startLocationMonitoring = (Button) findViewById(R.id.startLocationMonitoring);
        startLocationMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationMonitoring();
            }
        });

        Button startGeofenceMonitoring = (Button) findViewById(R.id.startGeofenceMonitoring);
        startGeofenceMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGeofenceMonitoring();
            }
        });

        Button stopGeofenceMonitoring = (Button) findViewById(R.id.stopGeofenceMonitoring);
        stopGeofenceMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopGeofenceMonitoring();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                }).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (response != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.reconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void startLocationMonitoring() {
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void startGeofenceMonitoring() {
        try {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(45.545184, -122.845018, 100)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(1000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            Intent intent = new Intent(this, GeofenceService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (!mGoogleApiClient.isConnected()) {

            } else {
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofencingRequest, pendingIntent)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Successfully added geofence");
                                } else {
                                    Log.d(TAG, "Failed to add geofence");
                                }
                            }
                        });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopGeofenceMonitoring() {
        ArrayList<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(GEOFENCE_ID);
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceIds);
    }
}
