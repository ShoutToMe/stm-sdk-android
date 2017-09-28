package me.shoutto.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
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

    /**
     * Gets the user defined description of the shout
     * @return The user defined description of the shout
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the user defined description of the shout
     * @param description The user defined description of the shout
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the shout media File object
     * @return The shout media File object
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the File object that represents the shout.  Must be a supported media file.
     * @param file The File object that represents the shout.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Gets the list of tags
     * @return The list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the list of tags
     * @param tags The list of tags
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the shout text
     * @return The shout text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the shout text.  The shout text is used for display in the Shout to Me broadcaster application
     * and various social media publishing
     * @param text The shout text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the shout topic
     * @return The shout topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the shout topic
     * @param topic The shout topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Helper function only for files in media storage.  For files in other storage, use setFile
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

    /**
     * Returns the logical result of the whether the CreateShoutRequest instance is ready to be
     * consumed by the Shout to Me SDK
     * @return true if CreateShoutRequest object is ready to be processed by the Shout to Me SDK
     */
    public boolean isValid() {
        return file != null && file.length() > 0;
    }

    @Override
    public StmBaseEntity adaptToBaseEntity() {

        Shout shout = new Shout();

        if (description != null) {
            shout.setDescription(description);
        }

        if (tags != null) {
            String tagString = TextUtils.join(",", tags);
            shout.setTags(tagString);
        }

        if (text != null) {
            shout.setText(text);
        }

        if (topic != null) {
            shout.setTopic(topic);
        }

        return shout;
    }
}
