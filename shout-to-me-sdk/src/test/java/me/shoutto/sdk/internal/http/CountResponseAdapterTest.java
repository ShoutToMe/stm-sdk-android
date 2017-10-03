package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * CountResponseAdapterTest
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class CountResponseAdapterTest {

    @Mock
    JSONObject mockJSONObject;

    @Test
    public void adapt_WhenFailStatus_ShouldLogErrorAndReturnNull() {
        PowerMockito.mockStatic(Log.class);

        JSONObject jsonObjectSpy = null;
        try {
            jsonObjectSpy = spy(new JSONObject("{ \"status\":\"fail\" }"));
            doReturn("fail").when(jsonObjectSpy).getString("status");
            doReturn("{ \"status\":\"fail\" }").when(jsonObjectSpy).toString();
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }

        CountResponseAdapter countResponseAdapter = new CountResponseAdapter();
        Integer response = countResponseAdapter.adapt(jsonObjectSpy);

        verifyStatic(times(1));
        Log.e(anyString(), anyString());

        assertEquals(response, null);
    }

    @Test
    public void adapt_WhenNoDataNode_ShouldLogErrorAndReturnNull() {
        PowerMockito.mockStatic(Log.class);

        JSONObject jsonObjectSpy = null;
        try {
            jsonObjectSpy = spy(new JSONObject("{ \"status\":\"success\" }"));
            doReturn("success").when(jsonObjectSpy).getString("status");
            doThrow(JSONException.class).when(jsonObjectSpy).getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }

        CountResponseAdapter countResponseAdapter = new CountResponseAdapter();
        Integer response = countResponseAdapter.adapt(jsonObjectSpy);

        verifyStatic(times(1));
        Log.e(anyString(), anyString(), any(JSONException.class));

        assertEquals(response, null);
    }

    @Test
    public void adapt_WithNotInteger_ShouldLogErrorAndReturnNull() {
        PowerMockito.mockStatic(Log.class);

        JSONObject jsonObjectSpy = null;
        try {
            jsonObjectSpy = spy(new JSONObject("{ \"status\":\"success\" }"));
            doReturn("success").when(jsonObjectSpy).getString("status");

            JSONObject dataNodeSpy = spy(new JSONObject("{\"count\": \"a string\"}"));
            doThrow(JSONException.class).when(dataNodeSpy).getInt("count");

            doReturn(dataNodeSpy).when(jsonObjectSpy).getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }

        CountResponseAdapter countResponseAdapter = new CountResponseAdapter();
        Integer response = countResponseAdapter.adapt(jsonObjectSpy);

        verifyStatic(times(1));
        Log.e(anyString(), anyString(), any(JSONException.class));

        assertEquals(response, null);
    }

    @Test
    public void adapt_WithValidJsonObject_ShouldReturnResult() {
        PowerMockito.mockStatic(Log.class);

        Integer count = 10;
        JSONObject jsonObjectSpy = null;
        try {
            jsonObjectSpy = spy(new JSONObject("{ \"status\":\"success\" }"));
            doReturn("success").when(jsonObjectSpy).getString("status");

            JSONObject dataNodeSpy = spy(new JSONObject("{\"count\": \"a string\"}"));
            doReturn(count).when(dataNodeSpy).getInt("count");

            doReturn(dataNodeSpy).when(jsonObjectSpy).getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }

        CountResponseAdapter countResponseAdapter = new CountResponseAdapter();
        Integer response = countResponseAdapter.adapt(jsonObjectSpy);

        assertEquals(response, count);
    }
}
