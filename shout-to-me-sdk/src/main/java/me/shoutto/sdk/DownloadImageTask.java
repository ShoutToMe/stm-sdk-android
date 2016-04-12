package me.shoutto.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by tracyrojas on 4/11/16.
 */
class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = "DownloadImageTask";
    ImageDownloadListener listener;
    UUID contextId;
    boolean isListImage = false;
    int screenWidth;

    public DownloadImageTask(ImageDownloadListener listener, UUID contextId, boolean isListImage, int screenWidth) {
        this.listener = listener;
        this.contextId = contextId;
        this.isListImage = isListImage;
        this.screenWidth = screenWidth;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        InputStream in;
        try {
            in = new java.net.URL(urldisplay).openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            in.close();

            BitmapFactory.Options options = new BitmapFactory.Options();
            if (!isListImage) {
                BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()), null, options);
                options.inJustDecodeBounds = true;
                options.inSampleSize = calculateInSampleSize(options.outHeight, options.outWidth);
            }
            options.inJustDecodeBounds = false;
            mIcon11 = BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()), null, options);
            baos.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        listener.onDownloadComplete(contextId, result);
    }

    private int calculateInSampleSize(int height, int width) {
        int inSampleSize = 1;

        if (width > screenWidth) {
            double resizeRatio = ((double)screenWidth) / width;
            long reqHeight = Math.round(resizeRatio * height);
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > screenWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public interface ImageDownloadListener {
        void onDownloadComplete(UUID contextId, Bitmap image);
    }
}
