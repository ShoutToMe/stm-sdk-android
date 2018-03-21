package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * GetChannelSubscriptionTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class GetChannelSubscriptionTest {

    private static final String CHANNEL_ID = "channelId";
    private static final String USER_ID = "userId";

    @Mock
    StmRequestProcessor<StmBaseEntity> mockStmRequestProcessor;

    @Mock
    StmCallback<Boolean> mockCallback;

    @Captor
    ArgumentCaptor<User> userArgumentCaptor;

    @Captor
    ArgumentCaptor<StmError> errorArgumentCaptor;

    @Captor
    ArgumentCaptor<StmError> stmErrorArgumentCaptor;

    @Test
    public void get_WithNullChannelId_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(null, USER_ID, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Boolean.class));
    }

    @Test
    public void get_WithEmptyStringChannelId_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get("", USER_ID, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Boolean.class));
    }

    @Test
    public void get_WithNullUserId_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Boolean.class));
    }

    @Test
    public void get_WithEmptyStringUserId_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, "", mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Boolean.class));
    }

    @Test
    public void get_WithValidInput_ShouldCallProcessRequestWithUser() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, null);

        verify(mockStmRequestProcessor, times(1))
                .processRequest(any(HttpMethod.class), userArgumentCaptor.capture());
        assertEquals(USER_ID, userArgumentCaptor.getValue().getId());
    }

    @Test
    public void get_WithValidInput_ShouldReturnFalseWithNullArray() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, mockCallback);

        User user = new User();
        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(user);
        getChannelSubscription.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(false);
    }

    @Test
    public void get_WithValidInput_ShouldReturnFalseWithEmptyArray() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, mockCallback);

        User user = new User();
        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        user.setChannelSubscriptions(new ArrayList<String>());
        stmObservableResults.setResult(user);
        getChannelSubscription.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(false);
    }

    @Test
    public void get_WithValidInput_ShouldReturnFalseIfChannelNotInArray() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, mockCallback);

        User user = new User();
        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add("different channel 1");
        channelSubscriptions.add("different channel 2");
        channelSubscriptions.add("different channel 3");
        user.setChannelSubscriptions(channelSubscriptions);
        stmObservableResults.setResult(user);
        getChannelSubscription.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(false);
    }

    @Test
    public void get_WithValidInput_ShouldReturnTrueIfChannelInArray() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, mockCallback);

        User user = new User();
        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add("different channel 1");
        channelSubscriptions.add(CHANNEL_ID);
        channelSubscriptions.add("different channel 2");
        user.setChannelSubscriptions(channelSubscriptions);
        stmObservableResults.setResult(user);
        getChannelSubscription.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(true);
    }

    @Test
    public void get_WithValidInput_ShouldCallBackErrorOnProcessingError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetChannelSubscription getChannelSubscription = new GetChannelSubscription(mockStmRequestProcessor);
        getChannelSubscription.get(CHANNEL_ID, USER_ID, mockCallback);
        getChannelSubscription.processCallbackError(new StmObservableResults());

        verify(mockCallback, times(1)).onError(stmErrorArgumentCaptor.capture());
        assertNotNull(stmErrorArgumentCaptor.getValue());
        assertEquals(StmError.SEVERITY_MINOR, stmErrorArgumentCaptor.getValue().getSeverity());
    }
}
