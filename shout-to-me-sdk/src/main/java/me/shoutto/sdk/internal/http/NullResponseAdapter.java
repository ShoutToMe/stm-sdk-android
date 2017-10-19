package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Response adapter that only returns null
 */

public class NullResponseAdapter implements StmHttpResponseAdapter<Void> {

    private static final String TAG = NullResponseAdapter.class.getSimpleName();
    private static final String SUCCESS = "success";

    @Override
    public Void adapt(JSONObject jsonObject) {

        try {
            String status = jsonObject.getString("status");
            if (!SUCCESS.equals(status)) {
                String errorMessage = String.format("Error occurred calling Shout to Me service. JSON response = %s", jsonObject.toString());
                Log.e(TAG, errorMessage);
            }
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONObject: %s", jsonObject.toString());
            Log.e(TAG, errorMessage, ex);
        }

        return null;
    }
}
