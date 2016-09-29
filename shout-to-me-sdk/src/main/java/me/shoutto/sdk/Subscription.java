package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Subscription represents a Shout to Me Subscription entity
 */
public class Subscription extends StmBaseEntity {

    public static final String BASE_ENDPOINT = "/subscriptions";
    public static final String OBJECT_JSON_KEY = "subscription";
    public static final String LIST_JSON_KEY = "subscriptions";
    public static final String SERIALIZATION_KEY = "subscription";
    private String channelId;
    private Date createdDate;

    public Subscription() { super(SERIALIZATION_KEY); }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    public String getChannelId() {
        return channelId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public static Type getSerializationType() {
        return new TypeToken<Subscription>(){}.getType();
    }

    public static Type getListSerializationType() {
        return new TypeToken<List<Subscription>>(){}.getType();
    }
}
