package org.trv.alex.wifisharefiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilesListActivity extends AppCompatActivity {

    private static final String M_B = "B";
    private static final String M_KB = "kB";
    private static final String M_MB = "MB";
    private static final String M_GB = "GB";
    private static final String SPACE = " ";

    private static final String CURRENT_DIR_KEY = "currentDirKey";
    public static final String CHOOSE_DIR = "chooseDir";
    public static final int REQUEST_CODE = 1;
    public static final String SELECTED_FILE_OR_DIR = "selectedFileOrDir";
    public static final String START_PATH = "filePath";

    private static final File HOME_DIR = Environment.getExternalStorageDirectory();

    private ListView mFilesListView;
    private ArrayAdapter<File> mAdapter;
    private List<File> mListFiles = new ArrayList<>();
    private File mCurrentDirectory;
    private boolean mChooseDir;
    private String mStartDir;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list);

        Intent intent = getIntent();
        mChooseDir = intent.getBooleanExtra(CHOOSE_DIR, false);
        mStartDir = intent.getStringExtra(START_PATH);

        if (savedInstanceState == null) {
            if (mStartDir == null) {
                mCurrentDirectory = HOME_DIR;
            } else {
                mCurrentDirectory = new File(mStartDir);
                if (!mCurrentDirectory.isDirectory()) {
                    mCurrentDirectory = HOME_DIR;
                }
            }
        } else {
            mCurrentDirectory = new File(savedInstanceState
                    .getString(CURRENT_DIR_KEY, HOME_DIR.getAbsolutePath()));
        }

        mFilesListView = findViewById(R.id.files_list_view);
        mFilesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = mListFiles.get(position);
                if (selectedFile.isDirectory()) {
                    mCurrentDirectory = mListFiles.get(position);
                    updateUI();
                } else {
                    selectAndExit(selectedFile);
                }
            }
        });
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(CURRENT_DIR_KEY, mCurrentDirectory.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentDirectory.equals(HOME_DIR)) {
            super.onBackPressed();
        } else {
            mCurrentDirectory = mCurrentDirectory.getParentFile();
            updateUI();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mChooseDir) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.files_list_menu, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_choose_directory) {
            selectAndExit(mCurrentDirectory);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {

        if (!mCurrentDirectory.isDirectory()) {
            return;
        }
        mListFiles.clear();
        if (mChooseDir) {
            mListFiles.addAll(Arrays.asList(mCurrentDirectory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            })));
        } else {
            mListFiles.addAll(Arrays.asList(mCurrentDirectory.listFiles()));
        }
        Collections.sort(mListFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() == o2.isDirectory()) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
                return o1.isDirectory() ? -1 : 1;
            }
        });

        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<File>(this, android.R.layout.simple_list_item_2, android.R.id.text1,
                    mListFiles) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = view.findViewById(android.R.id.text1);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    File file = mListFiles.get(position);
                    String name = file.getName();
                    text1.setText(name);
                    String extraInfo;
                    if (file.isDirectory()) {
                        extraInfo = getString(R.string.directory);
                    } else {
                        extraInfo = convertSize(file.length());
                    }
                    text2.setText(extraInfo);
                    return view;
                }
            };
            mFilesListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
        String title = mCurrentDirectory.equals(HOME_DIR) ?
                getString(R.string.internal_storage) : mCurrentDirectory.getName();
        getSupportActionBar().setTitle(title);
    }

    private void selectAndExit(File selected) {
        Intent intent = new Intent();
        intent.putExtra(SELECTED_FILE_OR_DIR, selected.getAbsolutePath());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public static String convertSize(long length) {
        StringBuilder sb = new StringBuilder();
        if (length < 1024) {
            sb.append(length).append(SPACE).append(M_B);
        } else if (length < 1024 * 1024) {
            sb.append(length / 1024f).append(SPACE).append(M_KB);
        } else if (length < 1024 * 1024 * 1024) {
            sb.append(length / 1024f / 1024f).append(SPACE).append(M_MB);
        } else {
            sb.append(length / 1024f / 1024f / 1024f).append(SPACE).append(M_GB);
        }
        return sb.toString();
    }
}
