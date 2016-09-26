package me.shoutto.sdk.internal.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.sample.geofencing.GeofenceErrorMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.shoutto.sdk.MessageNotificationIntentWrapper;
import me.shoutto.sdk.R;
import me.shoutto.sdk.internal.NotificationManager;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.geofence.database.GeofenceDbHelper;

/**
 * An {@link IntentService} subclass for handling Geofence transitions.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = GeofenceTransitionsIntentService.class.getCanonicalName();
    private GeofenceDbHelper geofenceDbHelper;

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        geofenceDbHelper = new GeofenceDbHelper(getApplicationContext());

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Remove the geofences.
            // Get the Ids of each geofence that was triggered.
            List<String> triggeringGeofencesIdsList = new ArrayList<>();
            for (Geofence geofence : triggeringGeofences) {
                triggeringGeofencesIdsList.add(geofence.getRequestId());
            }

            List<Bundle> bundles = createBundles(triggeringGeofencesIdsList);

            GeofenceManager geofenceManager = new GeofenceManager(this, new GeofenceDbHelper(this));
            geofenceManager.removeGeofencesByIds(triggeringGeofencesIdsList);

            // Send notification.
            sendNotification(bundles);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type,
                    geofenceTransition));
        }
    }

    private List<Bundle> createBundles(List<String> conversationIds) {

        String inClause = " IN (" + TextUtils.join(",", Collections.nCopies(conversationIds.size(), "?")) + ")";

        String[] projection = {
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE
        };

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        Cursor cursor = readableDatabase.query(
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                projection,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID + inClause,
                conversationIds.toArray(new String[conversationIds.size()]),
                null,
                null,
                null
        );

        List<Bundle> bundles = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                try {
                    String conversationId = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID));
                    String channelId = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID));
                    String messageBody = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY));
                    String messageTitle = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE));
                    String messageType = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE));

                    Bundle bundle = new Bundle();
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_CONVERSATION_ID, conversationId);
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_CHANNEL_ID, channelId);
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_BODY, messageBody);
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TITLE, messageTitle);
                    bundle.putString(MessageNotificationIntentWrapper.EXTRA_NOTIFICATION_TYPE, messageType);

                    bundles.add(bundle);
                } catch (Exception ex) {
                    Log.e(TAG, "Error creating bundle for geofence notification.", ex);
                }
            } while(cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        readableDatabase.close();

        return bundles;
    }

    /**
     * Posts a notification when a transition is detected.
     */
    private void sendNotification(List<Bundle> bundles) {
        StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(this);
        for (Bundle bundle : bundles) {
            NotificationManager notificationManager = new NotificationManager(this, bundle);
            notificationManager.processIncomingNotification(stmPreferenceManager.getServerUrl(),
                    stmPreferenceManager.getAuthToken(), stmPreferenceManager.getUserId());
        }
    }
}
