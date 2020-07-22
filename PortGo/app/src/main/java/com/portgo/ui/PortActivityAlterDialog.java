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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.PortSipSdkWrapper;
import com.portsip.PortSipSdk;

public class PortActivityAlterDialog extends Activity {
    TextView tvEvent=null;
    TextView tvTime=null;
    TextView tvRemote=null;
    int dissmisstime =2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if(!PortSipSdkWrapper.getInstance().isInitialized()){
            startActivity(new Intent(this,PortActivityLogin.class));
            this.finish();
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_autodissmiss_dialog);
        mHandler.postDelayed(suicide,dissmisstime);
        initView();
    }

    public static void showAutoDissmissDialog(Context context, String event, String time, String remote){
        Intent intent = new Intent(context,PortActivityAlterDialog.class);
        intent.putExtra("event",event);
        intent.putExtra("time",time);
        intent.putExtra("remote",remote);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    void initView(){
        tvEvent=(TextView)findViewById(R.id.tvevent);
        tvTime=(TextView)findViewById(R.id.tvtime);
        tvRemote=(TextView)findViewById(R.id.tvremote);

        tvEvent.setText(getIntent().getStringExtra("event"));
        tvTime.setText(getIntent().getStringExtra("time"));
        tvRemote.setText(getIntent().getStringExtra("remote"));

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(suicide);
    }

    private Handler mHandler = new Handler() ;
    Runnable suicide = new Runnable(){
        @Override
        public void run() {
            {
                PortActivityAlterDialog.this.finish();
            };
        }
    };
}
