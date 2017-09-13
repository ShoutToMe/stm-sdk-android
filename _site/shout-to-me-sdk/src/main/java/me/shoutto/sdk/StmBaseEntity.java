package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import me.shoutto.sdk.internal.PendingApiObjectChange;
import me.shoutto.sdk.internal.http.StmRequestQueue;

/**
 * The base class for Shout to Me entities.  Used internally by the Shout to ME SDK.
 */
public abstract class StmBaseEntity {

    /**
     * The serialization field used by Gson parsing.
     */
    public static final String SERIALIZATION_FIELD = "serializationType";

    protected transient StmService stmService;
    protected transient final String TAG;
    transient Map<String, PendingApiObjectChange> pendingChanges;
    protected String id;
    @SuppressWarnings("all")
    protected transient String serializationType;
    private transient String baseEndpoint;

    protected StmBaseEntity(StmService stmService, String tag, String baseEndpoint) {
        this.stmService = stmService;
        this.TAG = tag;
        this.baseEndpoint = baseEndpoint;
        pendingChanges = new HashMap<>();
    }

    protected StmBaseEntity(String serializationType) {
        TAG = this.getClass().getName();
        this.serializationType = serializationType;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    /**
     * Returns a String that represents the Shout to Me REST API endpoint for a specific entity.
     * @return A URI for a single entity.
     */
    public String getSingleResourceEndpoint() {
        return stmService.getServerUrl() + baseEndpoint + "/:id";
    }

    /**
     * Gets the ID of the entity.
     * @return The ID of the entity.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the entity.  Generally used internally by the SDK following a Shout to Me API
     * create request.
     * @param id The ID of the entity.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the <code>StmService</code> reference for an entity for context.
     * @param stmService The StmService instance.
     */
    public void setStmService(StmService stmService) {
        this.stmService = stmService;
    }

    /**
     * Gets a list of pending changes to an object.  Generally used by the SDK to build a Shout to
     * Me API request.
     * @return A <code>Map</code> of the pending changes to the object.
     */
    public Map<String, PendingApiObjectChange> getPendingChanges() {
        return pendingChanges;
    }

    abstract protected void adaptFromJson(JSONObject jsonObject) throws JSONException;

    abstract public Type getEntitySerializationType();

    public String getSerializationKey() {
        return TAG;
    }

    /**
     * Synchronous version of create base entity. Should only be run in background threads.
     * @return StmError
     */
    @SuppressWarnings("all")
    synchronized public StmError save() {
        StmError stmError = null;
        try {
            JSONObject responseObject = stmService.getStmHttpSender().putEntityObject(this);

            if (responseObject != null && !responseObject.getString("status").equals("success")) {
                return new StmError("A server error occurring trying to create object " + getClass().toString()
                        , false, StmError.SEVERITY_MAJOR);
            }

            adaptFromJson(responseObject.getJSONObject("data").getJSONObject(TAG.toLowerCase()));
        } catch(Exception ex) {
            Log.e(TAG, "Could not PUT base entity", ex);
            stmError = new StmError("Error occurred saving " + this.getClass().toString(),
                    false, StmError.SEVERITY_MAJOR);
        }

        return stmError;
    }

    void sendAuthorizedGetRequest(final String urlSuffix,
                                            final Response.Listener<JSONObject> responseListener,
                                            final Response.ErrorListener errorListener) {
        stmService.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String authToken = stmService.getUserAuthToken();
                    String url = stmService.getServerUrl() + urlSuffix;
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
                } catch (Exception ex) {
                    Log.e(TAG, "An error occurred building the GET request. Aborting.", ex);
                }
            }
        });
    }

    void sendAuthorizedPostRequest(final String urlSuffix,
                                             final JSONObject data,
                                             final Response.Listener<JSONObject> responseListener,
                                             final Response.ErrorListener errorListener) {
        stmService.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String authToken = stmService.getUserAuthToken();
                    String url = stmService.getServerUrl() + urlSuffix;
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data,
                            responseListener, errorListener) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("Authorization", "Bearer " + authToken);
                            return params;
                        }
                    };
                    StmRequestQueue.getInstance().addToRequestQueue(request);
                } catch (Exception ex) {
                    Log.e(TAG, "An error occurred building the GET request. Aborting.", ex);
                }
            }
        });
    }

    void sendAuthorizedPutRequest(final StmBaseEntity entity, final JSONObject data,
                                            final Response.Listener<JSONObject> responseListener,
                                            final Response.ErrorListener errorListener) {

        stmService.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String authToken = stmService.getUserAuthToken();
                    String url = entity.getSingleResourceEndpoint().replace(":id", entity.getId());
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, data,
                            responseListener, errorListener) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("Authorization", "Bearer " + authToken);
                            return params;
                        }
                    };
                    StmRequestQueue.getInstance().addToRequestQueue(request);
                } catch (Exception ex) {
                    Log.e(TAG, "An error occurred building the PUT request. Aborting.", ex);
                }

            }
        });
    }

    void sendAuthorizedDeleteRequest(String urlSuffix,
                                               Response.Listener<JSONObject> responseListener,
                                               Response.ErrorListener errorListener) {

        try {
            final String authToken = stmService.getUserAuthToken();
            JsonObjectRequest deleteObjectRequest = new JsonObjectRequest(Request.Method.DELETE,
                    stmService.getServerUrl() + urlSuffix,
                    new JSONObject(),
                    responseListener,
                    errorListener) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", "Bearer " + authToken);
                    return params;
                }
            };

            StmRequestQueue.getInstance().addToRequestQueue(deleteObjectRequest);

        } catch (Exception ex) {
            Log.e(TAG, "Error occurred getting user's auth token. Aborting request", ex);
        }
    }
}
