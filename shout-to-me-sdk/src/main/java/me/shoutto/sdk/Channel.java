package me.shoutto.sdk;

/**
 * Created by tracyrojas on 4/11/16.
 */
public class Channel {

    private static final int GLOBAL_DEFAULT_MAX_RECORDING_TIME = 15;
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String listImageUrl;
    private int defaultMaxRecordingLengthSeconds;

    public Channel() {
        defaultMaxRecordingLengthSeconds = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getListImageUrl() {
        return listImageUrl;
    }

    public void setListImageUrl(String listImageUrl) {
        this.listImageUrl = listImageUrl;
    }

    public int getDefaultMaxRecordingLengthSeconds() {
        if (defaultMaxRecordingLengthSeconds == 0) {
            return GLOBAL_DEFAULT_MAX_RECORDING_TIME;
        } else {
            return defaultMaxRecordingLengthSeconds;
        }
    }

    public void setDefaultMaxRecordingLengthSeconds(int defaultMaxRecordingLengthSeconds) {
        this.defaultMaxRecordingLengthSeconds = defaultMaxRecordingLengthSeconds;
    }
}

