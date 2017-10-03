package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * Parse a response from the Shout to Me service that contains a "count" integer in the "data" JSON node
 */

public class CountResponseAdapter implements StmHttpResponseAdapter<Integer> {

    private static final String TAG = CountResponseAdapter.class.getSimpleName();
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String DATA = "data";
    private static final String COUNT = "count";

    @Override
    public Integer adapt(JSONObject jsonObject, String serializationKey, Type typeOfT) {
        Integer count = null;

        try {
            String status = jsonObject.getString(STATUS);
            if (!SUCCESS.equals(status)) {
                String errorMessage = String.format("Error occurred calling Shout to Me service. JSON response = %s", jsonObject.toString());
                Log.e(TAG, errorMessage);
                return null;
            }

            JSONObject dataNode = jsonObject.getJSONObject(DATA);
            count = dataNode.getInt(COUNT);
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONObject: %s", ex.getMessage());
            Log.e(TAG, errorMessage, ex);
        }

        return count;
    }
}
