package com.portgo.manager; //attation!!! do not change package name
//javadoc http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html


import android.content.Context;

import com.portsip.PortSipSdk;

import com.portsip.OnAudioManagerEvents;

import java.util.Random;

/**
 * @author PortSIP Solutions, Inc. All rights reserved.
 * @version 15
 */
public class PortSipSdkWrapper extends PortSipSdk {
	static private PortSipSdkWrapper sdkxxx= null;
    OnAudioManagerEvents mAudioManagerEvents;
	private PortSipSdkWrapper() {
		super();
	}

	public static synchronized PortSipSdkWrapper getInstance() {
		if(sdkxxx==null) {
			sdkxxx = new PortSipSdkWrapper();
		}
		return sdkxxx;
	}

    public synchronized int  initialize(Context context,int enum_transport, String localIP, int localSIPPort, int enum_LogLevel,
                                        String LogPath, int maxLines, String agent,
                                        int audioDeviceLayer, int videoDeviceLayer, String TLSCertificatesRootPath,
                                        String TLSCipherList, boolean verifyTLSCertificate, String dnsServers,OnAudioManagerEvents audioManagerEvents){

        if(initialized) {
            uninitialize();
        }
        initialized = true;
        CreateCallManager(context.getApplicationContext());
        super.setAudioManagerEvents(audioManagerEvents);
        return super.initialize(enum_transport, localIP, localSIPPort, enum_LogLevel,
                LogPath, maxLines, agent, audioDeviceLayer,
                videoDeviceLayer,TLSCertificatesRootPath,TLSCipherList, verifyTLSCertificate, dnsServers);
    }

    public boolean isInitialized(){
	    return initialized;
    }

    public synchronized int  initialize(Context context,int enum_transport,int localSIPPort, int enum_LogLevel,
                                        String LogPath, int maxLines, String agent,
                                        String dnsServers,OnAudioManagerEvents audioManagerEvents){

        return initialize(context,enum_transport, "0.0.0.0",localSIPPort, enum_LogLevel,LogPath,
                maxLines, agent,
                0, 0, null,null, false, dnsServers,audioManagerEvents);
    }

    volatile  boolean initialized= false;
    public synchronized void uninitialize() {
        if(initialized) {
            removeUser();
            DeleteCallManager();
        }
        initialized= false;
    }
}
