package me.shoutto.sdk.internal.database;

import java.util.List;

import me.shoutto.sdk.UserLocation;

/**
 * Interface for the user location data access object
 */

public interface UserLocationDao {

    public void addUserLocationRecord(UserLocationRecord userLocationRecord);
    public void deleteAllUserLocationRecords();
    public List<UserLocationRecord> getAllUserLocationRecords();
    public List<UserLocation> getAllUserLocations();
    public long getNumRows();
    public void truncateTable();
}
