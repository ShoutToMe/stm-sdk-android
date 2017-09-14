package me.shoutto.sdk;

/**
 * The interface for listeners that would like to register to receive hand wave gesture events.
 */
public interface HandWaveGestureListener {

    /**
     * Invoked when a hand wave gesture is detected.
     */
    void onHandWaveGesture();
}