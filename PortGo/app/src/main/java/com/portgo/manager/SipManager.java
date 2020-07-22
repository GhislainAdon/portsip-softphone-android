package com.portgo.manager;

import android.app.PendingIntent;
import androidx.annotation.NonNull;
import android.telecom.Call;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.TextureView;

import com.portgo.androidcontacts.TransactionContext;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.Ring;
import com.portsip.PortSIPVideoRenderer;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import java.util.Iterator;
import java.util.List;
import java.util.Observable;

import static com.portgo.BuildConfig.HASVIDEO;


public class SipManager extends Observable {
	PortSipSdk mSdk;
	
	boolean mConferencePlayer = false;
	boolean mConferenceMute = false;
	boolean mConferencehold = false;
	boolean useFrontCamera= true;
	boolean conference = false;
	boolean audioConference = false;
	boolean mInit = false;
    static SipManager instance = new SipManager();
    private SipManager(){
        mSdk = PortSipSdkWrapper.getInstance();
    }

    static public SipManager getSipManager(){
        return instance;
    }

	String serverAgent;

    String svrSupportPush = "";
	public void setServerAgent(String serverAgent,String xppush) {
		this.serverAgent = serverAgent;
		svrSupportPush = xppush;
	}

	public boolean svrEngentsupportPush() {
		boolean supportPush = false;
		if(!TextUtils.isEmpty(svrSupportPush)){
			supportPush = svrSupportPush.contains("true");
		}
		if(supportPush)
        {
            return supportPush;
        }
		if (!TextUtils.isEmpty(serverAgent)) {
			if (serverAgent.contains("PortSIP PBX ")) {
				String ver;
				ver = serverAgent.replace("PortSIP PBX ", "");
				try {
					if (ver != null) {
						String[] pbxVer = ver.split("\\.");
						if (pbxVer.length > 0) {
							ver = pbxVer[0];
						}
					}
					if (Float.parseFloat(ver) >= 9.0) {
						supportPush = true;
					}
				} catch (NumberFormatException e) {

				}

			}
		}
		return supportPush;
	}

	public boolean pbxSupportFileTransfer(){
		return !TextUtils.isEmpty(svrSupportPush);
    }

	public void createAudioConfrence() {
		if (conference == true) {
			return;
		}

		int rt = mSdk.createAudioConference();
		if (rt == PortSipErrorcode.ECoreErrorNone) {
			setConferenceSpeaker(true);
			conference = true;
            audioConference= true;
		} else {
			conference = false;
            audioConference= false;
		}
	}

	public void createConfrence(PortSIPVideoRenderer remoteview,int width,int height) {
		if (conference == true) {
			return;
		}

		int rt = mSdk.createVideoConference(remoteview, width,height, true);
		if (rt == PortSipErrorcode.ECoreErrorNone) {
			mSdk.setConferenceVideoWindow(remoteview);

            disPlayLocalVideo(useFrontCamera);
			setConferenceSpeaker(true);
			conference = true;
            audioConference= false;
		} else {
			conference = false;
		}

	}

	public void setDistrubMode(boolean enable){
		mSdk.setDoNotDisturb(enable);
	}

	long setPresenceMode(int mode){
        return mSdk.setPresenceMode(mode);
    }

	public int setPresenceStatus(long subscribeId, String statusText){
		return mSdk.setPresenceStatus(subscribeId,statusText);
	}

    long sendSubscription (String to, String eventName){
        return mSdk.sendSubscription(to,eventName);
    }

	public long presenceSubscribe(String contact, String subject){
        return mSdk.presenceSubscribe(contact, subject);
    }

	public int presenceTerminateSubscribe (long subscribeId){
        return mSdk.presenceTerminateSubscribe(subscribeId);
    }

	public int presenceRejectSubscribe (long subscribeId){
		return mSdk.presenceRejectSubscribe(subscribeId);
	}

	public int presenceAcceptSubscribe (long subscribeId){
        return mSdk.presenceAcceptSubscribe(subscribeId);
    }
	public int disableCallForward (){
		return mSdk.disableCallForward();
	}

	public int enableCallForward (boolean forbusy,String callee) {
		return mSdk.enableCallForward(forbusy,callee);
	}

	public void destroyConfrence() {
			if(conference==true){				
				mSdk.setConferenceVideoWindow(null);
				mSdk.destroyConference();
				conference = false;
			}
			setConferenceHold(false);
	}

    public String getSdkVersion(){
        return mSdk.getVersion();
    }
	public boolean isConference() {
		return conference;
	}
    public boolean isAudioConference() {
        return audioConference;
    }
	public boolean isConferenceMute() {
		return mConferenceMute;
	}
	
	public boolean setConferenceMuteState(boolean state) {
		return mConferenceMute = state;
	}
		
	public int portSipAnswerCall(long sessionId,SurfaceView remoteView, boolean hasVideo){
		if(sessionId == PortSipErrorcode.INVALID_SESSION_ID){
			return PortSipErrorcode.ECoreArgumentNull;
		}

        return  mSdk.answerCall(sessionId,hasVideo);
	}
	
	public int portSipJoinConference(long sessionId){
		int result = PortSipErrorcode.ECoreArgumentNull;
		if(isConference()){
			result = mSdk.joinToConference(sessionId);
		}
		return result;
	}
	public int setSendVideo(long sessionId,boolean sendVideo) {
		if(HASVIDEO) {
			return mSdk.sendVideo(sessionId, sendVideo);
		}
		return 0;
	}
	
	public int setMicState(long sessionId,boolean state){
		return mSdk.muteSession(sessionId, false, state, false, false);
	}
	
	public void portSipRemoteAnswer(long sessionId,boolean existsVideo,SurfaceView remoteView){

		return ;
	}

	public synchronized void setRemoteView(long sessionID,PortSIPVideoRenderer view){
		if(HASVIDEO) {
			mSdk.setRemoteVideoWindow(sessionID, view);
		}
	}


	public void portSipRemoteClose(long sessionId,boolean hasVideo){

        mSdk.hangUp(sessionId);
		return ;
	}

	public long portSipSendMessage(String to, String message,boolean isSMS,
			int messageLength){
		
		return mSdk.sendOutOfDialogMessage(to, "text", "plain",isSMS, message.getBytes(), message.getBytes().length);
	}

	public long portSipSendMessage(String to, String mime,boolean isSMS,byte[] content){
		if(mime==null){
			mime="text/plain";
		}
		String[] submime = mime.split("/");
		return mSdk.sendOutOfDialogMessage(to, submime[0],submime[1], isSMS,content,content.length);
	}
	
	public synchronized int portSipTerminateCall(long sessionId, boolean hasVideo) {
		if(HASVIDEO && hasVideo) {
			mSdk.setRemoteVideoWindow(sessionId, null);

			mSdk.sendVideo(sessionId, false);
		}
		return mSdk.hangUp(sessionId);

	}	

	public synchronized int portSipRejectCall(long sessionId){
		return mSdk.rejectCall(sessionId, 486);
	}

	public void disablePlayLocalVideo() {
		mSdk.displayLocalVideo(false,false);
	}
	public void disPlayLocalVideo(){
		disPlayLocalVideo(useFrontCamera);
	}

    public void disPlayLocalVideo(boolean front){
		useFrontCamera = front;
		int num = android.hardware.Camera.getNumberOfCameras();
		if(num==1){
			useFrontCamera = false;//force use camera 0,when only one camera.
		}
		mSdk.displayLocalVideo(false,false);
		if (useFrontCamera) {
			mSdk.setVideoDeviceId(1);
			mSdk.displayLocalVideo(true,true);

		} else {
			mSdk.setVideoDeviceId(0);
			mSdk.displayLocalVideo(true,false);
		}

    }

    protected void setConferenceVideoWindow(PortSIPVideoRenderer view){
        mSdk.setConferenceVideoWindow(view);
    }

	public long portSipMakeCall(@NonNull String callee, int enum_mediatype){

		long iResult = PortSipErrorcode.INVALID_SESSION_ID;
        if(!NgnStringUtils.isNullOrEmpty(callee)){
			iResult = mSdk.call(callee, true,enum_mediatype==PortSipCall.MEDIATYPE_AUDIOVIDEO||enum_mediatype==PortSipCall.MEDIATYPE_VIDEO);
		}
		return iResult;
	}
	
	public int portSipReferOut(long sessionID,String referto){
		String filteString="";
		for(int i=0;i<referto.length();i++){
			if(NgnUriUtils.isValidUriChar(""+referto.charAt(i))){
				filteString+=referto.charAt(i);
			}
		}
		referto = filteString;

		int iResult = PortSipErrorcode.ECoreErrorNone;
		if(sessionID != PortSipErrorcode.INVALID_SESSION_ID)
			iResult = mSdk.refer(sessionID,referto);
		return iResult;
	}

	public int portforwardCall(long sessionID,String referto){
		referto = referto.trim().replace(" ","");
		int iResult = PortSipErrorcode.ECoreErrorNone;
		if(sessionID != PortSipErrorcode.INVALID_SESSION_ID) {
			iResult = mSdk.forwardCall(sessionID, referto);
		}
		return iResult;
	}



	public int portSipHold(long sessionId){
		int	iResult = mSdk.hold(sessionId);

		return iResult;
	}

	public int portSipUnHold(long seesionId) {
		int iResult = PortSipErrorcode.ECoreErrorNone;
		
		if (seesionId != PortSipErrorcode.INVALID_SESSION_ID) {
			iResult = mSdk.unHold(seesionId);
		}

		return iResult;
	}
	
	public boolean portSipRevDtmf(long sessionID,int code){
		
		if(sessionID != PortSipErrorcode.INVALID_SESSION_ID)
		{
			return true;
		}
		return false;
	}

	public boolean portSipSendDtmf(long sessionID,int dtmfType,char code,boolean playtone){
		
		if(sessionID != PortSipErrorcode.INVALID_SESSION_ID)
		{
			int number = 0;
			
			if('0'<=code&&code<='9'){
				number = code-'0';
			}else
			if (code == '*') {
				number = 10;
			}else
			if (code == '#') {
				number = 11;
			}else {
				return false;
			}
			//ENUM_DTMF_MOTHOD_RFC2833 = 0
			//ENUM_DTMF_MOTHOD_INFO = 1
			mSdk.sendDtmf(sessionID,dtmfType, number,160, playtone);
			
			return true;
		}
		return false;
	}

	public boolean setConferenceSpeaker(boolean userSpeaker){
        mConferencePlayer =userSpeaker;
        
        return mConferencePlayer;
	}	
	
	public boolean setConferenceHold(boolean statu){
		return 	mConferencehold =statu;	
	}	
	
	public boolean isConferenceHold(){
		return mConferencehold;
	}

	
	public boolean isForntCamera(){
		return useFrontCamera;
	}
	
	public int setUserInfo(String userName, String displayName,
			String authName, String password,String userDomain, String SIPServer, int SIPServerPort,
			String stunserver, int stunport){

		mSdk.removeUser();
		return mSdk.setUser(userName, displayName, authName, password,
				userDomain, SIPServer, SIPServerPort,
                stunserver, stunport, null, 5060);// step 4
	}

	public void holdAllExcept(PortSipCall call){
		CallManager.getInstance().holdAllcallsExcept(call);
	}


	public int startMediaRecord(long sessionId, String filePath,String filename,boolean hasVideo) {

		if (filePath == null || filePath.length() <= 0) {
			return PortSipErrorcode.ECoreArgumentNull;
		}

		int videoMode = PortSipEnumDefine.ENUM_RECORD_MODE_BOTH,
				videoCode=PortSipEnumDefine.ENUM_VIDEOCODEC_H264;
		if(!hasVideo){
			videoMode = PortSipEnumDefine.ENUM_RECORD_MODE_BOTH;
			videoCode=PortSipEnumDefine.ENUM_VIDEOCODEC_NONE;
		}
		return mSdk.startRecord(sessionId, filePath, filename, false,
				PortSipEnumDefine.ENUM_AUDIO_FILE_FORMAT_WAVE,
				PortSipEnumDefine.ENUM_RECORD_MODE_BOTH,
				videoCode,
				videoMode);
	}

	public void stopMediaRecord(long sessionId) {
		mSdk.stopRecord(sessionId);
	}
	
	public void startRing(){ ////when answer/hangup by me or answer/reject by remote .stop ring tone
		Ring.getInstance().startRingTone();
	}
	
	public void startRingback(){ ////when reject/answer by me or reject by remote .stop ring tone
		Ring.getInstance().startRingBackTone();
	}	
	
	public void stopRing(){ ////when answer/hangup by me or answer/reject by remote .stop ring tone
		Ring.getInstance().stopRingTone();
	}
	
	public void stopRingback(){ ////when reject/answer by me or reject by remote .stop ring tone
		Ring.getInstance().stopRingBackTone();
	}
	
}

