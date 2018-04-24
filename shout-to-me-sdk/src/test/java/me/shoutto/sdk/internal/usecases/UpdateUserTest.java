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
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.UpdateUserRequest;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

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
    private StmRequestProcessor<StmBaseEntity> mockStmRequestProcessor;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Test
    public void update_InvalidInput_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUser.update(updateUserRequest, "userId", false, new Callback<User>() {
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
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("email");
        updateUser.update(updateUserRequest, null, false, new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                fail("Should not call back with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });

        verify(mockStmRequestProcessor, times(0)).processRequest(any(HttpMethod.class), any(User.class));
    }

    @Test
    public void update_EmptyStringUserId_ShouldCallBackWithError() {
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("email");
        updateUser.update(updateUserRequest, "", false, new Callback<User>() {
            @Override
            public void onSuccess(StmResponse<User> stmResponse) {
                fail("Should not callback with successful response");
            }

            @Override
            public void onFailure(StmError stmError) {
                assertNotNull(stmError);
            }
        });
        verify(mockStmRequestProcessor, times(0)).processRequest(any(HttpMethod.class), any(User.class));
    }

    @Test
    public void update_ValidInputAndGetRequired_ShouldCallProcessRequestTwiceForGetAndPut() {
        String userId = "userId";
        String email = "email";
        String gender = "gender";
        String handle = "handle";
        String phone = "phone";
        String channel = "channel";
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add(channel);
        String topic = "topic";
        List<String> topicPreferences = new ArrayList<>();
        topicPreferences.add(topic);
        User.MetaInfo metaInfo = new User.MetaInfo();
        metaInfo.setGender(gender);

        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail(email);
        updateUserRequest.setGender(gender);
        updateUserRequest.setHandle(handle);
        updateUserRequest.setPhone(phone);
        updateUserRequest.setChannelSubscriptions(channelSubscriptions);
        updateUserRequest.setTopicPreferences(topicPreferences);
        updateUser.update(updateUserRequest, userId, true, null);

        User user = new User();
        user.setEmail(email);
        user.setHandle(handle);
        user.setPhone(phone);
        user.setChannelSubscriptions(channelSubscriptions);
        user.setTopicPreferences(topicPreferences);

        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(user);
        stmObservableResults.setError(false);

        // GET callback
        updateUser.processCallback(stmObservableResults);

        // PUT callback
        updateUser.processCallback(stmObservableResults);

        verify(mockStmRequestProcessor, times(1)).processRequest(eq(HttpMethod.GET), userArgumentCaptor.capture());
        verify(mockStmRequestProcessor, times(1)).processRequest(eq(HttpMethod.PUT), userArgumentCaptor.capture());
        assertEquals(userId, userArgumentCaptor.getValue().getId());
        assertEquals(email, userArgumentCaptor.getValue().getEmail());
        assertEquals(gender, userArgumentCaptor.getValue().getMetaInfo().getGender());
        assertEquals(handle, userArgumentCaptor.getValue().getHandle());
        assertEquals(phone, userArgumentCaptor.getValue().getPhone());
        assertEquals(channel, userArgumentCaptor.getValue().getChannelSubscriptions().get(0));
        assertEquals(topic, userArgumentCaptor.getValue().getTopicPreferences().get(0));
    }

    @Test
    public void update_ValidInputGetRequiredGetFails_ShouldCallBackWithError() {
        String userId = "userId";
        String email = "email";
        String gender = "gender";
        String handle = "handle";
        String phone = "phone";
        String channel = "channel";
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add(channel);
        String topic = "topic";
        List<String> topicPreferences = new ArrayList<>();
        topicPreferences.add(topic);
        User.MetaInfo metaInfo = new User.MetaInfo();
        metaInfo.setGender(gender);

        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail(email);
        updateUserRequest.setGender(gender);
        updateUserRequest.setHandle(handle);
        updateUserRequest.setPhone(phone);
        updateUserRequest.setChannelSubscriptions(channelSubscriptions);
        updateUserRequest.setTopicPreferences(topicPreferences);
        updateUser.update(updateUserRequest, userId, true, null);

        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setResult(null);
        stmObservableResults.setError(true);
        stmObservableResults.setErrorMessage("Error");

        // GET callback
        updateUser.processCallback(stmObservableResults);

        verify(mockStmRequestProcessor, times(1)).processRequest(eq(HttpMethod.GET), userArgumentCaptor.capture());
        verify(mockStmRequestProcessor, times(0)).processRequest(eq(HttpMethod.PUT), userArgumentCaptor.capture());
        assertEquals(userId, userArgumentCaptor.getValue().getId());
        assertEquals(email, userArgumentCaptor.getValue().getEmail());
        assertEquals(gender, userArgumentCaptor.getValue().getMetaInfo().getGender());
        assertEquals(handle, userArgumentCaptor.getValue().getHandle());
        assertEquals(phone, userArgumentCaptor.getValue().getPhone());
        assertEquals(channel, userArgumentCaptor.getValue().getChannelSubscriptions().get(0));
        assertEquals(topic, userArgumentCaptor.getValue().getTopicPreferences().get(0));
    }

    @Test
    public void update_ValidInputAndGetNotRequired_ShouldCallProcessRequestWithUser() {
        String userId = "userId";
        String email = "email";
        String gender = "gender";
        String handle = "handle";
        String phone = "phone";
        String channel = "channel";
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add(channel);
        String topic = "topic";
        List<String> topicPreferences = new ArrayList<>();
        topicPreferences.add(topic);

        PowerMockito.mockStatic(Log.class);
        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));

        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail(email);
        updateUserRequest.setGender(gender);
        updateUserRequest.setHandle(handle);
        updateUserRequest.setPhone(phone);
        updateUserRequest.setChannelSubscriptions(channelSubscriptions);
        updateUserRequest.setTopicPreferences(topicPreferences);
        updateUser.update(updateUserRequest, userId, false, null);

        verify(mockStmRequestProcessor, times(0)).processRequest(eq(HttpMethod.GET), userArgumentCaptor.capture());
        verify(mockStmRequestProcessor, times(1)).processRequest(eq(HttpMethod.PUT), userArgumentCaptor.capture());
        assertEquals(userId, userArgumentCaptor.getValue().getId());
        assertEquals(email, userArgumentCaptor.getValue().getEmail());
        assertEquals(gender, userArgumentCaptor.getValue().getMetaInfo().getGender());
        assertEquals(handle, userArgumentCaptor.getValue().getHandle());
        assertEquals(phone, userArgumentCaptor.getValue().getPhone());
        assertEquals(channel, userArgumentCaptor.getValue().getChannelSubscriptions().get(0));
        assertEquals(topic, userArgumentCaptor.getValue().getTopicPreferences().get(0));
    }

    @Test
    public void input_ValidInputAndGetNotRequired_ShouldCallBackWithResults() {
        final User userResult = new User();
        userResult.setId("userResult");

        doNothing().when(mockStmRequestProcessor).addObserver(any(StmObserver.class));
        UpdateUser updateUser = new UpdateUser(mockStmRequestProcessor, null);
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setHandle("handle");
        updateUser.update(updateUserRequest, "userId", false, new Callback<User>() {
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
