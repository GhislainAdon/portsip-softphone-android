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

import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.portgo.R;
import com.portgo.adapter.StatusAdapter;
import com.portgo.manager.AccountManager;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.UserAccount;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class PortActivityStatus extends PortGoBaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener, Observer {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        mApp.addActivity(this);
        AccountManager.getInstance().addObserver(this);

        initView();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(R.string.status_title);
            toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    StatusAdapter mAdapter;
    ListView mlistView;
    void initView(){
        HashMap status  = new LinkedHashMap<Integer,String>();
        status.put(R.drawable.mid_content_status_online_ico,getString(R.string.status_online));
        status.put(R.drawable.mid_content_status_away_ico,getString(R.string.status_away));
        status.put(R.drawable.mid_content_status_nodisturb_ico,getString(R.string.status_nodistrub));
        status.put(R.drawable.mid_content_status_busy_ico ,getString(R.string.status_busy));
        status.put(R.drawable.mid_content_status_offline_ico,getString(R.string.status_offline));

        mlistView = (ListView) findViewById(R.id.status_listview);
        mlistView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mAdapter = new StatusAdapter(this, status);
        mlistView.setAdapter(mAdapter);
        AccountManager accountManager = AccountManager.getInstance();
        mAdapter.setOnline(accountManager.getLoginState() == UserAccount.STATE_ONLINE);

        int select = 4;
        if(accountManager.getLoginState()==UserAccount.STATE_ONLINE) {
            UserAccount defAccount = AccountManager.getDefaultUser(this);
            int presence = defAccount.getPresence();


            switch (presence) {
                case ContactsContract.StatusUpdates.AVAILABLE:
                    select = 0;
                    break;
                case ContactsContract.StatusUpdates.AWAY:
                    select = 1;
                    break;
                case ContactsContract.StatusUpdates.DO_NOT_DISTURB:
                    select = 2;
                    break;
                case ContactsContract.StatusUpdates.INVISIBLE:
                    select = 3;
                    break;
                case ContactsContract.StatusUpdates.OFFLINE:
                    select = 4;
                    break;
            }
        }

        mlistView.setItemChecked(select,true);
        mlistView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        }
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String status = null;
        int presence = ContactsContract.StatusUpdates.OFFLINE;
        AccountManager accountManager = AccountManager.getInstance();
        if (accountManager.getLoginState() != UserAccount.STATE_ONLINE) {
            return;
        }

        switch ((int) l){
            case R.drawable.mid_content_status_online_ico://Available
                status=getString(R.string.cmd_presence_online);
                presence = ContactsContract.StatusUpdates.AVAILABLE;
                break;
           case R.drawable.mid_content_status_away_ico://Away
               status=getString(R.string.cmd_presence_away);
               presence = ContactsContract.StatusUpdates.AWAY;
               break;
            case R.drawable.mid_content_status_nodisturb_ico://Do not disturb
                status=getString(R.string.cmd_presence_nodisturb);
                presence = ContactsContract.StatusUpdates.DO_NOT_DISTURB;
                break;
           case R.drawable.mid_content_status_busy_ico:
               status=getString(R.string.cmd_presence_busy);
               presence = ContactsContract.StatusUpdates.INVISIBLE;
               break;
            case R.drawable.mid_content_status_offline_ico://offine
                presence = ContactsContract.StatusUpdates.OFFLINE;
                status=getString(R.string.cmd_presence_offline);
                break;
        }
        long sessionid = 0;
        boolean presenceAgent= mConfigurationService.getBooleanValue(this,ConfigurationManager.PRESENCE_AGENT,
                getResources().getBoolean(R.bool.presence_agent));
        if(presenceAgent) {//agent
            mSipMgr.setPresenceStatus(sessionid,status);
        }else{//p2p
            HashMap<Integer,Long> map =  mCallMgr.getSubscrib();
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Integer key = (Integer) entry.getKey();
                Long val = (Long) entry.getValue();
                mSipMgr.setPresenceStatus(val,status);
            }

        }
        UserAccount defAccount = AccountManager.getDefaultUser(this);
        defAccount.setPresence(presence);
        AccountManager.updateUser(this,defAccount,true);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        AccountManager.getInstance().deleteObserver(this);
        mApp.removeActivity(this);
        super.onDestroy();
    }

    @Override
    public void update(Observable observable, Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mAdapter!=null) {
                    mAdapter.setOnline(AccountManager.getInstance().getLoginState() == UserAccount.STATE_ONLINE);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

    }
}
