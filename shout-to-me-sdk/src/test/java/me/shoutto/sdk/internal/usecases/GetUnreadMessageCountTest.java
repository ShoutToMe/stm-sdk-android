package me.shoutto.sdk.internal.usecases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
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
 * GetUnreadMessageCountTest
 */
@RunWith(PowerMockRunner.class)
public class GetUnreadMessageCountTest {

    @Mock
    StmRequestProcessor<StmBaseEntity> mockStmRequestProcessor;

    @Mock
    StmCallback<Integer> mockCallback;

    @Captor
    ArgumentCaptor<StmError> stmErrorArgumentCaptor;

    @Test
    public void get_shouldCallProcessRequestWithNewMessage() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetMessageCount getUnreadMessageCount = new GetMessageCount(mockStmRequestProcessor);
        getUnreadMessageCount.get(null);

        verify(mockStmRequestProcessor, times(1)).processRequest(any(HttpMethod.class), any(Message.class));
    }

    @Test
    public void get_shouldCallBackWithInteger() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetMessageCount getUnreadMessageCount = new GetMessageCount(mockStmRequestProcessor);
        getUnreadMessageCount.get(mockCallback);

        Integer fakeResult = 10;
        StmObservableResults<Integer> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(fakeResult);
        getUnreadMessageCount.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(fakeResult);
    }

    @Test
    public void get_ShouldCallBackWithErrorIfProcessingError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        GetMessageCount getUnreadMessageCount = new GetMessageCount(mockStmRequestProcessor);
        getUnreadMessageCount.get(mockCallback);
        getUnreadMessageCount.processCallbackError(new StmObservableResults());

        verify(mockCallback, times(1)).onError(stmErrorArgumentCaptor.capture());
        assertNotNull(stmErrorArgumentCaptor.getValue());
        assertEquals(StmError.SEVERITY_MINOR, stmErrorArgumentCaptor.getValue().getSeverity());
    }
}
