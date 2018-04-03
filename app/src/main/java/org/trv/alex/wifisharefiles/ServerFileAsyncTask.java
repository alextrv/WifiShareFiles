package org.trv.alex.wifisharefiles;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

import org.trv.alex.wifisharefiles.receivers.CancelServiceReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServerFileAsyncTask extends FileAsyncTask {

    private static final String TAG = "ServerFileAsyncTask";

    private final int NOTIFICATION_ID = 1;

    private ServerSocket mServerSocket;

    public ServerFileAsyncTask(Context context) {
        mContext = context;

        mTitleCompleted = mContext.getString(R.string.download_completed);

        Intent intent = new Intent(mContext, CancelServiceReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action = new NotificationCompat.Action
                .Builder(android.R.drawable.ic_menu_close_clear_cancel,
                mContext.getString(R.string.cancel),
                pendingIntent).build();

        mBuilder = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.file_downloading))
                .setContentText("0%")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .addAction(action);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected Void doInBackground(Uri... params) {

        try {
            mServerSocket = new ServerSocket(8888);
            Socket client = mServerSocket.accept();
            InputStream inputStream = client.getInputStream();

            byte[] serviceBuff = new byte[1024];
            int len = inputStream.read(serviceBuff, 0, serviceBuff.length);

            ByteBuffer byteBuffer = ByteBuffer.wrap(serviceBuff);

            long fileSize = byteBuffer.getLong();
            int fileNameSize = byteBuffer.getInt();

            byte[] fileNameBytes = new byte[fileNameSize];

            byteBuffer.get(fileNameBytes, 0, fileNameBytes.length);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
            } else {
                mFileName = new String(fileNameBytes, "UTF-8");
            }

            mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    mFileName);

            OutputStream outputStream = new FileOutputStream(mFile);

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);

            long received = transferData(bis, bos, fileSize);

            bos.close();
            bis.close();

            if (received != fileSize) {
                clearFailedDownload();
            }

            closeSocket();

        } catch (IOException e) {
            clearFailedDownload();
            e.printStackTrace();
        }

        return null;

    }

    private void clearFailedDownload() {
        mTitleCompleted = mContext.getString(R.string.download_failed);
        if (mFile != null && mFile.exists()) {
            mFile.delete();
        }
    }

    public void closeSocket() {
        try {
            if (mServerSocket != null && !mServerSocket.isClosed()) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

}
