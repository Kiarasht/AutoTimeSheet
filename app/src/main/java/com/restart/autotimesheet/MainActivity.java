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

    private GoogleApiClient mGoogleApiClient;

    /**
     * Set up the basic button layouts, set up the OnClickListeners, and GoogleApiClient with the
     * location permissions it requires to function.
     * <p>
     * OnCreate is the root of an activity life cycle.
     * http://www.javatpoint.com/images/androidimages/Android-Activity-Lifecycle.png
     *
     * @param savedInstanceState N/A
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        // Initialize all the three buttons
        Button startLocationMonitoring = (Button) findViewById(R.id.startLocationMonitoring);
        Button startGeofenceMonitoring = (Button) findViewById(R.id.startGeofenceMonitoring);
        Button stopGeofenceMonitoring = (Button) findViewById(R.id.stopGeofenceMonitoring);

        // OnClickListener for starting a location listener
        startLocationMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationMonitoring();
            }
        });

        // OnClickListener for starting the geofence functionality
        startGeofenceMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGeofenceMonitoring();
            }
        });

        // OnClickListener for stopping the geofence functionality
        stopGeofenceMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopGeofenceMonitoring();
            }
        });

        // Get GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d(TAG, "GoogleApiClient successfully connected! Yay!");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.e(TAG, "GoogleApiClient failed to connect! Boo!");
                    }
                }).build();

        // Get location permissions if on Marshmallow 6.0 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1234);
        }
    }

    /**
     * Check for Google Api availability every time the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        /**
         * Require the user to install Google services otherwise the app won't work.
         * http://www.abcdiamond.com/images/Get-Google-Play-services-300x153.jpg
         */
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (response != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();
        }
    }

    /**
     * Reconnect GoogleApiClient when activity starts
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        mGoogleApiClient.reconnect();
    }

    /**
     * Stop GoogleApiClient when activity stops
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        mGoogleApiClient.disconnect();
    }

    /**
     * Start asking for location updates using the GoogleApiClient.
     */
    private void startLocationMonitoring() {
        try {
            // Setup a location request
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // Start listening for location updates using the GoogleApiClient, previous location request criteria, and a listener
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                }
            });
        } catch (SecurityException e) { // Throw a SecurityException if permissions were not allowed
            Log.wtf(TAG, "Shouldn't happen, we have the location permissions. Wait you hit deny didn't you?");
            e.printStackTrace();
        }
    }

    /**
     * Start the Geofence process.
     */
    private void startGeofenceMonitoring() {
        try {
            // Set up a geofence criteria by giving it an id, location + radius, responsiveness, transitions, etc...
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(45.545184, -122.845018, 100)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(1000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            // Start the geofence process using our previous criteria
            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            // Initialize a service
            Intent intent = new Intent(this, GeofenceService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Now start geofence when all is well
            if (!mGoogleApiClient.isConnected()) {
                Log.e(TAG, "GoogleApiClient isn't connect!");
            } else {
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofencingRequest, pendingIntent)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Successfully started Geofence monitoring!");
                                } else {
                                    Log.d(TAG, "Failed to start the Geofence monitoring!");
                                }
                            }
                        });
            }
        } catch (SecurityException e) { // It can fail if user denies the location permission on >= 6.0
            Log.wtf(TAG, "Shouldn't happen, we have the location permissions. Wait you hit deny didn't you?");
            e.printStackTrace();
        }
    }

    /**
     * Removes any on going geofences using the tags given when set up.
     */
    private void stopGeofenceMonitoring() {
        ArrayList<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(GEOFENCE_ID);
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceIds);
    }
}
