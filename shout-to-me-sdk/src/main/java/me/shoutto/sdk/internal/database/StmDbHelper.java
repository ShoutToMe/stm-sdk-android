package me.shoutto.sdk.internal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database helper class for Shout to Me data storage needs.
 */

public class StmDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ShoutToMeSDK.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UserLocationContract.UserLocation.TABLE_NAME + " (" +
                    UserLocationContract.UserLocation._ID + " INTEGER PRIMARY KEY," +
                    UserLocationContract.UserLocation.COLUMN_NAME_DATE + " INTEGER," +
                    UserLocationContract.UserLocation.COLUMN_NAME_LAT + " REAL," +
                    UserLocationContract.UserLocation.COLUMN_NAME_LON + " REAL," +
                    UserLocationContract.UserLocation.COLUMN_NAME_METERS_SINCE_LAST_UPDDATE + " REAL," +
                    UserLocationContract.UserLocation.COLUMN_NAME_RADIUS + " REAL," +
                    UserLocationContract.UserLocation.COLUMN_NAME_TYPE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + UserLocationContract.UserLocation.TABLE_NAME;

    public StmDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
