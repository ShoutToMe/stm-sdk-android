package me.shoutto.sdk;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmRecorderActivity extends Activity implements HandWaveGestureListener,
        SoundPool.OnLoadCompleteListener, StmAudioRecorder.RecordingCountdownListener {

    private static final String TAG = "StmRecorderActivity";
    private static final String TAGS = "tags";
    private static final String TOPIC = "topic";
    public static final String MAX_RECORDING_TIME_IN_SECONDS = "maxRecordingTimeInSeconds";
    public static final String SILENCE_DETECTION_ENABLED = "silenceDetectionEnabled";
    private StmAudioRecorder stmAudioRecorder;
    private StmService stmService;
    private Boolean isStmServiceBound = false;
    private SoundPool soundPool;
    private int startListeningSoundId, cancelSoundId, finishSoundId, sentSoundId;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private boolean isAlreadyLaunchedRecorder = false;
    private AudioManager audioManager;
    private String shoutTags;
    private String shoutTopic;
    private int maxRecordingTimeInSeconds = 0;
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

            if (maxRecordingTimeInSeconds > 0) {
                stmService.setMaxRecordingTimeInSeconds(maxRecordingTimeInSeconds);
            } else {
                maxRecordingTimeInSeconds = stmService.getMaxRecordingTimeInSeconds();
            }
            Handler recorderHandler = new RecordingHandler(StmRecorderActivity.this);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isStmServiceBound = false;
            stmService.setOverlay(null);
        }
    };

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
        Intent intent = new Intent(this, StmService.class);
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
            Log.w(TAG, "Unable to get audio focus");
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

                        Future<StmShout> futureShout = executor.submit(new SendShoutCallable(recordingResult.getAudioBuffer()));
                        final StmShout newShout = futureShout.get();
                        newShout.setRecordingLengthInSeconds(shoutRecordingLengthInSeconds);

                        playShoutSentSound();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StmCallback<StmShout> stmCallback = stmService.getShoutCreationCallback();
                                stmCallback.onResponse(newShout);
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
                    intent.putExtra("result", "success");
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
        startListeningSoundId = soundPool.load(this, R.raw.listen, 1);
    }

    private void playCancelListeningSound() {
        cancelSoundId = soundPool.load(this, R.raw.abort, 1);
    }

    private void playFinishListeningSound() {
        finishSoundId = soundPool.load(this, R.raw.finish, 1);
    }

    private void playShoutSentSound() {
        sentSoundId = soundPool.load(this, R.raw.sent, 1);
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
    };

    private class SendShoutCallable implements Callable<StmShout> {
        private ByteArrayOutputStream stream;

        public SendShoutCallable(ByteArrayOutputStream stream) {
            this.stream = stream;
        }

        @Override
        public StmShout call() throws Exception {
            StmShout stmShout = new StmShout(stmService, stream.toByteArray());
            if (shoutTags != null) {
                stmShout.setTags(shoutTags);
            }
            if (shoutTopic != null) {
                stmShout.setTopic(shoutTopic);
            }
            return stmService.getStmHttpSender().postNewShout(stmShout);
        }
    }
}