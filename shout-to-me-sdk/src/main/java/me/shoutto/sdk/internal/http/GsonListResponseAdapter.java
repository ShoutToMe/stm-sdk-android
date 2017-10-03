package me.shoutto.sdk.internal.http;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;

/**
 * Gson adapter for converting JSON from Shout to Me service to Shout to Me a list of entity objects
 */

public class GsonListResponseAdapter<T extends List, U extends StmBaseEntity> implements StmHttpResponseAdapter<T> {

    private static final String TAG = GsonListResponseAdapter.class.getSimpleName();
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String DATA = "data";
    private String listSerializationKey;
    private String objectSerializationKey;
    private Type typeOfT;
    private Class<U> aClass;

    public GsonListResponseAdapter(String listSerializationKey, String objectSerializationKey, Type typeOfT, Class<U> aClass) {
        this.listSerializationKey = listSerializationKey;
        this.objectSerializationKey = objectSerializationKey;
        this.typeOfT = typeOfT;
        this.aClass = aClass;
    }

    @Override
    public T adapt(JSONObject jsonObject) {

        T list = null;

        try {
            String status = jsonObject.getString(STATUS);
            if (!SUCCESS.equals(status)) {
                String errorMessage = String.format("Error occurred calling Shout to Me service. JSON response = %s", jsonObject.toString());
                Log.e(TAG, errorMessage);
                return null;
            }

            JSONObject dataNode = jsonObject.getJSONObject(DATA);
            JSONArray jsonArray = dataNode.getJSONArray(listSerializationKey);

            RuntimeTypeAdapterFactory<StmBaseEntity> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                    .of(StmBaseEntity.class, StmBaseEntity.SERIALIZATION_FIELD)
                    .registerSubtype(aClass, objectSerializationKey);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                    .registerTypeAdapter(Date.class, new GsonDateAdapter())
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            list = gson.fromJson(jsonArray.toString(), typeOfT);
        } catch (JSONException ex) {
            String errorMessage = String.format("Error occurred parsing JSONArray of type %s. %s", listSerializationKey, ex.getMessage());
            Log.e(TAG, errorMessage, ex);
        }

        return list;
    }
}
