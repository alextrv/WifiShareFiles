package org.trv.alex.wifisharefiles;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;

public class AppPreferences {

    public static final String PREF_DOWNLOAD_DIR = "prefDownloadDir";

    public static String getDownloadDir(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_DOWNLOAD_DIR,
                        Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                .getAbsolutePath());
    }

    public static void setDownloadDir(Context context, String path) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_DOWNLOAD_DIR, path)
                .apply();
    }

}
