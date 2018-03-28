package org.trv.alex.wifisharefiles.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.trv.alex.wifisharefiles.PeersListFragment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiDirectBroadcast";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Fragment mFragment;

    private NetworkInfo mNetworkInfo;

    private List<WifiP2pDevice> mPeers = new ArrayList<>();

    private String mPeerIP;

    private ServerSocket mServerSocket;

    private WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {

            Collection<WifiP2pDevice> refreshedList = peers.getDeviceList();
            if (!refreshedList.equals(mPeers)) {
                mPeers.clear();
                mPeers.addAll(refreshedList);
            }

            ((PeersListFragment) mFragment).updateUI(mPeers);

            if (refreshedList.size() == 0) {
                Log.d(TAG, "Empty peer list");
            }

            if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
                mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {

                        if (info.groupFormed && info.isGroupOwner) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (mServerSocket != null && !mServerSocket.isClosed()) {
                                            return;
                                        }
                                        mServerSocket = new ServerSocket(3344);
                                        Socket client = mServerSocket.accept();
                                        mPeerIP = client.getInetAddress().getHostAddress();
                                        mServerSocket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Log.d(TAG, mPeerIP);
                                }
                            }).start();
                        } else if (info.groupFormed) {
                            // Get group owner (server) address
                            mPeerIP = info.groupOwnerAddress.getHostAddress();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Socket socket = new Socket();

                                    try {
                                        socket.bind(null);

                                        int attempts = 10;

                                        do {
                                            socket.connect(new InetSocketAddress(mPeerIP, 3344));
                                            --attempts;
                                        } while (!socket.isConnected() && attempts > 0);

                                    } catch (IOException e) {
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
                                    Log.d(TAG, mPeerIP);
                                }
                            }).start();

                        }

                    }
                });
            }

        }
    };

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       Fragment fragment) {
        mManager = manager;
        mChannel = channel;
        mFragment = fragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }

            mNetworkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (mManager != null) {
                mManager.requestPeers(mChannel, mPeerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // do something
        } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            // Check to see if Wi-Fi Direct is enabled
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "ENABLED");
            } else {
                Log.d(TAG, "DISABLED");
            }

        }

    }

    public String getPeerIP() {
        return mPeerIP;
    }
}
