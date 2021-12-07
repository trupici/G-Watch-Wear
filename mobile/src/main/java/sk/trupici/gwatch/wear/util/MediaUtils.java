/*
 * Copyright (C) 2019 Juraj Antal
 *
 * Originally created in G-Watch App
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.trupici.gwatch.wear.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.RequiresApi;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;

public class MediaUtils {

    public static final String MIME_TYPE_IMAGE = "image/*";
    public static final String[] MIME_TYPES_AUDIO = new String[] {"audio/wav", "audio/x-wav", "audio/ogg", "application/ogg"};

    private static final long MAX_MEDIA_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final int BACKGROUND_IMAGE_WIDTH = 360;
    private static final int BACKGROUND_IMAGE_HEIGHT = 360;

    public static MediaDesc checkImageAndGetName(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            final Bitmap bitmap = Build.VERSION.SDK_INT < Build.VERSION_CODES.P ? getBitmapOld(context, uri) : getBitmap(context, uri);

            if (bitmap.getHeight() != BACKGROUND_IMAGE_HEIGHT || bitmap.getWidth() != BACKGROUND_IMAGE_WIDTH) {
                Log.e(GWatchApplication.LOG_TAG, "Invalid image resolution: " + bitmap.getWidth() + " x " + bitmap.getHeight());
                UiUtils.showToast(context, R.string.error_file_resolution);
                return null;
            }

        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "Failed to check image format and size", t);
            UiUtils.showToast(context, R.string.error_file_or_format);
            return null;
        }

        return getMediaDescFromUri(context, uri);
    }

    private static MediaDesc getMediaDescFromUri(Context context, Uri uri) {
        // format and size is valid

        String displayName = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                if (displayName != null) {
                    return new MediaDesc(displayName, uri);
                }
            }
        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "Failed to get name from doc provider", t);
        }


        // try direct path decomposition
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            displayName = uri.getLastPathSegment();
            if (displayName != null) {
                return new MediaDesc(displayName, uri);
            }
        }

        // failed, nothing more to do
        UiUtils.showToast(context, R.string.error_file_path);
        return null;
    }

    @SuppressWarnings("deprecation")
    private static Bitmap getBitmapOld(Context context, Uri bitmapUri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), bitmapUri);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private static Bitmap getBitmap(Context context, Uri bitmapUri) throws IOException {
        final ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), bitmapUri);
        return ImageDecoder.decodeBitmap(source);
    }

    public static byte[] readMediaToByteArray(Context context, MediaDesc image) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(image.getUri())) {
            int bufSize = BACKGROUND_IMAGE_WIDTH * BACKGROUND_IMAGE_HEIGHT; // sized for background images
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufSize);
            int len;
            byte[] buffer = new byte[bufSize];
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    public static MediaDesc checkAudioFileAndGetName(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        long fileSize = getFileSize(context, uri);
        if (fileSize == 0) {
            UiUtils.showToast(context, R.string.error_file_path);
            return null;
        }
        if (fileSize > MAX_MEDIA_FILE_SIZE) {
            UiUtils.showToast(context, R.string.error_file_size);
            return null;
        }

        return getMediaDescFromUri(context, uri);
    }

    private static long getFileSize(Context context, Uri fileUri) {
        try {
            Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null);
            if (cursor == null) {
                return 0;
            }
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            return cursor.getLong(sizeIndex);
        } catch (Exception e) {
            Log.e(GWatchApplication.LOG_TAG, "Failed to get file size", e);
            return 0;
        }
    }

    public static class MediaDesc {
        private final String name;
        private final Uri uri;

        public MediaDesc(String name, Uri uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public Uri getUri() {
            return uri;
        }
    }

}
