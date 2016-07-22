package me.shoutto.sdk.http;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import me.shoutto.sdk.Conversation;
import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmBaseEntityList;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.gson.DateAdapter;

/**
 * Created by tracyrojas on 6/3/16.
 */
public class GetApiObjectsAsyncTask<T extends StmBaseEntity> extends AsyncTask<String, Void, Void> {

    private static final String TAG = "GetApiObjectsAsyncTask";
    private boolean isUnauthorized = false;
    private StmBaseEntityList<T> stmBaseEntityList;
    private StmError stmError;
    private boolean countOnly;

    public GetApiObjectsAsyncTask(StmBaseEntityList<T> stmBaseEntityList, boolean countOnly) {
        this.stmBaseEntityList = stmBaseEntityList;
        this.countOnly = countOnly;
    }

    @Override
    public Void doInBackground(String... strings) {
        String urlString = strings[0];
        processRequest(urlString);
        if (isUnauthorized) {
            Log.d(TAG, stmBaseEntityList.getBaseEndpoint()
                    + " request was unauthorized. Attempting to refresh user token and try again.");
            try {
                stmBaseEntityList.getStmService().refreshUserAuthToken();
                processRequest(urlString);
            } catch (Exception ex) {
                Log.d(TAG, "Could not refresh user auth token. ", ex);
                stmError = new StmError("Could not refresh user auth token. " + ex.getMessage(),
                        true, StmError.SEVERITY_MAJOR);
            }
        }
        return null;
    }

    @Override
    public void onPostExecute(Void voids) {
        if (countOnly) {
            stmBaseEntityList.executeCountCallback(stmError);
        } else {
            stmBaseEntityList.executeListCallback(stmError);
        }
    }

    private void processRequest(String urlString) {

        HttpURLConnection connection;
        int responseCode;
        try {
            String userAuthToken = stmBaseEntityList.getStmService().getUserAuthToken();
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.addRequestProperty("Authorization", "Bearer " + userAuthToken);
            responseCode = connection.getResponseCode();

            String response = "";
            try {
                if (responseCode == 200) {
                    final InputStream in = new BufferedInputStream(connection.getInputStream());
                    response = convertStreamToString(in);
                    in.close();
                } else {
                    final InputStream in = new BufferedInputStream(connection.getErrorStream());
                    response = convertStreamToString(in);
                    in.close();
                }
            } finally {
                connection.disconnect();
            }

            try {
                if (responseCode == 401) {
                    isUnauthorized = true;
                } else if (responseCode != 200) {
                    throw new Exception(response);
                } else {
                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.getString("status").equals("success")) {
                        if (countOnly) {
                            int count = responseJson.getJSONObject("data").getInt("count");
                            stmBaseEntityList.setCount(count);
                        } else {
                            JSONArray jsonArray = responseJson.getJSONObject("data").getJSONArray(Message.LIST_JSON_KEY);

                            RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                                    .of(StmBaseEntity.class, "serializationType")
                                    .registerSubtype(Conversation.class, Conversation.SERIALIZATION_KEY)
                                    .registerSubtype(Message.class, Message.SERIALIZATION_KEY);

                            Gson gson = new GsonBuilder()
                                    .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                                    .registerTypeAdapter(Date.class, new DateAdapter())
                                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                                    .create();

                            List<T> objectList = gson.fromJson(jsonArray.toString(),
                                    stmBaseEntityList.getSerializationListType());

                            stmBaseEntityList.setList(objectList);
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Could not parse " + stmBaseEntityList.getBaseEndpoint() + " from JSON", ex);
                stmError = new StmError("Could not parse " + stmBaseEntityList.getBaseEndpoint()
                        + " from JSON" + ex.getMessage(),
                        true, StmError.SEVERITY_MAJOR);
            }
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Could not create URL for Shout to Me service", ex);
            stmError = new StmError("Could not create URL for Shout to Me service. " + ex.getMessage(),
                    true, StmError.SEVERITY_MAJOR);
        } catch (IOException ex) {
            Log.e(TAG, "Could not connect to Shout to Me service", ex);
            stmError = new StmError("Could not connect to Shout to Me service. " + ex.getMessage(),
                    true, StmError.SEVERITY_MAJOR);
        } catch (Exception ex) {
            Log.e(TAG, "Error occurred in " + stmBaseEntityList.getBaseEndpoint()
                    + " call to Shout to Me service", ex);
            stmError = new StmError("Error occurred in " + stmBaseEntityList.getBaseEndpoint()
                    + " call to Shout to Me service. " + ex.getMessage(),
                    true, StmError.SEVERITY_MAJOR);
        }
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
