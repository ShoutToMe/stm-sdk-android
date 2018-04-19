package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.StmObserver;

/**
 * A synchronous request processor to process a list of entities
 */

public class EntityListRequestProcessorSync<T, S> extends StmHttpRequestBase implements StmRequestProcessor<S> {

    private static final String TAG = EntityListRequestProcessorSync.class.getSimpleName();
    private StmJsonRequestAdapter<S> requestAdapter;
    private StmHttpResponseAdapter<T> responseAdapter;
    private final String authToken;
    private StmUrlProvider urlProvider;
    private ArrayList<StmObserver> observers;

    public EntityListRequestProcessorSync(StmJsonRequestAdapter<S> requestAdapter,
                                          StmHttpResponseAdapter<T> responseAdapter,
                                          String authToken,
                                          StmUrlProvider urlProvider) {
        this.requestAdapter = requestAdapter;
        this.responseAdapter = responseAdapter;
        this.authToken = authToken;
        this.urlProvider = urlProvider;
        observers = new ArrayList<>();
    }

    @Override
    public void processRequest(HttpMethod httpMethod, S stmBaseEntities) {
        if (authToken == null || "".equals(authToken)) {
            StmObservableResults stmObservableResults = new StmObservableResults();
            stmObservableResults.setError(true);
            stmObservableResults.setErrorMessage("Attempted to call Shout to Me service with invalid authToken");
            notifyObservers(stmObservableResults);
            return;
        }

        T entity = null;

        HttpURLConnection connection;
        URL url;

        try {
            url = new URL(urlProvider.getUrl(null, httpMethod));
            if (url.getProtocol().equals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod(httpMethod.toString());
            connection.addRequestProperty("Authorization", "Bearer " + authToken);
            connection.addRequestProperty("Content-Type", "application/json");

            String jsonDataString = "";
            if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                connection.setDoOutput(true);
                if (requestAdapter != null) {
                    jsonDataString = requestAdapter.adapt(stmBaseEntities);
                    connection.setFixedLengthStreamingMode(jsonDataString.getBytes().length);
                }
            } else {
                connection.setDoOutput(false);
            }

            connection.setConnectTimeout(30000);
            connection.connect();

            if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                OutputStream outStream = connection.getOutputStream();
                outStream.write(jsonDataString.getBytes());
                outStream.close();
            }

            int responseCode = connection.getResponseCode();

            String response;
            if (responseCode == 200) {
                final InputStream in = new BufferedInputStream(connection.getInputStream());
                response = convertStreamToString(in);
                in.close();
            } else if (responseCode == 404) {
                StmObservableResults<T> stmObservableResults = new StmObservableResults<>();
                stmObservableResults.setError(false);
                stmObservableResults.setResult(null);
                stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
                notifyObservers(stmObservableResults);
                return;
            } else {
                InputStream in;

                in = connection.getErrorStream();

                if (null == in) {
                    in = connection.getInputStream();
                }

                if (null == in) {
                    response = "Unknown error occurred.";
                } else {
                    response = convertStreamToString(in);
                }

                if (!(null == in)) {
                    in.close();
                }

                Log.w(TAG, "Error info: " + response);
            }

            JSONObject responseJson = new JSONObject(response);
            if (!responseJson.getString("status").equals("success")) {
                Log.e(TAG, "Response status was " + responseJson.getString("status") + ". "
                        + responseJson.toString());

                StmObservableResults stmObservableResults = new StmObservableResults();
                stmObservableResults.setError(true);
                stmObservableResults.setErrorMessage("An error was received from the Shout to Me service" + responseJson.toString());
                notifyObservers(stmObservableResults);
            } else {
                entity = responseAdapter.adapt(responseJson);
                StmObservableResults<T> stmObservableResults = new StmObservableResults<>();
                stmObservableResults.setError(false);
                stmObservableResults.setResult(entity);
                stmObservableResults.setStmObservableType(StmObservableType.STM_SERVICE_RESPONSE);
                notifyObservers(stmObservableResults);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error.", ex);
            StmObservableResults stmObservableResults = new StmObservableResults();
            stmObservableResults.setError(true);
            stmObservableResults.setErrorMessage("An error occurred calling the Shout to Me service. " + ex.getMessage());
            notifyObservers(stmObservableResults);
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
    public void notifyObservers(StmObservableResults stmObserverResults) {
        for (StmObserver o : observers) {
            o.update(stmObserverResults);
        }
    }
}
