package com.restart.autotimesheet;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceService extends IntentService {

    private static final String TAG = ".GeofencingService";
    private static final int NOTIFICATION_ID = 1234;
    private Context mContext;

    public GeofenceService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getBaseContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        if (event.hasError()) {
            Log.e(TAG, "Service popped an error");
        } else {
            int transition = event.getGeofenceTransition();

            // If you have more than one geofence this is one way to keep track. Using requestId (MyGeofenceId)
            List<Geofence> geofences = event.getTriggeringGeofences();

            for (Geofence aGeofence : geofences) {
                String requestId = aGeofence.getRequestId();

                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    notification("You have entered area " + requestId + " !");
                } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    notification("You have exited area " + requestId + " !");
                    onDestroy();
                }
            }
        }
    }

    /**
     * Notification system that is used for this app. All we need to do is call this function
     * when we need to trigger a notification.
     */
    private void notification(String message) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setOngoing(false)
                        .setContentTitle("Auto Time Sheet")
                        .setContentText(message)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(icon)
                        .setSound(soundUri)
                        .setWhen(System.currentTimeMillis());

        Intent resultIntent = new Intent(mContext, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
