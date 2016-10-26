package me.shoutto.sdk;

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

import me.shoutto.sdk.internal.http.GsonDateAdapter;
import me.shoutto.sdk.internal.http.StmRequestQueue;

class MessageManager {

    private static final String TAG = MessageManager.class.getSimpleName();
    private StmService stmService;

    MessageManager(StmService stmService) {
        this.stmService = stmService;
    }

    void getMessages(final StmCallback<List<Message>> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                List<Message> messageList = null;
                StmError stmError = null;
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray messageArray = data.getJSONArray(Message.LIST_SERIALIZATION_KEY);

                    RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                            .of(StmBaseEntity.class, StmBaseEntity.SERIALIZATION_FIELD)
                            .registerSubtype(Message.class, Message.SERIALIZATION_KEY);

                    Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                            .registerTypeAdapter(Date.class, new GsonDateAdapter())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    messageList = gson.fromJson(messageArray.toString(), Message.getSerializationListType());
                } catch (JSONException ex) {
                    Log.e(TAG, "Unable to parse message array response JSON", ex);
                    stmError = new StmError("Unable to parse message array response JSON", false,
                            StmError.SEVERITY_MAJOR);
                } catch (Exception ex) {
                    Log.e(TAG, "Unknown error occurred getting messages", ex);
                    stmError = new StmError("Unknown error occurred getting messages", false,
                            StmError.SEVERITY_MAJOR);
                } finally {
                    if (callback != null) {
                        if (stmError != null) {
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(messageList);
                        }
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "An error occurred trying to get messages", error);
                if (callback != null) {
                    StmError stmError = new StmError("Error occurred getting messages.",
                            false, StmError.SEVERITY_MAJOR);
                    callback.onError(stmError);
                }
            }
        };

        String url = stmService.getServerUrl() + Message.BASE_ENDPOINT;
        final String authToken = stmService.getUserAuthToken();
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

    void getUnreadMessageCount(final StmCallback<Integer> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Integer count = null;
                StmError stmError = null;
                try {
                    JSONObject data = response.getJSONObject("data");
                    count = data.getInt("count");
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse get message count response JSON", ex);
                    stmError = new StmError("Unable to parse user update response JSON", false,
                            StmError.SEVERITY_MINOR);
                } catch (Exception ex) {
                    Log.e(TAG, "Unknown error occurred getting unread message count", ex);
                    stmError = new StmError("Unknown error occurred getting unread message count", false,
                            StmError.SEVERITY_MINOR);
                } finally {
                    if (callback != null) {
                        if (stmError != null) {
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(count);
                        }
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "An error occurred trying to get unread message count", error);
                if (callback != null) {
                    StmError stmError = new StmError("Error occurred getting unread message count.",
                            false, StmError.SEVERITY_MINOR);
                    callback.onError(stmError);
                }
            }
        };

        String url = stmService.getServerUrl() + Message.BASE_ENDPOINT + "?count_only=true&unread=true";
        final String authToken = stmService.getUserAuthToken();
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
}
