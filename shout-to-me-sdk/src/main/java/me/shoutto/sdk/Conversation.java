package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Conversation represents a Shout to Me Conversation entity.
 */
public class Conversation extends StmBaseEntity {

    public static final String BASE_ENDPOINT = "/conversations";
    public static final String OBJECT_JSON_KEY = "conversation";
    public static final String SERIALIZATION_KEY = "conversation";
    private Date expirationDate;
    private Location location;

    public Conversation() {
        super(SERIALIZATION_KEY);
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public Location getLocation() {
        return location;
    }

    public static Type getSerializationType() {
        return new TypeToken<Conversation>(){}.getType();
    }

    public class Location {

        private double lat;
        private double lon;
        private int radius;

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public int getRadius() {
            return radius;
        }
    }
}
