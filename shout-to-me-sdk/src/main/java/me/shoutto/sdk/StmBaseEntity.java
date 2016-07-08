package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tracyrojas on 9/20/15.
 */
abstract class StmBaseEntity {

    protected StmService stmService;
    protected final String TAG;
    private String baseEndpoint;
    protected Map<String, PendingApiObjectChange> pendingChanges;
    protected String id;


    protected StmBaseEntity(StmService stmService, String tag, String baseEndpoint) {
        this.stmService = stmService;
        this.TAG = tag;
        this.baseEndpoint = baseEndpoint;
        pendingChanges = new HashMap<>();
    }

    public String getSingleResourceEndpoint() {
        return stmService.getServerUrl() + baseEndpoint + "/:id";
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public Map<String, PendingApiObjectChange> getPendingChanges() {
        return pendingChanges;
    }

    abstract protected void adaptFromJson(JSONObject jsonObject) throws JSONException;

    /**
     * Synchronous version of create base entity. Should only be run in background threads.
     * @return StmError
     */
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

    protected void sendAuthorizedGetRequest(final String urlSuffix,
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

    protected void sendAuthorizedPostRequest(final String urlSuffix,
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

    protected void sendAuthorizedPutRequest(final StmBaseEntity entity, final JSONObject data,
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

    protected void sendAuthorizedDeleteRequest(String urlSuffix,
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
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Authorization", "Bearer " + authToken);
                    return params;
                }
            };

            StmRequestQueue.getInstance().addToRequestQueue(deleteObjectRequest);

        } catch (Exception ex) {
            Log.e(TAG, "Error occurred getting user's auth token. Aborting request", ex);
        }
    }

    protected Response.ErrorListener createErrorListener(final String message) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, new String(error.networkResponse.data));
                Log.e(TAG, message, error);
            }
        };
    }
}
