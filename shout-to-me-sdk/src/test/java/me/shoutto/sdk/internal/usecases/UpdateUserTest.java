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

import java.util.ArrayList;
import java.util.List;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.UpdateUserRequest;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import static org.mockito.Mockito.*;

/**
 * UpdateUserTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class UpdateUserTest {

    @Mock
    StmEntityRequestProcessor mockStmEntityRequestProcessor;

    @Captor
    ArgumentCaptor<User> userArgumentCaptor;

    @Test
    public void update_InvalidInput_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmEntityRequestProcessor);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUser.update(updateUserRequest, "userId", new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                fail("Should not callback with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });
    }

    @Test
    public void update_NullUserId_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmEntityRequestProcessor);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("email");
        updateUser.update(updateUserRequest, null, new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                fail("Should not call back with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });

        verify(mockStmEntityRequestProcessor, times(0)).processRequest(any(HttpMethod.class), any(User.class));
    }

    @Test
    public void update_EmptyStringUserId_ShouldCallBackWithError() {
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmEntityRequestProcessor);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("email");
        updateUser.update(updateUserRequest, "", new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                fail("Should not callback with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });
        verify(mockStmEntityRequestProcessor, times(0)).processRequest(any(HttpMethod.class), any(User.class));
    }

    @Test
    public void update_ValidInput_ShouldCallProcessRequestWithUser() {
        String userId = "userId";
        String email = "email";
        String handle = "handle";
        String phone = "phone";
        String channel = "channel";
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add(channel);
        String topic = "topic";
        List<String> topicPreferences = new ArrayList<>();
        topicPreferences.add(topic);

        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));

        UpdateUser updateUser = new UpdateUser(mockStmEntityRequestProcessor);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail(email);
        updateUserRequest.setHandle(handle);
        updateUserRequest.setPhone(phone);
        updateUserRequest.setChannelSubscriptions(channelSubscriptions);
        updateUserRequest.setTopicPreferences(topicPreferences);
        updateUser.update(updateUserRequest, userId, null);

        verify(mockStmEntityRequestProcessor, times(1)).processRequest(any(HttpMethod.class), userArgumentCaptor.capture());
        assertEquals(userId, userArgumentCaptor.getValue().getId());
        assertEquals(email, userArgumentCaptor.getValue().getEmail());
        assertEquals(handle, userArgumentCaptor.getValue().getHandle());
        assertEquals(phone, userArgumentCaptor.getValue().getPhone());
        assertEquals(channel, userArgumentCaptor.getValue().getChannelSubscriptions().get(0));
        assertEquals(topic, userArgumentCaptor.getValue().getTopicPreferences().get(0));
    }

    @Test
    public void input_ValidInput_ShouldCallBackWithResults() {
        final User userResult = new User();
        userResult.setId("userResult");

        doNothing().when(mockStmEntityRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmEntityRequestProcessor);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setHandle("handle");
        updateUser.update(updateUserRequest, "userId", new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                assertEquals(userResult.getId(), stmResponse.get().getId());
            }

            @Override
            public void onFailure(StmError stmError) {
                fail("Should not call back with failure");
            }
        });

        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(userResult);
        updateUser.update(stmObservableResults);
    }
}
