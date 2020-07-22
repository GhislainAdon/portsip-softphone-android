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
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.portgo.R;
import com.portgo.manager.UserAccount;

import java.util.ArrayList;

public class PortActivityImSelect extends PortGoBaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String IM_SELECT = "im_selector";
    public static final String IM_SELECT_DEFAULT = "im_default";
    public static final String IM_SELECT_RESULT = "im_result";
    ArrayList<String> selector;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im_select);
        Intent intent = getIntent();
        selector = intent.getStringArrayListExtra(IM_SELECT);
        int checkPosition = intent.getIntExtra(IM_SELECT_DEFAULT,0);
        if(checkPosition<0){
            checkPosition = 0;
        }else{
            checkPosition = checkPosition+1;//因为前面会加一个空巷
        }
        mlistView = (ListView) findViewById(R.id.im_listview);
        mlistView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mlistView.setAdapter(new ArrayAdapter<String>(this,R.layout.view_im_lv_item,selector));
        mlistView.setItemChecked(checkPosition,true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(R.string.select_im_tilte);
            toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setTitleMarginStart(0);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    ListView mlistView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_imselect,menu);
        return true;
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
                setResult(Activity.RESULT_CANCELED);
                this.finish();
                break;
            case R.id.menu_finish:
                int position = mlistView.getCheckedItemPosition();
                String address = "";
                if(position!=0){
                    address = selector.get(position);
                }
                setResult(Activity.RESULT_OK,new Intent().putExtra(IM_SELECT_RESULT,address));
                this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    }
}
