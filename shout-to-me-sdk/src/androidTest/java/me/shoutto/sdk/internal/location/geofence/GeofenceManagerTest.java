package me.shoutto.sdk.internal.location.geofence;

import android.support.test.InstrumentationRegistry;
import android.test.RenamingDelegatingContext;

import org.junit.Before;

import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.location.geofence.database.GeofenceDbHelper;

public class GeofenceManagerTest {

    private GeofenceDbHelper geofenceDbHelper;
    private StmPreferenceManager stmPreferenceManager;

    @Before
    public void setUp() {
        RenamingDelegatingContext context = new RenamingDelegatingContext(InstrumentationRegistry.getContext(), "test_");
        geofenceDbHelper = new GeofenceDbHelper(context);
        stmPreferenceManager = new StmPreferenceManager(context);
    }


}
