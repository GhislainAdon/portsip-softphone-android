package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.CursorEndEditTextView;
import com.portsip.PortSipSdk;

import java.util.Map;

public class ActivityMainSettingPrefrenceFragment extends PortBaseFragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener {
    Uri defaultRing;
    Uri defaultNotification;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        try {
            defaultRing = RingtoneManager.getActualDefaultRingtoneUri(baseActivity, RingtoneManager.TYPE_RINGTONE);
            defaultNotification = RingtoneManager.getActualDefaultRingtoneUri(baseActivity, RingtoneManager.TYPE_NOTIFICATION);
        }catch (SecurityException security){

        }

		return inflater.inflate(R.layout.activity_main_setting_fragment_advance, null);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initView(view);
        showToolBarAsActionBar(view,getString(R.string.title_preference),true);
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

	private void initView(View view){
        view.findViewById(R.id.activity_main_fragment_setting_advance_dtmf_type).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_video_fps).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_video_bitrate).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_srtp).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_video_resolution).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_imring).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_advance_callring).setOnClickListener(this);

        CursorEndEditTextView rtpEditor = (CursorEndEditTextView) view.findViewById(R.id.activity_main_fragment_setting_advance_rtp);
        int rtpPortStart = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_RTP_PORT_START
                ,getResources().getInteger(R.integer.prefrence_rtpport_default));
        rtpEditor.setOnFocusChangeListener(this);
        rtpEditor.setTextCursorEnd(""+rtpPortStart);

        ToggleButton tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_aec_switch);
		tg.setOnCheckedChangeListener(this);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_AEC,
                getResources().getBoolean(R.bool.prefrence_aec_default)));
		tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_cng_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CNG,
                getResources().getBoolean(R.bool.prefrence_cng_default)));
		tg.setOnCheckedChangeListener(this);
        //
		tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_nr_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NR,
                getResources().getBoolean(R.bool.prefrence_nr_default)));
		tg.setOnCheckedChangeListener(this);
        //
		tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_vad_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_VAD,
                getResources().getBoolean(R.bool.prefrence_vad_default)));
		tg.setOnCheckedChangeListener(this);
        //
		tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_agc_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_AGC,
                getResources().getBoolean(R.bool.prefrence_agc_default)));
		tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_dtmf_ringback_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_DTMF_BACK,
                getResources().getBoolean(R.bool.prefrence_dtmf_backtone_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_video_nack_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_VIDEO_NACK,
                getResources().getBoolean(R.bool.prefrence_video_nack_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_prack_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_PRACK,
                getResources().getBoolean(R.bool.prefrence_prack_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_earlymedia_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_EARLYMEDIA,
                getResources().getBoolean(R.bool.prefrence_earlymedia_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_sessiontime_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_PST,
                getResources().getBoolean(R.bool.prefrence_pst_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_ims_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_IMS,
                getResources().getBoolean(R.bool.prefrence_ims_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_autorec_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CALLING_RECORD,
                getResources().getBoolean(R.bool.prefrence_record_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_ring_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_ENABLE_RING,
                getResources().getBoolean(R.bool.prefrence_ring_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.activity_main_fragment_setting_advance_vibrate_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_ENABLE_VIBRATE,
                getResources().getBoolean(R.bool.prefrence_vibrate_default)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.fragment_setting_advance_dialer_intergrate_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NATIVE_DIALER,
                getResources().getBoolean(R.bool.prefrence_native_dialer)));
        tg.setOnCheckedChangeListener(this);

        tg = (ToggleButton)view.findViewById(R.id.fragment_setting_advance_default_account_switch);
        tg.setChecked(baseActivity.mConfigurationService.getBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NATIVE_DIALER_ACTIVITY,
                getResources().getBoolean(R.bool.prefrence_native_dialer_activity)));
        tg.setOnCheckedChangeListener(this);

        TextView textView;
        int select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_DTMF
                ,getResources().getInteger(R.integer.prefrence_dtmf_type_default));
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_dtmf_value);
        String[] selector = getResources().getStringArray(R.array.dtmf_type);
        textView.setText(selector[select]);

        select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_FPS
                ,getResources().getInteger(R.integer.prefrence_video_fps_default));
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_fps_value);
        selector = getResources().getStringArray(R.array.videofps_type);
        textView.setText(selector[select]);

        select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_BITRAT
                ,getResources().getInteger(R.integer.prefrence_video_bitrate_default));
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_bitrate_value);
        selector = getResources().getStringArray(R.array.videobits_type);
        textView.setText(selector[select]);

        select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_SRTP
                ,getResources().getInteger(R.integer.prefrence_srtp_default));
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_srtp_value);
        selector = getResources().getStringArray(R.array.srtp_type);
        textView.setText(selector[select]);

        select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_VIDEOREASOUTION
                ,getResources().getInteger(R.integer.prefrence_video_resolution_default));
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_resolution_value);
        selector = getResources().getStringArray(R.array.videoresolution_type);
        textView.setText(selector[select]);

        String uri = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_CALLRING
                ,defaultRing==null?"":defaultRing==null?"":defaultRing.toString());

        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_callring_value);
        textView.setText(getRingUriName(Uri.parse(uri)));

        uri = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_IMRING
                ,defaultNotification==null?"":defaultNotification.toString());
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_imring_value);
        textView.setText(getRingUriName(Uri.parse(uri)));

        String dirName = getString(R.string.prefrence_record_filepath_default);
        String defaultdir = baseActivity.getApplicationContext().getExternalFilesDir(dirName).getAbsolutePath();
        String dir = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_RECORD_DIR,defaultdir);
        getView().findViewById(R.id.activity_main_fragment_setting_advance_recdirectory).setOnClickListener(this);
        textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_recdirectory_value);
        textView.setText(dir);
    }

	@Override
	public void onClick(View view) {
		String[] data;
        int select = 0;
        Intent intent = new Intent(baseActivity,PortActivityPrefrenceSelector.class);
		switch (view.getId()){
			case R.id.activity_main_fragment_setting_advance_dtmf_type:
				data = getResources().getStringArray(R.array.dtmf_type);
                select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_DTMF,
                        getResources().getInteger(R.integer.prefrence_dtmf_type_default));
                intent.putExtra(PortActivityPrefrenceSelector.CONTENT_DATA,data);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_ID,select);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_TITLE,getString(R.string.dtmf_select_title));
                startActivityForResult(intent,R.id.activity_main_fragment_setting_advance_dtmf_type);
                break;
            case R.id.activity_main_fragment_setting_advance_video_fps:
                data = getResources().getStringArray(R.array.videofps_type);
                select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_FPS,
                        getResources().getInteger(R.integer.prefrence_video_fps_default));
                intent.putExtra(PortActivityPrefrenceSelector.CONTENT_DATA,data);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_ID,select);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_TITLE,getString(R.string.fps_select_title));
			    startActivityForResult(intent,R.id.activity_main_fragment_setting_advance_video_fps);
                break;
			case R.id.activity_main_fragment_setting_advance_video_bitrate:
				data = getResources().getStringArray(R.array.videobits_type);
                select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_BITRAT,
                        getResources().getInteger(R.integer.prefrence_video_bitrate_default));
                intent.putExtra(PortActivityPrefrenceSelector.CONTENT_DATA,data);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_ID,select);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_TITLE,getString(R.string.vbr_select_title));
                startActivityForResult(intent,R.id.activity_main_fragment_setting_advance_video_bitrate);
                break;
			case R.id.activity_main_fragment_setting_advance_srtp:
				data = getResources().getStringArray(R.array.srtp_type);
                select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_SRTP,
                        getResources().getInteger(R.integer.prefrence_srtp_default));
                intent.putExtra(PortActivityPrefrenceSelector.CONTENT_DATA,data);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_ID,select);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_TITLE,getString(R.string.srtp_select_title));
                startActivityForResult(intent,R.id.activity_main_fragment_setting_advance_srtp);
                break;
			case R.id.activity_main_fragment_setting_advance_video_resolution:
				data = getResources().getStringArray(R.array.videoresolution_type);
                select = baseActivity.mConfigurationService.getIntergerValue(getActivity(),ConfigurationManager.PRESENCE_VIDEOREASOUTION,
                        getResources().getInteger(R.integer.prefrence_video_resolution_default));
                intent.putExtra(PortActivityPrefrenceSelector.CONTENT_DATA,data);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_ID,select);
                intent.putExtra(PortActivityPrefrenceSelector.SELECT_TITLE,getString(R.string.vr_select_title));
                startActivityForResult(intent,R.id.activity_main_fragment_setting_advance_video_resolution);
                break;
            case R.id.activity_main_fragment_setting_advance_imring:
                Intent imsRingintent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                imsRingintent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                imsRingintent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.label_im_ring));
                String imRingUri = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_IMRING,defaultNotification==null?"":defaultNotification.toString());
                if (!TextUtils.isEmpty(imRingUri)) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(imRingUri));
                }
                startActivityForResult(imsRingintent, R.id.activity_main_fragment_setting_advance_imring);
                break;
            case R.id.activity_main_fragment_setting_advance_callring:
                Intent callRingintent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                callRingintent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                callRingintent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.label_call_ring));
                String callRingUri = baseActivity.mConfigurationService.getStringValue(getActivity(),ConfigurationManager.PRESENCE_CALLRING,defaultRing==null?"":defaultRing.toString());
                if (!TextUtils.isEmpty(callRingUri)) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(callRingUri));
                }
                startActivityForResult(callRingintent, R.id.activity_main_fragment_setting_advance_callring);
                break;
            case R.id.activity_main_fragment_setting_advance_recdirectory:
                startActivity(new Intent(getActivity(),PortActivityRecords.class));
                break;
        }
	}

	String getRingUriName(Uri uri){
        String name = "";
        if(uri!=null){
            if(ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                String path= uri.getPath();
                name = path.substring(uri.getPath().lastIndexOf("/") + 1, path.length());
            }
            if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())){
                Cursor ringCursor =CursorHelper.resolverQuery(getActivity().getContentResolver(),uri, null, null, null, null);
                if (CursorHelper.moveCursorToFirst(ringCursor)) {
                    int index = ringCursor.getColumnIndex(MediaStore.MediaColumns.TITLE);
                    if (index > -1) {
                        name = ringCursor.getString(index);
                    }
                }
                CursorHelper.closeCursor(ringCursor);
            }

        }
        return name;
    }
	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
		switch (compoundButton.getId()){
			case R.id.activity_main_fragment_setting_advance_aec_switch:
				baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_AEC,b);
				break;
			case R.id.activity_main_fragment_setting_advance_cng_switch:
				baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CNG,b);
				break;
			case R.id.activity_main_fragment_setting_advance_nr_switch:
				baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NR,b);
				break;
			case R.id.activity_main_fragment_setting_advance_vad_switch:
				baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_VAD,b);
				break;
			case R.id.activity_main_fragment_setting_advance_agc_switch:
				baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_AGC,b);
				break;
            case R.id.activity_main_fragment_setting_advance_dtmf_ringback_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_DTMF_BACK,b);
                break;
            case R.id.activity_main_fragment_setting_advance_video_nack_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_VIDEO_NACK,b);
                break;
            case R.id.activity_main_fragment_setting_advance_prack_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_PRACK,b);
                break;
            case R.id.activity_main_fragment_setting_advance_earlymedia_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_EARLYMEDIA,b);
                break;
            case R.id.activity_main_fragment_setting_advance_ims_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_IMS,b);
                break;
            case R.id.activity_main_fragment_setting_advance_autorec_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_CALLING_RECORD,b);
                break;

            case R.id.activity_main_fragment_setting_advance_sessiontime_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_PST,b);
                break;

            case R.id.activity_main_fragment_setting_advance_ring_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_ENABLE_RING,b);
                break;

            case R.id.activity_main_fragment_setting_advance_vibrate_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_ENABLE_VIBRATE,b);
                break;
            case R.id.fragment_setting_advance_dialer_intergrate_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NATIVE_DIALER,b);
                if(b) {
                    getView().findViewById(R.id.fragment_setting_advance_default_account_switch).setEnabled(true);
                }else {
                    ((ToggleButton)getView().findViewById(R.id.fragment_setting_advance_default_account_switch)).setChecked(false);
                    getView().findViewById(R.id.fragment_setting_advance_default_account_switch).setEnabled(false);
                }
                break;
            case R.id.fragment_setting_advance_default_account_switch:
                baseActivity.mConfigurationService.setBooleanValue(getActivity(),ConfigurationManager.PRESENCE_NATIVE_DIALER_ACTIVITY,b);
                break;

		}
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int select = 0;
        String[] selector;
        TextView textView = null;
        if(resultCode!= Activity.RESULT_OK)
            return;
        switch (requestCode){
            case R.id.activity_main_fragment_setting_advance_dtmf_type:
                select = data.getIntExtra(PortActivityPrefrenceSelector.SELECT_ID,getResources().getInteger(R.integer.prefrence_dtmf_type_default));
                textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_dtmf_value);
                selector = getResources().getStringArray(R.array.dtmf_type);
                textView.setText(selector[select]);
                baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_DTMF,select);
                break;
            case R.id.activity_main_fragment_setting_advance_video_fps:
                select = data.getIntExtra(PortActivityPrefrenceSelector.SELECT_ID,getResources().getInteger(R.integer.prefrence_video_fps_default));
                textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_fps_value);
                selector = getResources().getStringArray(R.array.videofps_type);
                textView.setText(selector[select]);
                baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_FPS,select);
                break;
            case R.id.activity_main_fragment_setting_advance_video_bitrate:
                select = data.getIntExtra(PortActivityPrefrenceSelector.SELECT_ID,getResources().getInteger(R.integer.prefrence_video_bitrate_default));
                textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_bitrate_value);
                selector = getResources().getStringArray(R.array.videobits_type);
                textView.setText(selector[select]);
                baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_BITRAT,select);
                break;
            case R.id.activity_main_fragment_setting_advance_srtp:
                select = data.getIntExtra(PortActivityPrefrenceSelector.SELECT_ID,getResources().getInteger(R.integer.prefrence_srtp_default));
                textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_srtp_value);
                selector = getResources().getStringArray(R.array.srtp_type);
                textView.setText(selector[select]);
                baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_SRTP,select);
                break;
            case R.id.activity_main_fragment_setting_advance_video_resolution:
                select = data.getIntExtra(PortActivityPrefrenceSelector.SELECT_ID,getResources().getInteger(R.integer.prefrence_video_resolution_default));
                textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_video_resolution_value);
                selector = getResources().getStringArray(R.array.videoresolution_type);
                textView.setText(selector[select]);
                baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_VIDEOREASOUTION,select);
                break;
            case R.id.activity_main_fragment_setting_advance_callring:
                try {
                    Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String callRing = pickedUri.toString();

                    baseActivity.mConfigurationService.setStringValue(getActivity(),ConfigurationManager.PRESENCE_CALLRING,callRing);
                    textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_callring_value);
                    textView.setText(getRingUriName(pickedUri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.activity_main_fragment_setting_advance_imring:
                try {
                    Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String imRing = pickedUri.toString();
                    baseActivity.mConfigurationService.setStringValue(getActivity(),ConfigurationManager.PRESENCE_IMRING,imRing);
                    textView = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_advance_imring_value);
                    textView.setText(getRingUriName(pickedUri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onDestroyView() {
        baseActivity.mConfigurationService.setMediaConfig(getActivity(), PortSipSdkWrapper.getInstance());//
        super.onDestroyView();
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        switch (view.getId()){
            case R.id.activity_main_fragment_setting_advance_rtp:
                if(!b){
                    String text = ((EditText)view).getText().toString();
                    int port = NgnStringUtils.parseInt(text,getResources().getInteger(R.integer.prefrence_rtpport_default));
                    baseActivity.mConfigurationService.setIntegerValue(getActivity(),ConfigurationManager.PRESENCE_RTP_PORT_START,port);
                }
                break;
        }

    }
}
