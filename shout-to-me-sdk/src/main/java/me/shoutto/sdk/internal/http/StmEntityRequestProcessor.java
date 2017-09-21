package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.internal.StmObservable;

/**
 * Interface for async HTTP request processors
 */

public interface StmEntityRequestProcessor extends StmObservable {

    void processRequest(HttpMethod httpMethod, StmBaseEntity stmBaseEntity);
}
