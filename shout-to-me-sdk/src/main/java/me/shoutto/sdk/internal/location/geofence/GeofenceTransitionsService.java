package me.shoutto.sdk.internal.location.geofence;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.sample.geofencing.GeofenceErrorMessages;

import java.util.ArrayList;
import java.util.List;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.User;
import me.shoutto.sdk.UserLocation;
import me.shoutto.sdk.internal.StmPreferenceManager;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.database.UserLocationDaoImpl;
import me.shoutto.sdk.internal.http.DefaultEntityRequestProcessorSync;
import me.shoutto.sdk.internal.http.EntityListRequestProcessorSync;
import me.shoutto.sdk.internal.http.GsonRequestAdapter;
import me.shoutto.sdk.internal.http.NullResponseAdapter;
import me.shoutto.sdk.internal.http.UserHistoricalLocationUrlProvider;
import me.shoutto.sdk.internal.http.UserLocationUrlProvider;
import me.shoutto.sdk.internal.usecases.PostUserHistoricalLocations;
import me.shoutto.sdk.internal.usecases.UpdateUserLocation;

/**
 * Handles events from geofence transitions. This process bypasses UpdateUserLocationController
 * because it has its own location.
 */

public class GeofenceTransitionsService extends Service {

    private static final String TAG = GeofenceTransitionsService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event was null. Cannot process geofence transition");
            stopSelf(startId);
        } else if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofencing error: " + errorMessage);
            stopSelf(startId);
        } else {
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                final Location location = geofencingEvent.getTriggeringLocation();
                if (location == null) {
                    Log.e(TAG, "Location is null. Cannot process geofence transition");
                    stopSelf(startId);
                } else {
                    Log.i(TAG,String.format("Geofence exit detected %f, %f", location.getLatitude(), location.getLongitude()));

                    final StmPreferenceManager stmPreferenceManager = new StmPreferenceManager(this);
                    String userId = stmPreferenceManager.getUserId();
                    final String authToken = stmPreferenceManager.getAuthToken();
                    final String serverUrl = stmPreferenceManager.getServerUrl();

                    final User user = new User();
                    user.setId(userId);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // If there are old locations, try them now
                            UserLocationDao userLocationDao = new UserLocationDaoImpl(GeofenceTransitionsService.this);
                            if (userLocationDao.getNumRows() > 0) {
                                List<UserLocation> userLocations = userLocationDao.getAllUserLocations();
                                List<StmBaseEntity> userLocationsAsBase = new ArrayList<>();
                                userLocationsAsBase.addAll(userLocations);

                                EntityListRequestProcessorSync<Void, List<StmBaseEntity>> stmRequestProcessor =
                                        new EntityListRequestProcessorSync<>(
                                                new GsonRequestAdapter<List<StmBaseEntity>>(),
                                                new NullResponseAdapter(),
                                                authToken,
                                                new UserHistoricalLocationUrlProvider(serverUrl, user)
                                        );
                                PostUserHistoricalLocations postUserHistoricalLocations =
                                        new PostUserHistoricalLocations(stmRequestProcessor, GeofenceTransitionsService.this);
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

                            DefaultEntityRequestProcessorSync<Void> defaultEntityRequestProcessorSync =
                                    new DefaultEntityRequestProcessorSync<>(
                                            new GsonRequestAdapter<StmBaseEntity>(),
                                            new NullResponseAdapter(),
                                            authToken,
                                            new UserLocationUrlProvider(serverUrl, user)
                                    );

                            UpdateUserLocation updateUserLocation
                                    = new UpdateUserLocation(defaultEntityRequestProcessorSync,
                                    new GeofenceManager(GeofenceTransitionsService.this), stmPreferenceManager, GeofenceTransitionsService.this, "GEOFENCE_EXIT");
                            updateUserLocation.update(location, new Callback<Void>() {
                                @Override
                                public void onSuccess(StmResponse stmResponse) {
                                    stopSelf(startId);
                                }

                                @Override
                                public void onFailure(StmError stmError) {
                                    Log.w(TAG, "Error occured. " + stmError.getMessage());
                                    stopSelf(startId);
                                }
                            });
                        }
                    }).start();
                }
            } else {
                stopSelf(startId);
            }
        }

        return START_REDELIVER_INTENT;
    }
}
