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
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;


import com.portgo.R;
import com.portgo.adapter.RecordFilesAdapter;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.SelectableObject;
import com.portgo.util.SelectableObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PortActivityRecords extends PortGoBaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {
    ArrayList<SelectableObject<File>> recordFiles = new ArrayList<>();
    RecordFilesAdapter mAdapter = null;
    Toolbar toolbar =null;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordfiles);
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        mAdapter = new RecordFilesAdapter(this,recordFiles);
        mlistView = (ListView) findViewById(R.id.recordfiles_listview);
        mlistView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mlistView.setAdapter(mAdapter);
        mlistView.setOnItemClickListener(this);
        mlistView.setOnItemLongClickListener(this);
        String dirName = getString(R.string.prefrence_record_filepath_default);
        String defaultdir = getExternalFilesDir(dirName).getAbsolutePath();
        String recordFilePath = mConfigurationService.getStringValue(this,ConfigurationManager.PRESENCE_RECORD_DIR, defaultdir);

        getRecordFiles(recordFiles,recordFilePath);
        mAdapter.notifyDataSetChanged();

        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(R.string.title_record);
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
        getMenuInflater().inflate(R.menu.menu_records,menu);
        return true;
    }

    final String AUDIO_FORMAT = "wav";
    final String VIDEO_FORMAT= "avi";
    private void getRecordFiles(ArrayList<SelectableObject<File>> filelist,String path){
        File dir = new File(path);
        if(filelist!=null){
            File[] files = dir.listFiles(); //
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String fileName = files[i].getName();
                    if (files[i].isDirectory()) { //
                    } else if (fileName.endsWith(VIDEO_FORMAT)) {
                        filelist.add(new SelectableObject(files[i]));
                    } else if (fileName.endsWith(AUDIO_FORMAT)) {
                        filelist.add(new SelectableObject(files[i]));
                    }
                }
                Collections.sort(filelist, new Comparator<SelectableObject<File>>() {
                    @Override
                    public int compare(SelectableObject<File> selectfile, SelectableObject<File> selectt1) {
                        File file = selectfile.getObject();
                        File t1 = selectt1.getObject();
                        if(file!=null&&t1!=null){
                            return (int) (file.lastModified()-t1.lastModified());
                        }
                        return 0;
                    }
                });
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bottombar_right:
                ArrayList<SelectableObject<File>> removes = new ArrayList<SelectableObject<File>>();
                for(SelectableObject<File> selectableObject:recordFiles){
                    if(selectableObject.isChecked()){
                        removes.add(selectableObject);
                    }
                }

                recordFiles.removeAll(removes);
                for(SelectableObject<File> file:removes) {
                    file.getObject().delete();
                }
                removes.clear();
                exitSelectorMode();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK&&mlistView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE){
            exitSelectorMode();
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if(mlistView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE) {
                    exitSelectorMode();
                }else {
                    this.finish();
                }
                break;
            case R.id.menu_records_select:
                if(recordFiles!=null&&SelectableObjectUtils.isAllObjectChecked(recordFiles)){
                    SelectableObjectUtils.setListSelectStatus(recordFiles,false);
                    item.setTitle(R.string.select_all);
                }else{
                    SelectableObjectUtils.setListSelectStatus(recordFiles,true);
                    item.setTitle(R.string.clear_all);
                }
                mAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        SelectableObject<File> selectfile = recordFiles.get(i);
        File file = selectfile.getObject();
        if(mlistView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE) {
            selectfile.toggle();
            if(recordFiles!=null&&SelectableObjectUtils.isAllObjectChecked(recordFiles)){
                toolbar.getMenu().findItem(R.id.menu_records_select).setTitle(R.string.select_all);
            }else{
                toolbar.getMenu().findItem(R.id.menu_records_select).setTitle(R.string.clear_all);
            }
            mAdapter.notifyDataSetChanged();
        }else{
            ArrayList<String> filePaths = new ArrayList<>();
            for(SelectableObject<File>  recodfile:recordFiles){
                filePaths.add(recodfile.getObject().getAbsolutePath());
            }
            Intent palyer = new Intent(this, PortActivityRecordPlayer.class);
            palyer.putExtra(PortActivityRecordPlayer.PLAY_SET, filePaths);
            palyer.putExtra(PortActivityRecordPlayer.PLAY_ITEM, i);
            startActivity(palyer);
        }
    }

    void enterSelectorMode(){
        mlistView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        findViewById(R.id.recordfiles_bootombar).setVisibility(View.VISIBLE);
        findViewById(R.id.bottombar_left).setVisibility(View.INVISIBLE);
        findViewById(R.id.bottombar_right).setOnClickListener(this);
        toolbar.getMenu().findItem(R.id.menu_records_select).setVisible(true);
        mAdapter.notifyDataSetChanged();
    }

    void exitSelectorMode(){
        mlistView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        findViewById(R.id.recordfiles_bootombar).setVisibility(View.GONE);
        toolbar.getMenu().findItem(R.id.menu_records_select).setVisible(false);
        SelectableObjectUtils.setListSelectStatus(recordFiles,false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(mlistView.getChoiceMode()!=AbsListView.CHOICE_MODE_MULTIPLE) {
            recordFiles.get(i).setChecked(true);
            enterSelectorMode();
        }
        return true;
    }
}
