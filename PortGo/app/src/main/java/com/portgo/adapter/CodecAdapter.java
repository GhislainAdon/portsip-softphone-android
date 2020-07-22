package com.portgo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.portgo.R;
import com.portgo.util.Codec;
import com.portgo.view.DragListViewAdapter;

import java.util.List;

/**
 * Created by huacai on 2017/5/24.
 */

public class CodecAdapter extends DragListViewAdapter<Codec> {
    LayoutInflater mInflater;
    boolean editMode =false;
    ToggleButton.OnCheckedChangeListener mCheckedChangeListener;
    public CodecAdapter(Context context, List<Codec> codecs, CompoundButton.OnCheckedChangeListener checkedChangeListener) {
        super(context, codecs);
        mInflater =  LayoutInflater.from(mContext);
        mCheckedChangeListener = checkedChangeListener;
    }

    @Override
    public View getItemView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.activity_main_setting_fragment_codec_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) convertView.findViewById(R.id.activity_main_fragment_setting_codec_itemname);
            viewHolder.button = (ToggleButton) convertView.findViewById(R.id.activity_main_fragment_setting_codec_itemtoggle);
            viewHolder.mover = (ImageView) convertView.findViewById(R.id.activity_main_fragment_setting_codec_itemmover);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Codec codec =mDragDatas.get(position);
        convertView.setBackgroundResource(R.color.portgo_color_white);
        if(codec.isPremiumPoints()){
            viewHolder.name.setCompoundDrawablesWithIntrinsicBounds(null,null,
                    mContext.getResources().getDrawable(R.drawable.set_important),null);
            if(!codec.getPreminumed()) {
                convertView.setBackgroundResource(R.color.portgo_color_unavalable_gray);
            }
        }else {
            viewHolder.name.setCompoundDrawablesWithIntrinsicBounds(null,null,null,null);
        }

        if (editMode){
            viewHolder.mover.setVisibility(View.VISIBLE);
            viewHolder.button.setVisibility(View.GONE);

        }else{
            viewHolder.button.setVisibility(View.VISIBLE);
            viewHolder.mover.setVisibility(View.GONE);
        }

        viewHolder.name.setText(codec.getCodecName());
        viewHolder.button.setTag(position);//

        viewHolder.button.setChecked(codec.isEnable());
        viewHolder.button.setOnCheckedChangeListener(mCheckedChangeListener);
        if(codec.isPremiumPoints()&&!codec.getPreminumed()) {
            viewHolder.button.setEnabled(false);
        }else{
            viewHolder.button.setEnabled(true);
        }

        return convertView;
    }

    public void setEditMode(boolean enableMover){
        editMode = enableMover;
    }

    @Override
    public long getItemId(int position) {
        Codec codec = getItem(position);
        return codec.getCodecId();
    }

    class ViewHolder{
        TextView name;
        ImageView mover;
        ToggleButton button;
    }
}