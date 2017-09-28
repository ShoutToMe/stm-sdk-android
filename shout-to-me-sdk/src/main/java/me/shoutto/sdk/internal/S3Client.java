package me.shoutto.sdk.internal;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import me.shoutto.sdk.StmService;
import me.shoutto.sdk.internal.usecases.UploadShout.FileUploader;

/**
 * The class used to upload files to S3
 */

public class S3Client implements FileUploader {

    private static final String TAG = S3Client.class.getSimpleName();
    private static final String SHOUT_UPLOAD_BUCKET = "s2m-shout-upload-inbox";
    private static final String SHOUT_URL_BUCKET_PREFIX = "https://s3-us-west-2.amazonaws.com/" + SHOUT_UPLOAD_BUCKET + "/";
    private TransferUtility transferUtility;
    private ArrayList<StmObserver> observers;

    public S3Client(Context context) {

        observers = new ArrayList<>();

        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                context.getApplicationContext(),
                StmService.AWS_COGNITO_IDENTITY_POOL_ID,
                Regions.US_EAST_1
        );
        AmazonS3Client s3Client = new AmazonS3Client(cognitoCachingCredentialsProvider);
        s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));

        transferUtility = new TransferUtility(s3Client, context.getApplicationContext());
    }

    @Override
    public void uploadFile(File file) {
        UUID uuid = UUID.randomUUID();
        Uri uri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                .toString());
        final String s3FileKey = String.format("%s.%s", uuid, fileExtension);

        TransferObserver transferObserver = transferUtility.upload(
                SHOUT_UPLOAD_BUCKET,
                s3FileKey,
                file
        );
        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, state.toString());
                if (state.equals(TransferState.COMPLETED)) {
                    StmObservableResults<String> stmObservableResults = new StmObservableResults<>();
                    stmObservableResults.setError(false);
                    stmObservableResults.setResult(String.format("%s%s", SHOUT_URL_BUCKET_PREFIX, s3FileKey));
                    stmObservableResults.setStmObservableType(StmObservableType.UPLOAD_FILE);
                    notifyObservers(stmObservableResults);
                } else if (state.equals(TransferState.CANCELED) || state.equals(TransferState.FAILED)) {
                    StmObservableResults stmObservableResults = new StmObservableResults();
                    stmObservableResults.setError(true);
                    stmObservableResults.setErrorMessage("File upload failed or canceled.");
                    notifyObservers(stmObservableResults);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal > 0) {
                    int percentage = (int) (bytesCurrent/bytesTotal * 100);
                    Log.d(TAG, String.format("File upload percentage completed: %d", percentage));
                } else {
                    Log.w(TAG, "bytesTotal = 0. This value should be a positive number representing the file size.");
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                StmObservableResults stmObservableResults = new StmObservableResults();
                stmObservableResults.setError(true);
                stmObservableResults.setErrorMessage("Error occurred uploading a file to S3. " + ex.getMessage());
                notifyObservers(stmObservableResults);
            }

        });
    }

    @Override
    public void addObserver(StmObserver o) {
        observers.add(o);
    }

    @Override
    public void deleteObserver(StmObserver o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(StmObservableResults stmObservableResults) {
        for (StmObserver o : observers) {
            o.update(stmObservableResults);
        }
    }
}
