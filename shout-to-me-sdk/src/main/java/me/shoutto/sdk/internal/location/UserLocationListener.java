package me.shoutto.sdk.internal.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.User;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObservableType;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.database.UserLocationDaoImpl;
import me.shoutto.sdk.internal.http.EntityListRequestProcessorSync;
import me.shoutto.sdk.internal.http.GsonRequestAdapter;
import me.shoutto.sdk.internal.http.NullResponseAdapter;
import me.shoutto.sdk.internal.http.UserLocationUrlProvider;
import me.shoutto.sdk.internal.http.UserLocationsRequestAdapter;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;
import me.shoutto.sdk.internal.usecases.UpdateUserLocation;

/**
 * Controls the process of triggering and listening for a location update, and then updating the user's location
 */

public class UserLocationListener implements LocationUpdateListener, StmObservable {

    private static final String TAG = UserLocationListener.class.getSimpleName();
    private LocationServicesClient locationServicesClient;
    private List<StmObserver> observers;
    private Context context;

    public UserLocationListener(LocationServicesClient locationServicesClient,
                                Context context) {
        this.context = context;

        this.locationServicesClient = locationServicesClient;
        this.locationServicesClient.registerLocationUpdateListener(this);
        observers = new ArrayList<>();
    }

    public double getLatitude() {
        return locationServicesClient.getLatitude();
    }

    public double getLongitude() {
        return locationServicesClient.getLongitude();
    }

    public void startTrackingUserLocation(Context context) {
        locationServicesClient.connectToService(context);
    }

    public void updateUserLocation(Context context) {
        locationServicesClient.refreshLocation(context);
    }

    @Override
    public void onLocationUpdate(final Location location) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(context);
                String serverUrl = stmPreferenceManager.getServerUrl();
                String userAuthToken = stmPreferenceManager.getAuthToken();
                String userId = stmPreferenceManager.getUserId();

                User user = new User();
                user.setId(userId);

                EntityListRequestProcessorSync<Void, SortedSet<? extends StmBaseEntity>> entityListRequestProcessorSync =
                        new EntityListRequestProcessorSync<>(
                                new UserLocationsRequestAdapter(),
                                new NullResponseAdapter(),
                                userAuthToken,
                                new UserLocationUrlProvider(serverUrl, user)
                        );

                UpdateUserLocation updateUserLocation = new UpdateUserLocation(
                        entityListRequestProcessorSync,
                        new GeofenceManager(context),
                        stmPreferenceManager,
                        new UserLocationDaoImpl(context),
                        context,
                        "LOCATION_SERVICE_UPDATE");

                updateUserLocation.update(location, new Callback<Void>() {
                    @Override
                    public void onSuccess(StmResponse stmResponse) {
                        StmObservableResults<Void> stmObservableResults = new StmObservableResults<>();
                        stmObservableResults.setError(false);
                        stmObservableResults.setResult(null);
                        stmObservableResults.setStmObservableType(StmObservableType.UPDATE_USER_LOCATION);
                        notifyObservers(stmObservableResults);
                    }

                    @Override
                    public void onFailure(StmError stmError) {
                        Log.e(TAG, stmError.getMessage());
                        StmObservableResults<Void> stmObservableResults = new StmObservableResults<>();
                        stmObservableResults.setError(true);
                        stmObservableResults.setErrorMessage(stmError.getMessage());
                        notifyObservers(stmObservableResults);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addObserver(StmObserver o) {
        observers.add(o);
    }

    @Override
    public void deleteObserver(StmObserver o) {
        if (observers.contains(o)) {
            observers.remove(o);
        }
    }

    @Override
    public void notifyObservers(StmObservableResults stmObserverResults) {
        for (StmObserver o : observers) {
            o.update(stmObserverResults);
        }
    }
}
