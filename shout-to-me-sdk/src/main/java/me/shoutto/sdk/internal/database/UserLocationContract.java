package me.shoutto.sdk.internal.database;

import android.provider.BaseColumns;

/**
 * Contract class for user location entries.  User location entries are stored for retry when
 * attempts to send the location to the Shout to Me service fail.
 */

public final class UserLocationContract {

    private UserLocationContract() {}

    public static class UserLocation implements BaseColumns {
        public static final String TABLE_NAME = "user_location";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
        public static final String COLUMN_NAME_METERS_SINCE_LAST_UPDDATE = "meters_since_last_update";
        public static final String COLUMN_NAME_RADIUS = "radius";
        public static final String COLUMN_NAME_TYPE = "type";
    }
}
