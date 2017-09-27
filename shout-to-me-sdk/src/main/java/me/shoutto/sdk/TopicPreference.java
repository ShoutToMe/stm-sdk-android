package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * TopicPreference object used for serialization
 */

public class TopicPreference extends StmBaseEntity {

    private static final String SERIALIZATION_KEY = "topic_preferences";
    private static final String BASE_ENDPOINT = "/topic_preference";
    private String topic;

    public TopicPreference() {
        super(SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        // Stubbed
    }

    @Override
    public Type getEntitySerializationType() {
        return new TypeToken<TopicPreference>(){}.getType();
    }
}
