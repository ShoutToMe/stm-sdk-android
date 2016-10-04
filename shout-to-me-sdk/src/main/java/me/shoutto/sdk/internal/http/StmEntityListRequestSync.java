package me.shoutto.sdk.internal.http;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import me.shoutto.sdk.Conversation;
import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.Subscription;

/**
 * This class GETs, POSTs, or PUTs a list of entities from the Shout to Me API
 */
public class StmEntityListRequestSync<T extends StmBaseEntity> extends StmHttpRequestBase {

    private static final String TAG = StmEntityListRequestSync.class.getSimpleName();

    /**
     * Main method to process the entity HTTP request
     * @param method a String that defines the HTTP method to be used for the request
     * @param authToken the String that represents the user's Shout to Me auth token
     * @param serverUrl the String that represents the full server URL endpoint
     * @param bodyJsonString the String that represents the JSON body parameters
     * @param serializationListType the Type used for Gson list deserialization. Can be found in the deserialized class
     * @param responseObjectKey the String name that is used as the key for the object in the JSON response
     * @return a list of StmBaseEntity subclass objects
     */
    public List<T> process(String method, String authToken, String serverUrl, String bodyJsonString,
                           Type serializationListType, String responseObjectKey) {

        List<T> entityList = null;

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
                    Log.e(TAG, "Response status was " + responseJson.getString("status"));
                } else {
                    JSONArray jsonArray = responseJson.getJSONObject("data").getJSONArray(responseObjectKey);
                    RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                            .of(StmBaseEntity.class, StmBaseEntity.SERIALIZATION_FIELD)
                            .registerSubtype(Conversation.class, Conversation.SERIALIZATION_KEY)
                            .registerSubtype(Message.class, Message.SERIALIZATION_KEY)
                            .registerSubtype(Subscription.class, Subscription.SERIALIZATION_KEY);

                    Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                            .registerTypeAdapter(Date.class, new GsonDateAdapter())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();

                    entityList = gson.fromJson(jsonArray.toString(), serializationListType);
                }
            }  catch (Exception ex) {
                Log.e(TAG, "Could not process request.", ex);
            }
        } else {
            Log.w(TAG, "Cannot process request due to no persisted authToken authToken=" + authToken);
        }

        return entityList;
    }
}
