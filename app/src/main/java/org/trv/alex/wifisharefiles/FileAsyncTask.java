package org.trv.alex.wifisharefiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.trv.alex.wifisharefiles.services.FileTransferService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public abstract class FileAsyncTask extends AsyncTask<Uri, Void, Void> {

    protected static final int NOTIFICATION_ID = 1;

    protected Context mContext;
    protected String mHost;
    protected int mPort;

    protected File mFile;
    protected String mFileName;
    protected String mTitleCompleted;

    protected NotificationCompat.Builder mBuilder;
    protected NotificationManager mNotificationManager;

    protected void updateProgress(int progress, boolean completedFlag) {
        if (completedFlag) {
            // remove all action buttons from notification
            mBuilder.mActions.clear();
            mBuilder.setContentTitle(mTitleCompleted)
                    .setContentText(mFileName)
                    .setOngoing(false)
                    .setProgress(0, 0, false);
        } else {
            mBuilder.setContentTitle(mFileName)
                    .setContentText(String.format(Locale.getDefault(), "%d%%", progress))
                    .setOngoing(true)
                    .setProgress(100, progress, false);
        }
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mContext.stopService(new Intent(mContext, FileTransferService.class));
        if (!isCancelled()) {
            updateProgress(0, true);
        }
    }

    long transferData(InputStream is, OutputStream os, long fileSize) throws IOException {

        byte[] buff = new byte[1024 * 100];

        int progress = 0;

        long transfered = 0;

        int len;

        final int ONE_SECOND = 1000;

        long millis = System.currentTimeMillis();

        while ((len = is.read(buff)) != -1 && !isCancelled()) {
            os.write(buff, 0, len);
            transfered += len;
            int percent = (int) (transfered * 100.0f / fileSize);

            // Update notification every 1 second or more
            boolean isOneSecondPassed = (System.currentTimeMillis() - millis) > ONE_SECOND;

            if (isOneSecondPassed && percent != progress) {
                progress = percent;
                updateProgress(progress, false);
                millis = System.currentTimeMillis();
            }
        }

        return transfered;

    }

    public void closeSocket() {
    }

    public Notification getNotification() {
        return mBuilder.build();
    }

    public int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
