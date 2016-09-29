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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.shoutto.sdk.GeofenceTransitionsIntentService;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;

/**
 * GeofenceManager handles all geofence Google Play API and Database activities.
 */
public class GeofenceManager {

    private final static String TAG = GeofenceManager.class.getName();
    public final static int MAX_GEOFENCES = 100;
    private Context context;
    private GeofenceDbHelper geofenceDbHelper;
    private StmPreferenceManager stmPreferenceManager;
    private int maxGeofences = MAX_GEOFENCES;

    public GeofenceManager(Context context, GeofenceDbHelper geofenceDbHelper) {
        this.context = context;
        this.geofenceDbHelper = geofenceDbHelper;
        stmPreferenceManager = new StmPreferenceManager(context);
        int maxGeofencesPreference = stmPreferenceManager.getMaxGeofences();
        if (maxGeofencesPreference > 0) {
            maxGeofences = maxGeofencesPreference;
        }
    }

    public synchronized void addGeofence(MessageGeofence messageGeofence, Location userLocation) {

        // If number of saved geofences == the max
        long numberOfGeofences = getNumberOfGeofencesPersisted();
        Log.d(TAG, "Number of entries " + numberOfGeofences);
        Log.d(TAG, "Max geofences " + maxGeofences);
        if (numberOfGeofences >= maxGeofences) {
            // Then find the furthest one away (including the new one)
            long numberGeofencesToDelete = numberOfGeofences - maxGeofences + 1;
            if (numberGeofencesToDelete > 0) {
                List<String> furthestGeofences =
                        getFurthestGeofences(messageGeofence, userLocation);

                // Then delete the furthest geofences if they're in the DB
                List<String> geofencesToRemove = new ArrayList<>();
                for (String geofenceId : furthestGeofences) {
                    if (!geofenceId.equals(messageGeofence.getConversationId())) {
                        geofencesToRemove.add(geofenceId);
                    }
                }
                removeGeofencesByIds(geofencesToRemove);

                if (furthestGeofences.contains(messageGeofence.getConversationId())) {
                    // We don't need to add the new geofence because it is one of the furthest
                    return;
                }
            }
        }

        // Then finally add the new one if needed
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (connectionResult.isSuccess()) {
            int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (finePermissionCheck == PackageManager.PERMISSION_GRANTED) {

                // 1. Create an IntentService PendingIntent
                Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
                PendingIntent pendingIntent =
                        PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // 2. Associate the service PendingIntent with the geofence and call addGeofences
                Status status = LocationServices.GeofencingApi.addGeofences(googleApiClient,
                        getAddGeofencingRequest(messageGeofence.buildGeofence()), pendingIntent)
                        .await(30, TimeUnit.SECONDS);

                if (status.isSuccess()) {
                    persistGeofenceToDatabase(messageGeofence);
                } else {
                    Log.e(TAG, "Registering geofence failed: " + status.getStatusMessage() + " : "
                            + status.getStatusCode());
                }
            }
        } else {
            Log.w(TAG, "Connection to GoogleApiClient failed. Cannot add geofence.");
        }
    }

    private long getNumberOfGeofencesPersisted() {

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        long numberOfGeofences = DatabaseUtils.queryNumEntries(readableDatabase,
                GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME);
        readableDatabase.close();

        return numberOfGeofences;
    }

    private List<String> getFurthestGeofences(MessageGeofence newGeofence, Location userLocation) {
        Map<String, Float> tempGeofences = new HashMap<>();
        Location newGeofenceLocation = new Location("");
        newGeofenceLocation.setLatitude(newGeofence.getLatitude());
        newGeofenceLocation.setLongitude(newGeofence.getLongitude());
        float distanceBetweenUserAndNewGeofence = newGeofenceLocation.distanceTo(userLocation) - newGeofence.getRadius();
        Log.d(TAG, "Distance between user and new geofence " + distanceBetweenUserAndNewGeofence);
        tempGeofences.put(newGeofence.getConversationId(), distanceBetweenUserAndNewGeofence);

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
                String conversationId = cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON));
                float radius = cursor.getFloat(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS));
                Location geofenceLocation = new Location("");
                geofenceLocation.setLatitude(lat);
                geofenceLocation.setLongitude(lon);
                Float distanceFromUser = geofenceLocation.distanceTo(userLocation) - radius;
                Log.d(TAG, "Distance: " + distanceFromUser);
                tempGeofences.put(conversationId, distanceFromUser);
            } while (cursor.moveToNext());
        }

        cursor.close();
        readableDatabase.close();

        // Sort the map of geofences by distance descending
        LinkedHashMap<String, Float> tempSortedGeofences = sortByDistanceDesc(tempGeofences);
        Log.d(TAG, "Sorted map");
        for (Map.Entry<String, Float> entry : tempSortedGeofences.entrySet()) {
            Log.d(TAG, entry.getKey() + " " + entry.getValue());
        }

        List<String> tempConversationIdList = new ArrayList<>(tempSortedGeofences.keySet());
        List<String> furthestGeofences = new ArrayList<>();
        for (int i = maxGeofences; i < tempConversationIdList.size(); i++) {
            furthestGeofences.add(tempConversationIdList.get(i));
            Log.d(TAG, "Furthest geofence " + tempConversationIdList.get(i));
        }

        return furthestGeofences;
    }

    private LinkedHashMap<String, Float> sortByDistanceDesc(Map<String, Float> unsortedMap) {
        List<Map.Entry<String, Float>> list = new LinkedList<>(unsortedMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> lhs, Map.Entry<String, Float> rhs) {
                return (lhs.getValue().compareTo(rhs.getValue()));
            }
        });
        LinkedHashMap<String, Float> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Float> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private GeofencingRequest getAddGeofencingRequest(Geofence geofence) {
        List<Geofence> geofencesToAdd = new ArrayList<>();
        geofencesToAdd.add(geofence);
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofencesToAdd);
        return builder.build();
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

    public void removeGeofencesByIds(List<String> geofenceIdsToRemove) {

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (connectionResult.isSuccess()) {
            if (geofenceIdsToRemove.size() > 0) {
                Status status = LocationServices.GeofencingApi.removeGeofences(
                        googleApiClient, geofenceIdsToRemove).await(30, TimeUnit.SECONDS);
                if (status.isSuccess()) {
                    deleteGeofencesFromDatabase(geofenceIdsToRemove);
                    Log.d(TAG, "Deleted geofences from GeofencingApi");
                } else {
                    Log.e(TAG, "Removing geofence failed: " + status.getStatusMessage());
                }
            }
        } else {
            Log.w(TAG, "Connection to GoogleApiClient failed. Cannot remove geofence.");
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

            removeGeofencesByIds(geofencesToRemove);
        }
    }
}
