package me.shoutto.sdk;

import java.io.ByteArrayOutputStream;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmAudioRecorderResult {

    private ByteArrayOutputStream audioBuffer;
    private boolean isCancelled;
    private boolean didUserSpeak = false;

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
}