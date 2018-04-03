package org.trv.alex.wifisharefiles;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.trv.alex.wifisharefiles.receivers.CancelServiceReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientFileAsyncTask extends FileAsyncTask {

    private final int NOTIFICATION_ID = 2;

    public ClientFileAsyncTask(String host, int port, Context context) {
        mHost = host;
        mPort = port;
        mContext = context;

        mTitleCompleted = mContext.getString(R.string.sending_completed);

        Intent intent = new Intent(mContext, CancelServiceReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action = new NotificationCompat.Action
                .Builder(android.R.drawable.ic_menu_close_clear_cancel,
                mContext.getString(R.string.cancel),
                pendingIntent).build();

        mBuilder = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.file_sending))
                .setContentText("0%")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .addAction(action);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected Void doInBackground(Uri... params) {

        if (params.length == 0) {
            return null;
        }

        FileProperties fileProperties = FileProperties.getInstance(mContext, params[0]);

        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mHost, mPort));

            OutputStream outputStream = socket.getOutputStream();
            ContentResolver contentResolver = mContext.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(fileProperties.getUri());

            if (inputStream == null) {
                return null;
            }

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);

            mFileName = fileProperties.getName();

            long fileSize = fileProperties.getSize();
            int fileNameSize = mFileName.getBytes().length;
            byte[] serviceBuff = ByteBuffer.allocate(1024)
                    .putLong(fileSize)
                    .putInt(fileNameSize)
                    .put(mFileName.getBytes())
                    .array();

            bos.write(serviceBuff);

            long sent = transferData(bis, bos, fileSize);

            if (sent != fileSize) {
                mTitleCompleted = mContext.getString(R.string.sending_failed);
            }

            bos.close();
            bis.close();

        } catch (IOException e) {
            mTitleCompleted = mContext.getString(R.string.sending_failed);
            e.printStackTrace();
        } finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
