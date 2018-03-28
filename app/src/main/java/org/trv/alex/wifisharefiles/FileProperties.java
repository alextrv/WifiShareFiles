package org.trv.alex.wifisharefiles;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.Locale;

public class FileProperties {

    private final Uri mUri;
    private final String mName;
    private final long mSize;

    private FileProperties(Uri uri, String name, long size) {
        mUri = uri;
        mName = name;
        mSize = size;
    }

    public static FileProperties getInstance(Context context, Uri uri) {
        if (uri == null) {
            throw new NullPointerException();
        }

        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            File file = new File(uri.getPath());

            if (file.exists()) {
                return new FileProperties(uri, file.getName(), file.length());
            }

        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {

            Cursor cursor = null;

            if (context == null) {
                throw new NullPointerException();
            }

            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);

                if (cursor != null) {
                    int columnIndexDisplayName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    int columnIndexSize = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);

                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(columnIndexDisplayName);
                        long size = cursor.getLong(columnIndexSize);

                        return new FileProperties(uri, name, size);
                    }
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        }

        return null;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getName() {
        return mName;
    }

    public long getSize() {
        return mSize;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "[%s, %s, %d]", mUri.toString(), mName, mSize);
    }
}
