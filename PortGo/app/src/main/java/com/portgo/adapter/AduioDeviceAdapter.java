package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.MessageEvent;
import com.portgo.util.DateTimeUtils;
import com.portgo.view.MessageView;
import com.portsip.PortSipEnumDefine;

import org.webrtc.apprtc.AppRTCAudioManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AduioDeviceAdapter extends BaseAdapter{
    Context mContext;
    List<PortSipEnumDefine.AudioDevice> audioDevicesList;
    public AduioDeviceAdapter(Context context, List<PortSipEnumDefine.AudioDevice> devices){
        audioDevicesList =devices;
        mContext = context;
    }

    @Override
    public int getCount() {
        if(audioDevicesList!=null){
            return audioDevicesList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if(audioDevicesList.size()>i){
            return audioDevicesList.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        PortSipEnumDefine.AudioDevice device = (PortSipEnumDefine.AudioDevice) getItem(i);
//        LayoutInflater.from(mContext).inflate()
        ImageButton childview = new ImageButton(mContext);

        setAudioIcon(childview,device,false);
        return childview;
    }

    public static void setAudioIcon(ImageButton button,PortSipEnumDefine.AudioDevice device,boolean big) {
        int resId ;
        if(big){
            resId = R.drawable.call_telephone_receiver_ico;
        }else{
            resId = R.drawable.call_telephone_receiver_smaall_ico;
        }
        switch (device) {
            case EARPIECE:
                if(big){
                    resId = R.drawable.call_telephone_receiver_ico;
                }else{
                    resId = R.drawable.call_telephone_receiver_smaall_ico;
                }
                break;
            case SPEAKER_PHONE:
                if(big){
                    resId = R.drawable.call_speaker_ico;
                }else{
                    resId = R.drawable.call_speaker_small_ico;
                }
                break;
            case WIRED_HEADSET:
                if(big){
                    resId = R.drawable.call_headset_ico;
                }else{
                    resId = R.drawable.call_headset_small_ico;
                }
                break;
            case BLUETOOTH:
                if(big){
                    resId = R.drawable.call_bluetooth_ico;
                }else{
                    resId = R.drawable.call_bluetooth_small_ico;
                }
                break;
            case NONE:
        }
        button.setImageResource(resId);
    }

    public static List<PortSipEnumDefine.AudioDevice> audioDeviceSort(Set<PortSipEnumDefine.AudioDevice> devices){

        List<PortSipEnumDefine.AudioDevice> audioDeviceList = new ArrayList<>();
        if(devices!=null){
            if(devices.contains(PortSipEnumDefine.AudioDevice.EARPIECE)){
                audioDeviceList.add(PortSipEnumDefine.AudioDevice.EARPIECE);
            }
            if(devices.contains(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)){
                audioDeviceList.add(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE);
            }
            if(devices.contains(PortSipEnumDefine.AudioDevice.BLUETOOTH)){
                audioDeviceList.add(PortSipEnumDefine.AudioDevice.BLUETOOTH);
            }
            if(devices.contains(PortSipEnumDefine.AudioDevice.WIRED_HEADSET)){
                audioDeviceList.add(PortSipEnumDefine.AudioDevice.WIRED_HEADSET);
            }
        }

        return audioDeviceList;

    }

}