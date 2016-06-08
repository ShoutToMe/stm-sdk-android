package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class User extends StmBaseEntity {

    private String authToken;
    private String handle;
    private Date lastReadMessagesDate;

    private boolean isInitialized = false;

    public User(StmService stmService) {
        super(stmService, "User", "/users");
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

    void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        pendingChanges.put("handle", this.handle);
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
        pendingChanges.put("last_read_messages_date", lastReadMessagesDateString);

        this.lastReadMessagesDate = lastReadMessagesDate;
    }

    public void save(final StmCallback<User> callback) {

        if (pendingChanges.size() == 0) {
            return;
        }

        // Prepare request
        JSONObject userUpdateJson = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : pendingChanges.entrySet()) {
                userUpdateJson.put(entry.getKey(), entry.getValue());
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
                    stmError.setMessage("An error occurred trying to save user");
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
                        Log.w(TAG, "Unable to populate user fields from JSON", ex);
                        stmError = new StmError("Unable to populate user fields from JSON", false,
                                StmError.SEVERITY_MINOR);
                    }
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse response JSON", ex);
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
            }
        };

        sendAuthorizedGetRequest(this, responseListener, errorListener);
    }

    private User populateUserFieldsFromJson(JSONObject json) throws JSONException {

        // Handle is an optional field so we bury the exception
        try {
            handle = json.getString("handle");
        } catch (JSONException ex) {
            Log.i(TAG, "User does not have a handle set");
        }


        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            lastReadMessagesDate = sdf.parse(json.getString("last_read_messages_date"));
        } catch(ParseException ex) {
            Log.e(TAG, "Could not parse date", ex);
            Log.w(TAG, "Could not parse date: " + json.getString("last_read_messages_date"));
        }


        return this;
    }

    private void rollbackPendingChanges() {
        for (Map.Entry property: pendingChanges.entrySet()) {
            if (property.getKey().equals("handle")) {
                if (property.getValue() == null) {
                    handle = null;
                } else {
                    handle = property.getValue().toString();
                }
            }
        }
        pendingChanges.clear();
    }
}
