package me.shoutto.sdk.internal.http;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import me.shoutto.sdk.User;

/**
 * Adapts JSON response for user to a User object. Custom adaptation required due to auth_token field.
 */

public class GsonUserResponseAdapter implements StmHttpResponseAdapter<User> {

    private static final String TAG = GsonUserResponseAdapter.class.getSimpleName();

    @Override
    public User adapt(JSONObject jsonObject) {

        GsonObjectResponseAdapter<User> gsonObjectResponseAdapter
                = new GsonObjectResponseAdapter<>(User.SERIALIZATION_KEY, User.getSerializationType());
        User user = gsonObjectResponseAdapter.adapt(jsonObject);

        if (user == null) {
            return null;
        }

        try {
            JSONObject dataNode = jsonObject.getJSONObject("data");
            String authToken = dataNode.getString("auth_token");
            user.setAuthToken(authToken);
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONObject of User. %s", ex.getMessage());
            Log.e(TAG, errorMessage, ex);
        }

        return user;
    }
}
