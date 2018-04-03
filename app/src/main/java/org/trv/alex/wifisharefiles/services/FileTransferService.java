package org.trv.alex.wifisharefiles.services;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.trv.alex.wifisharefiles.ClientFileAsyncTask;
import org.trv.alex.wifisharefiles.FileAsyncTask;
import org.trv.alex.wifisharefiles.ServerFileAsyncTask;

public class FileTransferService extends Service {

    public static final int CLIENT_ASYNC_TASK = 1;
    public static final int SERVER_ASYNC_TASK = 2;
    public static final String ASYNC_TASK_TYPE = "asyncTaskType";
    private static final String TAG = "FileTransferService";

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String FILE_PATH = "filePath";

    private FileAsyncTask mFileAsyncTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // get type which asyncTask to run
        int asyncTaskType = intent.getIntExtra(ASYNC_TASK_TYPE, 0);

        if (asyncTaskType == CLIENT_ASYNC_TASK) {

            // get server host
            String host = intent.getStringExtra(HOST);

            // get server port (8888 is default)
            int port = intent.getIntExtra(PORT, 8888);

            // get the file path which to be sent
            Uri fileUri = intent.getParcelableExtra(FILE_PATH);

            mFileAsyncTask = new ClientFileAsyncTask(host, port, this);
            mFileAsyncTask.execute(fileUri);
        } else if (asyncTaskType == SERVER_ASYNC_TASK) {
            mFileAsyncTask = new ServerFileAsyncTask(this);
            mFileAsyncTask.execute();
        } else {
            stopSelf();
        }

        startForeground(mFileAsyncTask.getNotificationId(), mFileAsyncTask.getNotification());

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mFileAsyncTask != null) {
            mFileAsyncTask.cancel(true);
            mFileAsyncTask.closeSocket();
        }
        stopForeground(false);
        Log.d(TAG, "destroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
