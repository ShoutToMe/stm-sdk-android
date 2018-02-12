package me.shoutto.sdk.internal.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import me.shoutto.sdk.internal.database.UserLocationRecord;
import me.shoutto.sdk.internal.http.DefaultEntityRequestProcessorSync;
import me.shoutto.sdk.internal.http.EntityListRequestProcessorSync;
import me.shoutto.sdk.internal.http.GsonRequestAdapter;
import me.shoutto.sdk.internal.http.NullResponseAdapter;
import me.shoutto.sdk.internal.http.UserHistoricalLocationUrlProvider;
import me.shoutto.sdk.internal.http.UserLocationUrlProvider;
import me.shoutto.sdk.internal.location.geofence.GeofenceManager;
import me.shoutto.sdk.internal.usecases.PostUserHistoricalLocations;
import me.shoutto.sdk.internal.usecases.UpdateUserLocation;

/**
 * Controls the process of triggering and listening for a location update, and then updating the user's location
 */

public class UpdateUserLocationController implements LocationUpdateListener, StmObservable {

    private static final String TAG = UpdateUserLocationController.class.getSimpleName();
    private GeofenceManager geofenceManager;
    private LocationServicesClient locationServicesClient;
    private StmPreferenceManager stmPreferenceManager;
    private String serverUrl;
    private String userAuthToken;
    private String userId;
    private List<StmObserver> observers;
    private Context context;

    public UpdateUserLocationController(LocationServicesClient locationServicesClient,
                                         Context context) {
        geofenceManager = new GeofenceManager(context);
        this.context = context;

        stmPreferenceManager = new StmPreferenceManager(context);
        serverUrl = stmPreferenceManager.getServerUrl();
        userAuthToken = stmPreferenceManager.getAuthToken();
        userId = stmPreferenceManager.getUserId();

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
                User user = new User();
                user.setId(userId);

                // If there are old locations, try them now
                UserLocationDao userLocationDao = new UserLocationDaoImpl(context);
                if (userLocationDao.getNumRows() > 0) {
                    List<UserLocation> userLocations = userLocationDao.getAllUserLocations();
                    List<StmBaseEntity> userLocationsAsBase = new ArrayList<>();
                    userLocationsAsBase.addAll(userLocations);

                    EntityListRequestProcessorSync<Void, List<StmBaseEntity>> stmRequestProcessor =
                            new EntityListRequestProcessorSync<>(
                                    new GsonRequestAdapter<List<StmBaseEntity>>(),
                                    new NullResponseAdapter(),
                                    userAuthToken,
                                    new UserHistoricalLocationUrlProvider(serverUrl, user)
                            );
                    PostUserHistoricalLocations postUserHistoricalLocations =
                            new PostUserHistoricalLocations(stmRequestProcessor,
                                    new UserLocationDaoImpl(context));
                    postUserHistoricalLocations.post(userLocationsAsBase, new Callback<Void>() {
                        @Override
                        public void onSuccess(StmResponse<Void> stmResponse) {
                            Log.d(TAG, "User historical locations posted successfully");
                        }

                        @Override
                        public void onFailure(StmError stmError) {
                            Log.w(TAG, "Failure posting user historical locations: " + stmError.getMessage());
                        }
                    });
                }

                DefaultEntityRequestProcessorSync<Void> defaultEntityRequestProcessorSync = new DefaultEntityRequestProcessorSync<>(
                        new GsonRequestAdapter<StmBaseEntity>(),
                        new NullResponseAdapter(),
                        userAuthToken,
                        new UserLocationUrlProvider(serverUrl, user)
                );

                UpdateUserLocation updateUserLocation = new UpdateUserLocation(
                        defaultEntityRequestProcessorSync, geofenceManager, stmPreferenceManager, context, "LOCATION_SERVICE_UPDATE");

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
