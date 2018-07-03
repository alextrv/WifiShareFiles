package org.trv.alex.wifisharefiles.services;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.trv.alex.wifisharefiles.FileAsyncTask;
import org.trv.alex.wifisharefiles.ReceiverFileAsyncTask;
import org.trv.alex.wifisharefiles.SenderFileAsyncTask;

public class FileTransferService extends Service {

    public static final int SENDER_ASYNC_TASK = 1;
    public static final int RECEIVER_ASYNC_TASK = 2;
    public static final String ASYNC_TASK_TYPE = "asyncTaskType";
    private static final String TAG = "FileTransferService";

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String FILE_PATH = "filePath";

    private FileAsyncTask mFileAsyncTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int asyncTaskType = intent.getIntExtra(ASYNC_TASK_TYPE, 0);

        if (asyncTaskType == SENDER_ASYNC_TASK) {

            String host = intent.getStringExtra(HOST);
            int port = intent.getIntExtra(PORT, 8888);
            Uri fileUri = intent.getParcelableExtra(FILE_PATH);

            mFileAsyncTask = new SenderFileAsyncTask(this, host, port);
            mFileAsyncTask.execute(fileUri);
        } else if (asyncTaskType == RECEIVER_ASYNC_TASK) {
            mFileAsyncTask = new ReceiverFileAsyncTask(this);
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
        }
        stopForeground(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
