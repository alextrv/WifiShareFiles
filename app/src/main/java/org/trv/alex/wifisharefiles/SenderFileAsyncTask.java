package org.trv.alex.wifisharefiles;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SenderFileAsyncTask extends FileAsyncTask {

    private final int NOTIFICATION_ID = 2;

    public SenderFileAsyncTask(Context context, String host, int port) {
        super(context, host, port);
    }

    @Override
    protected void init() {
        super.init();
        setContentTitle(R.string.sending_completed);
        getNotificationBuilder().setContentTitle(getString(R.string.file_sending));
    }

    @Override
    protected Void doInBackground(Uri... params) {

        if (params.length == 0) {
            return null;
        }

        FileProperties fileProperties = FileProperties.getInstance(getContext(), params[0]);
        if (fileProperties == null) {
            setContentTitle(R.string.sending_failed);
            return null;
        }

        try (Socket socket = new Socket();
             InputStream inputStream = fileProperties.openInputStream()) {

            socket.bind(null);
            socket.connect(new InetSocketAddress(getHost(), getPort()));

            try (OutputStream outputStream = socket.getOutputStream();
                 BufferedInputStream bis = new BufferedInputStream(inputStream);
                 BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {

                String fileName = fileProperties.getName();
                setFileName(fileName);

                long fileSize = fileProperties.getSize();
                int fileNameSize = fileName.getBytes().length;
                byte[] serviceBuff = ByteBuffer.allocate(1024)
                        .putLong(fileSize)
                        .putInt(fileNameSize)
                        .put(fileName.getBytes())
                        .array();

                bos.write(serviceBuff);

                long sent = transferData(bis, bos, fileSize);

                if (sent != fileSize) {
                    setContentTitle(R.string.sending_failed);
                }
            }

        } catch (IOException e) {
            setContentTitle(R.string.sending_failed);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
