package com.portgo.manager;

import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.portgo.R;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;


public class PortSipEngine{
	SipManager mSipMgr;

	PortSipSdkWrapper mSdk;
    Context mContext;
	static PortSipEngine instance;
	public static synchronized PortSipEngine getInstance(Context context){
		if(instance ==null){
			instance = new PortSipEngine(context);
		}
		return instance;
	}

	private PortSipEngine(Context context){
        mContext = context;
		mSdk= PortSipSdkWrapper.getInstance();
		mSipMgr = SipManager.getSipManager();

	}
	
	public SipManager getSipManager(){
		return mSipMgr;
	}

	public boolean unInit(){
		if(mSdk.isInitialized()) {
			mSdk.unRegisterServer();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mSdk.uninitialize();
		}
		ContactManager.getInstance().stop();
		mSipMgr= null;
		mSdk = null;
		instance = null;
		return true;
	}
}
