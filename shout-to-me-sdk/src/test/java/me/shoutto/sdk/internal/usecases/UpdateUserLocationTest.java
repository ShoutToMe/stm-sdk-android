package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.SortedSet;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.http.StmRequestProcessor;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * UpdateUserLocationTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
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
    private StmPreferenceManager mockStmPreferenceManager;

    @Mock
    private UserLocationDao mockUserLocationDao;

    @Captor
    ArgumentCaptor<StmError> errorArgumentCaptor;

    @Before
    public void before() {
        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
    }

    @Test
    public void post_WithNullLocation_ShouldCallBackWithError() {

        UpdateUserLocation updateUserLocation = new UpdateUserLocation(mockStmRequestProcessor,
                mockGeofenceManager, mockStmPreferenceManager, mockUserLocationDao, mockContext, "");
        updateUserLocation.update(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Void.class));
    }
}
