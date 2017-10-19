package me.shoutto.sdk.internal.usecases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * GetMessageTest
 */
@RunWith(PowerMockRunner.class)
public class GetMessageTest {

    private static final String MESSAGE_ID = "messageId";

    @Mock
    StmEntityRequestProcessor mockStmEntityRequestProcessor;

    @Mock
    StmCallback<Message> mockCallback;

    @Captor
    ArgumentCaptor<StmError> errorArgumentCaptor;

    @Captor
    ArgumentCaptor<Message> messageArgumentCaptor;

    @Test
    public void get_WithNullMessageId_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessage getMessage = new GetMessage(mockStmEntityRequestProcessor);
        getMessage.get(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Message.class));
    }

    @Test
    public void get_WithEmptyStringMessageId_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessage getMessage = new GetMessage(mockStmEntityRequestProcessor);
        getMessage.get("", mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(Message.class));
    }

    @Test
    public void get_WithValidMessageId_ShouldCallProcessRequestWithMessage() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessage getMessage = new GetMessage(mockStmEntityRequestProcessor);
        getMessage.get(MESSAGE_ID, null);

        verify(mockStmEntityRequestProcessor, times(1))
                .processRequest(any(HttpMethod.class), messageArgumentCaptor.capture());
        assertEquals(MESSAGE_ID, messageArgumentCaptor.getValue().getId());
    }

    @Test
    public void get_WithValidMessageId_ShouldCallBackWithResult() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessage getMessage = new GetMessage(mockStmEntityRequestProcessor);
        getMessage.get(MESSAGE_ID, mockCallback);

        Message message = new Message();
        message.setId(MESSAGE_ID);
        StmObservableResults<Message> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(message);
        getMessage.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(messageArgumentCaptor.capture());
        assertEquals(MESSAGE_ID, messageArgumentCaptor.getValue().getId());
    }

    @Test
    public void get_WithValidMessageId_ShouldCallBackWithErrorIfProcessingError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessage getMessage = new GetMessage(mockStmEntityRequestProcessor);
        getMessage.get(MESSAGE_ID, mockCallback);

        StmObservableResults<Message> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(true);
        getMessage.processCallbackError(stmObservableResults);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertEquals(StmError.SEVERITY_MINOR, errorArgumentCaptor.getValue().getSeverity());
        verify(mockCallback, times(0)).onResponse(any(Message.class));
    }
}
