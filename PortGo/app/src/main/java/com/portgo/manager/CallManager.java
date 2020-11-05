package com.portgo.manager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;

import com.portgo.BuildConfig;
import com.portgo.database.DBHelperBase;
import com.portgo.exception.LineBusyException;
import com.portgo.exception.OutOfMaxLineException;
import com.portgo.manager.PortSipCall.InviteState;
import com.portgo.util.CallReferTask;
import com.portgo.util.CallRule;
import com.portgo.util.NgnListUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnObservableList;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.Ring;
import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import org.w3c.dom.Text;
import org.webrtc.apprtc.AppRTCAudioManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;


public class CallManager implements Observer{
	static  private CallManager instance = new CallManager();
	protected NgnObservableList<PortSipCall> mCalls;
	protected HashMap<Integer,Long> mSubscrid= new HashMap<>();
	protected HashMap<Integer,Long> mSubscridTime= new HashMap<>();

	protected boolean mLoading;
	protected boolean mReady;

	static public int mMaxLine=2;
    volatile PortSipCall transCall;
    volatile boolean inAddingCall;
	private CallManager(){
		if(mCalls == null){
			mCalls = getObservableCalls();
		}
	}
	static public CallManager getInstance(){
		return instance;
	}

	public void clear(){
		if(mCalls != null){
			synchronized (mCalls) {
				mCalls.clear();
			}
		}
	}

    public boolean setAddCall(boolean addCall){
        if(addCall==true&&getCallsSize()>=getMaxLine()){//
            return false;
        }
        inAddingCall = addCall;
        return true;
    }

	Set<PortSipEnumDefine.AudioDevice> audioDevices=new HashSet<>();

	public void setAudioDevices(Set<PortSipEnumDefine.AudioDevice> audioDevices) {
		this.audioDevices.clear();
		this.audioDevices.addAll(audioDevices);
	}

	public Set<PortSipEnumDefine.AudioDevice> getAudioDevices() {
		return audioDevices;
	}

	public boolean getAddCall(){
        return inAddingCall;
    }

    public void setTrnsferCall(PortSipCall sipCall){
        transCall =sipCall;
    }


    public PortSipCall getTrnsferCall(){
        return transCall;
    }

	public boolean isReady(){
		return mReady;
	}

	public NgnObservableList<PortSipCall> getObservableCalls() {
		if(mCalls == null){
			mCalls = new NgnObservableList<PortSipCall>(true);
		}
		return mCalls;
	}

	public PortSipCall getDefaultCall() {
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterInCall());
        }
	}

	public PortSipCall getPendingCall() {
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterInPending());
        }
	}

	public PortSipCall getActiveCall() {//
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterInActive());
        }
	}

	public PortSipCall getCallBySessionId(long sessionId) {
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterBySessionId(sessionId));
        }
	}

    public PortSipCall getCallByCallId(int callId) {
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterByCallId(callId));
        }
    }

	public PortSipCall getCallByIdNot(long callId) {
        synchronized (mCalls) {
            return NgnListUtils.getFirstOrDefault(mCalls.getList(), new PortSipCall.CallFilterBySessionIDNot(callId));
        }
	}

    public boolean setMuteStateForAll(boolean state){
        synchronized (mCalls) {
            List<PortSipCall> calls = mCalls.getList();
            for (PortSipCall portSipCall : calls) {

                if (portSipCall.isMicMute() != state) {
                    portSipCall.setMuteMicrophone(state);
                }
            }
        }
		return state;
	}

	public void addCall(PortSipCall call) {
		synchronized (mCalls) {
			call.addObserver(this);
			mCalls.add(call);
		}
	}

	public void updateCall(PortSipCall call) {
		// TODO Auto-generated method stub		
	}

	private void deleteCall(PortSipCall call) {
    	synchronized (mCalls) {
			mCalls.remove(call);
		}
	}

//	public void deleteCall(long callId) {
//		synchronized (mCalls) {
//			mCalls.remove(getCallBySessionId(callId));
//		}
//	}


	public int getCallsSize() {
		synchronized (mCalls) {
			if (mCalls != null) {
				return mCalls.getList().size();
			}
		}
		return 0;
	}

	static public int getMaxLine(){
		return mMaxLine;
	}

	public void addAllCallsToConfrence(){
        synchronized (mCalls) {
            List<PortSipCall> calls = mCalls.getList();
            for (PortSipCall portSipCall : calls) {
                portSipCall.stopMediaRecord();
                if (portSipCall.isLocalHeld()) {
                    portSipCall.unHold();
                }
                portSipCall.joinToConference();
            }
        }
	}

	public void unholdAllcallsExcept(PortSipCall call){

		synchronized (mCalls) {
			List<PortSipCall> calls = mCalls.getList();
			for (Iterator<PortSipCall> iterator = calls.iterator(); iterator.hasNext(); ) {
				PortSipCall portSipCall = (PortSipCall) iterator.next();
				if (portSipCall != call && portSipCall != null
						&& portSipCall.isLocalHeld() == true) {
					portSipCall.unHold();
				}
			}
		}
	}

	public void holdAllcallsExcept(PortSipCall call){

		synchronized (mCalls) {
			List<PortSipCall> calls = mCalls.getList();
			for (Iterator<PortSipCall> iterator = calls.iterator(); iterator.hasNext(); ) {
				PortSipCall portSipCall = (PortSipCall) iterator.next();
				if (portSipCall != call && portSipCall != null
						&& portSipCall.isLocalHeld() == false) {
					portSipCall.hold();
				}
			}
		}
	}

    public void setConferenceView(PortSIPVideoRenderer view){
        setRemoteViewID(-1,null);
        SipManager.getSipManager().setConferenceVideoWindow(view);//
    }
    public void setRemoteViewID(long sessionID,PortSIPVideoRenderer view){
        synchronized (mCalls) {
            List<PortSipCall> calls = mCalls.getList();
            PortSipCall findcall = null;
            for (PortSipCall portSipCall : calls) {
                if (portSipCall != null) {
                    if (portSipCall.getSessionId() != sessionID) {
                        portSipCall.setremoteView(portSipCall.isVideoCall(), null);
                    } else {
                        findcall = portSipCall;
                    }
                }
                if (findcall != null) {
                    findcall.setremoteView(findcall.isVideoCall(), view);
                }
            }
        }
	}

    public void sendDtmfConference(int enum_dtmftype,char code,boolean playtone){
        synchronized (mCalls) {
            List<PortSipCall> calls = mCalls.getList();
            for (PortSipCall portSipCall : calls) {
                if (portSipCall != null)
                    portSipCall.sendDTMF(enum_dtmftype, code,playtone);
            }
        }
    }

	public void unholdAllCallExcept(PortSipCall call){
        synchronized (mCalls) {
            List<PortSipCall> calls = mCalls.getList();
            for (PortSipCall portSipCall : calls) {
                if (portSipCall.isLocalHeld() && portSipCall != call)
                    portSipCall.unHold();
            }
        }
	}
	

    public void holdAllCallExcept(PortSipCall call){
		synchronized (mCalls) {
			List<PortSipCall> calls = mCalls.getList();
			for (PortSipCall portSipCall : calls) {
				if (!portSipCall.isLocalHeld() && portSipCall != call)
					portSipCall.hold();
			}
		}
	}

     public void terminateAllCalls(Context context){
    	synchronized (mCalls) {
			List<PortSipCall> calls = mCalls.getList();
			for (PortSipCall portSipCall : calls) {
				if (!portSipCall.isLocalHeld())
					portSipCall.terminate(context);
			}
		}
	}

	/**

	 * @param context
	 * @param sipManager
	 * @param calleeDispalyName
	 * @param callee
	 * @param enum_mediatype
	 * @return
	 * @throws OutOfMaxLineException
	 */

    public PortSipCall portSipCallOut(Context context,SipManager sipManager,
            int accountid,String local,String calleeDispalyName,int remoteID,String callee, int enum_mediatype) throws OutOfMaxLineException{
        //匹配拨号计划
        String selection = DBHelperBase.CallRuleColumns.CALL_RULE_ACCOUNT_ID +"=?";
        Cursor ruleCursor = CursorHelper.resolverQuery(context.getContentResolver(),DBHelperBase.CallRuleColumns.CONTENT_URI,null,
                selection,new String[]{""+accountid}, DBHelperBase.CallRuleColumns.DEFAULT_ORDER);
        CallRule matchedRule = null;
        while (CursorHelper.moveCursorToNext(ruleCursor)){
            CallRule rule = CallRule.callRuleFromCursor(ruleCursor);
            String matcher = rule.getMatcher();
            if (rule.isValidate()&&
					(((!TextUtils.isEmpty(matcher))&&callee.startsWith(matcher))||TextUtils.isEmpty(matcher))){
                matchedRule  =rule;
                break;
            }
        }
        CursorHelper.closeCursor(ruleCursor);
        //完成匹配
//        long sessionID = PortSipErrorcode.INVALID_SESSION_ID;
        PortSipCall call;
        if(mMaxLine<= getCallsSize())
        {
            call = new PortSipCall(context,sipManager,null,PortSipErrorcode.INVALID_SESSION_ID,matchedRule,
					NgnMediaType.getTypeByIndex(enum_mediatype),local,remoteID,callee,calleeDispalyName,true); //add a call history
			call.terminate(context);
            HistoryAVCallEvent event = call.getHistoryEvent();
            event.createGroup(context.getContentResolver());
            ContentValues values =event.getContentValues();
            values.put(DBHelperBase.HistoryColumns.HISTORY_GROUP,event.getGroup());
            context.getContentResolver().insert(DBHelperBase.HistoryColumns.CONTENT_URI,values);
            throw new OutOfMaxLineException();
        }

        String cost="";
        //make new call
        call = new PortSipCall(context,sipManager,null,PortSipErrorcode.INVALID_SESSION_ID,matchedRule,NgnMediaType.getTypeByIndex(enum_mediatype),local,remoteID,callee,calleeDispalyName,true);
        addCall(call);
        call.setSpeakerLouder(false);

        HistoryAVCallEvent event = call.getHistoryEvent();
        event.createGroup(context.getContentResolver());
        ContentValues values =event.getContentValues();
        values.put(DBHelperBase.HistoryColumns.HISTORY_GROUP,event.getGroup());
        context.getContentResolver().insert(DBHelperBase.HistoryColumns.CONTENT_URI,values);
        return call;
    }

	public PortSipCall portSipCallIn(Context context, SipManager sipManager, CallReferTask referTask,long sessionId, String local,
									 int remoteID, String caller, String callerDisplayName, int enum_mediatype) throws OutOfMaxLineException,LineBusyException{
		
		PortSipCall call = new PortSipCall(context,sipManager,referTask,sessionId,null,NgnMediaType.getTypeByIndex(enum_mediatype),
                local,remoteID,caller,callerDisplayName,false);
		call.setVideoNegotiateResult(call.isVideoCall());
        HistoryAVCallEvent event = call.getHistoryEvent();
		event.createGroup(context.getContentResolver());
		ContentValues values =event.getContentValues();
		values.put(DBHelperBase.HistoryColumns.HISTORY_GROUP,event.getGroup());
        context.getContentResolver().insert(DBHelperBase.HistoryColumns.CONTENT_URI,values);

		if (getPendingCall()!=null) {
			call.reject(context);
			throw new LineBusyException();
		}
		
		if(mMaxLine< getCallsSize())
		{
			call.reject(context);
			throw new OutOfMaxLineException();
		}
		addCall(call); 
		call.setSpeakerLouder(true);
		
		return call;
	}
	void allCallClose(){
		if(CallManager.getInstance().getCallsSize()==1){

		}
	}

	void firstCallIn(){
		if(CallManager.getInstance().getCallsSize()==1){

            PortSipSdkWrapper sdk = PortSipSdkWrapper.getInstance();
            if(sdk.isInitialized()){
            	Set<PortSipEnumDefine.AudioDevice> audioDevices = sdk.getAudioDevices();
				if(audioDevices.contains(PortSipEnumDefine.AudioDevice.WIRED_HEADSET)) {
					sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.WIRED_HEADSET);
				}else if(audioDevices.contains(PortSipEnumDefine.AudioDevice.BLUETOOTH)) {
					sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.BLUETOOTH);
				}else if(audioDevices.contains(PortSipEnumDefine.AudioDevice.EARPIECE)){
					sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.EARPIECE);
				}else if(audioDevices.contains(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)){
					sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE);
				}else{
					sdk.setAudioDevice(PortSipEnumDefine.AudioDevice.EARPIECE);
				}
            }
		}
	}

	boolean set=false;
	@Override
	public void update(Observable arg0, Object arg1) {
		if(arg0 instanceof PortSipCall){
			PortSipCall call = (PortSipCall) arg0;
			if(call.getState()==InviteState.INCALL&&!set){
                set = true;
				firstCallIn();
			}else if(call.getState()==InviteState.TERMINATING&&!call.isAddToHistory()){
                set =false;
				allCallClose();
				call.setAddToHistory(true);
			} else if(call.getState()==InviteState.TERMINATED){
				deleteCall(call);
				if( getCallsSize()<=1){
                    SipManager.getSipManager().destroyConfrence();
                }
			}
		}
	}

	public HashMap<Integer,Long> getSubscrib(){
		return  mSubscrid;
	}

	public Long getSubscribId(int contactId){
		return  mSubscrid.get(contactId);
	}

	public void putsubScribeId(long subScribeId,int contactId){
		mSubscrid.put(contactId,subScribeId);
	}

	public Long getSubscribedTime(int contactId){
		return  mSubscridTime.get(contactId);
	}

	public void putsubScribedTime(int contactId,long subScribeTime){
		mSubscridTime.put(contactId,subScribeTime);
	}

	public void clearSubScribedTime(){
		mSubscridTime.clear();
	}

	public void removeSubScribeIdBySubId(long subScribeId){
		Iterator iter = mSubscrid.entrySet().iterator();
		int removekey = -1;
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Integer key = (Integer) entry.getKey();
			Long val = (Long) entry.getValue();
			if(subScribeId == val){
				removekey = key;
				break;
			}
		}
		if(removekey>=0){
			mSubscrid.remove(removekey);
		}
	}

	public void removeSubScribeIdByContactid(int contactId){
		mSubscrid.remove(contactId);
	}

	public void clearSubScribeId(){
		mSubscrid.clear();
	}

	static public HistoryAVCallEvent getLatestHistory(Context context,String localReam){
		HistoryAVCallEvent latest=null;
		String selection = DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=?";
		Cursor cursor = CursorHelper.resolverQuery(context.getContentResolver(),DBHelperBase.ViewHistoryColumns.CONTENT_URI,null,selection, new String[]{localReam},
				DBHelperBase.HistoryColumns.DEFAULT_ORDER);

		if(CursorHelper.moveCursorToFirst(cursor)){
			latest = HistoryAVCallEvent.historyAVCallEventFromViewCursor(context,cursor);
		}

		CursorHelper.closeCursor(cursor);
		return latest;
	}

}
