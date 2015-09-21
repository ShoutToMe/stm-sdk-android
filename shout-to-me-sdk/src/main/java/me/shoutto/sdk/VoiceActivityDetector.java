package me.shoutto.sdk;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class VoiceActivityDetector {

    private static final double MAX_REF = 32768;
    private static final String TAG = "VoiceActivityDetector";
    private VadState vadState;

    VoiceActivityDetector() {
        vadState = new VadState();
    }

    public int determineTalkingStatus(short[] samples, int numberOfSamples) {

        int result = -1;
        double[] samplesInDBFS = convertPCMToDecibelFullScale(samples, numberOfSamples);
        double decibelFullScale;

        for (int i = 0 ; i < numberOfSamples; i++) {
            decibelFullScale = samplesInDBFS[i];
            if (decibelFullScale == Double.NEGATIVE_INFINITY || decibelFullScale == Double.POSITIVE_INFINITY) {
                continue;
            }

            if (vadState.currentNumberOfSamples == vadState.samplesPerFrame) {
                result = checkSamples(vadState, vadState.samples, vadState.currentNumberOfSamples);
                if (result == 0 || result == 1) {
                    return result;
                }
                vadState.currentNumberOfSamples = 0;
            }

            vadState.samples[vadState.currentNumberOfSamples] = decibelFullScale;
            vadState.currentNumberOfSamples++;
        }

        return result;
    }

    private double[] convertPCMToDecibelFullScale(short[] samples, int numberOfSamples) {
        double[] samplesInDecibelFullScale = new double[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++) {
            samplesInDecibelFullScale[i] = 0 - 20 * Math.log10(Math.abs(samples[i] / MAX_REF));
        }
        return samplesInDecibelFullScale;
    }

    private int checkSamples(VadState vadState, double[] samples, int nb_samples) {

        int counter;
        double energy = calculateAverageEnergy(samples, nb_samples);
        int action = -1;

        if (vadState.sequence <= vadState.initialFrames) {
            setSilenceEnergyBaseline(vadState, energy, vadState.sequence);
        }

        counter = determineEnergyState(vadState, energy);

        if (vadState.sequence >= vadState.initialFrames && counter == 0 && !vadState.isTalking) {
            setSilenceEnergyBaseline(vadState, energy, vadState.sequence);
        }

        pushEnergyState(vadState.previousEnergyComparisonStates, vadState.previousStateMaxLen, counter);

        if (vadState.sequence < vadState.initialFrames) {
            vadState.sequence++;
            return -1;
        }

        if (!vadState.isTalking && determineIfTalking(vadState.previousEnergyComparisonStates, 1, 10)) {
            vadState.isTalking = true;
            action = 1;
        }
        else if (vadState.isTalking && determineIfNotTalking(vadState.previousEnergyComparisonStates, 0, vadState.previousStateMaxLen)) {
            vadState.isTalking = false;
            action = 0;
        }
        vadState.sequence++;

        return action;
    }

    private double calculateAverageEnergy(double[] samples, int numberOfSamples) {
        double energyTotal = 0.0f;
        for (int i = 0; i < numberOfSamples; i++) {
            energyTotal += samples[i];
        }
        energyTotal /= numberOfSamples;
        return energyTotal;
    }

    private void setSilenceEnergyBaseline(VadState vadState, double energy, int n) {
        n = (n > 10) ? 10 : n; //this correspond to 1/10 of a second
        vadState.silenceEnergyBaseline = (vadState.silenceEnergyBaseline * n + energy) / (n + 1);
//        Log.d(TAG, "min energy "+ vadState.silenceEnergyBaseline);
        vadState.isMinimumInitialized = true;
    }

    private int determineEnergyState(VadState vadState, double energy) {
        int counter = 0;
        if ((0 - (energy - vadState.silenceEnergyBaseline)) >= vadState.energyThreshold) {
            counter++;
        }
        return counter;
    }

    private void pushEnergyState(int[] memory, int length, int value) {
        while (--length > 0) {
            memory[length] = memory[length - 1];
        }
        memory[0] = value;
    }

    private boolean determineIfTalking(int[] memory, int value, int nb) {
        for (int i = 0; i < nb; i++) {
            if (memory[i] < value) {
                return false;
            }
        }
        return true;
    }

    private boolean determineIfNotTalking(int[] memory, int value, int nb) {
        for (int i = 0; i < nb; i++) {
            if (memory[i] > value) {
                return false;
            }
        }
        return true;
    }

    private class VadState {
        int sequence = 0;
        boolean isMinimumInitialized = false;
        int initialFrames = 30;
        double energyThreshold = 8.0;
        int previousStateMaxLen = 50;
        int[] previousEnergyComparisonStates = new int[previousStateMaxLen];
        boolean isTalking = false;
        int sampleRate = 16000;
        int samplesPerFrame = sampleRate / 100;
        double[] samples = new double[samplesPerFrame];
        int currentNumberOfSamples = 0;
        double silenceEnergyBaseline = 0.0;
    }
}
