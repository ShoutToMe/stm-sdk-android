package me.shoutto.sdk.internal.http;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.StmObserver;

/**
 * Volley based async HTTP request processor
 */

public class DefaultAsyncEntityRequestProcessor<T> implements StmEntityRequestProcessor {

    private static final String TAG = DefaultAsyncEntityRequestProcessor.class.getSimpleName();
    private ArrayList<StmObserver> observers;
    private StmEntityJsonRequestAdapter requestAdapter;
    private StmRequestQueue requestQueue;
    private StmHttpResponseAdapter<T> responseAdapter;
    private StmService stmService;
    private StmUrlProvider urlProvider;

    public DefaultAsyncEntityRequestProcessor(StmEntityJsonRequestAdapter stmHttpRequestAdapter,
                                              StmRequestQueue stmRequestQueue,
                                              StmHttpResponseAdapter<T> stmHttpResponseAdapter,
                                              StmService stmService,
                                              StmUrlProvider stmUrlProvider) {
        observers = new ArrayList<>();
        requestAdapter = stmHttpRequestAdapter;
        requestQueue = stmRequestQueue;
        responseAdapter = stmHttpResponseAdapter;
        this.stmService = stmService;
        urlProvider = stmUrlProvider;
    }

    @Override
    public void processRequest(final HttpMethod httpMethod, final StmBaseEntity stmBaseEntity) {
        String jsonDataString = "";
        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            if (requestAdapter != null) {
                jsonDataString = requestAdapter.adapt(stmBaseEntity);
            }
        }

        String url = urlProvider.getUrl(stmBaseEntity, httpMethod);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                T entity = responseAdapter.adapt(response);
                StmObservableResults<T> stmObservableResults = new StmObservableResults<>();
                stmObservableResults.setError(false);
                stmObservableResults.setResult(entity);
                stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
                notifyObservers(stmObservableResults);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse.statusCode == 404) {
                    StmObservableResults<T> stmObservableResults = new StmObservableResults<>();
                    stmObservableResults.setError(false);
                    stmObservableResults.setResult(null);
                    stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
                    notifyObservers(stmObservableResults);
                } else {
                    Log.w(TAG, "An error occurred calling Shout to Me service. " + error.getMessage());
                    StmObservableResults stmObservableResults = new StmObservableResults();
                    stmObservableResults.setError(true);
                    stmObservableResults.setErrorMessage("An error occurred calling Shout to Me service. " + error.getMessage());
                    notifyObservers(stmObservableResults);
                }
            }
        };

        JSONObject jsonDataObject = null;
        try {
            if (!"".equals(jsonDataString)) {
                jsonDataObject = new JSONObject(jsonDataString);
            }
        } catch (JSONException ex) {
            StmObservableResults stmObservableResults = new StmObservableResults();
            stmObservableResults.setError(true);
            stmObservableResults.setErrorMessage("An error occurred parsing JSON string. Aborting request. JSON:" + jsonDataString);
            notifyObservers(stmObservableResults);
            return;
        }

        final String authToken = stmService.getUserAuthToken();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                adaptHttpMethod(httpMethod),
                url,
                jsonDataObject,
                responseListener,
                errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + authToken);
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        requestQueue.addToRequestQueue(jsonObjectRequest);
    }

    private int adaptHttpMethod(HttpMethod httpMethod) {
        if (HttpMethod.DELETE.equals(httpMethod)) {
            return Request.Method.DELETE;
        } else if (HttpMethod.GET.equals(httpMethod)) {
            return Request.Method.GET;
        } else if (HttpMethod.POST.equals(httpMethod)) {
            return Request.Method.POST;
        } else if (HttpMethod.PUT.equals(httpMethod)) {
            return Request.Method.PUT;
        } else {
            return Request.Method.GET;
        }
    }

    @Override
    public void addObserver(StmObserver o) {
        observers.add(o);
    }

    @Override
    public void deleteObserver(StmObserver o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(StmObservableResults stmObservableResults) {
        for (StmObserver o : observers) {
            o.update(stmObservableResults);
        }
    }
}
