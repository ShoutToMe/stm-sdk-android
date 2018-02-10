package me.shoutto.sdk.internal.usecases;

import android.content.Context;
import android.util.Log;

import java.util.List;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.database.UserLocationDao;
import me.shoutto.sdk.internal.database.UserLocationDaoImpl;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Post historical user locations to the Shout to Me service
 */

public class PostUserHistoricalLocations extends BaseUseCase<List<StmBaseEntity>, Void> {

    private static final String TAG = PostUserHistoricalLocations.class.getSimpleName();
    private Context context;

    public PostUserHistoricalLocations(StmRequestProcessor<List<StmBaseEntity>> stmRequestProcessor,
                                       Context context) {
        super(stmRequestProcessor);
        this.context = context;
    }

    public void post(List<StmBaseEntity> locations, StmCallback<Void> callback) {
        if (locations == null || locations.size() == 0) {
            Log.w(TAG, "Cannot process location update. Location list is null or empty");
            if (callback != null) {
                StmError stmError = new StmError("Cannot process location update. Location is null", false, StmError.SEVERITY_MAJOR);
                callback.onError(stmError);
            }
            return;
        }

        this.callback = callback;

        stmRequestProcessor.processRequest(HttpMethod.POST, locations);
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        super.processCallback(stmObservableResults);

        // If successfully sent, delete all stored user location records
        UserLocationDao userLocationDao = new UserLocationDaoImpl(context);
        userLocationDao.deleteAllUserLocationRecords();
    }
}
