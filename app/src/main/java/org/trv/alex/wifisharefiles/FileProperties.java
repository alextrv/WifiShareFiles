package org.trv.alex.wifisharefiles;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

public class FileProperties {

    private final Uri mUri;
    private final String mName;
    private final long mSize;
    private final Context mContext;

    private FileProperties(Uri uri, String name, long size, Context context) {
        mUri = uri;
        mName = name;
        mSize = size;
        mContext = context.getApplicationContext();
    }

    public static FileProperties getInstance(@NonNull Context context, @NonNull Uri uri) {

        Objects.requireNonNull(context);
        Objects.requireNonNull(uri);

        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            File file = new File(uri.getPath());

            if (file.exists()) {
                return new FileProperties(uri, file.getName(), file.length(), context);
            }

        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {

            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {

                if (cursor != null) {
                    int columnIndexDisplayName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    int columnIndexSize = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);

                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(columnIndexDisplayName);
                        long size = cursor.getLong(columnIndexSize);

                        return new FileProperties(uri, name, size, context);
                    }
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

    public InputStream openInputStream() throws IOException {
        InputStream is = mContext.getContentResolver().openInputStream(mUri);
        if (is == null) {
            throw new IOException("Couldn't get InputStream from the ContentResolver");
        }
        return is;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "[%s, %s, %d]", mUri.toString(), mName, mSize);
    }
}
