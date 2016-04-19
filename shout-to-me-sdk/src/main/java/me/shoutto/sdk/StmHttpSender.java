package me.shoutto.sdk;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by tracyrojas on 9/20/15.
 */
class StmHttpSender {

    private static final String TAG = "StmHttpSender";
    private static final String ANONYMOUS_USER_PATH = "/users/skip";
    private StmService stmService;

    public StmHttpSender(StmService stmService) {
        this.stmService = stmService;
    }

    public Shout postNewShout(Shout shout) throws Exception {

        Shout shoutFromResponse = null;
        HttpURLConnection connection;
        int responseCode = 0;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("audio", new String(Base64.encode(shout.getAudio(), Base64.NO_WRAP)) ); //No_wrap to get rid of \n
            params.put("channel_id", stmService.getChannelId());
            if (shout.getTags() != null) {
                params.put("tags", shout.getTags());
            }
            if (shout.getTopic() != null) {
                params.put("topic", shout.getTopic());
            }
            String requestString = buildRequestString(params);

            URL url = new URL(stmService.getServerUrl() + "/shouts");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Authorization", "Bearer " + stmService.getUserAuthToken());
            connection.addRequestProperty("Content-Type", "application/json");
            connection.setFixedLengthStreamingMode(requestString.getBytes().length);
            connection.connect();

            OutputStream outStream = connection.getOutputStream();
            outStream.write(requestString.getBytes());
            outStream.close();

            responseCode = connection.getResponseCode();
            Log.d(TAG, String.valueOf(responseCode));

            String response = "";
            if (responseCode == 200) {
                final InputStream in = new BufferedInputStream(connection.getInputStream());
                response = convertStreamToString(in);
                in.close();
            } else {
                final InputStream in = new BufferedInputStream(connection.getErrorStream());
                response = convertStreamToString(in);
                in.close();
                throw new Exception("Error occurred in create shout server call. " + response);
            }
            connection.disconnect();

            try {
                JSONObject shoutResponseJson = new JSONObject(response);
                if (shoutResponseJson.getString("status").equals("success")) {
                    shoutFromResponse = new Shout(stmService, shoutResponseJson.getJSONObject("data").getJSONObject("shout"));
                }
                Log.d(TAG, "here i am in the httpsender reponse");
            } catch (JSONException ex) {
                Log.e(TAG, "Could not parse create shout response JSON", ex);
            }
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Could not create URL for Shout to Me service", ex);
            throw(ex);
        } catch (IOException ex) {
            Log.e(TAG, "Could not connect to Shout to Me service", ex);
            throw(ex);
        } catch (Exception ex) {
            Log.e(TAG, "Error occurred in trying to send Shout to Shout to Me service", ex);
            throw(ex);
        }

        return shoutFromResponse;
    }

    public void getUserWithClientToken(User user) throws Exception {

        HttpURLConnection connection;
        int responseCode = 0;
        try {
            String requestString = buildRequestString(null);

            URL url = new URL(stmService.getServerUrl() + ANONYMOUS_USER_PATH);
            Log.d(TAG, url.getProtocol());
            if (url.getProtocol().equals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Authorization", "Basic " + stmService.getAccessToken());
            connection.addRequestProperty("Content-Type", "application/json");
            connection.setFixedLengthStreamingMode(requestString.getBytes().length);
            connection.connect();

            OutputStream outStream = connection.getOutputStream();
            outStream.write(requestString.getBytes());
            outStream.close();

            responseCode = connection.getResponseCode();

            String response = "";
            try {
                if (responseCode == 200) {
                    final InputStream in = new BufferedInputStream(connection.getInputStream());
                    response = convertStreamToString(in);
                    Log.d(TAG, response);
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
                JSONObject responseJson = new JSONObject(response);
                Log.d(TAG, responseJson.toString());
                if (responseJson.getString("status").equals("success")) {
                    user.setAuthToken(responseJson
                            .getJSONObject("data")
                            .getString("auth_token"));
                    user.setId(responseJson.getJSONObject("data")
                            .getJSONObject("user")
                            .getString("id"));
                    stmService.setChannelId(responseJson
                            .getJSONObject("data")
                            .getJSONObject("user")
                            .getJSONObject("affiliate")
                            .getJSONObject("default_channel")
                            .getString("id"));
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Could not parse get user with client token response JSON", ex);
            }
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Could not create URL for Shout to Me service", ex);
            throw(ex);
        } catch (IOException ex) {
            Log.e(TAG, "Could not connect to Shout to Me service", ex);
            throw(ex);
        } catch (Exception ex) {
            Log.e(TAG, "Error occurred in trying to send Shout to Shout to Me service", ex);
            throw(ex);
        }
    }

    private String buildRequestString(Map<String, String> params) {
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("device_id", stmService.getDeviceId());
            requestJson.put("lat", stmService.getLocationServicesClient().getLatitude());
            requestJson.put("lon", stmService.getLocationServicesClient().getLongitude());
            if (params != null) {
                for (Map.Entry param: params.entrySet()) {
                    requestJson.put(param.getKey().toString(), param.getValue().toString());
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Error occurred trying to construct Shout to Me JSON request", ex);
        }
        Log.d(TAG, requestJson.toString());
        return requestJson.toString();
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
