package org.trv.alex.wifisharefiles.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.trv.alex.wifisharefiles.services.FileTransferService;

public class CancelServiceReceiver extends BroadcastReceiver {

    private static final String TAG = "CancelServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "canceled");
        context.stopService(new Intent(context, FileTransferService.class));
    }
}
