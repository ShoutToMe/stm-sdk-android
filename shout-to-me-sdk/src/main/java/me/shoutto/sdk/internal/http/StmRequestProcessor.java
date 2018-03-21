package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.internal.StmObservable;

/**
 * Interface for async HTTP request processors
 */

public interface StmRequestProcessor<T> extends StmObservable {
    void processRequest(HttpMethod httpMethod, T requestObject);
}
