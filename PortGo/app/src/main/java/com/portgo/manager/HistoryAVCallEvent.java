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


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.portgo.database.DBHelperBase;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnDateTimeUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnUriUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class HistoryAVCallEvent{
    int callid;
    private NgnMediaType mMediaType;
    private long mStartTime;
    private long mEndTime;
    private long mConnectTime;
    private int mRemoteID;
    private String mLocalParty;//must full name.like(101@192.168.1.198)
    private int mSeen;
    private boolean mConnect;
    private String mDisplayName;
    private boolean mCallOut;
    int mId;
    private int contactId;
    private int contactType;
    private String remoteUri;
    private String remoteDisname;
    private int group;
    /***
     *
     * @param callId callId
     * @param mediaType
     * @param remoteID
     * @param remoteDisName
     * @param callout true=callout false=incoming
     */

	public HistoryAVCallEvent(int callId,String localParty, int remoteID,String remoteDisName,NgnMediaType mediaType,boolean callout){
        this.callid =callId;
        localParty = localParty.replaceFirst(NgnUriUtils.SIP_HEADER,"");
        mLocalParty = localParty;
        mRemoteID = remoteID;
        mDisplayName = remoteDisName;
        mMediaType = mediaType;
        mCallOut = callout;
	}

    public void setStartTime(long time){
        mStartTime = time;
    }
    public void setEndTime(long time){
        mEndTime = time;
    }
    public void setConnectTime(long time){mConnectTime =time;}
    public void setConnected(boolean connect){mConnect = connect;}
    public void setRead(boolean read){mSeen = read?1:0;}
    public void setHasRecord(boolean record){hasRecord = record;}
    public void setRemoteUri(String remote){
        remoteUri = remote;
    }

    public long getStartTime(){return mStartTime;}
    public long getEndTime(){return mEndTime;}
    public long getConnectTime(){return mConnectTime;}

    public void setGroup(int group){
        group = group;
    }
    public int getGroup(){
        return group;
    }
    public NgnMediaType getMediaType(){return mMediaType;}
    public int getRemoteID(){return mRemoteID;}
    public String getLocalParty(){return mLocalParty;}
    public String getDisplayName(){
        if(TextUtils.isEmpty(mDisplayName)){
            mDisplayName = NgnUriUtils.getUserName(getRemoteUri());
        }
        return mDisplayName;
    }
    public int getCallid() {return callid;}

    boolean hasRecord = false;
    public boolean getHasRecord(){
        return  hasRecord;
    }
    public boolean getConnect(){return mConnect;}
    public boolean isSeen(){return mSeen==0;}
    public boolean getCallOut(){return mCallOut;}
    public Integer getId(){
        return mId;
    }

    /*

     */
    public void createGroup(ContentResolver contentResolver){
        Cursor cursor = CursorHelper.resolverQuery(contentResolver,DBHelperBase.HistoryColumns.CONTENT_URI,null,null,null,
                DBHelperBase.HistoryColumns.DEFAULT_ORDER);
        int newgroup =1;
        if(CursorHelper.moveCursorToFirst(cursor)){
            HistoryAVCallEvent latest = HistoryAVCallEvent.historyAVCallEventFromCursor(cursor);
            if(this.sameGroup(latest)){
                newgroup =latest.getGroup();
            }else{
                newgroup =latest.getGroup()+1;
            }
        }
        CursorHelper.closeCursor(cursor);
        group = newgroup;
    }

    public  ContentValues getContentValues(){
        ContentValues values = new ContentValues();
        values.put(DBHelperBase.HistoryColumns.HISTORY_STARTTIME, getStartTime());
        values.put(DBHelperBase.HistoryColumns.HISTORY_HASRECORD, getHasRecord()?1:0);
        values.put(DBHelperBase.HistoryColumns.HISTORY_ENDTIME, getEndTime());
        values.put(DBHelperBase.HistoryColumns.HISTORY_INCALLTIME, getConnectTime());
        values.put(DBHelperBase.HistoryColumns.HISTORY_DISPLAYNAME,getDisplayName());
        values.put(DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID,getRemoteID());
        values.put(DBHelperBase.HistoryColumns.HISTORY_LOCAL,getLocalParty());
        values.put(DBHelperBase.HistoryColumns.HISTORY_CONNECTED,getConnect()?1:0);
        values.put(DBHelperBase.HistoryColumns.HISTORY_MIDIATYPE,getMediaType().ordinal());
        values.put(DBHelperBase.HistoryColumns.HISTORY_SEEN,isSeen()?0:1);
        values.put(DBHelperBase.HistoryColumns.HISTORY_CALLID,getCallid());
        values.put(DBHelperBase.HistoryColumns.HISTORY_CALLOUT,getCallOut()?1:0);

        return values;
    }

    public  static HistoryAVCallEvent historyAVCallEventFromCursor( Cursor cursor){
        int indexCALLID = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CALLID);
        int indexSTARTTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_STARTTIME);
        int indexENDTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_ENDTIME);
        int indexINCALLTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_INCALLTIME);
        int indexSTATUS = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CONNECTED);
        int indexSEEN = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_SEEN);
        int indexGroup = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_GROUP);
        int indexCALLOUT = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CALLOUT);
        int indexMIDIATYPE = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_MIDIATYPE);
        int indexDISPLAYNAME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_DISPLAYNAME);
        int indexREMOTE = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID);
        int indexLOCAL = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_LOCAL);
        int indexRECORD = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_HASRECORD);
        int indexID = cursor.getColumnIndex(DBHelperBase.HistoryColumns._ID);
        long startTime=0,incallTime=0,endTime=0;int recod=0;
        String displayName="",local="";
        int status=0,type=0,callid=0,callout = 0,eventID=0,remote=0,group = 0;
        if(indexDISPLAYNAME>=0)
            displayName = cursor.getString(indexDISPLAYNAME);
        if(indexREMOTE>=0)
            remote = cursor.getInt(indexREMOTE);
        if(indexLOCAL>=0)
            local = cursor.getString(indexLOCAL);
        if(indexSTARTTIME>=0)
            startTime = cursor.getLong(indexSTARTTIME);
        if(indexENDTIME>=0)
            endTime = cursor.getLong(indexENDTIME);
        if(indexINCALLTIME>=0)
            incallTime= cursor.getLong(indexINCALLTIME);
        if(indexMIDIATYPE>=0)
            type = cursor.getInt(indexMIDIATYPE);
        if(indexSTATUS>=0)
            status = cursor.getInt(indexSTATUS);
        if(indexCALLID>=0)
            callid = cursor.getInt(indexCALLID);

        if(indexRECORD>=0)
            recod = cursor.getInt(indexRECORD);
        if(indexCALLOUT>=0)
            callout = cursor.getInt(indexCALLOUT);
        if(indexCALLOUT>=0)
            eventID = cursor.getInt(indexID);
        if(indexGroup>=0){
            group = cursor.getInt(indexGroup);
        }

        NgnMediaType mediaType = NgnMediaType.getTypeByIndex(type);
        HistoryAVCallEvent event = new HistoryAVCallEvent(callid,local,remote,displayName,mediaType,callout>0);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setConnectTime(incallTime);
        event.setConnected(status>0);
        event.setHasRecord(recod==1?true:false);
        event.mId = eventID;
        event.group = group;

        return  event;
    }
    public  static HistoryAVCallEvent historyAVCallEventFromViewCursor(Context context, Cursor cursor){
        int indexCALLID = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CALLID);
        int indexSTARTTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_STARTTIME);
        int indexENDTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_ENDTIME);
        int indexINCALLTIME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_INCALLTIME);
        int indexSTATUS = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CONNECTED);
        int indexSEEN = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_SEEN);
        int indexRECORD = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_HASRECORD);
        int indexCALLOUT = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_CALLOUT);
        int indexGroup = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_GROUP);
        int indexMIDIATYPE = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_MIDIATYPE);
        int indexDISPLAYNAME = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_DISPLAYNAME);
        int indexREMOTE = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID);
        int indexLOCAL = cursor.getColumnIndex(DBHelperBase.HistoryColumns.HISTORY_LOCAL);
        int indexCONTACT_ID = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID);
        int indexREMOTE_URI = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI);
        int indexCONTACT_TYPE = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_TYPE);
        int indexREMOTE_DISPPLAY_NAME = cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_DISPPLAY_NAME);


        int indexID = cursor.getColumnIndex(DBHelperBase.HistoryColumns._ID);
        long startTime=0,incallTime=0,endTime=0,contactId=0,contactType=0;
        String displayName="",local="",remoteUri="",remoteDisname="";
        int status=0,type=0,callid=0,callout = 0,eventID=0,remoteID=0,record=0,group = 0;
        if(indexREMOTE_URI>=0)
            remoteUri = cursor.getString(indexREMOTE_URI);
        if(indexREMOTE_DISPPLAY_NAME>=0)
            remoteDisname = cursor.getString(indexREMOTE_DISPPLAY_NAME);
        if(indexCONTACT_ID>=0)
            contactId = cursor.getInt(indexCONTACT_ID);
        if(indexCONTACT_TYPE>=0)
            contactType = cursor.getInt(indexCONTACT_TYPE);

        if(indexDISPLAYNAME>=0)
            displayName = cursor.getString(indexDISPLAYNAME);
        if(indexREMOTE>=0)
            remoteID = cursor.getInt(indexREMOTE);
        if(indexLOCAL>=0)
            local = cursor.getString(indexLOCAL);
        if(indexSTARTTIME>=0)
            startTime = cursor.getLong(indexSTARTTIME);
        if(indexENDTIME>=0)
            endTime = cursor.getLong(indexENDTIME);
        if(indexINCALLTIME>=0)
            incallTime= cursor.getLong(indexINCALLTIME);
        if(indexMIDIATYPE>=0)
            type = cursor.getInt(indexMIDIATYPE);
        if(indexSTATUS>=0)
            status = cursor.getInt(indexSTATUS);
        if(indexCALLID>=0)
            callid = cursor.getInt(indexCALLID);
        if(indexCALLOUT>=0)
            callout = cursor.getInt(indexCALLOUT);
        if(indexCALLOUT>=0)
            eventID = cursor.getInt(indexID);
        if(indexRECORD>=0){
            record = cursor.getInt(indexRECORD);
        }
        if(indexGroup>=0){
            group = cursor.getInt(indexGroup);
        }

        NgnMediaType mediaType = NgnMediaType.getTypeByIndex(type);
        HistoryAVCallEvent event = new HistoryAVCallEvent(callid,local,remoteID,displayName,mediaType,callout>0);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setConnectTime(incallTime);
        event.setConnected(status>0);
        event.setHasRecord(record==1?true:false);
        event.mId = eventID;
        event.contactId = (int) contactId;
        event.contactType = (int) contactType;
        event.group=group;
        event.remoteDisname =remoteDisname;
        event.remoteUri=remoteUri;
        return  event;
    }

    public boolean sameGroup(HistoryAVCallEvent callEvent){
        if(callEvent!=null){
            Calendar preStarttTime = Calendar.getInstance();
            Calendar curStarttTime = Calendar.getInstance();

            preStarttTime.setTimeInMillis(callEvent.getStartTime());
            curStarttTime.setTimeInMillis(this.getStartTime());

            if ((callEvent.getRemoteID() == this.getRemoteID())
                    && (callEvent.getMediaType() == this.getMediaType())
                    && (callEvent.getConnect() == this.getConnect())
                    && (NgnDateTimeUtils.isSameDay(preStarttTime, curStarttTime))) {
                return true;
            }
        }
        return false;
    }
    public Calendar getCallTime(){
        Calendar calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
        long callTime = 0;
        if(mConnect) {
            callTime = getEndTime() - getConnectTime();
        }else{
            callTime = 0;
        }
        calendar.setTimeInMillis(callTime);
        return calendar;
    }

    public int getContactId() {
        return contactId;
    }

    public int getContactType() {
        return contactType;
    }

    public String getRemoteUri() {
        return remoteUri;
    }

    public String getRemoteDisname() {
        return remoteDisname;
    }
}
