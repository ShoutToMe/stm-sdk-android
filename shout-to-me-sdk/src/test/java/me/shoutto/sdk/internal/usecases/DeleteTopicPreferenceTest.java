package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * DeleteTopicPreferenceTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class DeleteTopicPreferenceTest {

    @Mock
    StmEntityRequestProcessor mockStmEntityRequestProcessor;

    @Captor
    ArgumentCaptor<TopicPreference> topicPreferenceArgumentCaptor;

    @Test
    public void delete_WithNullTopic_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        DeleteTopicPreference deleteTopicPreference = new DeleteTopicPreference(mockStmEntityRequestProcessor);
        deleteTopicPreference.delete(null, new Callback<Void>() {
            @Override
            public void onSuccess(StmResponse<Void> stmResponse) {
                fail("Should not call back with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });
    }

    @Test
    public void delete_WithEmptyStringTopic_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        DeleteTopicPreference deleteTopicPreference = new DeleteTopicPreference(mockStmEntityRequestProcessor);
        deleteTopicPreference.delete("", new Callback<Void>() {
            @Override
            public void onSuccess(StmResponse<Void> stmResponse) {
                fail("Should not call back with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });
    }

    @Test
    public void delete_WithValidInput_ShouldCallProcessRequestWithTopicPreference() {

        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        String topic = "topic";
        DeleteTopicPreference deleteTopicPreference = new DeleteTopicPreference(mockStmEntityRequestProcessor);
        deleteTopicPreference.delete(topic, null);

        verify(mockStmEntityRequestProcessor, times(1))
                .processRequest(any(HttpMethod.class), topicPreferenceArgumentCaptor.capture());
        assertEquals(topic, topicPreferenceArgumentCaptor.getValue().getTopic());
    }

    @Test
    public void delete_WithValidInput_ShouldCallBackWithResult() {

        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        String topic = "topic";
        TopicPreference topicPreference = new TopicPreference();
        topicPreference.setTopic(topic);

        StmCallback<Void> callback = new Callback<Void>() {
            @Override
            public void onSuccess(StmResponse<Void> stmResponse) {

            }

            @Override
            public void onFailure(StmError stmError) {

            }
        };
        DeleteTopicPreference deleteTopicPreference = new DeleteTopicPreference(mockStmEntityRequestProcessor);
        deleteTopicPreference.delete(topic, callback);

        verify(callback, times(1)).onResponse(null);
    }
}
