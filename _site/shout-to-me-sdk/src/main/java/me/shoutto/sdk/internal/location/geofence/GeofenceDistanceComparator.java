package me.shoutto.sdk.internal.location.geofence;

import android.location.Location;

import java.util.Comparator;

public class GeofenceDistanceComparator implements Comparator<MessageGeofence> {

    private Location userLocation;

    public GeofenceDistanceComparator(Location userLocation) {
        this.userLocation = userLocation;
    }

    @Override
    public int compare(MessageGeofence lhs, MessageGeofence rhs) {
        Location lhsLocation = new Location("");
        lhsLocation.setLatitude(lhs.getLatitude());
        lhsLocation.setLongitude(lhs.getLongitude());

        Location rhsLocation = new Location("");
        rhsLocation.setLatitude(rhs.getLatitude());
        rhsLocation.setLongitude(rhs.getLongitude());

        if (userLocation.distanceTo(lhsLocation) - lhs.getRadius()
                < userLocation.distanceTo(rhsLocation) - rhs.getRadius()) {
            return -1;
        } else {
            return 1;
        }
    }
}
