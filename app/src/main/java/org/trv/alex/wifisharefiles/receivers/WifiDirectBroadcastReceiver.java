package org.trv.alex.wifisharefiles.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.HandlerThread;
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

    private HandlerThread mServerThread;
    private HandlerThread mClientThread;
    private Handler mServerHandler;
    private Handler mClientHandler;

    private boolean mIsServerRunning;
    private boolean mIsClientRunning;

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
            findPeerIP();
        }
    };

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       Fragment fragment) {
        mManager = manager;
        mChannel = channel;
        mFragment = fragment;
        mServerThread = new HandlerThread("SERVER");
        mServerThread.start();
        mServerHandler = new Handler(mServerThread.getLooper());
        mClientThread = new HandlerThread("CLIENT");
        mClientThread.start();
        mClientHandler = new Handler(mClientThread.getLooper());
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }

            mNetworkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            findPeerIP();

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

    public void findPeerIP() {
        if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(final WifiP2pInfo info) {

                    if (info.groupFormed && info.isGroupOwner) {
                        if (mServerSocket != null && !mServerSocket.isClosed()) {
                            return;
                        }
                        if (!mIsServerRunning) {
                            mServerHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mIsServerRunning = true;
                                    while (true) {
                                        if (!mIsServerRunning) {
                                            return;
                                        }
                                        try {
                                            mServerSocket = new ServerSocket(3344);
                                            Socket client = mServerSocket.accept();
                                            mPeerIP = client.getInetAddress().getHostAddress();
                                            mServerSocket.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d(TAG, mPeerIP + "");
                                    }
                                }
                            });
                        }
                    } else if (info.groupFormed) {
                        // Get group owner (server) address
                        mPeerIP = info.groupOwnerAddress.getHostAddress();

                        if (!mIsClientRunning) {
                            mClientHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mIsClientRunning = true;
                                    Socket socket = null;
                                    while (true) {
                                        try {
                                            socket = new Socket();
                                            socket.setReuseAddress(true);
                                            if (!socket.isBound()) {
                                                socket.bind(null);
                                            }
                                            socket.connect(new InetSocketAddress(mPeerIP, 3344));
                                            Log.d(TAG, mPeerIP + "");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (socket != null && socket.isConnected()) {
                                            try {
                                                socket.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            });
                        }

                    }

                }
            });
        }

    }

    public String getPeerIP() {
        return mPeerIP;
    }

    public void closeServerSocket() {
        mIsServerRunning = false;
        if (mServerSocket != null && !mServerSocket.isClosed()) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
