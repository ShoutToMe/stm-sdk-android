package me.shoutto.sdk.internal;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.shoutto.sdk.Channel;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.internal.http.GsonDateAdapter;
import me.shoutto.sdk.internal.http.StmEntityListRequestSync;
import me.shoutto.sdk.internal.http.StmRequestQueue;

/**
 * ChannelManager provides methods to retrieve channel information.
 */
public class ChannelManager {

    private static final String TAG = ChannelManager.class.getSimpleName();
    private StmPreferenceManager stmPreferenceManager;

    public ChannelManager(Context context) {
        stmPreferenceManager = new StmPreferenceManager(context);
    }

    public void getChannels(final StmService stmService, final StmCallback<List<Channel>> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                List<Channel> channelList = null;
                StmError stmError = null;
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray channelArray = data.getJSONArray(Channel.LIST_SERIALIZATION_KEY);

                    RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                            .of(StmBaseEntity.class, StmBaseEntity.SERIALIZATION_FIELD)
                            .registerSubtype(Channel.class, Channel.SERIALIZATION_KEY);

                    Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                            .registerTypeAdapter(Date.class, new GsonDateAdapter())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    channelList = gson.fromJson(channelArray.toString(), Channel.getSerializationListType());

                    for (Channel channel : channelList) {
                        channel.setStmService(stmService);
                    }
                } catch (JSONException ex) {
                    Log.e(TAG, "Unable to parse channel array response JSON", ex);
                    stmError = new StmError("Unable to parse channel array response JSON", false,
                            StmError.SEVERITY_MAJOR);
                } catch (Exception ex) {
                    Log.e(TAG, "Unknown error occurred getting channels", ex);
                    stmError = new StmError("Unknown error occurred getting channels", false,
                            StmError.SEVERITY_MAJOR);
                } finally {
                    if (callback != null) {
                        if (stmError != null) {
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(channelList);
                        }
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "An error occurred trying to get channels", error);
                if (callback != null) {
                    StmError stmError = new StmError("Error occurred getting channels.",
                            false, StmError.SEVERITY_MAJOR);
                    callback.onError(stmError);
                }
            }
        };

        String url = stmPreferenceManager.getServerUrl() + Channel.BASE_ENDPOINT;
        final String authToken = stmPreferenceManager.getAuthToken();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + authToken);
                return params;
            }
        };
        StmRequestQueue.getInstance().addToRequestQueue(request);
    }

    List<Channel> getChannels() {
        StmEntityListRequestSync<Channel> channelRequest = new StmEntityListRequestSync<>();
        return channelRequest.process("GET", stmPreferenceManager.getAuthToken(),
                stmPreferenceManager.getServerUrl() + Channel.BASE_ENDPOINT, null,
                Channel.getSerializationListType(), Channel.LIST_SERIALIZATION_KEY);
    }
}
