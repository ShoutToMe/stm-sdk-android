package me.shoutto.sdk;

import java.io.ByteArrayOutputStream;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmAudioRecorderResult {

    private ByteArrayOutputStream audioBuffer;
    private boolean isCancelled;

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
}