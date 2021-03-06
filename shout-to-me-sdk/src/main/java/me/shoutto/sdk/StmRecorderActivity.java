package me.shoutto.sdk;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shoutto.sdk.internal.RecordingGraphicView;
import me.shoutto.sdk.internal.audio.StmAudioRecorder;
import me.shoutto.sdk.internal.audio.StmAudioRecorderResult;

/**
 * The <code>Activity</code> class that is displayed to the user during audio recording.
 */
public class StmRecorderActivity extends Activity implements HandWaveGestureListener,
        SoundPool.OnLoadCompleteListener, StmAudioRecorder.RecordingCountdownListener {

    public static final String MAX_RECORDING_TIME_IN_SECONDS = "me.shoutto.sdk.MAX_RECORDING_TIME_IN_SECONDS";
    public static final String SILENCE_DETECTION_ENABLED = "me.shoutto.sdk.SILENCE_DETECTION_ENABLED";
    public static final String TAGS = "me.shoutto.sdk.tags";
    public static final String TOPIC = "me.shoutto.sdk.topic";

    /**
     * Activity result and reasons for failure
     */
    public static final String ACTIVITY_REASON = "me.shoutto.sdk.ACTIVITY_REASON";
    public static final String ACTIVITY_RESULT = "me.shoutto.sdk.ACTIVITY_RESULT";
    public static final String MAX_RECORDING_TIME_MISSING = "me.shoutto.sdk.MAX_RECORDING_TIME_MISSING";
    public static final String OBJECTS_UNINITIALIZED = "me.shoutto.sdk.OBJECTS_UNINITIALIZED";
    public static final String RECORD_AUDIO_PERMISSION_DENIED = "me.shoutto.sdk.RECORD_AUDIO_PERMISSION_DENIED";

    private static final String TAG = StmRecorderActivity.class.getSimpleName();
    private StmAudioRecorder stmAudioRecorder;
    private StmService stmService;
    private Boolean isStmServiceBound = false;
    private SoundPool soundPool;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private boolean isAlreadyLaunchedRecorder = false;
    private AudioManager audioManager;
    private String shoutTags;
    private String shoutTopic;
    private int maxRecordingTimeInSeconds;
    private Boolean isSilenceDetectionEnabled;
    private TextView countdownTextView;
    private String countdownText;
    private ProgressBar countdownTimer;
    private int progressMax;
    private ObjectAnimator animation;

    private ServiceConnection stmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            StmService.StmBinder binder = (StmService.StmBinder) service;
            stmService = binder.getService();
            isStmServiceBound = true;

            if (ContextCompat.checkSelfPermission(StmRecorderActivity.this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "App does not have permission to record.");
                Intent intent = new Intent();
                intent.putExtra(ACTIVITY_RESULT, StmService.FAILURE);
                intent.putExtra(ACTIVITY_REASON, RECORD_AUDIO_PERMISSION_DENIED);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                stmService.refreshUserLocation();

                finalizeRecordingDetailsAndStartRecording();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isStmServiceBound = false;
            stmService.setOverlay(null);
        }
    };

    private void finalizeRecordingDetailsAndStartRecording() {
        Handler recorderHandler = new RecordingHandler(StmRecorderActivity.this);
        try {
            stmAudioRecorder = new StmAudioRecorder(recorderHandler, maxRecordingTimeInSeconds);
            stmAudioRecorder.setRecordingCountdownListener(StmRecorderActivity.this);

            if (isSilenceDetectionEnabled == null) {
                stmAudioRecorder.setSilenceDetectionEnabled(true);
            } else {
                stmAudioRecorder.setSilenceDetectionEnabled(isSilenceDetectionEnabled);
            }

            progressMax = maxRecordingTimeInSeconds * 100; // To smooth animation
            animation = ObjectAnimator.ofInt (countdownTimer, "progress", 0, progressMax);

            stmService.setOverlay(StmRecorderActivity.this);

            startRecording();
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Could not initialize objects for recording.");
            Intent intent = new Intent();
            intent.putExtra(ACTIVITY_RESULT, StmService.FAILURE);
            intent.putExtra(ACTIVITY_REASON, OBJECTS_UNINITIALIZED);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private static class RecordingHandler extends Handler {
        private final WeakReference<StmRecorderActivity> currentActivity;

        public RecordingHandler(StmRecorderActivity activity) {
            currentActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            StmRecorderActivity activity = currentActivity.get();
            if (activity != null) {
                Bundle bundle = message.getData();
                if (bundle.getInt("amplitudeSqrt", -1) != -1) {
                    activity.updateRecordingViews(bundle.getInt("amplitudeSqrt"));
                }
            }
        }
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                cancelRecording(null);
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                shoutTags = extras.getString(TAGS);
                shoutTopic = extras.getString(TOPIC);
                maxRecordingTimeInSeconds = extras.getInt(MAX_RECORDING_TIME_IN_SECONDS);
                isSilenceDetectionEnabled = extras.getBoolean(SILENCE_DETECTION_ENABLED, true);
            }
        } else {
            shoutTags = (String) savedInstanceState.getSerializable(TAGS);
            shoutTopic = (String) savedInstanceState.getSerializable(TOPIC);
            maxRecordingTimeInSeconds = (int) savedInstanceState.getSerializable(MAX_RECORDING_TIME_IN_SECONDS);
            isSilenceDetectionEnabled = (Boolean) savedInstanceState.getSerializable(SILENCE_DETECTION_ENABLED);
            if (isSilenceDetectionEnabled == null) {
                isSilenceDetectionEnabled = true;
            }
        }

        if (maxRecordingTimeInSeconds <= 0) {
            Log.e(TAG, "StmRecorderActivity.MAX_RECORDING_TIME_IN_SECONDS is required and must be greater than 0");
            Intent intent = new Intent();
            intent.putExtra(ACTIVITY_RESULT, StmService.FAILURE);
            intent.putExtra(ACTIVITY_REASON, MAX_RECORDING_TIME_MISSING);
            setResult(RESULT_OK, intent);
            finish();
        }

        setContentView(R.layout.activity_stm_overlay);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Prepare objects for playing sounds
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(this);

        countdownTextView = (TextView) findViewById(R.id.countdown_timer);
        countdownText = getResources().getString(R.string.recording_countdown_timer);
        countdownTimer = (ProgressBar) findViewById(R.id.recording_timer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        soundPool.release();
        scheduler.shutdown();
        executor.shutdown();
        if (stmAudioRecorder != null) {
            stmAudioRecorder.setRecordingCountdownListener(null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to StmService
        Intent intent = new Intent(getApplicationContext(), StmService.class);
        startService(intent);
        bindService(intent, stmServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (isStmServiceBound) {
            unbindService(stmServiceConnection);
            isStmServiceBound = false;
            stmService.setOverlay(null);
        }
    }

    @Override
    public void onHandWaveGesture() {
        stopRecording(null);
    }

    @Override
    public void onCountdownUpdate(final int secondsRemaining, final int secondsElapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countdownTextView.setText(String.format(countdownText, String.valueOf(secondsRemaining)));
            }
        });
    }

    public void startRecording() {

        // We want this to run after all the visuals are loaded, however, we don't want it to
        // start running again in onStart if the overlay is stopped and restarted.
        if (isAlreadyLaunchedRecorder) {
            return;
        } else {
            isAlreadyLaunchedRecorder = true;
        }

        // Try to get exclusive audio focus
        int audioFocusRequestResult = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        if (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.d(TAG, "Unable to get audio focus");
        }

        playStartListeningSound();

        scheduler.schedule(new Runnable() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            countdownTimer.setMax(progressMax);
                            animation.setDuration(maxRecordingTimeInSeconds * 1000);
                            animation.start();
                        }
                    });

                    Future<StmAudioRecorderResult> futureRecorderResult = executor.submit(new RecordShoutCallable());
                    StmAudioRecorderResult recordingResult = futureRecorderResult.get();
                    final Integer shoutRecordingLengthInSeconds = recordingResult.getRecordingLengthInSeconds();

                    if (recordingResult.isCancelled() || !recordingResult.didUserSpeak()) {
                        playCancelListeningSound();
                    } else {
                        playFinishListeningSound();

                        Future<Shout> futureShout = executor.submit(new SendShoutCallable(recordingResult.getAudioBuffer()));
                        final Shout newShout = futureShout.get();
                        newShout.setRecordingLengthInSeconds(shoutRecordingLengthInSeconds);

                        playShoutSentSound();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StmCallback<Shout> stmCallback = stmService.getShoutCreationCallback();
                                if (stmCallback != null) {
                                    stmCallback.onResponse(newShout);
                                }
                            }
                        });
                    }

                    // Sleeping is necessary to prevent the onDestroy being called to early
                    // which results in the sent sound not being played
                    Thread.sleep(700);

                } catch (InterruptedException ex) {
                    Log.w(TAG, "Recording and/or sending shout was interrupted", ex);
                } catch (ExecutionException ex) {
                    Log.e(TAG, "An error occurred during the recording or sending of a new shout", ex);
                } finally {
                    // Release the audio focus
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                    }

                    // Close the overlay
                    Intent intent = new Intent();
                    intent.putExtra(ACTIVITY_RESULT, StmService.SUCCESS);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }, 300, TimeUnit.MILLISECONDS);
    }

    public void stopRecording(View view) {
        animation.end();
        stmAudioRecorder.finalizeRecording();
    }

    public void cancelRecording(View view) {
        animation.end();
        stmAudioRecorder.cancelRecording();
    }

    private void updateRecordingViews(int amplitudeSqrt) {
        RecordingGraphicView outerCircle = (RecordingGraphicView) findViewById(R.id.outerCircle);
        int newCircleSize = 400 + amplitudeSqrt;

        if (newCircleSize > outerCircle.getWidth() - 10) {
            newCircleSize = outerCircle.getWidth() - 10;
        }
        outerCircle.setSize(newCircleSize);
    }

    private void playStartListeningSound() {
        soundPool.load(this, R.raw.listen, 1);
    }

    private void playCancelListeningSound() {
        soundPool.load(this, R.raw.abort, 1);
    }

    private void playFinishListeningSound() {
        soundPool.load(this, R.raw.finish, 1);
    }

    private void playShoutSentSound() {
        soundPool.load(this, R.raw.sent, 1);
    }

    @Override
    public void onLoadComplete(final SoundPool soundPool, final int sampleId, int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                soundPool.play(sampleId, 1, 1, 0, 0, 1);
            }
        });
    }

    private class RecordShoutCallable implements Callable<StmAudioRecorderResult> {
        @Override
        public StmAudioRecorderResult call() throws Exception {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            return stmAudioRecorder.writeAudioToStream();
        }
    }

    private class SendShoutCallable implements Callable<Shout> {
        private ByteArrayOutputStream stream;

        public SendShoutCallable(ByteArrayOutputStream stream) {
            this.stream = stream;
        }

        @Override
        public Shout call() throws Exception {
            Shout shout = new Shout(stmService, stream.toByteArray());
            if (shoutTags != null) {
                shout.setTags(shoutTags);
            }
            if (shoutTopic != null) {
                shout.setTopic(shoutTopic);
            }
            return stmService.getStmHttpSender().postNewShout(shout);
        }
    }
}