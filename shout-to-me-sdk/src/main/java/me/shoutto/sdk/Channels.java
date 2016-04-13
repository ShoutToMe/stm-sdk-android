package me.shoutto.sdk;

import android.os.AsyncTask;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tracyrojas on 4/11/16.
 */
class Channels {

    private static final String TAG = "Channels";
    private final static String CHANNELS_ENDPOINT = "/channels";
    private List<Channel> channels;
    private StmService stmService;

    public Channels(StmService stmService, final StmCallback<List<Channel>> callback) {
        this.stmService = stmService;
        loadFromServer(callback);
    }

    private void loadFromServer(final StmCallback<List<Channel>> callback) {
        try {
            new GetChannelsAsyncTask(callback).execute();
        } catch (Exception ex) {
            Log.e(TAG, "Could not load channels due to problem with user auth token", ex);
        }
    }

    private void setChannels(List<Channel> channels) {
        if (this.channels != null) {
            this.channels.clear();
            this.channels = null;
        }
        this.channels = channels;
    }

    public Channel getChannel(String channelId) {
        Channel selectedChannel = null;
        if (channels != null) {
            for (Channel channel : channels) {
                if (channel.getId().equals(channelId)) {
                    selectedChannel = channel;
                }
            }
        }
        return selectedChannel;
    }

    private class GetChannelsAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        private boolean isUnauthorized = false;
        private StmCallback<List<Channel>> callback;
        private StmError stmError;

        public GetChannelsAsyncTask(StmCallback<List<Channel>> callback) {
            this.callback = callback;
        }

        public List<Channel> doInBackground(Void... voids) {

            List<Channel> channels = getChannels();
            if (isUnauthorized) {
                Log.d(TAG, "Channel request was unauthorized. Attempting to refresh user token and try again.");
                try {
                    Channels.this.stmService.refreshUserAuthToken();
                    channels = getChannels();
                } catch (Exception ex) {
                    Log.d(TAG, "Could not refresh user auth token. ", ex);
                    stmError = new StmError("Could not refresh user auth token. " + ex.getMessage(),
                            true, StmError.SEVERITY_MAJOR);
                }
            }
            return channels;
        }

        @Override
        public void onPostExecute(final List<Channel> channels) {
            Channels.this.setChannels(channels);
            if (stmError != null) {
                callback.onError(stmError);
            } else {
                callback.onResponse(channels);
            }
        }

        private List<Channel> getChannels() {
            List<Channel> channels = new ArrayList<>();

            HttpURLConnection connection;
            int responseCode;
            try {
                String userAuthToken = Channels.this.stmService.getUserAuthToken();
                URL url = new URL(stmService.getServerUrl() + CHANNELS_ENDPOINT);
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
                    } else {
                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.getString("status").equals("success")) {
                            JSONArray channelsJson = responseJson.getJSONObject("data").getJSONArray("channels");
                            for (int i = 0; i < channelsJson.length(); i++) {
                                JSONObject channelJson = channelsJson.getJSONObject(i);
                                Channel channel = new Channel();
                                channel.setId(channelJson.getString("id"));
                                channel.setName(channelJson.getString("name"));
                                channel.setDescription(channelJson.getString("description"));
                                channel.setImageUrl(channelJson.getString("channel_image"));
                                channel.setListImageUrl(channelJson.getString("channel_list_image"));
                                try {
                                    channel.setDefaultMaxRecordingLengthSeconds(channelJson.getInt("default_voigo_max_recording_length_seconds"));
                                } catch (JSONException ex) {
                                    Log.i(TAG, "Channel does not have default max recording length");
                                }
                                channels.add(channel);
                            }
                        }
                    }
                } catch (JSONException ex) {
                    Log.e(TAG, "Could not parse get user with client token response JSON", ex);
                    stmError = new StmError("Could not parse get user with client token response JSON. " + ex.getMessage(),
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
                Log.e(TAG, "Error occurred in trying to send Shout to Shout to Me service", ex);
                stmError = new StmError("Error occurred in trying to send Shout to Shout to Me service. " + ex.getMessage(),
                        true, StmError.SEVERITY_MAJOR);
            }

            return channels;
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
}
