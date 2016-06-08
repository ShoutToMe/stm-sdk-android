package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmBaseEntity {

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

    String getSingleResourceEndpoint() {
        return stmService.getServerUrl() + baseEndpoint + "/:id";
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    protected void sendAuthorizedGetRequest(final StmBaseEntity entity,
                                            final Response.Listener<JSONObject> responseListener,
                                            final Response.ErrorListener errorListener) {
        stmService.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String authToken = stmService.getUserAuthToken();
                    String url = entity.getSingleResourceEndpoint().replace(":id", entity.getId());
                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url,
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

    protected void sendAuthorizedDeleteJsonObjectRequest(Response.Listener<JSONObject> responseListener,
                                                         Response.ErrorListener errorListener) {

        try {
            final String authToken = stmService.getUserAuthToken();
            JsonObjectRequest deleteObjectRequest = new JsonObjectRequest(Request.Method.DELETE,
                    stmService.getServerUrl() + baseEndpoint + "/" + id,
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
