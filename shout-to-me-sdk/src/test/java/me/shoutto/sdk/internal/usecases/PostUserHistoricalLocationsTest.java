package me.shoutto.sdk.internal.usecases;

import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for PostUserHistoricalLocations
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, SQLiteOpenHelper.class})
public class PostUserHistoricalLocationsTest {

    @Mock
    private StmRequestProcessor<List<StmBaseEntity>> mockStmRequestProcessor;

    @Mock
    private StmCallback<Void> mockCallback;

    @Mock
    private UserLocationDao mockUserLocationDao;

    @Captor
    ArgumentCaptor<StmError> errorArgumentCaptor;

    @Captor
    ArgumentCaptor<Void> userArgumentCaptor;

    @Test
    public void post_WithNullLocations_ShouldCallBackWithError() {
        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        PostUserHistoricalLocations postUserHistoricalLocations =
                new PostUserHistoricalLocations(mockStmRequestProcessor, null);
        postUserHistoricalLocations.post(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Void.class));
    }

    @Test
    public void post_WithEmptyLocationList_ShouldCallBackWithError() {
        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        PostUserHistoricalLocations postUserHistoricalLocations =
                new PostUserHistoricalLocations(mockStmRequestProcessor, null);
        postUserHistoricalLocations.post(new ArrayList<StmBaseEntity>(), mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Void.class));
    }

    @Test
    public void post_WithValidInput_ShouldCallBack() {
        PowerMockito.mockStatic(SQLiteOpenHelper.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        UserLocation userLocation = new UserLocation();
        List<StmBaseEntity> userLocations = new ArrayList<>();
        userLocations.add(userLocation);

        PostUserHistoricalLocations postUserHistoricalLocations =
                new PostUserHistoricalLocations(mockStmRequestProcessor, mockUserLocationDao);
        postUserHistoricalLocations.post(userLocations, mockCallback);

        StmObservableResults<Message> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(null);
        postUserHistoricalLocations.processCallback(stmObservableResults);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(null);
        verify(mockUserLocationDao, times(1)).deleteAllUserLocationRecords();
    }
}
