package me.shoutto.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The data object that contains information about a new shout to be created.
 */

public class CreateShoutRequest implements StmEntityActionRequest {

    private static final String TAG = CreateShoutRequest.class.getSimpleName();
    private String description;
    private File file;
    private List<String> tags;
    private String text;
    private String topic;

    public CreateShoutRequest() {
        tags = new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Helper function only for files in media storage.  For other files, use setFile
     * @param uri the Uri of a media file
     * @param context the Context
     */
    public void setFileFromMediaUri(Uri uri, Context context) {
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            if (filePath != null) {
                file = new File(filePath);
            } else {
                Log.w(TAG, String.format("File path from Uri is null. Uri: %s", uri));
            }
            cursor.close();
        } else {
            Log.w(TAG, "Could not inspect file Uri. Please verify you are passing in a valid media Uri.");
        }
    }

    public boolean isValid() {
        return file != null && file.length() > 0;
    }
}
