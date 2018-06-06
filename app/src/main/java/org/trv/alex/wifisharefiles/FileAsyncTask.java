package org.trv.alex.wifisharefiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.trv.alex.wifisharefiles.receivers.CancelServiceReceiver;
import org.trv.alex.wifisharefiles.services.FileTransferService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public abstract class FileAsyncTask extends AsyncTask<Uri, Void, Void> {

    private Context mContext;
    private String mHost;
    private int mPort;

    private File mFile;
    private String mFileName;
    private String mContentTitle;

    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    public FileAsyncTask(Context context, String host, int port) {
        mContext = context;
        mHost = host;
        mPort = port;
        init();
    }

    public FileAsyncTask(Context context) {
        this(context, null, 0);
    }

    protected void init() {
        Intent intent = new Intent(getContext(), CancelServiceReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action = new NotificationCompat.Action
                .Builder(android.R.drawable.ic_menu_close_clear_cancel,
                getContext().getString(R.string.cancel),
                pendingIntent).build();

        mNotificationBuilder = new NotificationCompat.Builder(getContext())
                .setShowWhen(false)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .addAction(action);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected void updateProgress(int progress, boolean completedFlag) {
        if (completedFlag) {
            // remove all action buttons from notification
            mNotificationBuilder.mActions.clear();
            mNotificationBuilder.setContentTitle(mContentTitle)
                    .setContentText(mFileName)
                    .setOngoing(false)
                    .setSubText(null)
                    .setContentInfo(null)
                    .setShowWhen(true)
                    .setProgress(0, 0, false);
        } else {
            mNotificationBuilder.setContentTitle(mFileName)
                    .setOngoing(true)
                    .setProgress(100, progress, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mNotificationBuilder.setSubText(String.format(Locale.getDefault(), "%d%%", progress));
            } else {
                mNotificationBuilder.setContentInfo(String.format(Locale.getDefault(), "%d%%", progress));
            }
        }
        mNotificationManager.notify(getNotificationId(), mNotificationBuilder.build());
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mContext.stopService(new Intent(mContext, FileTransferService.class));
        if (!isCancelled()) {
            updateProgress(0, true);
        }
    }

    long transferData(InputStream is, OutputStream os, long fileSize) throws IOException {

        byte[] buff = new byte[4096];

        int progress = 0;

        long transferred = 0;

        int len;

        final int ONE_SECOND = 1000;

        long millis = System.currentTimeMillis();

        while ((len = is.read(buff)) != -1 && !isCancelled()) {
            os.write(buff, 0, len);
            transferred += len;
            int percent = (int) (transferred * 100.0f / fileSize);

            // Update notification every 1 second or more
            boolean isOneSecondPassed = (System.currentTimeMillis() - millis) > ONE_SECOND;

            if (isOneSecondPassed && percent != progress) {
                progress = percent;
                updateProgress(progress, false);
                millis = System.currentTimeMillis();
            }
        }

        return transferred;

    }

    public void closeSocket() {
    }

    protected String getString(int resId) {
        return mContext.getString(resId);
    }

    private String getFileNameExtension() {
        int index = mFileName.lastIndexOf(".");
        if (index >= 0) {
            return mFileName.substring(index + 1);
        }
        return "";
    }

    private String getFileNameWithNoExtension() {
        int index = mFileName.lastIndexOf(".");
        if (index > 0) {
            return mFileName.substring(0, index);
        } else if (index == 0) {
            return null;
        }
        return mFileName;
    }

    protected void setNewFileName() {
        String ext = getFileNameExtension();
        String noExt = getFileNameWithNoExtension();
        String delimiter = ext.isEmpty() ? "" : ".";
        if (noExt == null) {
            setFileName("(1)" + delimiter + ext);
        } else {
            int number = 0;
            int index = noExt.length();
            if (noExt.matches(".*\\(\\d+\\)")) {
                index = noExt.lastIndexOf("(");
                number = Integer.parseInt(
                        noExt.substring(index + 1, noExt.length() - 1));
            }
            ++number;
            setFileName(String.format("%s(%d)%s%s",
                    noExt.substring(0, index), number, delimiter, ext));
        }
    }

    public Notification getNotification() {
        return mNotificationBuilder.build();
    }

    protected Context getContext() {
        return mContext;
    }

    protected void setContext(Context context) {
        mContext = context;
    }

    protected String getHost() {
        return mHost;
    }

    protected void setHost(String host) {
        mHost = host;
    }

    protected int getPort() {
        return mPort;
    }

    protected void setPort(int port) {
        mPort = port;
    }

    protected File getFile() {
        return mFile;
    }

    private void setFile() {
        mFile = new File(AppPreferences.getDownloadDir(getContext()), mFileName);
    }

    protected String getFileName() {
        return mFileName;
    }

    protected void setFileName(String fileName) {
        mFileName = fileName;
        setFile();
    }

    protected String getContentTitle() {
        return mContentTitle;
    }

    protected void setContentTitle(String contentTitle) {
        mContentTitle = contentTitle;
    }

    protected NotificationCompat.Builder getNotificationBuilder() {
        return mNotificationBuilder;
    }

    protected void setNotificationBuilder(NotificationCompat.Builder notificationBuilder) {
        mNotificationBuilder = notificationBuilder;
    }

    protected NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    protected void setNotificationManager(NotificationManager notificationManager) {
        mNotificationManager = notificationManager;
    }

    public abstract int getNotificationId();
}
