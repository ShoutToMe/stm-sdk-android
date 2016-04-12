package me.shoutto.sdk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tracyrojas on 4/11/16.
 */
public class Channels implements DownloadImageTask.ImageDownloadListener {

    private static final String TAG = "Channels";
    private final static String CHANNELS_ENDPOINT = "/channels";
    private List<Channel> channels;
    private StmService stmService;
    private String selectedChannelId;
    private int screenWidth;
    private boolean isInitialized = false;
    private Map<UUID, ChannelImageDownloadContext> downloadContextMap;

    public Channels(StmService stmService, int screenWidth) {
        this.stmService = stmService;
        this.screenWidth = screenWidth;
        this.downloadContextMap = new HashMap<>();
    }

    public void loadFromServer() {
        if (!isInitialized) {
            isInitialized = true;
            try {
                new GetChannelsAsyncTask().execute();
            } catch (Exception ex) {
                Log.e(TAG, "Could not load channels due to problem with user auth token", ex);
            }
        } else {
            sendChannelsReadyBroadcast();
        }
    }

    public List<Channel> getChannelList() {
        if (channels == null) {
            loadFromServer();
        }
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        if (this.channels != null) {
            this.channels.clear();
            this.channels = null;
        }
        this.channels = channels;
    }

    public String getSelectedChannelId() {
        return selectedChannelId;
    }

    public void setSelectedChannelId(String selectedChannelId) {
        this.selectedChannelId = selectedChannelId;
    }

    public Channel getSelectedChannel() {
        Channel selectedChannel = null;
        if (channels != null) {
            for (Channel channel : channels) {
                if (channel.getId().equals(selectedChannelId)) {
                    selectedChannel = channel;
                }
            }
        }
        return selectedChannel;
    }

    @Override
    public void onDownloadComplete(UUID contextId, Bitmap image) {
        ChannelImageDownloadContext downloadContext = downloadContextMap.get(contextId);
        for (Channel channel : channels) {
            if (channel.getId().equals(downloadContext.getChannelId())) {
                if (downloadContext.isListImage()) {
                    channel.setListImage(image);
                } else {
                    channel.setImage(image);
                }
            }
        }
        downloadContext.setDownloadCompleted();

        sendBroadcastIfDownloadComplete();
    }

    private synchronized void sendBroadcastIfDownloadComplete() {
        boolean isReadyToBroadcast = true;
        for (ChannelImageDownloadContext context : downloadContextMap.values()) {
            if (!context.isDownloadCompleted()) {
                isReadyToBroadcast = false;
            }
        }
        if (isReadyToBroadcast) {
            sendChannelsReadyBroadcast();
            downloadContextMap.clear();
        }
    }

    private void sendChannelsReadyBroadcast() {
        Intent intent = new Intent(StmService.CHANNELS_LOADED);
        LocalBroadcastManager.getInstance(stmService).sendBroadcast(intent);
    }

    public void reloadChannelImage() {
        if (getChannelList() != null && selectedChannelId != null) {
            for (final Channel channel : getChannelList()) {
                if (selectedChannelId.equals(channel.getId())) {

                    ChannelImageDownloadContext downloadContext =
                            new ChannelImageDownloadContext(channel.getId(), false);
                    final UUID downloadContextUUID = UUID.randomUUID();
                    downloadContextMap.put(downloadContextUUID, downloadContext);

                    Handler imageHandler = new Handler();
                    imageHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            new DownloadImageTask(Channels.this, downloadContextUUID, false, screenWidth)
                                    .execute(channel.getImageUrl());
                        }
                    });
                } else {
                    channel.setImage(null);
                }
            }
        }
    }

    private class GetChannelsAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        private boolean isUnauthorized = false;

        public List<Channel> doInBackground(Void... voids) {

            List<Channel> channels = getChannels();
            if (isUnauthorized) {
                Log.d(TAG, "Channel request was unauthorized. Attempting to refresh user token and try again.");
                try {
                    Channels.this.stmService.refreshUserAuthToken();
                    channels = getChannels();
                } catch (Exception ex) {
                    Log.d(TAG, "Could not refresh user auth token", ex);
                }
            }
            return channels;
        }

        @Override
        public void onPostExecute(final List<Channel> channels) {
            Channels.this.setChannels(channels);
            for (final Channel channel : Channels.this.getChannelList()) {

                ChannelImageDownloadContext listImageDownloadContext =
                        new ChannelImageDownloadContext(channel.getId(), true);
                final UUID listImageContextUUID = UUID.randomUUID();
                downloadContextMap.put(listImageContextUUID, listImageDownloadContext);

                Handler listImageHandler = new Handler();
                listImageHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        new DownloadImageTask(Channels.this, listImageContextUUID, true, screenWidth)
                                .execute(channel.getListImageUrl());
                    }
                });

                if (selectedChannelId != null && selectedChannelId.equals(channel.getId())) {
                    ChannelImageDownloadContext imageDownloadContext =
                            new ChannelImageDownloadContext(channel.getId(), false);
                    final UUID imageContextUUID = UUID.randomUUID();
                    downloadContextMap.put(imageContextUUID, imageDownloadContext);

                    Handler imageHandler = new Handler();
                    imageHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            new DownloadImageTask(Channels.this, imageContextUUID, false, screenWidth)
                                    .execute(channel.getImageUrl());
                        }
                    });
                }
            }
        }

        private List<Channel> getChannels() {
            List<Channel> channels = new ArrayList<>();

            HttpURLConnection connection;
            int responseCode = 0;
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
                }
            } catch (MalformedURLException ex) {
                Log.e(TAG, "Could not create URL for Shout to Me service", ex);
            } catch (IOException ex) {
                Log.e(TAG, "Could not connect to Shout to Me service", ex);
            } catch (Exception ex) {
                Log.e(TAG, "Error occurred in trying to send Shout to Shout to Me service", ex);
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

    private class ChannelImageDownloadContext {

        private String channelId;
        private boolean isListImage;
        private boolean downloadCompleted;

        ChannelImageDownloadContext(String channelId, boolean isListImage) {
            this.channelId = channelId;
            this.isListImage = isListImage;
            this.downloadCompleted = false;
        }

        public String getChannelId() {
            return channelId;
        }

        public boolean isListImage() {
            return isListImage;
        }

        public void setDownloadCompleted() {
            downloadCompleted = true;
        }

        public boolean isDownloadCompleted() {
            return downloadCompleted;
        }
    }
}
