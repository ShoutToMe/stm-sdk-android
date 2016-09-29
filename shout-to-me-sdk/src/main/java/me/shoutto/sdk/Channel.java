package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

/**
 * The Channel class represents a Shout to Me Channel.
 */
public class Channel extends StmBaseEntity {

    public transient static final String BASE_ENDPOINT = "/channels";
    public transient static final String LIST_JSON_KEY = "channels";
    public transient static final String SERIALIZATION_KEY = "channel";
    public transient static final int GLOBAL_DEFAULT_MAX_RECORDING_TIME = 15;

    private transient static final String TAG = "Channel";
    private String name;
    private String description;
    @SerializedName("channel_image")
    private String imageUrl;
    @SerializedName("channel_list_image")
    private String listImageUrl;
    @SerializedName("default_voigo_max_recording_length_seconds")
    private int defaultMaxRecordingLengthSeconds;

    public Channel(StmService stmService) {
        super(stmService, TAG, BASE_ENDPOINT);
        defaultMaxRecordingLengthSeconds = 0;
    }

    public Channel() {
        super(SERIALIZATION_KEY);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getListImageUrl() {
        return listImageUrl;
    }

    public int getDefaultMaxRecordingLengthSeconds() {
        if (defaultMaxRecordingLengthSeconds == 0) {
            return GLOBAL_DEFAULT_MAX_RECORDING_TIME;
        } else {
            return defaultMaxRecordingLengthSeconds;
        }
    }

    public static Type getSerializationType() {
        return new TypeToken<Channel>(){}.getType();
    }

    public static Type getSerializationListType() { return new TypeToken<List<Channel>>(){}.getType(); }

    public void subscribe(final StmCallback<Void> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {
                        callback.onResponse(null);
                    } else {
                        Log.w(TAG, "Channel subscribe response was not 'success'");
                        StmError stmError = new StmError();
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setBlockingError(false);
                        stmError.setMessage("Channel subscribe response was not 'success'");
                        callback.onError(stmError);
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "Could not parse channel subscribe response.", ex);
                    StmError stmError = new StmError();
                    stmError.setSeverity(StmError.SEVERITY_MINOR);
                    stmError.setBlockingError(false);
                    stmError.setMessage("Could not parse channel subscribe response.");
                    callback.onError(stmError);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                StmError stmError = new StmError();
                stmError.setSeverity(StmError.SEVERITY_MINOR);
                stmError.setBlockingError(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from channel subscribe response");
                    stmError.setMessage("An error occurred trying to subscribe to channel.");
                }
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        sendAuthorizedPostRequest(BASE_ENDPOINT + "/" + id + "/subscriptions", null, responseListener, errorListener);
    }

    public void unsubscribe(final StmCallback<Void> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {
                        callback.onResponse(null);
                    } else {
                        Log.w(TAG, "Channel unsubscribe response was not 'success'");
                        StmError stmError = new StmError();
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setBlockingError(false);
                        stmError.setMessage("Channel unsubscribe response was not 'success'");
                        callback.onError(stmError);
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "Could not parse channel unsubscribe response.", ex);
                    StmError stmError = new StmError();
                    stmError.setSeverity(StmError.SEVERITY_MINOR);
                    stmError.setBlockingError(false);
                    stmError.setMessage("Could not parse channel unsubscribe response.");
                    callback.onError(stmError);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                StmError stmError = new StmError();
                stmError.setSeverity(StmError.SEVERITY_MINOR);
                stmError.setBlockingError(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from channel subscribe response");
                    stmError.setMessage("An error occurred trying to subscribe to channel.");
                }
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        sendAuthorizedDeleteRequest(BASE_ENDPOINT + "/" + id + "/subscriptions", responseListener, errorListener);
    }

    public void isSubscribed(final StmCallback<Boolean> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (!"success".equals(status)) {
                        Log.w(TAG, "Channel isSubscribe status does not equal 'success'");
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "Could not parse channel isSubscribed response.", ex);
                }
                callback.onResponse(true);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (callback != null) {
                    if (error == null || error.networkResponse == null) {
                        StmError stmError = new StmError();
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setBlockingError(false);
                        stmError.setMessage("Error occurred trying to get channel isSubscribed status.");
                        callback.onError(stmError);
                    } else if (error.networkResponse.statusCode == 404) {
                        callback.onResponse(false);
                    } else {
                        StmError stmError = new StmError();
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setBlockingError(false);
                        try {
                            JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                            stmError.setMessage(responseData.getString("message"));
                        } catch (JSONException ex) {
                            Log.e(TAG, "Error parsing JSON from channel isSubscribed response");
                            stmError.setMessage("An error occurred trying to get channel isSubscribed status.");
                        }
                        callback.onError(stmError);
                    }
                }
            }
        };

        sendAuthorizedGetRequest(BASE_ENDPOINT + "/" + id + "/subscriptions", responseListener, errorListener);
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        // Stubbed
    }
}

