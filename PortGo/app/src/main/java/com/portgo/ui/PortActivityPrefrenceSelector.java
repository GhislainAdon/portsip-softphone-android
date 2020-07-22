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

import android.content.Intent;
import android.os.Bundle;

import com.portgo.R;

public class PortActivityPrefrenceSelector extends PortGoBaseActivity {
    static final String CONTENT_DATA = "CONTENT_DATA";
    static final String SELECT_ID = "SELECT_ID";
    static final String SELECT_TITLE = "SELECT_TITLE";

    String[] arr = {"a","b"};
    int select;
    String title = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        if(intent!=null) {
            arr = intent.getStringArrayExtra(CONTENT_DATA);
            select = intent.getIntExtra(SELECT_ID,0);
            title= intent.getStringExtra(SELECT_TITLE);
        }
        initView();

    }

    ActivityMainSettingAdvanceSelectFragment prefrenceSelectFragment;
    void initView(){
        prefrenceSelectFragment = new ActivityMainSettingAdvanceSelectFragment();

        Bundle bundle = new Bundle();
        bundle.putStringArray(CONTENT_DATA,arr);
        bundle.putInt(SELECT_ID,select);
        bundle.putString(SELECT_TITLE ,title);
        Bundle extraBundle = new Bundle();
        extraBundle.putBundle(PortBaseFragment.EXTRA_ARGS,bundle);
        prefrenceSelectFragment.setArguments(extraBundle);
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,prefrenceSelectFragment).show(prefrenceSelectFragment).commit();

    }
}
