package com.restart.autotimesheet;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceService extends IntentService {

    private static final String TAG = ".GeofencingService";

    public GeofenceService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        if (event.hasError()) {
            Log.e(TAG, "Service popped an error");
        } else {
            int transition = event.getGeofenceTransition();

            // If you have more than one geofence this is one way to keep track. Using requestId (MyGeofenceId)
/*            List<Geofence> geofences = event.getTriggeringGeofences();
            Geofence geofence = geofences.get(0);
            String requestId = geofence.getRequestId(); */

            // TODO How about we have a notification here instead of a Log?
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(TAG, "Entering geofence area");
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.d(TAG, "Exiting geofence area");
            }
        }
    }
}
