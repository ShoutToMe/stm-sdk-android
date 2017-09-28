package me.shoutto.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CreateShoutRequest
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, TextUtils.class})
public class CreateShoutRequestTest {

    @Mock
    ContentResolver mockContentResolver;

    @Mock
    Context mockContext;

    @Mock
    Cursor mockCursor;

    @Mock
    File mockFile;

    @Mock
    Uri mockUri;

    @Test
    public void isValidTest() {
        CreateShoutRequest createShoutRequest = new CreateShoutRequest();

        // mockFile will be null
        assertFalse(createShoutRequest.isValid());

        // mockFile.length() will be 0
        createShoutRequest.setFile(mockFile);
        assertFalse(createShoutRequest.isValid());

        when(mockFile.length()).thenReturn(100l);
        createShoutRequest.setFile(mockFile);
        assertTrue(createShoutRequest.isValid());
    }

    @Test
    public void setFileFromMediaUriTest() {
        PowerMockito.mockStatic(Log.class);

        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        CreateShoutRequest createShoutRequest = new CreateShoutRequest();
        createShoutRequest.setFileFromMediaUri(mockUri, mockContext);

        // Cursor is null
        assertNull(createShoutRequest.getFile());

        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
        when(mockContentResolver.query(mockUri, filePathColumn, null, null, null)).thenReturn(mockCursor);
        createShoutRequest.setFileFromMediaUri(mockUri,mockContext);

        // filePath is null
        assertNull(createShoutRequest.getFile());

        when(mockCursor.getString(0)).thenReturn("mock file path");
        createShoutRequest.setFileFromMediaUri(mockUri,mockContext);

        // File should be set
        assertNotNull(createShoutRequest.getFile());
    }

    @Test
    public void adaptBaseEntity_ShouldReturnShoutObjectWithAdaptedProperties() {
        PowerMockito.mockStatic(TextUtils.class);

        String description = "description";
        String text = "text";
        String topic = "topic";
        List<String> tags = new ArrayList<>();
        tags.add(0, "tag 1");
        tags.add(1, "tag 2");

        when(TextUtils.join(",", tags)).thenReturn("tag 1,tag 2");

        CreateShoutRequest createShoutRequest = new CreateShoutRequest();
        createShoutRequest.setDescription(description);
        createShoutRequest.setTags(tags);
        createShoutRequest.setText(text);
        createShoutRequest.setTopic(topic);

        Shout shout = (Shout)createShoutRequest.adaptToBaseEntity();
        assertEquals(description, shout.getDescription());
        assertEquals("tag 1,tag 2", shout.getTags());
        assertEquals(text, shout.getText());
        assertEquals(topic, shout.getTopic());
    }
}
