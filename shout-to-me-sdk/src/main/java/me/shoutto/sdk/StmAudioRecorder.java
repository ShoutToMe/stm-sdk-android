package me.shoutto.sdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * StmAudioRecorder
 *
 * This class uses the device's audio resources to capture audio into a ByteArrayOutputStream
 * and returns a StmAudioRecorderResult.
 */
public class StmAudioRecorder {

    private static final String TAG = "StmAudioRecorder";
    private final AudioRecord audioRecord;
    private final int minBufferSize;
    private boolean doRecord = false;
    private Handler handler;
    private ScheduledFuture stopRecordingFuture;
    private StmAudioRecorderResult stmAudioRecorderResult;
    private VoiceActivityDetector voiceActivityDetector;
    ByteArrayOutputStream realTimeStream;
    ByteArrayOutputStream finalStream;

    final Runnable StopRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            stopRecording();
        }
    };

    final Runnable CancelRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            cancelRecording();
        }
    };

    public StmAudioRecorder(Handler handler) {
        this.handler = handler;
        stmAudioRecorderResult = new StmAudioRecorderResult();
        voiceActivityDetector = new VoiceActivityDetector();

        minBufferSize = AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2);

        realTimeStream = new ByteArrayOutputStream();
        finalStream = new ByteArrayOutputStream();
    }

    public StmAudioRecorderResult writeAudioToStream() {
        realTimeStream.reset();
        finalStream.reset();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        doRecord = true;
        audioRecord.startRecording();

        ScheduledFuture cancelRecordingFuture = scheduler.schedule(CancelRecordingRunnable, 15, TimeUnit.SECONDS);

        byte[] buffer = new byte[minBufferSize * 4];
        byte[] bytes;
        Log.d(TAG, String.valueOf(minBufferSize));
        short[] shortBuffer = new short[minBufferSize * 2];

        while(doRecord) {
            int shortsWritten = audioRecord.read(shortBuffer, 0, shortBuffer.length);
            short2byte(shortBuffer, shortsWritten, buffer);
            bytes = Arrays.copyOf(buffer, shortsWritten * 2);
            realTimeStream.write(bytes, 0, shortsWritten * 2);

            // VAD
            int isUserStillTalking = voiceActivityDetector.determineTalkingStatus(shortBuffer, shortsWritten);
            if (isUserStillTalking == 1) {
                Log.i(TAG, "Speech detected");
                if (stopRecordingFuture != null && !stopRecordingFuture.isCancelled()) {
                    stopRecordingFuture.cancel(false);
                }
                if (!cancelRecordingFuture.isCancelled()) {
                    cancelRecordingFuture.cancel(false);
                }
            } else if (isUserStillTalking == 0) {
                Log.d(TAG, "Silence detected");
                pushPendingAudioToFinalOutputStream();
                stopRecordingFuture = scheduler.schedule(StopRecordingRunnable, 2, TimeUnit.SECONDS);
            }

            // Value to manipulate UI "speaking" animation
            double sum = 0;
            for (int i = 0; i < shortsWritten; i++) {
                sum += shortBuffer[i] * shortBuffer[i];
            }

            if (shortsWritten > 0) {
                final double amplitude = sum / (shortsWritten * 2);

                Bundle bundle = new Bundle();
                bundle.putInt("amplitudeSqrt", (int) Math.sqrt(amplitude));
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }

        Log.d(TAG, "about to close file");
        stmAudioRecorderResult.setAudioBuffer(finalStream);

        audioRecord.stop();
        audioRecord.release();
        scheduler.shutdown();
        return stmAudioRecorderResult;
    }

    public void finalizeRecording() {
        pushPendingAudioToFinalOutputStream();
        stopRecording();
    }

    public void cancelRecording() {
        stmAudioRecorderResult.setIsCancelled(true);
        doRecord = false;
    }

    private void stopRecording() {
        Log.d(TAG, "stopRecording");
        stmAudioRecorderResult.setIsCancelled(false);
        doRecord = false;
    }

    private void short2byte(short[] shorts, int nb, byte[] bytes) {
        for (int i = 0; i < nb; i++) {
            bytes[i * 2] = (byte)(shorts[i] & 0xff);
            bytes[i * 2 + 1] = (byte)((shorts[i] >> 8) & 0xff);
        }
    }

    private void pushPendingAudioToFinalOutputStream() {
        if (realTimeStream.size() > 0) {
            try {
                realTimeStream.writeTo(finalStream);
            } catch (IOException ex) {
                Log.e(TAG, "Error writing from the real time buffer to the final buffer", ex);
            } finally {
                realTimeStream.reset();
            }
        }
    }
}