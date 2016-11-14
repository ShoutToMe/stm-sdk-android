package me.shoutto.sdk;

/**
 * This class is returned in <code>onFailure</code> method of the {@link Callback} class. It provides
 * information about a failure that occurred.
 */
public class StmError {

    /**
     * A constant that represents a major severity of an error.  Should be used by the client to
     * evaluate the severity of an error.
     */
    @SuppressWarnings("all")
    public static final String SEVERITY_MAJOR = "SEVERITY_MAJOR";

    /**
     * A constant that represents a minor severity of an error.  Should be used by the client to
     * evaluate the severity of an error.
     */
    @SuppressWarnings("all")
    public static final String SEVERITY_MINOR = "SEVERITY_MINOR";

    private boolean blocking;
    private String message;
    private String severity;

    /**
     * The constructor used by the SDK when handling a failure scenario.
     * @param message A descriptive message of the error that occurred.
     * @param blocking A boolean that describes whether the error is blocking or not.
     * @param severity The severity of the error.
     */
    public StmError(String message, boolean blocking, String severity) {
        this.message = message;
        this.blocking = blocking;
        this.severity = severity;
    }

    /**
     * The default constructor.
     */
    public StmError() {}

    /**
     * Gets the descriptive message of the error.
     * @return The error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     * @param message The error message.
     */
    void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the severity of the error.
     * @return The severity.
     */
    @SuppressWarnings("unused")
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity of the error.
     * @param severity The severity.
     */
    void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Gets the boolean describing whether the error is blocking or not.
     * @return The boolean describing whether the error is blocking or not.
     */
    @SuppressWarnings("unused")
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Sets the boolean that describes whether the error is blocking or not.
     * @param blocking The boolean describing whether the error is blocking or not.
     */
    void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
}
