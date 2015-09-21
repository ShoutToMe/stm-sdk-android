package me.shoutto.sdk;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmError {

    public static final String SEVERITY_MAJOR = "major";
    public static final String SEVERITY_MINOR = "minor";

    private boolean blockingError;
    private String message;
    private String severity;

    public StmError(String message, boolean blockingError, String severity) {
        this.message = message;
        this.blockingError = blockingError;
        this.severity = severity;
    }
    public StmError() {}

    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isBlockingError() {
        return blockingError;
    }

    void setBlockingError(boolean blockingError) {
        this.blockingError = blockingError;
    }
}
