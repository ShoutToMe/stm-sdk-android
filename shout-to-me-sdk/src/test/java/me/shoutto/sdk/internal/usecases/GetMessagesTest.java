package me.shoutto.sdk.internal.usecases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * GetMessagesTest
 */
@RunWith(PowerMockRunner.class)
public class GetMessagesTest {

    private static final String MESSAGE_ID = "messageId";

    @Mock
    StmEntityRequestProcessor mockStmEntityRequestProcessor;

    @Mock
    StmCallback<List<Message>> mockCallback;

    @Captor
    ArgumentCaptor<List<Message>> messagesArgumentCaptor;

    @Captor
    ArgumentCaptor<StmError> errorArgumentCaptor;

    @Test
    public void get_ShouldCallProcessRequest() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessages getMessages = new GetMessages(mockStmEntityRequestProcessor);
        getMessages.get(null);

        verify(mockStmEntityRequestProcessor, times(1)).processRequest(any(HttpMethod.class), any(Message.class));
    }

    @Test
    public void get_ShouldCallBackWithResult() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessages getMessages = new GetMessages(mockStmEntityRequestProcessor);
        getMessages.get(mockCallback);

        Message message = new Message();
        message.setId(MESSAGE_ID);
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        StmObservableResults<List<Message>> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(messages);
        getMessages.processCallback(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(messagesArgumentCaptor.capture());
        List<Message> messagesResult = messagesArgumentCaptor.getValue();
        assertEquals(1, messagesResult.size());
        assertEquals(MESSAGE_ID, messagesResult.get(0).getId());
    }

    @Test
    public void get_WithValidMessageId_ShouldCallBackWithErrorIfProcessingError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        GetMessages getMessages = new GetMessages(mockStmEntityRequestProcessor);
        getMessages.get(mockCallback);

        StmObservableResults<Message> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(true);
        getMessages.processCallbackError(stmObservableResults);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertEquals(StmError.SEVERITY_MINOR, errorArgumentCaptor.getValue().getSeverity());
        verify(mockCallback, times(0)).onResponse(any(List.class));
    }
}
