package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmUser extends StmBaseEntity {

    private String authToken;
    private String handle;

    private boolean isInitialized = false;

    public StmUser(StmService stmService) {
        super(stmService, "StmUser", stmService.STM_BASE_API_URL + "/users");
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

    public void save(final StmCallback<StmUser> callback) {

        // Validate handle
        if (handle == null) {
            callback.onError(new StmError("Handle cannot be null", false, StmError.SEVERITY_MINOR));
            return;
        }
        if (handle.length() == 0) {
            callback.onError(new StmError("Handle cannot be an empty string", false, StmError.SEVERITY_MINOR));
            return;
        }

        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{1,15}$");
        Matcher matcher = pattern.matcher(handle);
        if (!matcher.matches()) {
            callback.onError(new StmError("Handle must only contain letters, digits, and "
                    + "underscore, and must not be longer than 15 characters", false, StmError.SEVERITY_MINOR));
            return;
        }

        // Prepare request
        JSONObject userUpdateJson = new JSONObject();
        try {
            userUpdateJson.put("handle", handle);
        } catch (JSONException ex) {
            Log.w(TAG, "Could not prepare JSON for user update request. Aborting", ex);
            callback.onError(new StmError("Could not prepare JSON for user update request. Aborting",
                    false, StmError.SEVERITY_MINOR));
            return;
        }

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                StmUser stmUser = null;
                StmError stmError = null;
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONObject user = data.getJSONObject("user");
                    try {
                        stmUser = populateUserFieldsFromJson(user);
                    } catch(JSONException ex) {
                        Log.w(TAG, "Unable to populate user fields from JSON", ex);
                        stmError = new StmError("Unable to populate user fields from JSON", false,
                                StmError.SEVERITY_MINOR);
                    }
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse user update response JSON", ex);
                    stmError = new StmError("Unable to parse user update response JSON", false,
                            StmError.SEVERITY_MINOR);
                } finally {
                    if (callback != null) {
                        if (stmError != null) {
                            rollbackPendingChanges();
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(stmUser);
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
                    stmError.setMessage("Cannot determine error from response");
                }
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        sendAuthorizedPutRequest(this, userUpdateJson, responseListener, errorListener);
    }

    void get(final StmCallback<StmUser> callback) {

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                StmUser stmUser = null;
                StmError stmError = null;
                try {
                    // We don't update the ID or authToken here as that is handled elsewhere
                    JSONObject data = response.getJSONObject("data");
                    JSONObject user = data.getJSONObject("user");
                    try {
                        stmUser = populateUserFieldsFromJson(user);
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
                    if (callback != null) {
                        if (stmError != null) {
                            callback.onError(stmError);
                        } else {
                            callback.onResponse(stmUser);
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

    private StmUser populateUserFieldsFromJson(JSONObject json) throws JSONException {

        // Handle is an optional field so we bury the exception
        try {
            handle = json.getString("handle");
        } catch (JSONException ex) {
            Log.i(TAG, "User does not have a handle set");
        }
        return this;
    }

    private void rollbackPendingChanges() {
        for (Map.Entry property: pendingChanges.entrySet()) {
            if (property.getKey().equals("handle")) {
                handle = property.getValue().toString();
            }
        }
        pendingChanges.clear();
    }
}
