package me.shoutto.sdk.internal.database;

import java.util.Date;

/**
 * A POJO representation of a user location data record
 */

public class UserLocationRecord {

    private Date date;
    private double lat;
    private double lon;
    private float metersSinceLastUpdate;
    private float radius;
    private String type;

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public float getMetersSinceLastUpdate() {
        return metersSinceLastUpdate;
    }

    public void setMetersSinceLastUpdate(float metersSinceLastUpdate) {
        this.metersSinceLastUpdate = metersSinceLastUpdate;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
