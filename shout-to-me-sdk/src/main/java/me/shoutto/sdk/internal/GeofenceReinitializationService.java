package me.shoutto.sdk.internal;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import me.shoutto.sdk.Callback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmResponse;
import me.shoutto.sdk.internal.location.LocationServicesClient;
import me.shoutto.sdk.internal.location.UserLocationListener;
import me.shoutto.sdk.internal.usecases.InitializeLocationAfterBoot;

/**
 * Reinitializes the user location geofence when certain events happen that delete the geofence.
 * Reference: https://developer.android.com/training/location/geofencing.html#BestPractices
 */
@TargetApi(21)
public class GeofenceReinitializationService extends JobService {

    private static final String TAG = GeofenceReinitializationService.class.getSimpleName();

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "Reinitializing Shout to Me user geofence");

        InitializeLocationAfterBoot initializeLocationAfterBoot = new InitializeLocationAfterBoot(
                this,
                new UserLocationListener(LocationServicesClient.getInstance(), this)
        );
        initializeLocationAfterBoot.initialize(new Callback<Void>() {
            @Override
            public void onSuccess(StmResponse stmResponse) {
                Log.d(TAG, "Completed Shout to Me user geofence reinitialization");
                jobFinished(params, false);
            }

            @Override
            public void onFailure(StmError stmError) {
                Log.w(TAG, "An error occurred reinitializing Shout to Me user geofence: " + stmError.getMessage());
                jobFinished(params, true);
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.w(TAG, TAG + " was stopped");
        return false;
    }
}
