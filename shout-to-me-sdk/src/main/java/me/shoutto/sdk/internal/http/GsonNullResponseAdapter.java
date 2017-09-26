package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * Response adapter that only returns null
 */

public class GsonNullResponseAdapter<T> implements StmHttpResponseAdapter<T> {

    private static final String TAG = GsonNullResponseAdapter.class.getSimpleName();
    private static final String SUCCESS = "success";

    @Override
    public T adapt(JSONObject jsonObject, String serializationKey, Type typeOfT) {

        try {
            String status = jsonObject.getString("status");
            if (!SUCCESS.equals(status)) {
                String errorMessage = String.format("Error occurred calling Shout to Me service. JSON response = %s", jsonObject.toString());
                Log.e(TAG, errorMessage);
            }
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONObject of type %s. %s", serializationKey, ex.getMessage());
            Log.e(TAG, errorMessage, ex);
        }

        return null;
    }
}
