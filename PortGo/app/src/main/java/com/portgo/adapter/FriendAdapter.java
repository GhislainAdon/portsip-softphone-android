package com.portgo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.view.RoundedImageView;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huacai on 2017/5/24.
 */

public class FriendAdapter extends BaseAdapter{
    private final LayoutInflater mInflater;
    private final Context mContext;

    private List<Contact> mContacts ;
    HashMap<Integer,Boolean> selectItems = new HashMap();
    int avartaTextsize  = 16;

    public FriendAdapter(Context context, List<Contact> contacts) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mContacts = contacts;
        avartaTextsize = mContext.getResources().getInteger(R.integer.contact_avatar_textsize);
    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public Object getItem(int i) {
        if(mContacts.size()>i) {
            return mContacts.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return mContacts.get(i).getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder.FriendViewHolder holder = null;

        if (view == null) {
            holder = new ViewHolder.FriendViewHolder();
            view = mInflater.inflate(R.layout.activity_friend_lvitem, null);
            holder.friend_item_radiobox = (CheckBox) view.findViewById(R.id.friend_item_radiobox);
            holder.friend_item_textView_displayname = (TextView) view.findViewById(R.id.friend_item_textView_displayname);
            holder.friend_item_txavatar= (TextView) view.findViewById(R.id.user_avatar_text);
            holder.friend_item_avatar = (RoundedImageView) view.findViewById(R.id.user_avatar_image);
            holder.friend_item_txavatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,avartaTextsize);
            holder.friend_item_presence = (TextView) view.findViewById(R.id.friend_item_textView_presence);
            view.setTag(holder);
        } else {
            holder = (ViewHolder.FriendViewHolder) view.getTag();
        }

        updateFriendView(holder,(Contact) getItem(i), (ListView) viewGroup);

        return view;
    }

    private void updateFriendView(ViewHolder.FriendViewHolder holder, Contact contact,ListView listView) {
        String disPlayname = contact.getDisplayName();
        int contactid = contact.getId();
        holder.friend_item_radiobox.setTag(contactid);
        holder.friend_item_textView_displayname.setText(disPlayname);

        if (listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
            holder.friend_item_radiobox.setVisibility(View.VISIBLE);
            boolean check = isContactChecked(contactid);
            holder.friend_item_radiobox.setChecked(check);
        } else {
            holder.friend_item_radiobox.setVisibility(View.GONE);
            selectItems.clear();
        }
        Bitmap avatar = contact.getAvatar();

        if (avatar == null) {
            holder.friend_item_txavatar.setText(contact.getAvatarText());
            holder.friend_item_txavatar.setVisibility(View.VISIBLE);
            holder.friend_item_avatar.setVisibility(View.GONE);
        }else {
            holder.friend_item_avatar.setVisibility(View.VISIBLE);
            holder.friend_item_avatar.setImageBitmap(avatar);
            holder.friend_item_txavatar.setVisibility(View.GONE);
        }

        int resLable= contact.getPresence_resLable();
        int resIcon = contact.getPresence_resicon();
        String status = contact.getPresence_status();
        if(resLable<=0||resIcon<=0) {
            resLable = R.string.status_offline;
            resIcon = R.drawable.mid_content_status_offline_ico;
        }
        holder.friend_item_presence.setCompoundDrawablesWithIntrinsicBounds(null,null,mContext.getResources().getDrawable(resIcon),null);
        holder.friend_item_presence.setText(mContext.getString(resLable));

//        holder.friend_item_status.setText(status);
    }

    public void setSelectAll(){
        selectItems.clear();
        for (int i=0;i<getCount();i++) {
            int id = (int) getItemId(i);
            selectItems.put(id,true);
        }
    }

    public boolean isAllSelect(){
        for (int i = 0; i < getCount(); i++) {
            int id = (int) getItemId(i);
            if(!selectItems.containsKey(id))
                return false;
        }
        return true;
    }

    public void clearSelect(){
        selectItems.clear();
    }
    

    public void setContactCheck(int id,boolean check){
        if(check) {
        selectItems.put(id, check);
        }else {
        selectItems.remove(id);
        }
        notifyDataSetChanged();
        }

    public boolean isContactChecked(int contactId){
        boolean result= false;
        try {
        result =selectItems.get(contactId);
        }catch (NullPointerException e){

        }
        return  result;

        }
    public ArrayList<Integer> getSelectContact(){
        ArrayList<Integer> result = new ArrayList<Integer>();
        result.addAll(selectItems.keySet());
        return result;
        }

    public void setSelectContact(ArrayList<Integer> contacts){
        selectItems.clear();
        for (Integer contactId:contacts) {
            selectItems.put(contactId,true);
        }
        notifyDataSetChanged();
    }
}