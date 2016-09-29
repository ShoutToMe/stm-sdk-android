package me.shoutto.sdk.internal.location.geofence.database;

import android.provider.BaseColumns;

/**
 * Class to store and retrieve geofence data.
 */
public final class GeofenceDatabaseSchema {

    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public GeofenceDatabaseSchema() {}

    public static abstract class GeofenceEntry implements BaseColumns {
        public static final String TABLE_NAME = "geofence";
        public static final String COLUMN_CHANNEL_ID = "channel_id";
        public static final String COLUMN_CHANNEL_IMAGE_URL = "channel_image_url";
        public static final String COLUMN_CONVERSATION_ID = "conversation_id";
        public static final String COLUMN_EXPIRATION_DATE = "expiration_date";
        public static final String COLUMN_LAT = "lat";
        public static final String COLUMN_LON = "lon";
        public static final String COLUMN_MESSAGE_BODY = "message_body";
        public static final String COLUMN_MESSAGE_TITLE = "message_title";
        public static final String COLUMN_MESSAGE_TYPE = "message_type";
        public static final String COLUMN_RADIUS = "radius";
    }

    public static String getCreateGeofenceTableStatement() {
        return "CREATE TABLE " +  GeofenceEntry.TABLE_NAME + " ("
                + GeofenceEntry.COLUMN_CONVERSATION_ID + TEXT_TYPE + " PRIMARY KEY,"
                + GeofenceEntry.COLUMN_LAT + REAL_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_LON + REAL_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_RADIUS + REAL_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_CHANNEL_ID + TEXT_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL + TEXT_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_MESSAGE_BODY + TEXT_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_MESSAGE_TITLE + TEXT_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_MESSAGE_TYPE + TEXT_TYPE + COMMA_SEP
                + GeofenceEntry.COLUMN_EXPIRATION_DATE + INTEGER_TYPE + ")";
    }

    public static String getDeleteGeofenceTableStatement() {
        return "DROP TABLE IF EXISTS " + GeofenceEntry.TABLE_NAME;
    }
}
