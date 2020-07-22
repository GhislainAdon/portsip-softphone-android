package com.portgo.manager;

import android.content.Context;
import android.graphics.Rect;
import android.view.SurfaceView;

import com.portgo.database.DBHelperBase;
import com.portgo.util.CallReferTask;
import com.portgo.util.CallRule;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnObservableObject;
import com.portgo.util.NgnPredicate;
import com.portgo.util.NgnTimer;
import com.portgo.util.NgnUriUtils;
import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipErrorcode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

public class PortSipCallGroup extends NgnObservableObject{
    final int mcallID;
	long mSessionID = PortSipErrorcode.INVALID_SESSION_ID;
	CallRule mCallRule= null;
	NgnMediaType mEnum_CallType = NgnMediaType.Audio;

    private boolean mLocalHold = false;
    private boolean mRemoteHold = false;
    private boolean mRecord = false;
	private boolean mSendVideo = true;
    private boolean mMuteRecord = false;
    private boolean mSpeakerLouder = false;
    private boolean mearlyMedia = false;
    private boolean mToHistory = false;
    private boolean mVideoConnect = false;
	private boolean dtmfStatus = false;
	private String dtmfMessageRecord = "";
    private Contact attachContact = null;
    InviteState mCallState = InviteState.NONE;
    String mCdt = null;
	private final HistoryAVCallEvent mHistoryEvent;
	private SipManager mSipMgr;
	boolean called = false;
	private final SimpleDateFormat sDurationTimerFormat = new SimpleDateFormat("HH:mm:ss");
	Rect videoSize = new Rect(0,0,352,288);
	NgnTimer ngnTimer = null;

    public enum InviteState{
        NONE,
        TRIING,
        INCOMING,
        INPROGRESS,
        REMOTE_RINGING,
        EARLY_MEDIA,
        INCALL,
        TERMINATING,
        TERMINATED;
        public static InviteState valueof(int ordinal){
            if(ordinal>-1&&values().length>ordinal)
                return values()[ordinal];
            return values()[0];
        }

    }

    static public final int MEDIATYPE_AUDIO = NgnMediaType.Audio.ordinal();
    static public final int MEDIATYPE_AUDIOVIDEO = NgnMediaType.AudioVideo.ordinal();
    static public final int MEDIATYPE_VIDEO = NgnMediaType.Video.ordinal();

    public PortSipCallGroup(SipManager sipMgr, CallReferTask referTask, long sessionId, CallRule rule, NgnMediaType type, String local ,
                            int remote, String remoteUri, String remoteDisName, boolean inout) {
        mSipMgr = sipMgr;
        mcallID =new Random().nextInt();
        mSessionID = sessionId;
        mHistoryEvent = new HistoryAVCallEvent(mcallID,local,remote,remoteDisName,type,inout);
        mHistoryEvent.setStartTime(new Date().getTime());
		mHistoryEvent.setEndTime(mHistoryEvent.getStartTime());
		mCallRule = rule;
        mEnum_CallType = type;
        mHistoryEvent.setConnected(false);
		mHistoryEvent.setRemoteUri(remoteUri);
        sDurationTimerFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        if(inout){
            setState(InviteState.INPROGRESS,"");
        }else {
            setState(InviteState.INCOMING,"");
            if(referTask!=null&&referTask.timeAvailable()) {
                ngnTimer = new NgnTimer();
                ngnTimer.schedule( referTask,referTask.getDelayTime());
            }
        }

		if(isOutGoingCall()&&!isCalled()){//
			callOut();//呼出
		}

	}

	public void setAttactContact(Contact contact){
        attachContact = contact;
    }

    public Contact getAttachContact() {
        return attachContact;
    }

    public long accept(Context context, SurfaceView remoteSurfaceView, boolean acceptVideo){
        boolean hasVideo = isVideoNegotiateSucess();//底层编码器协商结果
		if(ngnTimer!=null){//接通通话，停止转移功能
			ngnTimer.cancel();
		}
        hasVideo = setVideoNegotiateResult(hasVideo&&acceptVideo);//最终用户选择结果
        mSipMgr.stopRing();////when answer/hangup by me or answer/reject by remote .stop ring tone
        int iReturn = mSipMgr.portSipAnswerCall(mSessionID,remoteSurfaceView, hasVideo);
        mHistoryEvent.setConnected(true);
        mHistoryEvent.setConnectTime(new Date().getTime());//呼叫建立，更新呼叫建立时间和结束时间
		mHistoryEvent.setEndTime(mHistoryEvent.getConnectTime());
        if(iReturn==PortSipErrorcode.ECoreErrorNone){
            if(hasVideo){
                setSendVideo(hasVideo);
            }
            setSpeakerLouder(hasVideo);
            setState(InviteState.INCALL,null);
        }else {
            terminate(context);
        }
        mHistoryEvent.setRead(true);
        HistoryAVCallEvent event = getHistoryEvent();
        context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI,event.getContentValues(),
                DBHelperBase.HistoryColumns.HISTORY_CALLID+"="+event.getCallid(),null);
        return iReturn;
    }

    public void remoteAccept(Context context,boolean existsVideo,
                             SurfaceView remoteView) {
        mSipMgr.stopRingback();
        boolean hasVideo = setVideoNegotiateResult(existsVideo);
		mHistoryEvent.setConnected(true);
		mHistoryEvent.setConnectTime(new Date().getTime());//呼叫建立，更新呼叫建立时间和结束时间
		mHistoryEvent.setEndTime(mHistoryEvent.getConnectTime());
        setState(InviteState.INCALL, null);

        if (isVideoNegotiateSucess()) {
            if (hasVideo) {
                setSendVideo(hasVideo);
            }
        }

        setSpeakerLouder(isVideoNegotiateSucess());
        mSipMgr.portSipRemoteAnswer(getSessionId(), existsVideo, remoteView);
        HistoryAVCallEvent event = getHistoryEvent();
        context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI,event.getContentValues(),
                DBHelperBase.HistoryColumns.HISTORY_CALLID+"="+event.getCallid(),null);
        return;
    }

    synchronized public long terminate(Context context){
        mSipMgr.stopRingback();//when answer/hangup by me or answer/reject by remote .stop ring tone
        if(ngnTimer!=null) {//挂断，停止转移功能
            ngnTimer.cancel();
        }
        int result = mSipMgr.portSipTerminateCall(mSessionID,isVideoNegotiateSucess());
        setState(InviteState.TERMINATING,null);
        setState(InviteState.TERMINATED,null);
        mHistoryEvent.setEndTime(new Date().getTime());//呼叫结束，更新呼叫结束时间
        HistoryAVCallEvent event = getHistoryEvent();
        context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI,event.getContentValues(),
                DBHelperBase.HistoryColumns.HISTORY_CALLID+"="+event.getCallid(),null);
        return result;
    }

    //restrict to used to hangup incoming call
    public long reject(Context context){
        if(ngnTimer!=null) {//拒绝通话，停止转移功能
            ngnTimer.cancel();
        }
        mSipMgr.stopRing();////when answer/hangup by me or answer/reject by remote .stop ring tone
        int result = mSipMgr.portSipRejectCall(mSessionID);
        setState(InviteState.TERMINATING,null);
        mHistoryEvent.setEndTime(new Date().getTime());//呼叫结束，更新呼叫结束时间
        mHistoryEvent.setRead(true);
        setState(InviteState.TERMINATED,null);

        HistoryAVCallEvent event = getHistoryEvent();
        context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI,event.getContentValues(),
                DBHelperBase.HistoryColumns.HISTORY_CALLID+"="+event.getCallid(),null);
        return result;
    }


    public long remoteTerminate(Context context,String status){
        if(mHistoryEvent.getCallOut()){//out going
            mSipMgr.stopRingback();////when answer/hangup by me or answer/reject by remote .stop ring tone
        }else {
            mSipMgr.stopRing();////when answer/hangup by me or answer/reject by remote .stop ring tone
        }

        if(ngnTimer!=null) {//对方挂断，停止转移功能
            ngnTimer.cancel();
        }
        int result =PortSipErrorcode.ECoreErrorNone;
        setState(InviteState.TERMINATING,status);
        setState(InviteState.TERMINATED,status);
        mSipMgr.portSipRemoteClose(getSessionId(),isVideoNegotiateSucess());

        HistoryAVCallEvent event = getHistoryEvent();
		event.setEndTime(new Date().getTime());
        context.getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI,event.getContentValues(),
                DBHelperBase.HistoryColumns.HISTORY_CALLID+"="+event.getCallid(),null);
        return result;
    }


    public void callOut(){
        called =true;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                String call = NgnUriUtils.replaceInvalidUriString(mHistoryEvent.getRemoteUri());
				if(mCallRule!=null)
				{
					call = mCallRule.getRuledUrl(call);
				}
                mSessionID = mSipMgr.portSipMakeCall(call , mEnum_CallType.ordinal());
                if(mSessionID<PortSipErrorcode.ECoreErrorNone){
					setState(InviteState.TERMINATING,"call failed");
                    setState(InviteState.TERMINATED,"call failed");
                }
//            }
//        }).start();
    }

    public boolean isCalled() {
        return called;
    }

    public void setearlyMedia(boolean earlyMedia) {
		mearlyMedia = earlyMedia;
	}
	
	public boolean getearlyMedia() {
		return mearlyMedia;
	}
	


    public void setAddToHistory(boolean addHistory){
        mToHistory =addHistory;
    }

    public  boolean isAddToHistory(){
        return mToHistory;
    }

		
	public boolean setVideoNegotiateResult(boolean existVideo){
		 return mVideoConnect = existVideo;
	}
	
	//// is video channel created successfully.when video channel created successfully,we still can not send local video.(see: issendvideo)
	public boolean isVideoNegotiateSucess() {
		return mVideoConnect;
	}
	
	public boolean isSpeakerLouder() {
		return mSpeakerLouder;
	}
	
	public void setRemoteHold(boolean hold) {
		mRemoteHold = hold;

		this.setChanged();
		this.notifyObservers();
	}
	
	public boolean isRemoteHold() {
		return mRemoteHold;
	}

//	public String getCallTime(){
//		Date date;
//		if(mHistoryEvent.getConnect()) {
//			date = new Date(getEn - getStartTime());
//		}else{
//			date = new Date(new Date().getTime() - getStartTime());
//		}
//		return sDurationTimerFormat.format(date);
//	}

	public void setremoteView(boolean existsVideo,	PortSIPVideoRenderer remoteView){
		mSipMgr.setRemoteView(mSessionID, remoteView);
	}
	
	public InviteState getState(){
		return mCallState;
	}
	
	
	public int startMediaRecord(String filePath) {
		int result =PortSipErrorcode.ECoreArgumentNull;
		
		if (filePath == null || filePath.length() <= 0) {
			return result;
		}
		if(mRecord == true){
			return result = PortSipErrorcode.ECoreErrorNone;
		}
		if (mSessionID != PortSipErrorcode.INVALID_SESSION_ID) {

			String fileName = NgnUriUtils.getUserName(getRemoteParty());
//			result = mSipMgr.startMediaRecord(mSessionID, filePath,fileName);
			mRecord = true;
		}
		
		return result;
	}
	
	public void stopMediaRecord() {
		if(mRecord==true){
			mRecord =false;
			mSipMgr.stopMediaRecord(mSessionID);
		}
		return ;
	}
	
	public boolean isMediaRecord(){
		return mRecord;
	}

	//restrict to used to hangup outgoing call
	public void remoteRing(){
		setState(InviteState.REMOTE_RINGING,null);
	}
	
	public void remoteTring(){
		setState(InviteState.TRIING,null);
	}
	

	public long transfer(String referto){
		int result =mSipMgr.portSipReferOut(mSessionID,referto);
		return result;
	}

	public boolean isDtmfStatus(){
        return dtmfStatus;
    }

    public void setDtmfStatus(boolean dtmfStatus) {
        this.dtmfStatus = dtmfStatus;
    }

    public boolean isLocalHeld(){
		return mLocalHold;
	}
	
	public boolean isOutGoingCall(){
		return mHistoryEvent.getCallOut();
	}
	
	public void updateMediaType(NgnMediaType mediaType){
		if(mediaType!= mEnum_CallType){
			mEnum_CallType =mediaType;
			super.setChangedAndNotifyObservers(mCallState);
		}
	}
	
	public boolean setMuteMicrophone(boolean state){
        mMuteRecord =state;
        mSipMgr.setMicState(mSessionID, mMuteRecord);
		return mMuteRecord;
	}	
	
	public boolean setSpeakerLouder(boolean state){
		mSpeakerLouder =state;
		return mSpeakerLouder;
	}
	
	
	public boolean isMicMute(){
		return mMuteRecord;
	}
	
	public long hold(){
		mLocalHold = true;
		int iResult = PortSipErrorcode.ECoreErrorNone;
		if(mSessionID != PortSipErrorcode.INVALID_SESSION_ID){
			iResult = mSipMgr.portSipHold(mSessionID);
		}
		return iResult;
	}

	public void sendDTMF(int enum_dtmftype,char code,boolean playtone){
		dtmfMessageRecord+=code;
		mSipMgr.portSipSendDtmf(mSessionID,enum_dtmftype, code,playtone);
	}

	public String getDtmfMessageRecord(){
		return dtmfMessageRecord;
	}
	public long unHold(){
		mLocalHold = false;
		int iResult = PortSipErrorcode.ECoreErrorNone;
		if(mSessionID != PortSipErrorcode.INVALID_SESSION_ID){
			iResult = mSipMgr.portSipUnHold(mSessionID);
		}

		return iResult;
		
	}
	
	public long getSessionId(){
		return mSessionID;
	}
    public int getCallId(){
        return mcallID;
    }
	
	public int getCallType(){		
		return mEnum_CallType.ordinal();
	}
	
	public boolean isVideoCall(){		
		return mEnum_CallType==NgnMediaType.AudioVideo||mEnum_CallType==NgnMediaType.Video;
	}
	public void setCallType(NgnMediaType callType){
        mEnum_CallType = callType;
    }
	public long getStartTime(){		
		return mHistoryEvent.getStartTime();
	}

    public long getEndTime(){
        return mHistoryEvent.getEndTime();
    }
    public long getConnectTime(){
        return mHistoryEvent.getConnectTime();
    }

    public Calendar getCallTime(){
		long begin = 0;

		if(isConnect()) {
			begin = getConnectTime();
		}else{
			begin = getStartTime();
		}
		long callTime = 0;
		callTime = new Date().getTime()-begin;

		Calendar calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
		calendar.setTimeInMillis(callTime);
		return calendar;
	}

    public boolean isConnect(){
        return mHistoryEvent.getConnect();
    }

    public InviteState getStatus() {
        return mCallState;
    }
	public final String getRemoteParty(){		
		return mHistoryEvent.getRemoteUri();
	}
	
	public final String getRemoteDisName(){		
		return mHistoryEvent.getDisplayName();
	}
	
	private InviteState setState(InviteState state,String status){
		mCallState = state;
		super.setChangedAndNotifyObservers(status);
		return mCallState; 
	}
	
	public HistoryAVCallEvent getHistoryEvent() {
		return mHistoryEvent;
	}
	
	public boolean isSendVideo() {  //we send local video or not
		return mSendVideo;
	}
	
	public boolean setSendVideo(boolean sendVideo) {
		mSipMgr.setSendVideo(mSessionID, sendVideo);
		return mSendVideo = sendVideo;
	}
	
	private boolean isValidate() {
        return !(mCallState == InviteState.INCOMING || mCallState == InviteState.INPROGRESS || mCallState == InviteState.REMOTE_RINGING //not in pending
                || mCallState != InviteState.TERMINATED || mCallState != InviteState.TERMINATING);
    }

	

	public void portSipUpdate(int MediaType){
		updateMediaType(NgnMediaType.getTypeByIndex(MediaType));
		return ;
	}
	
	public void joinToConference() {
		mSipMgr.portSipJoinConference(mSessionID);
		
	}
	
	public void portSipRemoteRing(long sessionId){

		mSipMgr.startRingback();
		return ;
	}
	
	public void portSipRemotetrying(long sessionId){
		setState(InviteState.TRIING,null);
		return ;
	}
	
	
	public static class CallFilterInCall implements NgnPredicate<PortSipCallGroup>{
		
		@Override
		public boolean apply(PortSipCallGroup call) {
            return call != null && call.getState() != InviteState.TERMINATING &&
                    call.getState() != InviteState.TERMINATED;
        }
	}

    boolean isActive(){
        return !(mCallState == InviteState.INCOMING || mCallState == InviteState.INPROGRESS || mCallState == InviteState.REMOTE_RINGING //not in pending
                || mCallState == InviteState.TERMINATED || mCallState == InviteState.TERMINATING || isLocalHeld());
    }
	public static class CallFilterInPending implements NgnPredicate<PortSipCallGroup>{
		
		@Override
		public boolean apply(PortSipCallGroup call) {
            return call != null && (call.getState() == InviteState.INCOMING ||
                    call.getState() == InviteState.INPROGRESS || call.getState() == InviteState.REMOTE_RINGING);
        }
	}
	
	public static class CallFilterInActive implements NgnPredicate<PortSipCallGroup>{
		
		@Override
		public boolean apply(PortSipCallGroup call) {
			if(call==null)
				return false;
            else{
                return call.isActive();
            }
		}
	}

	
	public static class CallFilterBySessionId implements NgnPredicate<PortSipCallGroup>{
		private final long mCallId;
		public CallFilterBySessionId(long callId){
			mCallId = callId;
		}
		
		@Override
		public boolean apply(PortSipCallGroup call) {
			return mCallId == call.getSessionId();
		}
	}

    public static class CallFilterByCallId implements NgnPredicate<PortSipCallGroup>{
        private final String mCallId;
        public CallFilterByCallId(String callId){
            mCallId = callId;
        }

        @Override
        public boolean apply(PortSipCallGroup call) {
            return mCallId.equals(""+call.getCallId());
        }
    }

	public static class CallFilterBySessionIDNot implements NgnPredicate<PortSipCallGroup>{
		private final long mCallId;
		public CallFilterBySessionIDNot(long callId){
			mCallId = callId;
		}
		
		@Override
		public boolean apply(PortSipCallGroup call) {
			return mCallId != call.getSessionId();
		}
	}

	void setVideoSize(int videoWidth,int videoHeight){
		if(videoWidth>0&&videoHeight>0&&(videoSize.bottom != videoHeight)&&(videoSize.right  != videoWidth)) {
			videoSize.bottom = videoHeight;
			videoSize.right = videoWidth;
			setVideoSizeChanged();
		}
	}

	public void setVideoSizeChanged(){
		this.setChanged();
		this.notifyObservers(videoSize);
	}

    public Rect getVideoSize(){
        return videoSize;
    }

    public void notifyObserverUpdate(){
        this.setChanged();
        this.notifyObservers(videoSize);
    }
}
