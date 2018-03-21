package me.shoutto.sdk.internal.location.geofence;

import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.location.GeofencingEvent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.powermock.api.mockito.PowerMockito;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//import static org.junit.Assert.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.powermock.api.mockito.PowerMockito.when;

/**
 * GeofenceTransitionsServiceTest
 */
@RunWith(AndroidJUnit4.class)
public class GeofenceTransitionsServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(60L, TimeUnit.SECONDS);

//    @Mock
//    private GeofencingEvent mockGeofencingEvent;

//    @Test
//    public void onStartCommand() throws TimeoutException {
//        PowerMockito.mockStatic(GeofencingEvent.class);
//        when(GeofencingEvent.fromIntent(any(Intent.class))).thenReturn(null);
//
//        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), GeofenceTransitionsService.class);
//        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
//        intent.putExtra("gms_error_code", 999);
//        try {
//            mServiceRule.startService(intent);
//        } catch (TimeoutException ex) {
//            ex.printStackTrace();
//        }
//
//    }

}