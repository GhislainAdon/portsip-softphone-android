package com.portgo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.view.ViewHolder;

import java.util.List;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASIM;
import static com.portgo.BuildConfig.HASVIDEO;

/**
 * Created by huacai on 2017/5/24.
 */

public class ContactDetailAdapter extends BaseAdapter{
    private final LayoutInflater mInflater;
    private final Context mContext;
    private List<Contact.ContactDataNumber> allNumbers;
    private View.OnClickListener actionListener;

    public ContactDetailAdapter(Context context,List<Contact.ContactDataNumber> numbers){
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        allNumbers = numbers;
    }

    public void setActionListener(View.OnClickListener listener){
        actionListener = listener;
    }

    @Override
    public int getCount() {
        return allNumbers.size();
    }

    @Override
    public Object getItem(int i) {
        return allNumbers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder.DetaiPhoneHolder holder;

        if(view==null){
            holder = new ViewHolder.DetaiPhoneHolder();
            view = mInflater.inflate(R.layout.contact_detail_phones_lvitem,null);
            holder.audio = (ImageView) view.findViewById(R.id.activity_main_contact_fragment_detail_audiocall);
            holder.video = (ImageView) view.findViewById(R.id.activity_main_contact_fragment_detail_videocall);
            holder.message = (ImageView) view.findViewById(R.id.activity_main_contact_fragment_detail_sendmsg);
            if(!HASIM) {
                holder.message.setVisibility(View.INVISIBLE);
            }
            holder.message.setEnabled(ENABLEIM);
            if(!HASVIDEO) {
                holder.video.setVisibility(View.INVISIBLE);
            }
            holder.video.setEnabled(ENABLEVIDEO);
            holder.phoneNumber = (TextView) view.findViewById(R.id.activity_main_contact_fragment_detail_nubmer);
            holder.phoneType = (TextView) view.findViewById(R.id.activity_main_contact_fragment_detail_type);

            holder.audio.setOnClickListener(actionListener);
            holder.video.setOnClickListener(actionListener);
            holder.message.setOnClickListener(actionListener);

            view.setTag(holder);
        }else {
            holder = (ViewHolder.DetaiPhoneHolder) view.getTag();
        }

        Contact.ContactDataNumber number = (Contact.ContactDataNumber) getItem(i);
        if(number instanceof Contact.ContactDataSipAddress){
            holder.phoneType.setText(mContext.getString(R.string.phone_type_sip));
        }else{
            holder.phoneType.setText(mContext.getString(R.string.phone_type_phone));
        }

        holder.phoneNumber.setText(number.getNumber());
        holder.audio.setTag(number.getNumber());
        holder.video.setTag(number.getNumber());
        holder.message.setTag(number.getNumber());

        return view;
    }

}