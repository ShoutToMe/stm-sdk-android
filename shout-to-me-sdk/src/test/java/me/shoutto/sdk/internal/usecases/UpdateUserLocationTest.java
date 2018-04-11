package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.SortedSet;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.database.UserLocationRecord;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * UpdateUserLocationTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, UpdateUserLocation.class})
public class UpdateUserLocationTest {

    @Mock
    private StmRequestProcessor<SortedSet<? extends StmBaseEntity>> mockStmRequestProcessor;

    @Mock
    private StmCallback<Void> mockCallback;

    @Mock
    private Context mockContext;

    @Mock
    private GeofenceManager mockGeofenceManager;

    @Mock
    private Intent mockIntent;

    @Mock
    private Location mockLocation;

    @Mock
    private Location mockLocationFromPreferences;

    @Mock
    private StmPreferenceManager mockStmPreferenceManager;

    @Mock
    private UserLocationDao mockUserLocationDao;

    @Captor
    private ArgumentCaptor<StmError> errorArgumentCaptor;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);

        when(mockLocationFromPreferences.toString()).thenReturn("Mock Location From Preferences");
        try {
            whenNew(Location.class).withAnyArguments().thenReturn(mockLocationFromPreferences);
        } catch (Exception ex) {
            fail("Failed to mock Location constructor");
        }

        when(mockLocation.getLatitude()).thenReturn(33.123456);
        when(mockLocation.getLongitude()).thenReturn(-117.123456);
        when(mockLocation.getTime()).thenReturn(60000L);

        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        try {
            whenNew(Intent.class).withNoArguments().thenReturn(mockIntent);
        } catch (Exception ex) {
            fail("Failed to mock Intent constructor");
        }
        PowerMockito.doReturn(mockIntent).when(mockIntent).setAction(any(String.class));
//        doCallRealMethod().when(mockIntent).putExtra(any(String.class), any(Long.class));
//        doCallRealMethod().when(mockIntent).putExtra(any(String.class), any(double.class));
//        doCallRealMethod().when(mockIntent).putExtra(any(String.class), any(float.class));
//        doCallRealMethod().when(mockIntent).putExtra(any(String.class), any(String.class));
    }

    @Test
    public void update_WithNullLocation_ShouldCallBackWithError() {
        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Void.class));
    }

    @Test
    public void update_WithNearbyLocation_ShouldCallBackWithNullResultAndReturn() {

        when(mockLocationFromPreferences.distanceTo(mockLocation)).thenReturn(1.0f);

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");

        updateUserLocation.update(mockLocation, mockCallback);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(null);
        verify(mockStmPreferenceManager, times(0)).setUserLocationLat(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationLon(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationTime(any(Long.class));
        verify(mockContext, times(0)).sendBroadcast(any(Intent.class));
        verify(mockGeofenceManager, times(0)).addUserLocationGeofence(any(Location.class));
        verify(mockStmRequestProcessor, times(0)).processRequest(any(HttpMethod.class), ArgumentMatchers.<SortedSet<UserLocation>>any());
    }

    @Test
    public void update_WithTooRecentLocation_ShouldCallBackWithNullResultAndReturn() {

        when(mockLocationFromPreferences.distanceTo(mockLocation)).thenReturn(10000.0f);
        when(mockStmPreferenceManager.getUserLocationTime()).thenReturn(59000L);

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(mockLocation, mockCallback);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(null);
        verify(mockStmPreferenceManager, times(0)).setUserLocationLat(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationLon(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationTime(any(Long.class));
        verify(mockContext, times(0)).sendBroadcast(any(Intent.class));
        verify(mockGeofenceManager, times(0)).addUserLocationGeofence(any(Location.class));
        verify(mockStmRequestProcessor, times(0)).processRequest(any(HttpMethod.class), ArgumentMatchers.<SortedSet<UserLocation>>any());
    }

    @Test
    public void update_WithOutOfOrderLocation_ShouldCallBackWithNullResultAndReturn() {

        when(mockLocationFromPreferences.distanceTo(mockLocation)).thenReturn(10000.0f);
        when(mockStmPreferenceManager.getUserLocationTime()).thenReturn(61000L);

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(mockLocation, mockCallback);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(null);
        verify(mockStmPreferenceManager, times(0)).setUserLocationLat(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationLon(any(double.class));
        verify(mockStmPreferenceManager, times(0)).setUserLocationTime(any(Long.class));
        verify(mockContext, times(0)).sendBroadcast(any(Intent.class));
        verify(mockGeofenceManager, times(0)).addUserLocationGeofence(any(Location.class));
        verify(mockStmRequestProcessor, times(0)).processRequest(any(HttpMethod.class), ArgumentMatchers.<SortedSet<UserLocation>>any());
    }

    @Test
    public void update_WithValidLocation_ShouldProcessLocationAndCallBackWithNullResult() {
        when(mockLocationFromPreferences.distanceTo(mockLocation)).thenReturn(10000.0f);
        when(mockStmPreferenceManager.getUserLocationTime()).thenReturn(30000L);

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(mockLocation, mockCallback);

        // StmRequestProcessor response
        StmObservableResults<Void> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(false);
        stmObservableResults.setResult(null);
        stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
        updateUserLocation.update(stmObservableResults);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(null);
        verify(mockStmPreferenceManager, times(1)).setUserLocationLat(any(double.class));
        verify(mockStmPreferenceManager, times(1)).setUserLocationLon(any(double.class));
        verify(mockStmPreferenceManager, times(1)).setUserLocationTime(any(Long.class));
        verify(mockContext, times(1)).sendBroadcast(any(Intent.class));
        verify(mockGeofenceManager, times(1)).addUserLocationGeofence(any(Location.class));
        verify(mockStmRequestProcessor, times(1)).processRequest(any(HttpMethod.class), ArgumentMatchers.<SortedSet<UserLocation>>any());
        verify(mockUserLocationDao, times(1)).deleteAllUserLocationRecords();
    }

    @Test
    public void update_WithCallbackError_ShouldAddRecordToUserLocationTable() {
        when(mockLocationFromPreferences.distanceTo(mockLocation)).thenReturn(10000.0f);
        when(mockStmPreferenceManager.getUserLocationTime()).thenReturn(30000L);

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(mockLocation, mockCallback);

        // StmRequestProcessor response
        StmObservableResults<Void> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(true);
        stmObservableResults.setResult(null);
        stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
        updateUserLocation.update(stmObservableResults);

        verify(mockUserLocationDao, times(1)).addUserLocationRecord(any(UserLocationRecord.class));
        verify(mockUserLocationDao, times(1)).truncateTable();
    }
}
