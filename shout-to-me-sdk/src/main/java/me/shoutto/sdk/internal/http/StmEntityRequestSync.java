package me.shoutto.sdk.internal.http;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;

/**
 * This class is used to make HTTP requests for Shout to Me entity objects. Note that
 * use of this class without going through StmService may result in partial object trees.
 */
public class StmEntityRequestSync<T extends StmBaseEntity> {

    private static final String TAG = "StmEntityRequestSync";

    /**
     * Main method to process the entity HTTP request
     * @param method a String that defines the HTTP method to be used for the request
     * @param authToken the String that represents the user's Shout to Me auth token
     * @param serverUrl the String that represents the full server URL endpoint
     * @param bodyJsonString the String that represents the JSON body parameters
     * @param serializationType the Type used for Gson deserialization. Can be found in the deserialized class
     * @param responseObjectKey the String name that is used as the key for the object in the JSON response
     * @return a subclass of StmBaseEntity that is passed in as the parameterized type T
     */
    public T process(String method, String authToken, String serverUrl, String bodyJsonString,
                      Type serializationType, String responseObjectKey) {

        if (!"".equals(authToken)) {
            HttpURLConnection connection;
            URL url;

            try {
                url = new URL(serverUrl);
                if (url.getProtocol().equals("https")) {
                    connection = (HttpsURLConnection) url.openConnection();
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }

                connection.setRequestMethod(method);
                connection.addRequestProperty("Authorization", "Bearer " + authToken);
                connection.addRequestProperty("Content-Type", "application/json");

                if (method.equals("POST") || method.equals("PUT")) {
                    connection.setDoOutput(true);
                    if (bodyJsonString != null) {
                        connection.setFixedLengthStreamingMode(bodyJsonString.getBytes().length);
                    }
                } else {
                    connection.setDoOutput(false);
                }

                connection.connect();

                if (method.equals("POST") || method.equals("PUT")) {
                    if (bodyJsonString != null) {
                        OutputStream outStream = connection.getOutputStream();
                        outStream.write(bodyJsonString.getBytes());
                        outStream.close();
                    }
                }

                int responseCode = connection.getResponseCode();

                String response;
                if (responseCode == 200) {
                    final InputStream in = new BufferedInputStream(connection.getInputStream());
                    response = convertStreamToString(in);
                    in.close();
                } else {
                    final InputStream in = new BufferedInputStream(connection.getErrorStream());
                    response = convertStreamToString(in);
                    in.close();
                }

                JSONObject responseJson = new JSONObject(response);
                if (!responseJson.getString("status").equals("success")) {
                    Log.e(TAG, "Response status was " + responseJson.getString("status") + ". "
                            + responseJson.toString());
                } else {
                    JSONObject responseObject = responseJson.getJSONObject("data").getJSONObject(responseObjectKey);

                    RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                            .of(StmBaseEntity.class, StmBaseEntity.SERIALIZATION_FIELD)
                            .registerSubtype(Message.class, Message.SERIALIZATION_KEY);

                    Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                            .registerTypeAdapter(Date.class, new GsonDateAdapter())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    return gson.fromJson(responseObject.toString(), serializationType);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Could not process request.", ex);
            }
        } else {
            Log.w(TAG, "Cannot process request due to no persisted authToken authToken=" + authToken);
        }
        return null;
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        is.close();

        return sb.toString();
    }
}
