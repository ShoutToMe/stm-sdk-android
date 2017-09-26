package me.shoutto.sdk;

/**
 * The interface to be used for SDK client entity request objects
 */

public interface StmEntityActionRequest {
    boolean isValid();
    StmBaseEntity adaptToBaseEntity();
}
