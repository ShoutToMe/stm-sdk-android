package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * ChannelSubscription object used for serialization
 */

public class ChannelSubscription extends StmBaseEntity {

    private static final String SERIALIZATION_KEY = "channel_subscriptions";
    private static final String BASE_ENDPOINT = "/channel_subscription";
    private String channelId;

    public ChannelSubscription() {
        super(SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        // Stubbed
    }

    @Override
    public Type getEntitySerializationType() {
        return new TypeToken<ChannelSubscription>(){}.getType();
    }
}
