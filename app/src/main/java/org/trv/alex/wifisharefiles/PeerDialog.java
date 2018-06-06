package org.trv.alex.wifisharefiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class PeerDialog extends DialogFragment {

    public static final String TAG = "peerDialog";

    private static final String DIALOG_TYPE = "dialogType";
    private static final String DEVICE_ADDRESS = "deviceAddress";
    private static final String DEVICE_NAME = "deviceName";

    public interface DialogActions {
        void connect(String deviceAddress);
        void disconnect();
        void pickFile();
    }

    public enum DialogType {
        CONNECT,
        CONNECTED,
        DISCONNECT
    }

    private DialogActions mActions;

    public static PeerDialog newInstance(DialogType dialogType, String deviceAddress, String deviceName) {
        Bundle args = new Bundle();
        args.putSerializable(DIALOG_TYPE, dialogType);
        args.putString(DEVICE_ADDRESS, deviceAddress);
        args.putString(DEVICE_NAME, deviceName);
        PeerDialog dialog = new PeerDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActions = (DialogActions) getFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    public void onDetach() {
        mActions = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (mActions == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        DialogType dialogType = (DialogType) getArguments().getSerializable(DIALOG_TYPE);
        final String deviceAddress = getArguments().getString(DEVICE_ADDRESS);
        final String deviceName = getArguments().getString(DEVICE_NAME);

        switch (dialogType) {

            case CONNECT:
                return dialogBuilder
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mActions.connect(deviceAddress);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setTitle(getString(R.string.connect_title, deviceName))
                    .create();

            case CONNECTED:
                return dialogBuilder
                    .setPositiveButton(R.string.disconnect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mActions.disconnect();
                        }
                    })
                    .setNegativeButton(R.string.send_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mActions.pickFile();
                        }
                    })
                    .setTitle(getString(R.string.connected_title, deviceName))
                    .create();

            case DISCONNECT:
                return dialogBuilder
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mActions.disconnect();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setTitle(getString(R.string.disconnect_title, deviceName))
                        .create();

            default:
                return super.onCreateDialog(savedInstanceState);
        }
    }
}
