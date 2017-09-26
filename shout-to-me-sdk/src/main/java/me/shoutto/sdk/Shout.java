package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * This class represents a Shout to Me Shout object.
 */
public class Shout extends StmBaseEntity {

    /**
     * The base endpoint of shouts on the Shout to Me REST API.
     */
    public static final String BASE_ENDPOINT = "/shouts";
    /**
     * The key used for JSON serialization of conversation objects.
     */
    public static final String SERIALIZATION_KEY = "shout";

    private static final String TAG = Shout.class.getSimpleName();

    private byte[] audio;
    private String channelId;
    private String description;
    private String mediaFileUrl;
    private String tags;
    private String text;
    private String topic;
    private Integer recordingLengthInSeconds;

    public Shout(StmService stmService) {
        super(stmService, SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    Shout(StmService stmService, byte[] rawData) {
        this(stmService);
        this.audio = addHeaderToRawData(rawData);
    }

    public Shout() {
        super(SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    /**
     * This method is called internally to create a Shout object following a successful HTTP POST.
     * @param stmService The <code>StmService</code> used for context.
     * @param json The <code>JSONObject</code> from the response.
     */
    public Shout(StmService stmService, JSONObject json) {
        this(stmService);
        try {
            this.id = json.getString("id");
        } catch (JSONException ex) {
            Log.e(TAG, "Could not instantiate shout from JSON object");
        }
    }

    /**
     * Returns the byte array that contains the raw audio.
     * @return The raw audio.
     */
    public byte[] getAudio() {
        return audio;
    }

    /**
     * Adds a wav header onto the raw audio data
     * @param rawData - byte[]
     * @return byte[]
     */
    private byte[] addHeaderToRawData(byte[] rawData) {
        int totalDataLength = 36 + rawData.length; //44-8 since first part of header isnt included

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLength & 0xff);
        header[5] = (byte) ((totalDataLength  >> 8) & 0xff);
        header[6] = (byte) ((totalDataLength  >> 16) & 0xff);
        header[7] = (byte) ((totalDataLength  >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1 for PCM
        header[21] = 0;
        header[22] = (byte) 1;
        header[23] = 0;
        header[24] = (byte) (16000 & 0xff);
        header[25] = (byte) ((16000 >> 8) & 0xff);
        header[26] = (byte) ((16000 >> 16) & 0xff);
        header[27] = (byte) ((16000 >> 24) & 0xff);
        header[28] = (byte) (32000 & 0xff);
        header[29] = (byte) ((32000 >> 8) & 0xff);
        header[30] = (byte) ((32000 >> 16) & 0xff);
        header[31] = (byte) ((32000 >> 24) & 0xff);
        header[32] = (byte) (1 * 16 / 8);  // block align(channel*bitrate/8)
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (rawData.length  & 0xff);
        header[41] = (byte) ((rawData.length  >> 8) & 0xff);
        header[42] = (byte) ((rawData.length  >> 16) & 0xff);
        header[43] = (byte) ((rawData.length  >> 24) & 0xff);

        byte[] wavData = new byte[header.length + rawData.length];
        System.arraycopy(header, 0, wavData, 0, header.length);
        System.arraycopy(rawData, 0, wavData, header.length, rawData.length);

        return wavData;
    }

    /**
     * Gets the shout ID.
     * @return The shout ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sends a request to the Shout to Me REST API to delete the Shout. Generally used as a way
     * for the user to take back a newly created Shout.
     * @param stmCallback The callback to be executed or null.
     */
    public void delete(final StmCallback<String> stmCallback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String result;
                try {
                    result = response.getString("status");
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse response JSON", ex);
                    if (stmCallback != null) {
                        StmError stmError = new StmError();
                        stmError.setBlocking(false);
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setMessage("Error occurred during JSON parsing of delete shout response" +
                                " Error message: " + ex.getMessage());
                        stmCallback.onError(stmError);
                    }
                    return;
                }

                if (stmCallback != null) {
                    if (result.equals("success")) {
                        stmCallback.onResponse(StmService.SUCCESS);
                    } else {
                        StmError stmError = new StmError();
                        stmError.setBlocking(false);
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setMessage("Delete shout failed.  Response from server: " + result);
                        stmCallback.onError(stmError);
                    }
                }

            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                StmError stmError = new StmError();
                stmError.setSeverity(StmError.SEVERITY_MINOR);
                stmError.setBlocking(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from delete shout response");
                    stmError.setMessage("Cannot determine error from response");
                } finally {
                    if (stmCallback != null) {
                        stmCallback.onError(stmError);
                    }
                }
            }
        };
        sendAuthorizedDeleteRequest(BASE_ENDPOINT + "/" + id, responseListener, errorListener);
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediaFileUrl() {
        return mediaFileUrl;
    }

    public void setMediaFileUrl(String mediaFileUrl) {
        this.mediaFileUrl = mediaFileUrl;
    }

    /**
     * Gets the tags associated with the Shout.  The format of the tags is a comma separated list.
     * @return A String representing a comma separated list of tags.
     */
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the topic associated with the Shout.
     * @return The topic.
     */
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Gets the length of the recording in seconds.
     * @return The length of the recording in seconds.
     */
    public Integer getRecordingLengthInSeconds() {
        return recordingLengthInSeconds;
    }

    void setRecordingLengthInSeconds(int recordingLengthInSeconds) {
        this.recordingLengthInSeconds = recordingLengthInSeconds;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    /**
     * Gets the serialization type that is used in Gson parsing.
     * @return The serialization type to be used in Gson parsing.
     */
    @SuppressWarnings("unused")
    public static Type getSerializationType() {
        return new TypeToken<Shout>(){}.getType();
    }

    @Override
    public Type getEntitySerializationType() {
        return Shout.getSerializationType();
    }
}