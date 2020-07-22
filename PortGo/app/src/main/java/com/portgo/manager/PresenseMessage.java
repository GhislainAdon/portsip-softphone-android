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

import android.content.ContentValues;
import android.database.Cursor;
import android.widget.Checkable;

import com.portgo.androidcontacts.ContactsDatabaseHelper;
import com.portgo.database.DBHelperBase;
import com.portgo.util.DateTimeUtils;

import java.util.Date;

public class PresenseMessage implements Checkable{

    int mId;
	protected long mMessageTime;
	protected String mRemoteParty;//must full name.like(101@192.168.1.198)
	protected String mLocalParty;//must full name.like(101@192.168.1.198)
	private  int mAction;
	private String mDisplayName;
	protected String mStatus;

	public PresenseMessage(String localParty, String remoteParty, String remoteDisName, String status,long time,int id) {
        mRemoteParty = remoteParty;
        mDisplayName = remoteDisName;
        mLocalParty = localParty;
        mMessageTime = time;
		mId = id;
		mStatus = status;
	}
	public void setAction(int action){
		mAction = action;
	}

	public boolean isReject() {
		return mAction == DBHelperBase.SubscribeColumns.ACTION_REJECTED;
	}

	public boolean isAccept() {
		return mAction == DBHelperBase.SubscribeColumns.ACTION_ACCEPTED;
	}

	public int getPresenceId()
	{
		return  mId;
	}

	public String getRemoteParty() {
		return mRemoteParty;
	}

	public long getMessageTime(){
		return mMessageTime;
	}
	public  String getDisplayName(){
		return mDisplayName;
	}

	public  String getPresenceStatus(){
		return mStatus;
	}

	boolean check;
	@Override
	public void setChecked(boolean b) {
		check  = b;
	}

	@Override
	public boolean isChecked() {
		return check;
	}

	@Override
	public void toggle() {
		check  = !check;
	}

}
