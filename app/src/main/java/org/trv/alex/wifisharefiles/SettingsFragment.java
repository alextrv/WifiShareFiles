package org.trv.alex.wifisharefiles;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class SettingsFragment extends PreferenceFragment {

    public static final String PREF_DOWNLOAD_DIR = "prefDownloadDir";

    Preference mDownloadPref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mDownloadPref = findPreference(PREF_DOWNLOAD_DIR);
        mDownloadPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), FilesListActivity.class);
                intent.putExtra(FilesListActivity.CHOOSE_DIR, true);
                intent.putExtra(FilesListActivity.START_PATH, getDownloadDir(getActivity()));
                startActivityForResult(intent, FilesListActivity.REQUEST_CODE);
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mDownloadPref.setSummary(getDownloadDir(getActivity()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FilesListActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String path = data.getStringExtra(FilesListActivity.SELECTED_FILE_OR_DIR);
                setDownloadDir(getActivity(), path);
            }
        }
    }

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
