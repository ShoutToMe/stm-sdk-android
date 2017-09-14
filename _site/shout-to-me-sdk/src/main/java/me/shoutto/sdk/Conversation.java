package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * This class represents a Shout to Me Conversation object. The conversation object contains
 * meta information about one or more Shouts.
 */
public class Conversation extends StmBaseEntity {

    /**
     * The base endpoint of conversations on the Shout to Me REST API.
     */
    public static final String BASE_ENDPOINT = "/conversations";

    /**
     * The key used for JSON serialization of conversation objects.
     */
    public static final String SERIALIZATION_KEY = "conversation";

    /**
     * The key used for JSON serialization of conversation lists.
     */
    @SuppressWarnings("all")
    public static final String LIST_SERIALIZATION_KEY = SERIALIZATION_KEY + "s";

    @SuppressWarnings("unused")
    private String channelId;
    @SuppressWarnings("unused")
    private Date expirationDate;
    @SuppressWarnings("unused")
    private Location location;
    @SuppressWarnings("unused")
    private String publishingMessage;

    /**
     * The default constructor.
     */
    public Conversation() {
        super(SERIALIZATION_KEY);
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    /**
     * Gets the channel ID of the conversation.
     * @return The channel ID.
     */
    public String getChannelId() { return channelId; }

    /**
     * Gets the expiration date of the conversation.
     * @return The expiration date of the conversation.
     */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Gets the location of the conversation. Location is an inner class that contains details
     * of the location a Shout is associated with.
     * @return The location of the conversation.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the publishing message associated with the conversation.  This message is set in the
     * Shout to Me broadcaster application.
     * @return The publishing message associated with the conversation.
     */
    @SuppressWarnings("all")
    public String getPublishingMessage() { return publishingMessage; }

    /**
     * Gets the serialization type that is used in Gson parsing.
     * @return The serialization type to be used in Gson parsing.
     */
    public static Type getSerializationType() {
        return new TypeToken<Conversation>(){}.getType();
    }

    /**
     * Gets the serialization type of a conversation list that is used in Gson parsing.
     * @return The serialization type of a conversation list to be used in Gson parsing.
     */
    @SuppressWarnings("all")
    public static Type getListSerializationType() {
        return new TypeToken<List<Conversation>>(){}.getType();
    }

    @Override
    public Type getEntitySerializationType() {
        return Conversation.getSerializationType();
    }

    /**
     * The location coordinates and area associated with a Conversation.
     */
    public class Location {

        @SuppressWarnings("unused")
        private double lat;
        @SuppressWarnings("unused")
        private double lon;
        @SuppressWarnings("unused")
        private float radiusInMeters;

        /**
         * Gets the latitude of the conversation.
         * @return The latitude of the conversation.
         */
        public double getLat() {
            return lat;
        }

        /**
         * Gets the longitude of the conversation.
         * @return The longitude of the conversation.
         */
        public double getLon() {
            return lon;
        }

        /**
         * Gets the radius in meters of the circular area associated with a conversation.
         * @return The radius of the conversation.
         */
        public float getRadiusInMeters() {
            return radiusInMeters;
        }
    }
}
