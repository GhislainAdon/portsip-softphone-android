/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.manager;

import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnObservableObject;
import com.portgo.util.SipUri;

import java.util.Date;

public abstract class HistoryEvent extends NgnObservableObject implements Comparable<HistoryEvent> {
	
	public enum StatusType{
		Outgoing,
		Incoming,
		Missed,
		Failed,
		SUCCESS;
		public static StatusType valueof(int ordinal){
			if(ordinal>-1&&values().length>ordinal)
				return values()[ordinal];
			return values()[0];
		}
	}

	protected NgnMediaType mMediaType;
	protected long mStartTime;
	protected long mEndTime;
	protected String mRemoteParty;//must full name.like(101@192.168.1.198)
	protected String mLocalParty;//must full name.like(101@192.168.1.198) 
	protected boolean mSeen;
	protected StatusType mStatus;	
	private String mDisplayName;	
	protected long mInCallTime;
	int mId;
	
	public HistoryEvent(){
		super();
	}
	
	protected HistoryEvent(NgnMediaType mediaType, String remoteParty,String remoteDisName){
		mMediaType = mediaType;
		mStartTime = new Date().getTime();
		mEndTime = mStartTime;
		mRemoteParty = remoteParty;
		mStatus = StatusType.Missed;
		if(remoteDisName==null||remoteDisName.length()<1)
		{
			SipUri uri = new SipUri(remoteParty, null);
			mDisplayName = uri.getDisplayName();
		}else {
			mDisplayName = remoteDisName;	
		}
		
	}
	
	public void setStartTime(long time){
		mStartTime = time;
	}
	
	public long getStartTime(){
		return mStartTime;
	}	
	
	public long getEndTime(){
		return mEndTime;
	}
	
	public void setEndTime(long time){
		mEndTime = time;
	}
	
	public long getId(){
		return mId;
	}
	
	public void setId(int id){
		mId =id;
	}
	//2.22 modify to show the time in a call
	public long getInCallTime(){
		return mInCallTime;
	}

	public long getConnectTime(){return 0;}

	public void setInCallTime(long inCallTime){
			mInCallTime = inCallTime;
	}
	
	public NgnMediaType getMediaType(){
		return mMediaType;
	}

	public String getRemoteParty(){
		return mRemoteParty;
	}

	public String getLocalParty(){
		return mLocalParty;
	}
	
	public void setLocalParty(String localParty){
		mLocalParty = localParty;
	}
	
	
	public void setRemoteParty(String remoteParty){
		mRemoteParty = remoteParty;
	}
	
	public boolean isSeen(){
		return mSeen;
	}
	
	public void setSeen(boolean seen){
		mSeen = seen;
	}
	
	public StatusType getStatus(){
		return mStatus;
	}
	
	public void setStatus(StatusType status){
        if(status!=mStatus) {
            mStatus = status;
            setChangedAndNotifyObservers(status);
        }
	}
	
	public void setDisplayName(String displayName){
//		if(mDisplayName == null||mDisplayName.trim().length()<=0){
//			mDisplayName = NgnUriUtils.getDisplayName(getRemoteParty(),contactManager);
//		}
		mDisplayName =displayName;
	}
	
	public String getDisplayName(){
//		if(mDisplayName == null||mDisplayName.trim().length()<=0){
//			mDisplayName = NgnUriUtils.getDisplayName(getRemoteParty(),contactManager);
//		}
		return mDisplayName;
	}
	
	@Override
	public int compareTo(HistoryEvent another) {
		return (int)(mStartTime - another.mStartTime);
	}

	public boolean isIncoming(){
		return  getStatus() == HistoryEvent.StatusType.Incoming;
	}
}
