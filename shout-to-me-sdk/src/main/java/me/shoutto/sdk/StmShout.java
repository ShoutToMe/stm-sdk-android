package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmShout extends StmBaseEntity {

    private static final String TAG = "StmShout";
    private byte[] audio;

    StmShout(StmService stmService, byte[] rawData) {
        super(stmService, TAG, "/shouts");
        this.audio = addHeaderToRawData(rawData);
    }

    StmShout(StmService stmService, JSONObject json) {
        super(stmService, TAG, "/shouts");
        try {
            this.id = json.getString("id");
        } catch (JSONException ex) {
            Log.e(TAG, "Could not instantiate shout from JSON object");
        }
    }

    public byte[] getAudio() {
        return audio;
    }

    private void createPostBody() {
        JSONObject postBody = new JSONObject();
    }

    /**
     * Adds a wav header onto the raw data we get from wit
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toJSONString() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
        } catch (JSONException ex) {
            Log.e(TAG, "Error converting shout object to JSON.  ID=" + id);
        }
        return json.toString();
    }

    public void delete(final StmCallback<String> stmCallback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String result = "";
                try {
                    result = response.getString("status");
                } catch(JSONException ex) {
                    Log.e(TAG, "Unable to parse response JSON", ex);
                    if (stmCallback != null) {
                        StmError stmError = new StmError();
                        stmError.setBlockingError(false);
                        stmError.setSeverity(StmError.SEVERITY_MINOR);
                        stmError.setMessage("Error occurred during JSON parsing of delete shout response" +
                                " Error message: " + ex.getMessage());
                        stmCallback.onError(stmError);
                    }
                    return;
                }

                if (stmCallback != null) {
                    if (result.equals("success")) {
                        stmCallback.onResponse("success");
                    } else {
                        StmError stmError = new StmError();
                        stmError.setBlockingError(false);
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
                stmError.setBlockingError(false);
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
        sendAuthorizedDeleteJsonObjectRequest(responseListener, errorListener);
    }
}