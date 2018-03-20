package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.StmObserver;

/**
 * The default HTTP request process for synchronous entity calls
 */

public class DefaultEntityRequestProcessorSync<T>
        extends StmHttpRequestBase implements StmRequestProcessor<StmBaseEntity> {

    private static final String TAG = DefaultEntityRequestProcessorSync.class.getSimpleName();
    private StmJsonRequestAdapter<StmBaseEntity> requestAdapter;
    private StmHttpResponseAdapter<T> responseAdapter;
    private final HttpAuthHeaderProvider httpAuthHeaderProvider;
    private StmUrlProvider urlProvider;
    private ArrayList<StmObserver> observers;

    public DefaultEntityRequestProcessorSync(StmJsonRequestAdapter<StmBaseEntity> requestAdapter,
                                             StmHttpResponseAdapter<T> responseAdapter,
                                             HttpAuthHeaderProvider httpAuthHeaderProvider,
                                             StmUrlProvider urlProvider) {
        this.requestAdapter = requestAdapter;
        this.responseAdapter = responseAdapter;
        this.httpAuthHeaderProvider = httpAuthHeaderProvider;
        this.urlProvider = urlProvider;
        observers = new ArrayList<>();
    }


    @Override
    public void processRequest(HttpMethod httpMethod, StmBaseEntity stmBaseEntity) {

        try {
            if (httpAuthHeaderProvider.getHeaderValue() == null || "".equals(httpAuthHeaderProvider.getHeaderValue())) {
                StmObservableResults stmObservableResults = new StmObservableResults();
                stmObservableResults.setError(true);
                stmObservableResults.setErrorMessage("Attempted to call Shout to Me service with invalid httpAuthHeaderProvider value");
                notifyObservers(stmObservableResults);
                return;
            }
        } catch (IllegalStateException ex) {
            StmObservableResults stmObservableResults = new StmObservableResults();
            stmObservableResults.setError(true);
            stmObservableResults.setErrorMessage("Illegal argument passed to HttpAuthHeaderProvider");
            notifyObservers(stmObservableResults);
            return;
        }

        T entity;

        HttpURLConnection connection;
        URL url;

        try {
            url = new URL(urlProvider.getUrl(stmBaseEntity, httpMethod));
            if (url.getProtocol().equals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod(httpMethod.toString());
            connection.addRequestProperty("Authorization", httpAuthHeaderProvider.getHeaderValue());
            connection.addRequestProperty("Content-Type", "application/json");

            String jsonDataString = "";
            if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
                connection.setDoOutput(true);
                if (requestAdapter != null) {
                    jsonDataString = requestAdapter.adapt(stmBaseEntity);
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
                final InputStream in = new BufferedInputStream(connection.getErrorStream());
                response = convertStreamToString(in);
                in.close();
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
