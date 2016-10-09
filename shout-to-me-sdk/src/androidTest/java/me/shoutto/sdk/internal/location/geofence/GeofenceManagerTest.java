package me.shoutto.sdk.internal.location.geofence;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;

import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDatabaseSchema;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GeofenceManagerTest {

    private RenamingDelegatingContext context;
    private GeofenceDbHelper geofenceDbHelper;
    private StmPreferenceManager stmPreferenceManager;

    @Before
    public void setUp() {
        context = new RenamingDelegatingContext(InstrumentationRegistry.getContext(), "test_");
        geofenceDbHelper = new GeofenceDbHelper(context);
        stmPreferenceManager = new StmPreferenceManager(context);
    }

    @Test
    public void testAddGeofence() {
        GeofenceManager geofenceManager = new GeofenceManager(context, geofenceDbHelper, stmPreferenceManager);
        geofenceManager.setMaxGeofences(2);

        // User's location
        Location userLocation = new Location("");
        userLocation.setLatitude(32.759666);
        userLocation.setLongitude(-117.203951);

        // 2nd closest
        MessageGeofence messageGeofence1 = new MessageGeofence();
        messageGeofence1.setConversationId("message1");
        messageGeofence1.setLatitude(32.762212);
        messageGeofence1.setLongitude(-117.163568);
        messageGeofence1.setRadius(500);
        messageGeofence1.setExpirationDate(new Date());
        geofenceManager.addGeofence(messageGeofence1, userLocation);

        // 4th closest
        MessageGeofence messageGeofence2 = new MessageGeofence();
        messageGeofence2.setConversationId("message2");
        messageGeofence2.setLatitude(32.778719);
        messageGeofence2.setLongitude(-117.111940);
        messageGeofence2.setRadius(500);
        messageGeofence2.setExpirationDate(new Date());
        geofenceManager.addGeofence(messageGeofence2, userLocation);

        // 3rd closest
        MessageGeofence messageGeofence3 = new MessageGeofence();
        messageGeofence3.setConversationId("message3");
        messageGeofence3.setLatitude(32.771976);
        messageGeofence3.setLongitude(-117.131939);
        messageGeofence3.setRadius(500);
        messageGeofence3.setExpirationDate(new Date());
        geofenceManager.addGeofence(messageGeofence3, userLocation);

        // Closest
        MessageGeofence messageGeofence4 = new MessageGeofence();
        messageGeofence4.setConversationId("message4");
        messageGeofence4.setLatitude(32.756943);
        messageGeofence4.setLongitude(-117.222962);
        messageGeofence4.setRadius(500);
        messageGeofence4.setExpirationDate(new Date());
        geofenceManager.addGeofence(messageGeofence4, userLocation);


        // Test the Comparator
        MessageGeofence[] messageGeofences = new MessageGeofence[4];
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
                GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID + " ASC"
        );

        int idx = 0;
        if (cursor.moveToFirst()) {
            do {
                MessageGeofence messageGeofence = new MessageGeofence();
                messageGeofence.setConversationId(cursor.getString(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_CONVERSATION_ID)));
                messageGeofence.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LAT)));
                messageGeofence.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_LON)));
                messageGeofence.setRadius(cursor.getFloat(cursor.getColumnIndexOrThrow(GeofenceDatabaseSchema.GeofenceEntry.COLUMN_RADIUS)));
                messageGeofence.setId(messageGeofence.getConversationId());
                messageGeofences[idx] = messageGeofence;
                idx++;
            } while (cursor.moveToNext());
        }

        cursor.close();
        readableDatabase.close();

        assertThat(messageGeofences[0].getId(), is("message1"));
        assertThat(messageGeofences[1].getId(), is("message2"));
        assertThat(messageGeofences[2].getId(), is("message3"));
        assertThat(messageGeofences[3].getId(), is("message4"));

        Arrays.sort(messageGeofences, new GeofenceDistanceComparator(userLocation));

        assertThat(messageGeofences[0].getId(), is("message4"));
        assertThat(messageGeofences[1].getId(), is("message1"));
        assertThat(messageGeofences[2].getId(), is("message3"));
        assertThat(messageGeofences[3].getId(), is("message2"));
    }
}
