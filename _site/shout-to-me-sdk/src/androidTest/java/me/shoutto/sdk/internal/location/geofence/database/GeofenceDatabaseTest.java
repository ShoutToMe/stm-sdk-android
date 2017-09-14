package me.shoutto.sdk.internal.location.geofence.database;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GeofenceDatabaseTest {

    private GeofenceDbHelper geofenceDbHelper;

    @Before
    public void setUp() {
        RenamingDelegatingContext context = new RenamingDelegatingContext(InstrumentationRegistry.getContext(), "test_");
        geofenceDbHelper = new GeofenceDbHelper(context);
    }

    @Test
    public void testDatabase() {
        String conversationId = "conversation" + new Date().getTime();
        double lat = 32.001;
        double lon = -114.001;
        float radius = 5.0f;
        String channelId = "channel123";
        String channelImageUrl = "http://channel-image";
        String notificationBody = "This is the message body";
        String notificationTitle = "Notification title";
        String notificationType = "notification type";
        long expirationTime = new Date().getTime();

        SQLiteDatabase db = geofenceDbHelper.getWritableDatabase();
        assertThat(db.isOpen(), equalTo(true));

        db.beginTransaction();
        ContentValues geofenceValues = new ContentValues();
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID, conversationId);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT, lat);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON, lon);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS, radius);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID, channelId);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL, channelImageUrl);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY, notificationBody);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE, notificationTitle);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE, notificationType);
        geofenceValues.put(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_EXPIRATION_DATE, expirationTime);

        try {
            long pk = db.insert(GeofenceDatabaseSchema.GeofenceEntry.TABLE_NAME,
                    null, geofenceValues);
            assertThat(pk, is(1l));
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception ex) {
            db.endTransaction();
        } finally {
            db.close();
        }

        SQLiteDatabase readableDatabase = geofenceDbHelper.getReadableDatabase();
        String[] projection = {
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE,
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_EXPIRATION_DATE
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
        cursor.moveToFirst();
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID)), equalTo(conversationId));
        assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT)), equalTo(lat));
        assertThat(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON)), equalTo(lon));
        assertThat(cursor.getFloat(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS)), equalTo(radius));
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_ID)), equalTo(channelId));
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CHANNEL_IMAGE_URL)), equalTo(channelImageUrl));
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_BODY)), equalTo(notificationBody));
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TITLE)), equalTo(notificationTitle));
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_MESSAGE_TYPE)), equalTo(notificationType));
    }
}
