package me.shoutto.sdk.internal.location.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.shoutto.sdk.GeofenceTransitionsIntentService;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;

/**
 * GeofenceManager handles all geofence Google Play API and Database activities.
 */
public class GeofenceManager {

    public final static int MAX_GEOFENCES = 100;

    private final static String TAG = GeofenceManager.class.getName();
    private final static int SUCCESS = 0;
    private final static int FAILURE = 1;

    private Context context;
    private GeofenceDbHelper geofenceDbHelper;
    private int maxGeofences = MAX_GEOFENCES;

    public GeofenceManager(Context context, GeofenceDbHelper geofenceDbHelper,
                           StmPreferenceManager stmPreferenceManager) {
        this.context = context;
        this.geofenceDbHelper = geofenceDbHelper;
        int maxGeofencesPreference = stmPreferenceManager.getMaxGeofences();
        if (maxGeofencesPreference > 0) {
            maxGeofences = maxGeofencesPreference;
        }
    }

    void setMaxGeofences(int maxGeofences) {
        this.maxGeofences = maxGeofences;
    }

    public List<String> getGeofenceIds() {
        List<String> geofenceIds = new ArrayList<>();

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        Cursor cursor = readableDatabase.query(
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                new String[]{GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID},
                null,
                null,
                null,
                null,
                null
        );
        if (cursor.moveToFirst()) {
            do {
                int columnIndex = cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID);
                if (!cursor.isNull(columnIndex)) {
                    geofenceIds.add(cursor.getString(columnIndex));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        readableDatabase.close();

        return geofenceIds;
    }

    public synchronized void addGeofence(MessageGeofence messageGeofence, Location userLocation) throws SecurityException {

        int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (finePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Cannot add Geofence. ACCESS_FINE_LOCATION not granted.");
        }

        persistGeofenceToDatabase(messageGeofence);

        if (getNumberOfGeofencesPersisted() > maxGeofences) {
            rebuildGeofencesInPlayServices(userLocation);
        } else {
            List<MessageGeofence> messageGeofences = new ArrayList<>();
            messageGeofences.add(messageGeofence);
            addGeofencesToPlayServices(messageGeofences);
        }
    }

    private void persistGeofenceToDatabase(MessageGeofence geofence) {

        if (geofenceRecordExists(geofence)) {
            updateGeofenceRecord(geofence);
        } else {
            createGeofenceRecord(geofence);
        }
    }

    private boolean geofenceRecordExists(MessageGeofence geofence) {

        boolean recordExists = false;

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        Cursor cursor = readableDatabase.query(
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                new String[] { GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID },
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID + " = ?",
                new String[] { geofence.getConversationId() },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            String conversationId = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID));
            recordExists = geofence.getConversationId().equals(conversationId);
            cursor.close();
            readableDatabase.close();
        }

        return  recordExists;
    }

    private void createGeofenceRecord(MessageGeofence geofence) {
        ContentValues geofenceValues = new ContentValues();
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID, geofence.getConversationId());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT, geofence.getLatitude());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON, geofence.getLongitude());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS, geofence.getRadius());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID, geofence.getChannelId());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL, geofence.getChannelImageUrl());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY, geofence.getMessageBody());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE, geofence.getTitle());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE, geofence.getType());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_EXPIRATION_DATE, geofence.getExpirationDate().getTime());

        SQLiteDatabase writableDatabase = geofenceDbHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            long pk = writableDatabase.insert(GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                    null, geofenceValues);
            if (pk == -1) {
                Log.w(TAG, "Record already exists for conversation " + geofence.getConversationId() + ". No updates allowed at this time.");
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "Could not insert new geofence.", ex);
            writableDatabase.endTransaction();
        } finally {
            writableDatabase.close();
        }
    }

    private void updateGeofenceRecord(MessageGeofence geofence) {
        ContentValues geofenceValues = new ContentValues();
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT, geofence.getLatitude());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON, geofence.getLongitude());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS, geofence.getRadius());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY, geofence.getMessageBody());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE, geofence.getTitle());
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_EXPIRATION_DATE, geofence.getExpirationDate().getTime());

        SQLiteDatabase writableDatabase = geofenceDbHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            writableDatabase.update(
                    GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                    geofenceValues,
                    GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID + " = ?",
                    new String[] { geofence.getConversationId() }
            );
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "Could not insert new geofence.", ex);
            writableDatabase.endTransaction();
        } finally {
            writableDatabase.close();
        }
    }

    private long getNumberOfGeofencesPersisted() {

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        long numberOfGeofences = DatabaseUtils.queryNumEntries(readableDatabase,
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME);
        readableDatabase.close();

        return numberOfGeofences;
    }

    public synchronized void rebuildGeofencesInPlayServices(Location userLocation) {

        Log.d(TAG, "Rebuilding geofences");

        MessageGeofence[] messageGeofences = getAllGeofenceRecords();
        Arrays.sort(messageGeofences, new GeofenceDistanceComparator(userLocation));

        List<MessageGeofence> messageGeofencesToAdd = new ArrayList<>();
        List<String> geofenceIdsToRemove = new ArrayList<>();
        for (int i = 0; i < messageGeofences.length; i++) {
            if (i < maxGeofences) {
                messageGeofencesToAdd.add(messageGeofences[i]);
            } else {
                geofenceIdsToRemove.add(messageGeofences[i].getId());
            }
        }

        removeGeofencesFromPlayServices(geofenceIdsToRemove);
        addGeofencesToPlayServices(messageGeofencesToAdd);
    }

    private MessageGeofence[] getAllGeofenceRecords() {

        List<MessageGeofence> messageGeofences = new ArrayList<>();

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        String[] projection = {
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS
        };

        Cursor cursor = readableDatabase.query(
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                MessageGeofence messageGeofence = new MessageGeofence();
                messageGeofence.setConversationId(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID)));
                messageGeofence.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT)));
                messageGeofence.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON)));
                messageGeofence.setRadius(cursor.getFloat(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS)));
                messageGeofence.setId(messageGeofence.getConversationId());
                messageGeofences.add(messageGeofence);
            } while (cursor.moveToNext());
        }

        cursor.close();
        readableDatabase.close();

        return messageGeofences.toArray(new MessageGeofence[messageGeofences.size()]);
    }

    private int removeGeofencesFromPlayServices(List<String> geofenceIdsToRemove) {

        int result = SUCCESS;

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);
        if (connectionResult.isSuccess()) {
            if (geofenceIdsToRemove.size() > 0) {
                Status status = LocationServices.GeofencingApi.removeGeofences(
                        googleApiClient, geofenceIdsToRemove).await(5, TimeUnit.SECONDS);
                if (status.isSuccess()) {
                    Log.d(TAG, "Deleted geofences from GeofencingApi");
                } else {
                    Log.e(TAG, "Removing geofence failed: " + status.getStatusMessage());
                    result = FAILURE;
                }
            }
        } else {
            Log.w(TAG, "Connection to GoogleApiClient failed. Cannot remove geofence.");
            result = FAILURE;
        }

        return result;
    }

    private void addGeofencesToPlayServices(List<MessageGeofence> messageGeofences) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);
        if (connectionResult.isSuccess()) {
            int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (finePermissionCheck == PackageManager.PERMISSION_GRANTED) {

                for (MessageGeofence messageGeofence : messageGeofences) {
                    // 1. Create an IntentService PendingIntent
                    Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
                    PendingIntent pendingIntent =
                            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // 2. Associate the service PendingIntent with the geofence and call addGeofences
                    Status status = LocationServices.GeofencingApi.addGeofences(googleApiClient,
                            getAddGeofencingRequest(messageGeofence.buildGeofence()), pendingIntent)
                            .await(5, TimeUnit.SECONDS);

                    if (!status.isSuccess()) {
                        Log.e(TAG, "Registering geofence failed: " + status.getStatusMessage() + " : "
                                + status.getStatusCode());
                    }
                }
            }
        } else {
            Log.w(TAG, "Connection to GoogleApiClient failed. Cannot add geofence.");
        }
    }

    private GeofencingRequest getAddGeofencingRequest(Geofence geofence) {
        List<Geofence> geofencesToAdd = new ArrayList<>();
        geofencesToAdd.add(geofence);
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofencesToAdd);
        return builder.build();
    }

    public void removeGeofencesByIds(List<String> geofenceIdsToRemove, Location userLocation) throws SecurityException {

        int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (finePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Cannot remove Geofences. ACCESS_FINE_LOCATION not granted.");
        }

        int removeResult = removeGeofencesFromPlayServices(geofenceIdsToRemove);
        if (removeResult == 0) {
            deleteGeofencesFromDatabase(geofenceIdsToRemove);
            if (userLocation != null && getNumberOfGeofencesPersisted() >= maxGeofences) {
                rebuildGeofencesInPlayServices(userLocation);
            }
        }
    }

    private void deleteGeofencesFromDatabase(List<String> messageGeofenceIdsToRemove) {

        SQLiteDatabase writableDatabase = geofenceDbHelper.getWritableDatabase();
        writableDatabase.beginTransaction();

        try {
            for (String conversationId : messageGeofenceIdsToRemove) {
                String selection = GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID + " LIKE ?";
                String[] selectionArgs = { conversationId };
                writableDatabase.delete(GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME, selection, selectionArgs);
                Log.d(TAG, "Deleted geofence from database: " + conversationId);
            }
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "An error occurred deleting a geofence.", ex);
            writableDatabase.endTransaction();
        } finally {
            writableDatabase.close();
        }
    }

    public void removeAllGeofences() {

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        String[] projection = { GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID };

        Cursor cursor = readableDatabase.query(
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        List<String> geofencesToRemove = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                geofencesToRemove.add(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID)));
            } while(cursor.moveToNext());
            cursor.close();
            readableDatabase.close();

            removeGeofencesByIds(geofencesToRemove, null);
        }
    }
}
