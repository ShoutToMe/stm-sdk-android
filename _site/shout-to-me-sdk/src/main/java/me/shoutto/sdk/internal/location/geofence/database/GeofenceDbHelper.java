package me.shoutto.sdk.internal.location.geofence.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Class to handle SQLite database tasks
 */
public class GeofenceDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "me.shoutto.sdk.geofence.db";
    public static final int DATABASE_VERSION = 1;

    public GeofenceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(GeofenceDatabaseSchema.getCreateGeofenceTableStatement());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(GeofenceDatabaseSchema.getDeleteGeofenceTableStatement());
        onCreate(db);
    }
}
