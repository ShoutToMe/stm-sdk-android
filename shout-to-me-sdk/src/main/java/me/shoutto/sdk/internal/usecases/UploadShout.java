package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import java.io.File;

import me.shoutto.sdk.CreateShoutRequest;
import me.shoutto.sdk.Shout;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.StmService;
import me.shoutto.sdk.internal.StmObservable;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Used to upload a Shout to the Shout to Me system.  The two steps of the process are
 *      1) upload the file to Shout to Me media storage, then
 *      2) post data to the Shout to Me REST API.
 */

public class UploadShout extends BaseUseCase<StmBaseEntity, Shout> {

    private static final String TAG = UploadShout.class.getSimpleName();
    private static final String FILE_NULL_MESSAGE = "CreateShoutRequest did not return a File object. If you used a Uri, ensure that it can be converted to a File. Aborting upload.";
    private static final String INVALID_REQUEST_OBJECT_MESSAGE = "CreateShoutRequest object not valid. Aborting upload.";
    private CreateShoutRequest createShoutRequest;
    private FileUploader fileUploader;
    private StmService stmService;
    private boolean shoutPostedToApi = false;

    public UploadShout(StmService stmService, FileUploader fileUploader, StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
        this.fileUploader = fileUploader;
        this.stmService = stmService;
    }

    public void upload(final CreateShoutRequest createShoutRequest, StmCallback<Shout> callback) {
        this.callback = callback;
        this.createShoutRequest = createShoutRequest;

        if (createShoutRequest.isValid()) {
            File file = createShoutRequest.getFile();
            if (file == null) {
                if (callback == null) {
                    Log.w(TAG, FILE_NULL_MESSAGE);
                } else {
                    StmError error = new StmError(FILE_NULL_MESSAGE, false, StmError.SEVERITY_MINOR);
                    callback.onError(error);
                }
            } else {
                stmRequestProcessor.addObserver(this);
                fileUploader.addObserver(this);
                fileUploader.uploadFile(file);
            }
        } else {
            if (callback != null) {
                StmError error = new StmError(INVALID_REQUEST_OBJECT_MESSAGE, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, INVALID_REQUEST_OBJECT_MESSAGE);
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

    private synchronized void processFileUploadResult(String fileUrl) {
        if (createShoutRequest.isTemporaryFile()) {
            createShoutRequest.deleteTemporaryFile();
        }

        if (!shoutPostedToApi) {
            shoutPostedToApi = true;
            Shout shout = (Shout)createShoutRequest.adaptToBaseEntity();
            shout.setChannelId(stmService.getChannelId());
            shout.setMediaFileUrl(fileUrl);
            stmRequestProcessor.processRequest(HttpMethod.POST, shout);
        }
    }

    private void processPostShoutResult(Shout shout) {
        if (callback != null) {
            callback.onResponse(shout);
        }
    }

    @Override
    public void processCallbackError(StmObservableResults stmObservableResults) {
        if (createShoutRequest.isTemporaryFile()) {
            createShoutRequest.deleteTemporaryFile();
        }

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
