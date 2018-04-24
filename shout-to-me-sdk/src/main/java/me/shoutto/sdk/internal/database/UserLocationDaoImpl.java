package me.shoutto.sdk.internal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.UserLocation;

/**
 * Data access object for user location records
 */

public class UserLocationDaoImpl implements UserLocationDao {

    private static final String TAG = UserLocationDaoImpl.class.getSimpleName();
    private static final long MAX_NUM_RECORDS = 1000;
    private StmDbHelper stmDbHelper;

    public UserLocationDaoImpl(Context context) {
        stmDbHelper = new StmDbHelper(context);
    }

    @Override
    public void addUserLocationRecord(UserLocationRecord userLocationRecord) {
        ContentValues values = new ContentValues();
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_DATE, userLocationRecord.getDate().getTime());
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_LAT, userLocationRecord.getLat());
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_LON, userLocationRecord.getLon());
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_METERS_SINCE_LAST_UPDDATE, userLocationRecord.getMetersSinceLastUpdate());
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_RADIUS, userLocationRecord.getRadius());
        values.put(UserLocationContract.UserLocation.COLUMN_NAME_TYPE, userLocationRecord.getType());

        SQLiteDatabase db = stmDbHelper.getWritableDatabase();
        try {
            db.insert(UserLocationContract.UserLocation.TABLE_NAME, null, values);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    @Override
    public void deleteAllUserLocationRecords() {
        SQLiteDatabase db = stmDbHelper.getWritableDatabase();
        try {
            db.delete(UserLocationContract.UserLocation.TABLE_NAME, null, null);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    @Override
    public List<UserLocationRecord> getAllUserLocationRecords() {
        List<UserLocationRecord> userLocationRecords = new ArrayList<>();

        SQLiteDatabase db = stmDbHelper.getReadableDatabase();

        String[] projection = {
                UserLocationContract.UserLocation.COLUMN_NAME_DATE,
                UserLocationContract.UserLocation.COLUMN_NAME_LAT,
                UserLocationContract.UserLocation.COLUMN_NAME_LON,
                UserLocationContract.UserLocation.COLUMN_NAME_METERS_SINCE_LAST_UPDDATE,
                UserLocationContract.UserLocation.COLUMN_NAME_RADIUS,
                UserLocationContract.UserLocation.COLUMN_NAME_TYPE
        };

        Cursor cursor = null;

        try {
            cursor  = db.query(
                    UserLocationContract.UserLocation.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            while(cursor.moveToNext()) {
                UserLocationRecord userLocationRecord = new UserLocationRecord();
                userLocationRecord.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_DATE))));
                userLocationRecord.setLat(cursor.getDouble(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_LAT)));
                userLocationRecord.setLon(cursor.getDouble(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_LON)));
                userLocationRecord.setMetersSinceLastUpdate(cursor.getFloat(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_METERS_SINCE_LAST_UPDDATE)));
                userLocationRecord.setRadius(cursor.getFloat(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_RADIUS)));
                userLocationRecord.setType(cursor.getString(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation.COLUMN_NAME_TYPE )));
                userLocationRecords.add(userLocationRecord);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return userLocationRecords;
    }

    @Override
    public List<UserLocation> getAllUserLocations() {
        List<UserLocationRecord> userLocationRecords = getAllUserLocationRecords();

        List<UserLocation> userLocations = new ArrayList<>();
        for (UserLocationRecord locationRecord : userLocationRecords) {
            UserLocation userLocation = new UserLocation();
            userLocation.setDate(locationRecord.getDate());
            if (locationRecord.getMetersSinceLastUpdate() > 0.0) {
                userLocation.setMetersSinceLastUpdate(locationRecord.getMetersSinceLastUpdate());
            }
            Double[] coordinates = { locationRecord.getLon(), locationRecord.getLat() };
            userLocation.setLocation(new UserLocation.Location(coordinates));
            userLocations.add(userLocation);
        }

        return userLocations;
    }

    @Override
    public long getNumRows() {
        SQLiteDatabase db = stmDbHelper.getReadableDatabase();
        long numRows = 0;

        try {
            numRows = DatabaseUtils.queryNumEntries(db, UserLocationContract.UserLocation.TABLE_NAME);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return numRows;
    }

    @Override
    public void truncateTable() {
        long numRows = getNumRows();
        if (numRows > MAX_NUM_RECORDS) {
            long numRowsToDelete = numRows - MAX_NUM_RECORDS;
            String[] idsToDelete = new String[(int)(numRowsToDelete)];
            SQLiteDatabase readableDatabase = stmDbHelper.getReadableDatabase();
            List<String> placeholderList = new ArrayList<>((int)(numRowsToDelete));

            String[] projection = { UserLocationContract.UserLocation._ID };
            Cursor cursor = null;
            try {
                cursor = readableDatabase.query(
                        UserLocationContract.UserLocation.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        "date asc",
                        String.valueOf(numRowsToDelete)
                );

                int index = 0;
                while(cursor.moveToNext()) {
                    idsToDelete[index] = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(UserLocationContract.UserLocation._ID)));
                    placeholderList.add(index, "?");
                    index++;
                }
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
                if (readableDatabase != null && readableDatabase.isOpen()) {
                    readableDatabase.close();
                }
            }

            String placeholdersStr = TextUtils.join(",", placeholderList);

            SQLiteDatabase writableDatabase = stmDbHelper.getWritableDatabase();
            try {
                int result = writableDatabase.delete(UserLocationContract.UserLocation.TABLE_NAME, "_id in (" + placeholdersStr + ")", idsToDelete);
                Log.d(TAG, String.format("Removed %d record(s) from the User Location database", result));
            } finally {
                if (writableDatabase != null && writableDatabase.isOpen()) {
                    writableDatabase.close();
                }
            }

        }
    }
}
