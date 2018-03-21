package me.shoutto.sdk.internal.http;

/**
 * Interface for adapting a Shout to Me entity to a JSON object for HTTP calls
 */

interface StmJsonRequestAdapter<T> {

    String adapt(T objectToAdapt);
}
