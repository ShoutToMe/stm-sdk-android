package me.shoutto.sdk.internal.location.geofence;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.runner.RunWith;

//@RunWith(AndroidJUnit4.class)
//@SmallTest
public class GeofenceManagerTest {

    private RenamingDelegatingContext context;

    @Before
    public void setUp() {
        context = new RenamingDelegatingContext(InstrumentationRegistry.getContext(), "test_");
    }

}
