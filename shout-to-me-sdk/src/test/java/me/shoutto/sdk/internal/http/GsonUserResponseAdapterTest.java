package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import me.shoutto.sdk.User;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * GsonUserResponseAdapterTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class GsonUserResponseAdapterTest {

    @Mock
    private JSONObject mockUserJsonObject;

    @Mock
    private JSONObject mockDataJsonObject;

    @Mock
    private JSONObject mockRootJsonObject;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void adapt_WithFailResponse_ShouldReturnNull() {

        try {
            when(mockRootJsonObject.getString("status")).thenReturn("fail");
            GsonUserResponseAdapter gsonUserResponseAdapter = new GsonUserResponseAdapter();
            User user = gsonUserResponseAdapter.adapt(mockRootJsonObject);
            assertNull(user);
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void adapt_WithSuccessResponse_ShouldReturnUser() {

        String userId = "1234";
        String authToken = "auth_token";

        try {
            when(mockUserJsonObject.getString("id")).thenReturn(userId);
            when(mockUserJsonObject.toString()).thenReturn(String.format("{\"id\": \"%s\"}", userId));
            when(mockDataJsonObject.getJSONObject("user")).thenReturn(mockUserJsonObject);
            when(mockDataJsonObject.getString("auth_token")).thenReturn(authToken);
            when(mockRootJsonObject.getString("status")).thenReturn("success");
            when(mockRootJsonObject.getJSONObject("data")).thenReturn(mockDataJsonObject);

            GsonUserResponseAdapter gsonUserResponseAdapter = new GsonUserResponseAdapter();
            User user = gsonUserResponseAdapter.adapt(mockRootJsonObject);

            assertEquals(userId, user.getId());
            assertEquals(authToken, user.getAuthToken());
        } catch (Exception ex) {
            fail();
        }
    }
}