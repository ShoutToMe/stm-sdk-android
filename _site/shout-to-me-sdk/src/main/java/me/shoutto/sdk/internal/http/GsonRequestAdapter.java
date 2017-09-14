package me.shoutto.sdk.internal.http;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Gson adapter for converting Shout to Me entities to JSON objects
 */

public class GsonRequestAdapter implements StmHttpRequestAdapter {
    @Override
    public String adapt(StmBaseEntity entity) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        return gson.toJson(entity, entity.getClass());
    }
}
