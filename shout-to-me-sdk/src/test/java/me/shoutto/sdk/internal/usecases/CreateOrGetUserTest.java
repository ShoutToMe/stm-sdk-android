package me.shoutto.sdk.internal.usecases;

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

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.User;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * CreateOrGetUserTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class CreateOrGetUserTest {

    @Mock
    private StmRequestProcessor<StmBaseEntity> mockStmRequestProcessor;

    @Mock
    private StmCallback<User> mockCallback;

    @Captor
    private ArgumentCaptor<StmError> errorArgumentCaptor;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void createOrGet_WithNullUser_ShouldCallBackWithError() {
        CreateOrGetUser createOrGetUser = new CreateOrGetUser(mockStmRequestProcessor);
        createOrGetUser.createOrGet(null, mockCallback);

        verify(mockCallback, times(1)).onError(errorArgumentCaptor.capture());
        assertNotNull(errorArgumentCaptor.getValue());
        verify(mockCallback, times(0)).onResponse(any(User.class));
    }

    @Test
    public void createOrGet_WithValidUser_ShouldCallBackWithUser() {
        String USER_ID = "1234";

        User user = new User();
        user.setId(USER_ID);

        CreateOrGetUser createOrGetUser = new CreateOrGetUser(mockStmRequestProcessor);
        createOrGetUser.createOrGet(user, mockCallback);

        StmObservableResults<User> stmObservableResults = new StmObservableResults<>();
        stmObservableResults.setError(false);
        stmObservableResults.setResult(user);
        stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
        createOrGetUser.update(stmObservableResults);

        verify(mockCallback, times(0)).onError(any(StmError.class));
        verify(mockCallback, times(1)).onResponse(userArgumentCaptor.capture());
        assertNotNull(userArgumentCaptor.getValue());
    }
}