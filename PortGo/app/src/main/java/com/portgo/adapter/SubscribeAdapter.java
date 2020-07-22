package com.portgo.adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.PresenseMessage;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SubscribeAdapter extends BaseAdapter implements View.OnClickListener {
    private LayoutInflater mInflater;
    private Context mContext;
    HashMap<Long,PresenseMessage>  mMessages=null;
    OnSubscribeActionClick actionClick = null;
    public static enum ACTION_CODE{
        ACCEPT,
        REJECT
    }
    public SubscribeAdapter(Context context, HashMap<Long,PresenseMessage> messages,OnSubscribeActionClick clickListener ) {
        super();
        mMessages = messages;
        mContext= context;
        actionClick = clickListener;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder.SubViewHolder holder = new ViewHolder.SubViewHolder();
        if(convertView==null) {
            convertView = mInflater.inflate(R.layout.activity_subscribe_item, null);
            holder.tvRemote = (TextView) convertView.findViewById(R.id.subscribe_item_textView_remote);
            holder.tvDate = (TextView) convertView.findViewById(R.id.subscribe_item_textView_date);
            holder.tvContent = (TextView) convertView.findViewById(R.id.subscribe_item_textView_content);
            holder.tvAvatar = (TextView) convertView.findViewById(R.id.user_avatar_text);
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.user_avatar_image);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.subscribe_item_radiobox);
            holder.tvAccept= (TextView) convertView.findViewById(R.id.subscribe_item_accept);
            holder.tvReject= (TextView) convertView.findViewById(R.id.subscribe_item_reject);

            convertView.setTag(holder);
        }else{
            holder = (ViewHolder.SubViewHolder) convertView.getTag();
        }

        PresenseMessage event = (PresenseMessage) getItem(position);
        updateSubScribeItemView(holder,event, (ListView) parent);
        return convertView;
    }

    @Override
    public int getCount() {
        return mMessages==null?0:mMessages.size();
    }

    @Override
    public Object getItem(int i) {
        Iterator it = mMessages.entrySet().iterator();
        Map.Entry<Long,PresenseMessage> message= null;
        int positon = 0;
        while (it.hasNext()){
            message = (Map.Entry<Long, PresenseMessage>) it.next();
            if(i==positon) {
                break;
            }
            positon++;

        }
        return message.getValue();
    }

    @Override
    public long getItemId(int i) {
        return ((PresenseMessage)getItem(i)).getPresenceId();
    }

    private void updateSubScribeItemView(ViewHolder.SubViewHolder holder, PresenseMessage presenseMessage,ListView parent){

        String disName = presenseMessage.getDisplayName();
        String status =presenseMessage.getPresenceStatus();

        holder.tvRemote.setText(disName);
        holder.tvContent.setText(NgnStringUtils.isNullOrEmpty(status)? NgnStringUtils.emptyValue() : status);

        holder.imgAvatar.setVisibility(View.GONE);
        holder.tvAvatar.setVisibility(View.VISIBLE);
        holder.tvAvatar.setText(NgnStringUtils.getAvatarText(disName));

        holder.tvAccept.setTag(presenseMessage.getPresenceId());
        holder.tvReject.setTag(presenseMessage.getPresenceId());

        holder.tvAccept.setOnClickListener(this);
        holder.tvReject.setOnClickListener(this);

        if(parent.getChoiceMode()==ListView.CHOICE_MODE_MULTIPLE){
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(presenseMessage.isChecked());
        }else{
            holder.checkBox.setVisibility(View.GONE);
        }

        if(presenseMessage.isAccept()){
            holder.tvAccept.setEnabled(false);
            holder.tvReject.setEnabled(false);
            holder.tvAccept.setVisibility(View.VISIBLE);
            holder.tvReject.setVisibility(View.GONE);
            holder.tvAccept.setText(R.string.string_accepted);
        }else
        if(presenseMessage.isReject()){
            holder.tvAccept.setEnabled(false);
            holder.tvReject.setEnabled(false);
            holder.tvAccept.setVisibility(View.VISIBLE);
            holder.tvReject.setVisibility(View.GONE);
            holder.tvAccept.setText(R.string.string_rejected);
        }else{
            holder.tvAccept.setEnabled(true);
            holder.tvReject.setEnabled(true);
            holder.tvAccept.setVisibility(View.VISIBLE);
            holder.tvReject.setVisibility(View.VISIBLE);
            holder.tvReject.setText(R.string.string_reject);
            holder.tvAccept.setText(R.string.string_accept);
        }

        return ;
    }

    @Override
    public void notifyDataSetChanged(){
        super.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        ACTION_CODE code;
        int id;
        switch (view.getId()){
            case R.id.subscribe_item_accept:
                code = ACTION_CODE.ACCEPT;
                id =(int)view.getTag();
                break;
            case R.id.subscribe_item_reject:
                code = ACTION_CODE.REJECT;
                id =(int)view.getTag();
                break;
            default:
                return;
        }
        if(actionClick!=null){
            actionClick.onClick(id,code);
        }
    }

    public interface OnSubscribeActionClick{
        void onClick(long id,ACTION_CODE code);
    }
}
