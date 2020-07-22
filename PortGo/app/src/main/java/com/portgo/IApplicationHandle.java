package com.portgo;

import android.app.Activity;
import android.app.PendingIntent;
import android.view.SurfaceView;

import com.portgo.manager.PortSipEngine;

public interface IApplicationHandle {
	void addActivity(Activity act);	
	void removeActivity(Activity act);	
	void closeActivitys();

	void startAVActivity(int callID);

	void setIncomingPending(PendingIntent pi);
	PendingIntent getIncomingPending();
    PortSipEngine getPortSipEngine();
}
