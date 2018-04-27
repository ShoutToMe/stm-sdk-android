package me.shoutto.sdk.internal.usecases;

import com.amazonaws.services.sns.model.Topic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * CreateTopicPreferenceTest
 */
@RunWith(PowerMockRunner.class)
public class CreateTopicPreferenceTest {

    @Mock
    private StmRequestProcessor<StmBaseEntity> mockStmRequestProcessor;

    @Mock
    private StmCallback<Void> mockCallback;

    @Captor
    private ArgumentCaptor<StmError> errorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Void> successArgumentCaptor;

    @Test
    public void create_WithNullTopic_ShouldCallBackWithError() throws Exception {
        CreateTopicPreference createTopicPreference = new CreateTopicPreference(mockStmRequestProcessor);
        createTopicPreference.create(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(null);
    }

    @Test
    public void create_WithEmptyStringTopic_ShouldCallBackWithError() throws Exception {
        CreateTopicPreference createTopicPreference = new CreateTopicPreference(mockStmRequestProcessor);
        createTopicPreference.create("", mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(null);
    }

    @Test
    public void create_WithValidInput_ShouldCallPutOnStmRequestProcessor() throws Exception {
        CreateTopicPreference createTopicPreference = new CreateTopicPreference(mockStmRequestProcessor);
        createTopicPreference.create("topic", mockCallback);

        verify(mockStmRequestProcessor, times(1))
                .processRequest(eq(HttpMethod.PUT), any(TopicPreference.class));
        verify(mockStmRequestProcessor, times(0))
                .processRequest(eq(HttpMethod.POST), any(TopicPreference.class));
    }

    @Test
    public void create_WithValidInput_ShouldCallBackWithoutError() throws Exception {
        CreateTopicPreference createTopicPreference = new CreateTopicPreference(mockStmRequestProcessor);
        createTopicPreference.create("topic", mockCallback);

        StmObservableResults<Void> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(false);
        stmObservableResults.setErrorMessage("");
        stmObservableResults.setResult(null);
        createTopicPreference.update(stmObservableResults);

        verify(mockCallback, times(1)).onResponse(successArgumentCaptor.capture());
        assertNull(successArgumentCaptor.getValue());
    }
}