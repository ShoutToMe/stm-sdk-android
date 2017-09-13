package me.shoutto.sdk;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shoutto.sdk.internal.StmPreferenceManager;

/**
 * An <code>IntentService</code> implementation that registers an app to receive push notifications
 * from GCM via the Shout to Me platform.
 */
public class GcmNotificationRegistrationIntentService extends IntentService {

    /**
     * The Intent name that is broadcast when the GCM registration is complete.
     */
    public static final String PN_REGISTRATION_COMPLETE = "me.shoutto.sdk.GCM_REGISTRATION_COMPLETE";

    private static final String TAG = GcmNotificationRegistrationIntentService.class.getSimpleName();
    private static final String GCM_DEFAULT_SENDER_ID = "895198831543";
    private static final String NOTIFICATION_APP_ID = "me.shoutto.sdk.NotificationAppId";
    private static final String PLATFORM_APPLICATION_ARN_PREFIX = "arn:aws:sns:us-west-2:810633828709:app/GCM/";
    private static final String[] TOPICS = {"global"};
    private AmazonSNSClient snsClient;
    private boolean isStmServiceBound = false;
    private StmService stmService;
    private String platformApplicationArn;
    private StmPreferenceManager stmPreferenceManager;

    public GcmNotificationRegistrationIntentService() {
        super(TAG);
    }

    /**
     * Handles the Android onCreate lifecycle event. This is where some of the initialization takes place.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(getApplicationContext(), StmService.class);
        startService(intent);
        bindService(intent, stmServiceConnection, BIND_AUTO_CREATE);
        stmPreferenceManager = new StmPreferenceManager(getApplicationContext());
    }

    /**
     * Handles the Android onDestroy lifecycle event. This is where some of the cleanup takes place.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isStmServiceBound) {
            unbindService(stmServiceConnection);
            isStmServiceBound = false;
        }
    }

    /**
     * Handles the Android onHandleIntent lifecycle event and is the main method where the GCM
     * registration occurs.
     * @param intent The Intent delievered to this service.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA);
            Bundle bundle = serviceInfo.metaData;

            String notificationAppId = bundle.getString(NOTIFICATION_APP_ID);
            if (notificationAppId == null || "".equals(notificationAppId)) {
                Log.e(TAG, "me.shoutto.sdk.NotificationAppId is null. Please ensure the value is set in AndroidManifest.xml");
            }

            String serverUrl = stmPreferenceManager.getServerUrl();
            if (serverUrl.contains("-test")) {
                notificationAppId += "-test";
            }

            platformApplicationArn = PLATFORM_APPLICATION_ARN_PREFIX + notificationAppId;

        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Package name not found. Cannot start GcmNotificationRegistrationIntentService.", ex);
        }

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                StmService.AWS_COGNITO_IDENTITY_POOL_ID,
                Regions.US_EAST_1 // Region
        );
        snsClient = new AmazonSNSClient(credentialsProvider);
        snsClient.setRegion(Region.getRegion(Regions.US_WEST_2));


        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(GCM_DEFAULT_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.w(TAG, "Failed to complete token refresh", e);
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(PN_REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {

        String endpointArn = retrieveEndpointArn();

        boolean updateNeeded = false;
        boolean createNeeded = (null == endpointArn);

        if (createNeeded) {
            // No platform endpoint ARN is stored; need to call createEndpoint.
            endpointArn = createEndpoint(token);
            createNeeded = false;
        }

        // Look up the platform endpoint and make sure the data in it is current, even if
        // it was just created.
        try {
            GetEndpointAttributesRequest geaReq =
                    new GetEndpointAttributesRequest()
                            .withEndpointArn(endpointArn);
            GetEndpointAttributesResult geaRes =
                    snsClient.getEndpointAttributes(geaReq);

            updateNeeded = !geaRes.getAttributes().get("Token").equals(token)
                    || !geaRes.getAttributes().get("Enabled").equalsIgnoreCase("true")
                    || geaRes.getAttributes().get("CustomUserData") == null
                    || !geaRes.getAttributes().get("CustomUserData").equals(buildUserDataAttributes());

        } catch (NotFoundException nfe) {
            // We had a stored ARN, but the platform endpoint associated with it
            // disappeared. Recreate it.
            createNeeded = true;
        }

        if (createNeeded) {
            createEndpoint(token);
        }

        if (updateNeeded) {
            // The platform endpoint is out of sync with the current data;
            // update the token and enable it.
            Map<String, String> attribs = new HashMap<>();
            attribs.put("Token", token);
            attribs.put("Enabled", "true");
            attribs.put("CustomUserData", buildUserDataAttributes());
            SetEndpointAttributesRequest saeReq =
                    new SetEndpointAttributesRequest()
                            .withEndpointArn(endpointArn)
                            .withAttributes(attribs);
            snsClient.setEndpointAttributes(saeReq);
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

    /**
     * @return never null
     * */
    private String createEndpoint(String token) {

        String endpointArn;
        try {
            CreatePlatformEndpointRequest cpeReq =
                    new CreatePlatformEndpointRequest()
                            .withPlatformApplicationArn(platformApplicationArn)
                            .withToken(token)
                            .withCustomUserData(buildUserDataAttributes());
            CreatePlatformEndpointResult cpeRes = snsClient
                    .createPlatformEndpoint(cpeReq);
            endpointArn = cpeRes.getEndpointArn();
        } catch (InvalidParameterException ipe) {
            String message = ipe.getErrorMessage();
            Log.w(TAG, "Exception message: " + message);
            Pattern p = Pattern
                    .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " +
                            "with the same token.*");
            Matcher m = p.matcher(message);
            if (m.matches()) {
                // The platform endpoint already exists for this token, but with
                // additional custom data that
                // createEndpoint doesn't want to overwrite. Just use the
                // existing platform endpoint.
                endpointArn = m.group(1);
            } else {
                // Rethrow the exception, the input is actually bad.
                throw ipe;
            }
        }
        Log.d(TAG, "Created ARN = " + endpointArn);
        storeEndpointArn(endpointArn);
        return endpointArn;
    }

    /**
     * @return the ARN the app was registered under previously, or null if no
     *         platform endpoint ARN is stored.
     */
    private String retrieveEndpointArn() {
        // Retrieve the platform endpoint ARN from permanent storage,
        // or return null if null is stored.
        String platformEndpointArn = stmService.getUser().getPlatformEndpointArn();
        if (platformEndpointArn != null) {
            Log.d(TAG, "Retrieved ARN = " + platformEndpointArn);
        }
        return platformEndpointArn;
    }

    /**
     * Stores the platform endpoint ARN in permanent storage for lookup next time.
     * */
    private void storeEndpointArn(String endpointArn) {
        // Write the platform endpoint ARN to permanent storage.
        stmService.getUser().setPlatformEndpointArn(endpointArn);
        StmError stmError = stmService.getUser().save();
        if (stmError != null) {
            Log.w(TAG, "An error occurred trying to save the user platform endpoint arn. Message" + stmError.getMessage());
        }
    }

    private String buildUserDataAttributes() {
        String userId = (stmPreferenceManager.getUserId() != null ? stmPreferenceManager.getUserId() : "");
        return "{ \"user_id\": \"" + userId + "\" }";
    }

    private ServiceConnection stmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            stmService = ((StmService.StmBinder) service).getService();
            isStmServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isStmServiceBound = false;
        }
    };
}
