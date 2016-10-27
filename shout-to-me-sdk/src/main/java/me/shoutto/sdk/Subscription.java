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

    /**
     * The default constructor.
     */
    public Subscription() { super(SERIALIZATION_KEY); }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    public String getChannelId() {
        return channelId;
    }

    /**
     * Gets the <code>Date</code> the subscription was created.
     * @return The <code>Date</code> subscription was created.
     */
    @SuppressWarnings("unused")
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the serialization type that is used in Gson parsing.
     * @return The serialization type to be used in Gson parsing.
     */
    @SuppressWarnings("unused")
    public static Type getSerializationType() {
        return new TypeToken<Subscription>(){}.getType();
    }

    /**
     * Gets the serialization type of a subscription list that is used in Gson parsing.
     * @return The serialization type of a subscription list to be used in Gson parsing.
     */
    @SuppressWarnings("all")
    public static Type getListSerializationType() {
        return new TypeToken<List<Subscription>>(){}.getType();
    }
}
