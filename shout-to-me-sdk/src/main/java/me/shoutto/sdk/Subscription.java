package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * This class represents a Shout to Me Subscription entity.
 */
public class Subscription extends StmBaseEntity {

    /**
     * The base endpoint of subscriptions on the Shout to Me REST API.
     */
    public static final String BASE_ENDPOINT = "/subscriptions";

    /**
     * The key used for JSON serialization of subscription objects.
     */
    public static final String SERIALIZATION_KEY = "subscription";

    /**
     * The key used for JSON serialization of subscription lists.
     */
    @SuppressWarnings("all")
    public static final String LIST_SERIALIZATION_KEY = SERIALIZATION_KEY + "s";

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
