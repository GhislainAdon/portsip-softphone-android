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
package com.portgo.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.database.RemoteRecord;
import com.portgo.exception.LineBusyException;
import com.portgo.exception.OutOfMaxLineException;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CallManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.manager.SipManager;
import com.portgo.manager.UserAccount;
import com.portgo.util.CallReferTask;
import com.portgo.util.ContactQueryTask;
import com.portgo.util.NgnUriUtils;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import java.util.Observable;
import java.util.Observer;

public class PortActivityReferDialog extends Activity implements Observer, View.OnClickListener {
    TextView title,tvMessage=null;
    int dissmisstime =10*1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if(!PortSipSdkWrapper.getInstance().isInitialized()){
            startActivity(new Intent(this,PortActivityLogin.class));
            this.finish();
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_refer_dialog);
        mHandler.postDelayed(suicide,dissmisstime);
        CallManager.getInstance().getObservableCalls().addObserver(this);
        initView();
    }

    public static void showReferDialog(Context context, long sessionId, final long referId, final String to,
                                       String from, final String referSipMessage){
        Intent intent = new Intent(context,PortActivityReferDialog.class);
        intent.putExtra("sessionId",sessionId);
        intent.putExtra("referId",referId);
        intent.putExtra("referto",to);
        intent.putExtra("referfrom",from);
        intent.putExtra("referSipMessage",referSipMessage);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    long sessionId,referId;
    String referto,referfrom,referSipMessage;
    void initView(){
        title=(TextView)findViewById(R.id.dialog_title);
        tvMessage=(TextView)findViewById(R.id.refer_message);

        findViewById(R.id.refer_ok).setOnClickListener(this);
        findViewById(R.id.refer_cancel).setOnClickListener(this);
        Intent intent = getIntent();
        long sessionId = intent.getLongExtra("sessionId",-1);
        referId = intent.getLongExtra("referId",-1);
        referto = intent.getStringExtra("referto");
        referfrom = intent.getStringExtra("referfrom");
        referSipMessage =  intent.getStringExtra("referSipMessage");

        tvMessage.setText(String.format(getString(R.string.refer_tips),referfrom,referto));

    }

    @Override
    protected void onResume() {
        super.onResume();
//        mThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CallManager.getInstance().getObservableCalls().deleteObserver(this);
        mHandler.removeCallbacks(suicide);
    }

    private Handler mHandler = new Handler() ;
    Runnable suicide = new Runnable(){
        @Override
        public void run() {
        {
            if(processed==false){
                PortSipSdkWrapper.getInstance().rejectRefer(referId);
            }
            PortActivityReferDialog.this.finish();
        };
        }
    };

    @Override
    public void update(Observable observable, Object o) {
        CallManager callManager = CallManager.getInstance();
        PortSipCall call = callManager.getCallBySessionId(sessionId);
        if(call==null||call.getState()== PortSipCall.InviteState.TERMINATED||call.getState()== PortSipCall.InviteState.TERMINATING){
            this.finish();
        }
    }

    boolean processed = false;
    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case  R.id.refer_cancel:
                processed = true;
                PortSipSdkWrapper.getInstance().rejectRefer(referId);
                this.finish();
                break;
            case  R.id.refer_ok:
                processed = true;
                long referSessionId =PortSipSdkWrapper.getInstance().acceptRefer(referId,referSipMessage);

                CallManager callManager = CallManager.getInstance();
                PortSipCall call = callManager.getCallBySessionId(sessionId);
                int mediaType = PortSipCall.MEDIATYPE_AUDIO;
                if(call!=null) {
                    mediaType = call.getCallType();
                    call.terminate(this);
                }
                int callid =PortSipErrorcode.INVALID_SESSION_ID;
                try {
                    PortSipEngine engine = PortSipEngine.getInstance(this);
                    SipManager sipManager = engine.getSipManager();
                    String disname = NgnUriUtils.getUserName(referto);
                    RemoteRecord remoteId = RemoteRecord.getRemoteRecord(getContentResolver(),referto,disname);

                    CallReferTask referTask = null;
                    final UserAccount user = AccountManager.getDefaultUser(this);
                    if(user!=null&&UserAccount.FORWARD_BUSY==user.getFowardMode()&&user.getFowardNoAnswerTime()>0&&!TextUtils.isEmpty(user.getFowardTo())){
                        referTask = new CallReferTask(remoteId.getRowID(),user.getFowardNoAnswerTime(),user.getFowardTo(),sipManager,callManager,this);
                    }

                    call= CallManager.getInstance().portSipCallIn(this,sipManager,referTask,
                            referSessionId, user.getFullAccountReamName(), (int) remoteId.getRowID(),referto, disname, mediaType);

                    callid = call.getCallId();
                    new ContactQueryTask().execute(this,remoteId,referto);

                } catch (OutOfMaxLineException e) {
                    e.printStackTrace();
                } catch (LineBusyException e) {
                    e.printStackTrace();
                }
                this.finish();
                PortApplication mApp =(PortApplication)this.getApplicationContext();
                mApp.startAVActivity(callid);
                break;
        }

    }
}
