package me.shoutto.sdk;

import java.io.ByteArrayOutputStream;

public class StmAudioRecorderResult {

    private ByteArrayOutputStream audioBuffer;
    private boolean isCancelled;
    private boolean didUserSpeak = false;
    private int recordingLengthInSeconds;

    public ByteArrayOutputStream getAudioBuffer() {
        return audioBuffer;
    }

    public void setAudioBuffer(ByteArrayOutputStream audioBuffer) {
        this.audioBuffer = audioBuffer;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setIsCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public boolean didUserSpeak() {
        return didUserSpeak;
    }

    public void setDidUserSpeak(boolean didUserSpeak) {
        this.didUserSpeak = didUserSpeak;
    }

    public Integer getRecordingLengthInSeconds() {
        return recordingLengthInSeconds;
    }

    public void setRecordingLengthInSeconds(Integer recordingLengthInSeconds) {
        this.recordingLengthInSeconds = recordingLengthInSeconds;
    }
}