package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Conversation represents a Shout to Me Conversation entity.
 */
public class Conversation extends StmBaseEntity {

    public static final String BASE_ENDPOINT = "/conversations";
    public static final String OBJECT_JSON_KEY = "conversation";
    public static final String LIST_JSON_KEY = "conversations";
    public static final String SERIALIZATION_KEY = "conversation";
    private String channelId;
    private Date expirationDate;
    private Location location;
    private String publishingMessage;

    public Conversation() {
        super(SERIALIZATION_KEY);
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    public String getChannelId() { return channelId; }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public Location getLocation() {
        return location;
    }

    public String getPublishingMessage() { return publishingMessage; }

    public static Type getSerializationType() {
        return new TypeToken<Conversation>(){}.getType();
    }

    public static Type getListSerializationType() {
        return new TypeToken<List<Conversation>>(){}.getType();
    }

    public class Location {

        private double lat;
        private double lon;
        private float radiusInMeters;

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public float getRadiusInMeters() {
            return radiusInMeters;
        }
    }
}
