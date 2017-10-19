package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;

import me.shoutto.sdk.internal.location.geofence.GeofenceManager;

/**
 * The object that represents a user's location
 */

public class UserLocation extends StmBaseEntity {

    public static final String BASE_ENDPOINT = "/location";
    public static final String SERIALIZATION_KEY = "location";
    private Location location;
    private Date date;
    private Float metersSinceLastUpdate;

    public UserLocation() {
        super(SERIALIZATION_KEY, BASE_ENDPOINT);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Float getMetersSinceLastUpdate() {
        return metersSinceLastUpdate;
    }

    public void setMetersSinceLastUpdate(Float metersSinceLastUpdate) {
        this.metersSinceLastUpdate = metersSinceLastUpdate;
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) throws JSONException {
        // Stubbed
    }

    @Override
    public Type getEntitySerializationType() {
        return new TypeToken<UserLocation>(){}.getType();
    }

    public static class Location {
        String type;
        Double[] coordinates;
        String radius;

        public Location(Double[] coordinates) {
            this.coordinates = coordinates;
            type = "circle";
            radius = Float.toString(GeofenceManager.GEOFENCE_RADIUS_IN_METERS);
        }
    }
}
