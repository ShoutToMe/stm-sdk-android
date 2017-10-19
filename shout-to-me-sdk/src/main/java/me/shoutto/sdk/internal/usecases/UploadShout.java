package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import java.io.File;

import me.shoutto.sdk.CreateShoutRequest;
import me.shoutto.sdk.Shout;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.StmObserver;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Used to upload a Shout to the Shout to Me system.  The two steps of the process are
 *      1) upload the file to Shout to Me media storage, then
 *      2) post data to the Shout to Me REST API.
 */

public class UploadShout extends BaseUseCase<Shout> {

    private static final String TAG = UploadShout.class.getSimpleName();
    private CreateShoutRequest createShoutRequest;
    private FileUploader fileUploader;
    private StmService stmService;

    public UploadShout(StmService stmService, FileUploader fileUploader, StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
        this.fileUploader = fileUploader;
        this.stmService = stmService;
    }

    public void upload(final CreateShoutRequest createShoutRequest, StmCallback<Shout> callback) {
        this.callback = callback;
        this.createShoutRequest = createShoutRequest;

        if (createShoutRequest.isValid()) {
            stmEntityRequestProcessor.addObserver(this);
            fileUploader.addObserver(this);
            fileUploader.uploadFile(createShoutRequest.getFile());
        } else {
            if (callback != null) {
                StmError error = new StmError("CreateShoutRequest object not valid. Aborting upload.",
                        false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, "CreateShoutRequest object not valid. Aborting upload.");
            }

        }
    }

    @Override
    public void processCallback(StmObservableResults stmObservableResults) {
        switch (stmObservableResults.getStmObservableType()) {
            case STM_SERVICE_RESPONSE:
                processPostShoutResult((Shout)stmObservableResults.getResult());
                break;

            case UPLOAD_FILE:
                processFileUploadResult(stmObservableResults.getResult().toString());
                break;

            default:
                Log.w(TAG, "Unexpected StmObservableType " + String.valueOf(stmObservableResults.getStmObservableType()));
        }
    }

    private void processFileUploadResult(String fileUrl) {
        Shout shout = (Shout)createShoutRequest.adaptToBaseEntity();
        shout.setChannelId(stmService.getChannelId());
        shout.setMediaFileUrl(fileUrl);
        stmEntityRequestProcessor.processRequest(HttpMethod.POST, shout);
    }

    private void processPostShoutResult(Shout shout) {
        if (callback != null) {
            callback.onResponse(shout);
        }
    }

    @Override
    public void processCallbackError(StmObservableResults stmObservableResults) {
        if (callback != null) {
            StmError stmError = new StmError(stmObservableResults.getErrorMessage(), false, StmError.SEVERITY_MAJOR);
            callback.onError(stmError);
        } else {
            Log.w(TAG, stmObservableResults.getErrorMessage());
        }
    }

    public interface FileUploader extends StmObservable {
        void uploadFile(File file);
    }
}
