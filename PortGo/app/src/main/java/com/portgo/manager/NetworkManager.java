package com.portgo.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.telephony.TelephonyManager;

import com.portgo.R;

public class NetworkManager{
	private static final String WIFI_TAG_NAME = "PortGO";
	
	private WifiManager mWifiManager;
	private WifiLock mWifiLock;
	private boolean mAcquired;
	private boolean mStarted;
	boolean mConnect;
	Context mContext;
	private BroadcastReceiver mNetStatusWatcher;
    NetWorkChangeListner mNetWorkChangeListner;

	static  NetworkManager instance = null;
	static  public NetworkManager getNetWorkmanager(){
		if(instance ==null){
			instance = new NetworkManager();
		}
		return instance;
	}

    interface NetWorkChangeListner{
		void handleNetworkChangeEvent(boolean ethernet,boolean wifiConnect,boolean mobileConnect,boolean netTypeChange);
    }
	
	static public boolean checkNetWorkStatus(Context context,ConfigurationManager configManager ){
		boolean connected = false;
		NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo == null|| !networkInfo.isAvailable()
			      ||!(networkInfo.getState() == State.CONNECTED)) {
			return false;
		}

		int netType = networkInfo.getType();
		int netSubType = networkInfo.getSubtype();

        boolean useWifi = configManager.getBooleanValue(context,ConfigurationManager.PRESENCE_USEWIFI,context.getResources().getBoolean(R.bool.wifi_default));
        boolean use3G = configManager.getBooleanValue(context,ConfigurationManager.PRESENCE_VOIP,
				context.getResources().getBoolean(R.bool.prefrence_voipcall_default));

		if(netType==ConnectivityManager.TYPE_ETHERNET&&networkInfo.isConnected()){
			connected = true;
		}else if (useWifi && (netType == ConnectivityManager.TYPE_WIFI)) {
			connected = true;
		} else if (use3G
				&& (netType == ConnectivityManager.TYPE_MOBILE || netType == ConnectivityManager_TYPE_WIMAX)) {
			if ((netSubType >= TelephonyManager.NETWORK_TYPE_UMTS)
					|| // HACK
					(netSubType == TelephonyManager.NETWORK_TYPE_GPRS)
					|| (netSubType == TelephonyManager.NETWORK_TYPE_EDGE)) {
				connected = true;
			}
		}

		if (!connected) {
			return false;
		}		
		return connected;
	}

	// Will be added in froyo SDK
	private static int ConnectivityManager_TYPE_WIMAX = 6;

	private NetworkManager() {
	}

	public boolean  start(Context context) {
        mContext = context;
		mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		
		if(mWifiManager == null){
			return false;
		}
//
		if(mNetStatusWatcher ==null){
			mConnect = false;
			mNetStatusWatcher = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo = null;
					boolean bEthernet = false;
                    boolean bWifiConnect = false;
                    boolean bMobileConnect = false;

                    ConnectivityManager connManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


					networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
					if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
						bEthernet = true;
					}

                    networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
                        //wifi
						bWifiConnect = true;//
                    }

                    networkInfo= connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    if(networkInfo!=null&& NetworkInfo.State.CONNECTED == networkInfo.getState()){
                        //3g
						bMobileConnect = true;
                    }

                    if(mNetWorkChangeListner!=null) {
						mNetWorkChangeListner.handleNetworkChangeEvent(bEthernet, bWifiConnect, bMobileConnect, false);
					}

				}
			};
		}
		
		if(mNetStatusWatcher!=null)
		{
			IntentFilter intentNetWatcher = new IntentFilter();
			intentNetWatcher.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetStatusWatcher, intentNetWatcher);
		}
		
		mStarted = true;
		return true;
	}

	public boolean stop() {
		if(!mStarted){
			return false;
		}

		if(mNetStatusWatcher!=null){
            mContext.unregisterReceiver(mNetStatusWatcher);
			mNetStatusWatcher = null;
		}
		
		release();
		mStarted = false;
		instance = null;
		return true;
	}
		
	public boolean acquire(ConfigurationManager configurationMgr) {
		if (mAcquired||!mStarted) {
			return true;
		}

		boolean connected = false;
		NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo == null) {
			return false;
		}

		int netType = networkInfo.getType();
		int netSubType = networkInfo.getSubtype();

        boolean useWifi = configurationMgr.getBooleanValue(mContext,ConfigurationManager.PRESENCE_USEWIFI,mContext.getResources().getBoolean(R.bool.wifi_default));
        boolean use3G = configurationMgr.getBooleanValue(mContext,ConfigurationManager.PRESENCE_VOIP,  mContext.getResources().getBoolean(R.bool.prefrence_voipcall_default));
		if(netType == ConnectivityManager.TYPE_ETHERNET){
			connected = true;
		}else if (useWifi && (netType == ConnectivityManager.TYPE_WIFI)){
			if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
				mWifiLock = mWifiManager.createWifiLock(
						WifiManager.WIFI_MODE_FULL, WIFI_TAG_NAME);
				final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				if (wifiInfo != null && mWifiLock != null) {
					final DetailedState detailedState = WifiInfo
							.getDetailedStateOf(wifiInfo.getSupplicantState());
					if (detailedState == DetailedState.CONNECTED
							|| detailedState == DetailedState.CONNECTING
							|| detailedState == DetailedState.OBTAINING_IPADDR) {
						mWifiLock.acquire();
						connected = true;
					}
				}
			} else {

			}
		} else if (use3G
				&& (netType == ConnectivityManager.TYPE_MOBILE || netType == ConnectivityManager_TYPE_WIMAX)) {
			if ((netSubType >= TelephonyManager.NETWORK_TYPE_UMTS)
					|| // HACK
					(netSubType == TelephonyManager.NETWORK_TYPE_GPRS)
					|| (netSubType == TelephonyManager.NETWORK_TYPE_EDGE)) {
				connected = true;
			}
		}

		if (!connected) {
			return false;
		}

		mAcquired = true;
		return true;
	}

	public boolean release() {
		if (mWifiLock != null) {
			if(mWifiLock.isHeld()){
				mWifiLock.release();
			}	
			mWifiLock = null;
		}

		mAcquired = false;
		return true;
	}	

    public void setNetWorkChangeListner(NetWorkChangeListner listner){
        mNetWorkChangeListner = listner;
    }
		
}
