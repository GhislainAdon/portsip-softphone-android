package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.portgo.R;
import com.portgo.adapter.CodecAdapter;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.util.Codec;
import com.portgo.view.DragListView;
import com.portsip.PortSipSdk;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ActivityMainSettingCodecFragment extends PortBaseFragment implements
        AdapterView.OnItemClickListener, View.OnClickListener,DragListView.OnDragMode{
	Context context = null;
    List<Codec> audioCodecs = null;
    List<Codec> videoCodecs = null;
	boolean editMode = false;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View myView;
		super.onCreateView(inflater, container, savedInstanceState);
		context = getActivity();
		myView = inflater.inflate(R.layout.activity_main_setting_fragment_codec, null);

        Set<String> audiokeys = baseActivity.mConfigurationService.getStringSet(getActivity(),ConfigurationManager.PRESENCE_AUDIO_CODE);
        Set<String> videokeys = baseActivity.mConfigurationService.getStringSet(getActivity(),ConfigurationManager.PRESENCE_VIDEO_CODE);
        if(audiokeys!=null&&audiokeys.size()>0){
            audioCodecs = Codec.getCodecList(audiokeys);
        }else {
            audioCodecs = Codec.getCodecList(getResources().getStringArray(R.array.audio_codecs));
        }
        if(videokeys!=null&&videokeys.size()>0){
            videoCodecs = Codec.getCodecList(videokeys);
        }else
        {
            videoCodecs = Codec.getCodecList(getResources().getStringArray(R.array.video_codecs));
        }
        premiuned = baseActivity.mConfigurationService.getPremium();
        for(Codec codec:audioCodecs){
            if(codec.isPremiumPoints())
            {
                codec.setPreminumed(premiuned);
            }
        }
        for(Codec codec:videoCodecs){
            if(codec.isPremiumPoints())
            {
                codec.setPreminumed(premiuned);
            }
        }

        Collections.sort(audioCodecs, null);
        Collections.sort(videoCodecs, null);
		return myView;
	}

	boolean premiuned = false;
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initView(view);
        showToolBarAsActionBar(view,getString(R.string.title_codec),true);
		baseActivity.setSupportActionBar((Toolbar) view.findViewById(R.id.toolBar));
	}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseActivity.mConfigurationService.setMediaConfig(getActivity(), PortSipSdkWrapper.getInstance());//更新配置
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
        inflater.inflate(R.menu.menu_codec,menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                baseActivity.finish();
                break;
            case R.id.menu_codec_edit:
                editMode = !editMode;
                if(editMode)
                {
                    item.setTitle(getString(R.string.string_finish));
                }else{
                    item.setTitle(getString(R.string.string_edit));
                }
                aduioAdapter.setEditMode(editMode);
                videoAdapter.setEditMode(editMode);
                //
                aduioAdapter.notifyDataSetChanged();
                videoAdapter.notifyDataSetChanged();
                break;
        }
        return true;
    }

    @Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

	}

	@Override
	public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result) {
		if (mNeedRemoveFormList) {
			fragments.remove(mFragmentId);
		}
		manager.beginTransaction().remove(this).commit();
		PortBaseFragment backFragment = (PortBaseFragment) fragments.get(mBackFragmentId);
		if (mBackFragmentId != -1 && backFragment != null && mFragmentResId != -1) {
			showFramegment(getActivity(),manager, fragments, mFragmentResId, backFragment);
			return true;
		}
		return false;
	}

    CodecAdapter aduioAdapter;
    CodecAdapter videoAdapter;

    private void initView(View view) {
		DragListView audiolistView = (DragListView) view.findViewById(R.id.activity_main_fragment_setting_codec_audio);
		DragListView videolistView = (DragListView) view.findViewById(R.id.activity_main_fragment_setting_codec_video);
		aduioAdapter = new CodecAdapter(getActivity(), audioCodecs,new AudioCodecSwithListener());
		videoAdapter= new CodecAdapter(getActivity(), videoCodecs,new VideoCodecSwithListener());
		audiolistView.setAdapter(aduioAdapter);
		videolistView.setAdapter(videoAdapter);
        audiolistView.setDragModeJudgeMent(this);
        videolistView.setDragModeJudgeMent(this);
	}

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            default:
                break;
        }
    }

    @Override
    synchronized public boolean startDragMode(View view, int positon,int posRawX, int posRawY) {//
		if(view==null||positon==AdapterView.INVALID_POSITION)
			return false;
        ImageView mover = (ImageView) view.findViewById(R.id.activity_main_fragment_setting_codec_itemmover);
        if(mover.getVisibility() != View.VISIBLE)
            return false;

        return inRangeOfView(mover, posRawX, posRawY);
    }

    @Override
    public void stopDragMode() {
        saveCodec();
        baseActivity.mConfigurationService.setMediaConfig(getActivity(),PortSipSdkWrapper.getInstance());//更新配置
    }

    private boolean inRangeOfView(View view, int posRawX, int posRawY ){
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = view.getWidth();
        int height = view.getHeight();
        return new Rect(x,y,x+width,y+height).contains(posRawX,posRawY);
    }

    private void saveCodec(){

        saveAudioCodec();
        saveVideoCodec();


    }
    private void saveAudioCodec(){
        if(aduioAdapter!=null){
            int size = aduioAdapter.getCount();
            for (int index=0;index<size;index++){
                Codec codec = aduioAdapter.getItem(index);
                codec.setPriority(index);
            }
        }
        Set<String> values = new HashSet<String>();
        if (audioCodecs != null && audioCodecs.size() >= 0) {
            for (Object val : audioCodecs) {
                values.add(val.toString());
            }
        }
        baseActivity.mConfigurationService.putStringSet(getActivity(),
                ConfigurationManager.PRESENCE_AUDIO_CODE,values);//保存优先级
    }
    private void saveVideoCodec(){
        Set<String> values = new HashSet<String>();

        if(videoAdapter!=null){
            int size = videoAdapter.getCount();
            for (int index=0;index<size;index++){
                Codec codec = videoAdapter.getItem(index);
                codec.setPriority(index);
            }
        }

        if (videoCodecs != null && videoCodecs.size() >= 0) {
            for (Object val : videoCodecs) {
                values.add(val.toString());
            }
        }
        baseActivity.mConfigurationService.putStringSet(getActivity(),ConfigurationManager.PRESENCE_VIDEO_CODE,values);
    }

    class VideoCodecSwithListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int positon = (int) compoundButton.getTag();
            if(videoAdapter!=null){
                Codec codec =  videoAdapter.getItem(positon);
                codec.setEnable(b);
                saveVideoCodec();
            }
        }
    }

    class AudioCodecSwithListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int positon = (int) compoundButton.getTag();
            if(aduioAdapter!=null){
                Codec codec =  aduioAdapter.getItem(positon);
                codec.setEnable(b);
                saveAudioCodec();
            }
        }
    }
}
