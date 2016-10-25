package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import me.shoutto.sdk.internal.PendingApiObjectChange;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class User extends StmBaseEntity {

    private String authToken;
    private Date lastReadMessagesDate;
    private String handle;
    private String platformEndpointArn;

    private boolean isInitialized = false;

    public User(StmService stmService) {
        super(stmService, "user", "/users");
    }

    boolean isInitialized() {
        return isInitialized;
    }

    void setIsInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        pendingChanges.put("handle", new PendingApiObjectChange("handle", handle, this.handle));
        this.handle = handle;
    }

    public Date getLastReadMessagesDate() {
        return lastReadMessagesDate;
    }

    public void setLastReadMessagesDate(Date lastReadMessagesDate) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        String lastReadMessagesDateString = df.format(lastReadMessagesDate);
        pendingChanges.put("last_read_messages_date",
                new PendingApiObjectChange("last_read_messages_date", lastReadMessagesDateString, null));

        this.lastReadMessagesDate = lastReadMessagesDate;
    }

    public String getPlatformEndpointArn() {
        return platformEndpointArn;
    }

    public void setPlatformEndpointArn(String platformEndpointArn) {
        pendingChanges.put("platform_endpoint_arn",
                new PendingApiObjectChange("platform_endpoing_arn", platformEndpointArn, this.platformEndpointArn));
        this.platformEndpointArn = platformEndpointArn;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        populateUserFieldsFromJson(jsonObject);
    }

    public static Type getSerializationType() {
        return new TypeToken<User>(){}.getType();
    }

    public void save(final StmCallback<User> callback) {

        if (pendingChanges.size() == 0) {
            return;
        }

        // Prepare request
        JSONObject userUpdateJson = new JSONObject();
        try {
            Set<Map.Entry<String, PendingApiObjectChange>> entrySet = pendingChanges.entrySet();
            for (Map.Entry<String, PendingApiObjectChange> entry : entrySet) {
                if (entry.getValue() != null) {
                    userUpdateJson.put(entry.getKey(), entry.getValue().getNewValue());
                }
            }
        } catch (JSONException ex) {
            Log.w(TAG, "Could not prepare JSON for user update request. Aborting", ex);
            callback.onError(new StmError("An error occurred trying to user",
                    false, StmError.SEVERITY_MINOR));
            return;
        }

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                User user = null;
                StmError stmError = null;
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONObject userJson = data.getJSONObject("user");
                    try {
                        user = populateUserFieldsFromJson(userJson);
                    } catch(JSONException ex) {
                        Log.w(TAG, "Unable to populate user fields from JSON", ex);
                        stmError = new StmError("An error occurred trying to user", false,
                                StmError.SEVERITY_MINOR);
                    }
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse user update response JSON", ex);
                    stmError = new StmError("An error occurred trying to user", false,
                            StmError.SEVERITY_MINOR);
                } finally {
                    if (callback != null) {
                        if (stmError != null) {
                            rollbackPendingChanges();
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(user);
                        }
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                rollbackPendingChanges();

                StmError stmError = new StmError();
                stmError.setSeverity(StmError.SEVERITY_MINOR);
                stmError.setBlockingError(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from user update response");
                    stmError.setMessage("An error occurred trying to create user");
                }
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        sendAuthorizedPutRequest(this, userUpdateJson, responseListener, errorListener);
    }

    void get(final StmCallback<User> callback) {

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                User user = null;
                StmError stmError = null;
                try {
                    // We don't update the ID or authToken here as that is handled elsewhere
                    JSONObject data = response.getJSONObject("data");
                    JSONObject userJson = data.getJSONObject("user");
                    try {
                        user = populateUserFieldsFromJson(userJson);
                    } catch(JSONException ex) {
                        Log.w(TAG, "Unable to populate user fields from JSON. "  + response.toString(), ex);
                        stmError = new StmError("Unable to populate user fields from JSON", false,
                                StmError.SEVERITY_MINOR);
                    }
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse response JSON. "  + response.toString(), ex);
                    stmError = new StmError("Unable to parse user update response JSON", false,
                            StmError.SEVERITY_MINOR);
                } finally {
                    pendingChanges.clear();
                    if (callback != null) {
                        if (stmError != null) {
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(user);
                        }
                    }
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "An error occurred trying to contact the Shout to Me API", error);
                StmError stmError = new StmError();
                stmError.setMessage("Error occurred loading user");
                stmError.setSeverity(StmError.SEVERITY_MAJOR);
                stmError.setBlockingError(true);
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        sendAuthorizedGetRequest("/users/me", responseListener, errorListener);
    }

    private User populateUserFieldsFromJson(JSONObject json) throws JSONException {

        // Handle is an optional field so we bury the exception
        try {
            handle = json.getString("handle");
        } catch (JSONException ex) {
            Log.i(TAG, "User does not have a handle set");
        }

        try {
            String lastReadMessagesDateString = json.getString("last_read_messages_date");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            lastReadMessagesDate = sdf.parse(lastReadMessagesDateString);
        } catch(ParseException ex) {
            Log.e(TAG, "Could not parse last read messages date", ex);
        } catch(JSONException ex) {
            // Ignore. It just may occur if message page was never viewed.
        }

        try {
            platformEndpointArn = json.getString("platform_endpoint_arn");
        } catch (JSONException ex) {
            // Ignore. Scenario occurs during first app launch.
        }

        return this;
    }

    private void rollbackPendingChanges() {
        Set<Map.Entry<String, PendingApiObjectChange>> entrySet = pendingChanges.entrySet();
        for (Map.Entry<String, PendingApiObjectChange> property: entrySet) {
            if (property.getKey().equals("handle")) {
                if (property.getValue().getOldValue() == null) {
                    handle = null;
                } else {
                    handle = property.getValue().getOldValue();
                }
            }
        }
        pendingChanges.clear();
    }
}
