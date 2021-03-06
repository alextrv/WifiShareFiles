package org.trv.alex.wifisharefiles;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.trv.alex.wifisharefiles.receivers.WifiDirectBroadcastReceiver;
import org.trv.alex.wifisharefiles.services.FileTransferService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PeersListFragment extends Fragment implements PeerDialog.DialogActions {

    private final IntentFilter mIntentFilter = new IntentFilter();

    private WifiDirectBroadcastReceiver mReceiver;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private Context mContext;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mPeersListView;

    private Uri mSharedUri;
    private List<WifiP2pDevice> mDevicesList;
    private ArrayAdapter<WifiP2pDevice> mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mDevicesList = new ArrayList<>();

        Intent intent = getActivity().getIntent();

        mSharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mContext = getActivity();

        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);

        refreshItems();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_list_fragment, container, false);

        mPeersListView = view.findViewById(R.id.peers_list_view);
        mPeersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = mDevicesList.get(position);
                PeerDialog dialog;
                switch (device.status) {
                    case WifiP2pDevice.AVAILABLE:
                        dialog = PeerDialog.newInstance(PeerDialog.DialogType.CONNECT, device.deviceAddress, device.deviceName);
                        dialog.show(getFragmentManager(), PeerDialog.TAG);
                        break;
                    case WifiP2pDevice.CONNECTED:
                        dialog = PeerDialog.newInstance(PeerDialog.DialogType.CONNECTED, device.deviceAddress, device.deviceName);
                        dialog.show(getFragmentManager(), PeerDialog.TAG);
                        break;
                    case WifiP2pDevice.INVITED:
                    case WifiP2pDevice.FAILED:
                        dialog = PeerDialog.newInstance(PeerDialog.DialogType.DISCONNECT, device.deviceAddress, device.deviceName);
                        dialog.show(getFragmentManager(), PeerDialog.TAG);
                        break;
                }
            }
        });

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshItems();
            }
        });

        updateUI(null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.registerReceiver(mReceiver, mIntentFilter);
        refreshItems();
    }

    @Override
    public void onPause() {
        super.onPause();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.peers_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_refresh_list:
                refreshItems();
                return true;

            case R.id.action_send_file:
                sendFile(mSharedUri);
                return true;

            case R.id.action_receive_file:
                intent = new Intent(getContext(), FileTransferService.class);
                intent.putExtra(FileTransferService.ASYNC_TASK_TYPE,
                        FileTransferService.RECEIVER_ASYNC_TASK);
                mContext.startService(intent);
                return true;

            case R.id.action_settings:
                intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        mReceiver.closeServerSocket();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FilesListActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String path = data.getStringExtra(FilesListActivity.SELECTED_FILE_OR_DIR);
                Uri fileUri = Uri.fromFile(new File(path));
                sendFile(fileUri);
            }
        }
    }

    // Update peers list
    private void refreshItems() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    Toast.makeText(mContext, R.string.p2p_unsupported, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendFile(Uri fileUri) {
        if (fileUri == null || mReceiver.getPeerIP() == null) {
            Toast.makeText(mContext, R.string.error_sending_file, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(getContext(), FileTransferService.class);
        intent.putExtra(FileTransferService.ASYNC_TASK_TYPE,
                FileTransferService.SENDER_ASYNC_TASK);
        intent.putExtra(FileTransferService.HOST, mReceiver.getPeerIP());
        intent.putExtra(FileTransferService.PORT, 8888);
        intent.putExtra(FileTransferService.FILE_PATH, fileUri);
        mContext.startService(intent);
    }

    // Update List content
    public void updateUI(List<WifiP2pDevice> devices) {

        if (devices == null) {
            devices = new ArrayList<>();
        }

        mDevicesList.clear();
        mDevicesList.addAll(devices);

        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<WifiP2pDevice>(mContext,
                    android.R.layout.simple_list_item_2,
                    android.R.id.text1,
                    mDevicesList) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = view.findViewById(android.R.id.text1);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    WifiP2pDevice device = mDevicesList.get(position);
                    text1.setText(device.deviceName);
                    switch (device.status) {
                        case WifiP2pDevice.AVAILABLE:
                            text2.setText(R.string.available);
                            break;

                        case WifiP2pDevice.CONNECTED:
                            text2.setText(R.string.connected);
                            break;

                        case WifiP2pDevice.FAILED:
                            text2.setText(R.string.failed);
                            break;

                        case WifiP2pDevice.UNAVAILABLE:
                            text2.setText(R.string.unavailable);
                            break;

                        case WifiP2pDevice.INVITED:
                            text2.setText(R.string.invited);
                            break;
                    }
                    return view;
                }
            };
            mPeersListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void connect(String deviceAddress) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }

    @Override
    public void disconnect() {
        mManager.cancelConnect(mChannel, null);
        mManager.removeGroup(mChannel, null);
    }

    @Override
    public void pickFile() {
        Intent intent = new Intent(mContext, FilesListActivity.class);
        intent.putExtra(FilesListActivity.CHOOSE_DIR, false);
        startActivityForResult(intent, FilesListActivity.REQUEST_CODE);
    }
}
