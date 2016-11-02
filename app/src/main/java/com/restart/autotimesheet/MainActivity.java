package com.restart.autotimesheet;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = ".MainActivity";
    private static final int REQUEST_PERMISSION_LOCATION = 1234;

    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    /**
     * Set up the basic button layouts, set up the OnClickListeners, and GoogleApiClient with the
     * location permissions it requires to function.
     *
     * @param savedInstanceState N/A
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        Button button = (Button) findViewById(R.id.startLocationMonitoring);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, GeofenceActivity.class));
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
    }

    /**
     * Check for Google Api availability every time the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Require the user to install Google services otherwise the app won't work.
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
        mGoogleApiClient.reconnect();
    }

    /**
     * Stop GoogleApiClient when activity stops
     */
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Start the Geofence process.
     */
    private void startGeofenceMonitoring() {
        try {
            // Set up a geofence criteria by giving it an id, location + radius, responsiveness, transitions, etc...
            Geofence geofence = new Geofence.Builder()
                    .setRequestId("Job")
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
                throw new SecurityException("Unable to connect to servers");
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
        } catch (SecurityException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Removes any on going geofences using the tags given when set up.
     */
    private void stopGeofenceMonitoring() {
        ArrayList<String> geofenceIds = new ArrayList<>();
        //geofenceIds.add(GEOFENCE_Job);
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceIds);
    }

    public void newGeofence(View view) {
        String[] locationPermission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        if (isPermissionGranted(locationPermission)) {
            startActivity(new Intent(mContext, GeofenceActivity.class));
        } else {
            getPermission(locationPermission);
        }
    }

    /**
     * Check to see if user has given us the permission(s) to access something. Location, camera, etc...
     *
     * @param permissions A string array of permission(s) that is being checked
     * @return Returns true if permission(s) are granted, and false if denied
     */
    private boolean isPermissionGranted(String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean allGiven = true;
            for (String aPermission : permissions) {
                if (mContext.checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                    allGiven = false;
                    break;
                }
            }
            return allGiven;
        } else {
            return true;
        }
    }

    /**
     * Gets called when we need to get permissions from the user
     *
     * @param permission The permission(s) we need from user.
     */
    private void getPermission(String[] permission) {
        ActivityCompat.requestPermissions(MainActivity.this, permission, REQUEST_PERMISSION_LOCATION);
    }

    /**
     * Switch case for each of the permission cases for managing each of the requests and their
     * results separately.
     *
     * @param requestCode  Used for calling the correct switch case
     * @param permissions  Array of permissions that were requested
     * @param grantResults Result from the permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(mContext, GeofenceActivity.class));
                } else {
                    Toast.makeText(mContext, "Need location permission to track a time sheet", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
