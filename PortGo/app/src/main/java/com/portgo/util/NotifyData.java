package com.portgo.util;

import java.util.Observable;

public class NotifyData{

	public enum Action{
		ACTIONT_ADD,
		ACTIONT_ADD_CONNECTION,
		ACTIONT_REMOVE,
		ACTIONT_REMOVE_CONNECTION,
		ACTIONT_CLEAR,
		ACTIONT_UPDATE,
	}
	Observable mObservable;
	Object mObject;
	boolean mchlid=false;
	Action aciton = Action.ACTIONT_UPDATE;

	public NotifyData(Observable observable, Object data,Action aciton) {
		this.mObservable= observable;
		this.mObject = data;
		this.aciton = aciton;
	}

	public Action getAction(){
		return this.aciton;
	}
	public Object getObject() {
		return mObject;
	}
	public Observable getObservable() {
		return mObservable;
	}
}