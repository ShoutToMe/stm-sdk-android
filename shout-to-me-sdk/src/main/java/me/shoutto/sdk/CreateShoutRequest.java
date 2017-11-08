package me.shoutto.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * The data object that contains information about a new shout to be created.
 */

public class CreateShoutRequest implements StmEntityActionRequest {

    private static final String TAG = CreateShoutRequest.class.getSimpleName();
    private Context context;
    private String description;
    private File file;
    private List<String> tags;
    private boolean temporaryFile = false;
    private String text;
    private String topic;
    private Uri uri;

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
        if (file == null && uri != null) {
            processUri();
        }
        return file;
    }

    /**
     * Sets the File object that represents the shout.  Must be a supported media file.
     * @param file The File object that represents the shout.
     */
    public void setFile(File file) {
        this.file = file;
    }

    public boolean isTemporaryFile() {
        return temporaryFile;
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
     * Helper function that will attempt to create a File from a Uri.  Not guaranteed to work for all
     * types of files. If files in question don't work, use setFile()
     * @param uri the Uri of a media file
     * @param context the Context
     */
    public void setFileFromMediaUri(Uri uri, Context context) {
        this.context = context;
        this.uri = uri;
    }

    private void processUri() {

        if (context == null || uri == null) {
            Log.w(TAG, "Cannot process Uri to File because either Uri or Context is null");
            return;
        }

        ContentResolver contentResolver = context.getContentResolver();
        try {
            // Attempt 1: Try to query Uri data field
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                if (filePath != null) {
                    file = new File(filePath);
                    return;
                } else {
                    Log.w(TAG, String.format("File path from Uri is null. Uri: %s", uri));
                }
                cursor.close();
            } else {
                Log.w(TAG, "Could not inspect file Uri. Please verify you are passing in a valid media Uri.");
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage(), ex);
        }

        // Attempt 2: Try to use InputStream to make a copy of the file
        try {
            InputStream is = contentResolver.openInputStream(uri);
            String fileExtension = "";
            String fileType = contentResolver.getType(uri);
            if (fileType != null) {
                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                fileExtension = mimeTypeMap.getExtensionFromMimeType(fileType);
            }

            UUID uuid = UUID.randomUUID();
            String filePath = context.getFilesDir() + "/" + uuid + ("".equals(fileExtension) ? "" : "." + fileExtension);

            FileOutputStream os = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while((bytesRead = is.read(buffer)) !=-1){
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            //flush OutputStream to write any buffered data to file
            os.flush();
            os.close();

            file = new File(filePath);
            temporaryFile = true;
        } catch (FileNotFoundException ex) {
            Log.w(TAG, "Could not get input stream from Uri");
        } catch (IOException ex) {
            Log.e(TAG, "Error writing file");
        } catch (Exception ex) {
            Log.e(TAG, "Unknown error trying to upload file from InputStream", ex);
        }
    }

    public boolean deleteTemporaryFile() {
        return file != null && file.delete();
    }

    /**
     * Returns the logical result of the whether the CreateShoutRequest instance is ready to be
     * consumed by the Shout to Me SDK
     * @return true if CreateShoutRequest object is ready to be processed by the Shout to Me SDK
     */
    public boolean isValid() {
        return (file != null && file.length() > 0) || uri != null;
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
