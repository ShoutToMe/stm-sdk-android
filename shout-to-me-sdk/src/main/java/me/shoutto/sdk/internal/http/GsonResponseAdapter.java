package me.shoutto.sdk.internal.http;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;

import me.shoutto.sdk.StmBaseEntity;

/**
 * GsonAdapter for converting JSON from Shout to Me service to Shout to Me entity objects
 */

public class GsonResponseAdapter<T extends StmBaseEntity> implements StmHttpResponseAdapter<T> {

    private static final String TAG = GsonResponseAdapter.class.getSimpleName();

    @Override
    public T adapt(JSONObject jsonObject, String serializationKey, Type typeOfT) {

        T obj = null;
        try {
            JSONObject dataNode = jsonObject.getJSONObject("data");
            JSONObject objNode = dataNode.getJSONObject(serializationKey);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new GsonDateAdapter())
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            obj = gson.fromJson(objNode.toString(), typeOfT);
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONObject of type %s. %s", serializationKey, ex.getMessage());
            Log.e(TAG, errorMessage, ex);
        }

        return obj;
    }
}
