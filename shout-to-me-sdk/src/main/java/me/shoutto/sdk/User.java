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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import me.shoutto.sdk.internal.PendingApiObjectChange;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

/**
 * This class represents a Shout to Me user entity. A Shout to Me User entity is generally used
 * by client apps to enable push notifications, get and set various properties and preferences,
 * and to access the user's auth token in the event the client app wants to call the Shout to Me REST API directly.
 */
public class User extends StmBaseEntity {

    /**
     * The base endpoint of users on the Shout to Me REST API.
     */
    public static final String BASE_ENDPOINT = "/users";

    /**
     * The key used for JSON serialization of conversation objects.
     */
    public static final String SERIALIZATION_KEY = "user";

    private String authToken;
    private List<String> channelSubscriptions;
    private String deviceId;
    private String email;
    private String handle;
    private String phone;
    private String platformEndpointArn;
    private Boolean platformEndpointEnabled;
    private List<String> topicPreferences;
    private Locations locations;
    private MetaInfo metaInfo;

    private transient boolean isInitialized = false;

    /**
     * A constructor that allows setting <code>StmService</code> which is used for context and other
     * SDK functionality.
     * @param stmService the Shout to Me service
     */
    public User(StmService stmService) {
        super(stmService, SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    public User() { super(SERIALIZATION_KEY, BASE_ENDPOINT); }

    public boolean isInitialized() {
        return isInitialized;
    }

    void setIsInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    public String getAuthToken() {
        return authToken;
    }

    /**
     * Sets the auth token to be used in requests to the Shout to Me service.
     * @param authToken the auth token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public List<String> getChannelSubscriptions() {
        return channelSubscriptions;
    }

    public void setChannelSubscriptions(List<String> channelSubscriptions) {
        this.channelSubscriptions = channelSubscriptions;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the user's email address
     * @return The user's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address
     * @param email The user's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's handle.
     * @return The user's handle.
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Sets the user's handle.
     * @param handle The user's handle.
     */
    public void setHandle(String handle) {
        pendingChanges.put("handle", new PendingApiObjectChange("handle", handle, this.handle));
        this.handle = handle;
    }

    /**
     * Gets the <code>Date</code> of the last time the user read messages.
     * @return The <code>Date</code> representing the last time the user read their messages.
     *
     * @deprecated This property is no longer used
     */
    @SuppressWarnings("unused")
    @Deprecated
    public Date getLastReadMessagesDate() {
        return null;
    }

    /**
     * Sets the <code>Date</code> of the last time the user read their messages.
     * @param lastReadMessagesDate The <code>Date</code> representing the last time the user read their messages.
     *
     * @deprecated This property is no longer used
     */
    @Deprecated
    public void setLastReadMessagesDate(Date lastReadMessagesDate) {
        // Stubbed for backwards compatibility but does not do anything
    }

    public Locations getLocations() {
        return locations;
    }

    public void setLocations(Locations locations) {
        this.locations = locations;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    /**
     * Gets the user's phone number
     * @return The user's phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number
     * @param phone The user's phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the platform endpoint ARN. This is the unique ID that the Shout to Me system uses to
     * send push notifications.
     * @return The platform endpoint ARN.
     */
    public String getPlatformEndpointArn() {
        return platformEndpointArn;
    }

    void setPlatformEndpointArn(String platformEndpointArn) {
        pendingChanges.put("platform_endpoint_arn",
                new PendingApiObjectChange("platform_endpoing_arn", platformEndpointArn, this.platformEndpointArn));
        this.platformEndpointArn = platformEndpointArn;
    }

    public Boolean getPlatformEndpointEnabled() {
        return platformEndpointEnabled;
    }

    public void setPlatformEndpointEnabled(Boolean platformEndpointEnabled) {
        this.platformEndpointEnabled = platformEndpointEnabled;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        populateUserFieldsFromJson(jsonObject);
    }

    /**
     * Gets the serialization type that is used in Gson parsing.
     * @return The serialization type to be used in Gson parsing.
     */
    @SuppressWarnings("unused")
    public static Type getSerializationType() {
        return new TypeToken<User>(){}.getType();
    }

    @Override
    public Type getEntitySerializationType() {
        return User.getSerializationType();
    }

    public List<String> getTopicPreferences() {
        return topicPreferences;
    }

    public void setTopicPreferences(List<String> topicPreferences) {
        this.topicPreferences = topicPreferences;
    }

    /**
     * Sends an update request to save the <code>User</code> object to the Shout to Me platform.
     * @deprecated This method has been moved to {@link StmService#updateUser(UpdateUserRequest, StmCallback)}
     * @param callback The callback to be executed on completion of the request or null.
     */
    @Deprecated
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
                stmError.setBlocking(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from user update response");
                    stmError.setMessage("An error occurred trying to PUT user");
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
                stmError.setBlocking(true);
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

    public static class MetaInfo {

        private String gender;
        private String operatingSystem;
        private String operatingSystemVersion;

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getOperatingSystem() {
            return operatingSystem;
        }

        public void setOperatingSystem(String operatingSystem) {
            this.operatingSystem = operatingSystem;
        }

        public String getOperatingSystemVersion() {
            return operatingSystemVersion;
        }

        public void setOperatingSystemVersion(String operatingSystemVersion) {
            this.operatingSystemVersion = operatingSystemVersion;
        }
    }

    static class Locations {

        private Location location;
        private Date date;
        private Float metersSinceLastUpdate;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public Float getMetersSinceLastUpdate() {
            return metersSinceLastUpdate;
        }

        public void setMetersSinceLastUpdate(Float metersSinceLastUpdate) {
            this.metersSinceLastUpdate = metersSinceLastUpdate;
        }

        static class Location {
            String type;
            Double[] coordinates;
            String radius;

            public Location(Double[] coordinates) {
                this.coordinates = coordinates;
                type = "circle";
                radius = Float.toString(GeofenceManager.GEOFENCE_RADIUS_IN_METERS);
            }

            public Double[] getCoordinates() {
                return coordinates;
            }

            public float getRadius() {
                return Float.parseFloat(radius);
            }

            public String getType() {
                return type;
            }
        }
    }
}
