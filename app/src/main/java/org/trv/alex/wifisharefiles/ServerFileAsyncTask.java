package org.trv.alex.wifisharefiles;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

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
import java.util.UUID;

public class ServerFileAsyncTask extends FileAsyncTask {

    private static final String TAG = "ServerFileAsyncTask";

    private final int NOTIFICATION_ID = 1;

    private ServerSocket mServerSocket;

    public ServerFileAsyncTask(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();
        setContentTitle(getString(R.string.download_completed));
        getNotificationBuilder().setContentTitle(getString(R.string.file_downloading));
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
                setFileName(new String(fileNameBytes, StandardCharsets.UTF_8));
            } else {
                setFileName(new String(fileNameBytes, "UTF-8"));
            }

            File file = getFile();
            int i;
            // Doing limited number of attempts to avoid infinite loop
            for (i = 0; file.exists() && i < Integer.MAX_VALUE; ++i) {
                setNewFileName();
                file = getFile();
            }

            // Almost impossible situation, but who knows...
            if (i == Integer.MAX_VALUE) {
                setFileName(UUID.randomUUID().toString().replace("-", ""));
                file = getFile();
            }

            OutputStream outputStream = new FileOutputStream(file);

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
        setContentTitle(getString(R.string.download_failed));
        if (getFile() != null && getFile().exists()) {
            getFile().delete();
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
