package me.shoutto.sdk.internal;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Listens for the various broadcast Intents and starts applicable actions
 */

public class StmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = StmBroadcastReceiver.class.getSimpleName();
    private static final int GEOFENCE_REINITIALIZATION_JOB_SERVICE = 1;
    private static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            startLocationInitializationService(context);
        } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)){
            if (data != null) {
                String packageName = data.getSchemeSpecificPart();
                if (GOOGLE_PLAY_SERVICES_PACKAGE.equals(packageName)) {
                    startLocationInitializationService(context);
                }
            }
        }
    }

    private void startLocationInitializationService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.schedule(new JobInfo.Builder(GEOFENCE_REINITIALIZATION_JOB_SERVICE,
                        new ComponentName(context, GeofenceReinitializationService.class))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .build());
            } else {
                Log.w(TAG, "Could not get JobScheduler service. Cannot process post boot tasks");
            }
        } else {
            Log.w(TAG, "Cannot perform post boot initialization tasks. API version less than 21");
        }
    }
}
