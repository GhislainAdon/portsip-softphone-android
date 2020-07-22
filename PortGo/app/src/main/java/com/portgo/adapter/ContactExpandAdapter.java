package com.portgo.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.RoundedImageView;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by huacai on 2017/5/24.
 */

public class ContactExpandAdapter extends BaseExpandableListAdapter implements Observer{
    private final LayoutInflater mInflater;
    private final Context mContext;

    private List<Contact> mContacts;
    HashMap<Integer,Boolean> selectItems = new HashMap();
    HashMap<Integer,Boolean> allItems = new HashMap();
    private final Map<Integer ,Bitmap> avatrarCache  = new HashMap<>();//存储扩展的mime数据信息
    LinkedHashMap<String,ArrayList<Contact>> data = new LinkedHashMap();
    int avartaTextsize = 16;

    public ContactExpandAdapter(Context context, List<Contact> contacts) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        avartaTextsize = mContext.getResources().getInteger(R.integer.contact_avatar_textsize);
        mContacts=contacts;

        super.registerDataSetObserver(update);
        updateSections();
        }

    DataSetObserver update = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateSections();
        }
    };
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        unregisterDataSetObserver(update);
        super.registerDataSetObserver(observer);
        super.registerDataSetObserver(update);
    }
    @Override
    public int getGroupCount() {
        return data.keySet().size();
    }

    @Override
    public int getChildrenCount(int i) {
        List<Contact> children = data.get(data.keySet().toArray()[i]);
        return children.size();
    }

    @Override
    public Object getGroup(int i) {
        return data.keySet().toArray()[i];
    }

    @Override
    public Object getChild(int i, int i1) {
        List<Contact> children = data.get(data.keySet().toArray()[i]);
        return children.get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        Contact contact = (Contact) getChild(i,i1);
        return contact.getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean expanded, View view, ViewGroup viewGroup) {

        view = mInflater.inflate(R.layout.view_contact_group, viewGroup, false);
        TextView tvDisplayName = (TextView) view.findViewById(R.id.view_list_header_title);
        String groupName = getGroup(i) +"( "+getChildrenCount(i)+" )";
        SpannableString spanString = new SpannableString(groupName);
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(0.7f);

        spanString.setSpan(relativeSizeSpan, groupName.indexOf("("), groupName.indexOf(")")+1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        tvDisplayName.setText(spanString);
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        ViewHolder.ContactViewHolder holder = null;
        if (view == null) {
            holder = new ViewHolder.ContactViewHolder();
            view = mInflater.inflate(R.layout.activity_contacts_lvitem, null);
            holder.contacts_item_radiobox = (CheckBox) view.findViewById(R.id.contacts_item_radiobox);
            holder.contacts_item_textView_displayname = (TextView) view.findViewById(R.id.contacts_item_textView_displayname);
            holder.contacts_item_txavatar= (TextView) view.findViewById(R.id.user_avatar_text);
            holder.contacts_item_txavatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,avartaTextsize);
            holder.contacts_item_avatar = (RoundedImageView) view.findViewById(R.id.user_avatar_image);
            view.setTag(holder);
        } else {
            holder = (ViewHolder.ContactViewHolder) view.getTag();
        }
        Contact contact = (Contact) getChild(i,i1);
        updateContactView(holder,contact, (ListView) viewGroup);
        allItems.put(contact.getId(),true);
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void update(Observable observable, Object o) {
        updateSections();
    }


    private void updateContactView(ViewHolder.ContactViewHolder holder, Contact contact,ListView listView) {
        String disPlayname = contact.getDisplayName();
        int contactid = contact.getId();
        holder.contacts_item_radiobox.setTag(contactid);
        holder.contacts_item_textView_displayname.setText(disPlayname);

        if (listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
            holder.contacts_item_radiobox.setVisibility(View.VISIBLE);
            boolean check = isContactChecked(contactid);
            holder.contacts_item_radiobox.setChecked(check);
        } else {
            holder.contacts_item_radiobox.setVisibility(View.GONE);
        }
        Bitmap avatar = contact.getAvatar();

        if (avatar == null) {
            holder.contacts_item_txavatar.setText(contact.getAvatarText());
            holder.contacts_item_txavatar.setVisibility(View.VISIBLE);
            holder.contacts_item_avatar.setVisibility(View.GONE);
        }else {
            holder.contacts_item_avatar.setVisibility(View.VISIBLE);
            holder.contacts_item_avatar.setImageBitmap(avatar);
            holder.contacts_item_txavatar.setVisibility(View.GONE);
        }
    }


    public void setContactCheck(View itemView,int id,boolean check){
        if(check) {
            selectItems.put(id, check);
        }else {
            selectItems.remove(id);
        }

        if(itemView!=null) {
            ViewHolder.ContactViewHolder holderview = (ViewHolder.ContactViewHolder) itemView.getTag();
            holderview.contacts_item_radiobox.setChecked(check);
        }

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
        updateSections();
        notifyDataSetChanged();
    }

    public void setSelectAll(){
        selectItems.clear();
        for (int group=0;group<getGroupCount();group++) {
            for (int i = 0; i < getChildrenCount(group); i++) {
                int id = (int) getChildId(group,i);
                selectItems.put(id, true);
            }
        }
    }

    public boolean isAllSelect(){
        for (int group=0;group<getGroupCount();group++) {
            for (int i = 0; i < getChildrenCount(group); i++) {
                int id = (int) getChildId(group,i);
                if(!selectItems.containsKey(id))
                    return false;
            }
        }
        return true;
    }

    public void clearSelect(){
        selectItems.clear();
    }

    private void updateSections() {
        data.clear();
        synchronized (mContacts) {
            List<Contact> contacts = mContacts;
            String lastGroup = "#", sortKey;

            for (Contact contact : contacts) {//
                sortKey = contact.getSortKey();
                if (NgnStringUtils.isNullOrEmpty(sortKey)) {
                    continue;
                }

                String group = sortKey.substring(0,1);
                if(!NgnStringUtils.firstCharactIsletter(sortKey)){
                    group = lastGroup;
                }

                ArrayList<Contact> children = data.get(group);
                if (children == null) {
                    children = new ArrayList<Contact>();
                    data.put(group, children);
                }
                children.add(contact);

            }

            //
            Set<Map.Entry<String,ArrayList<Contact>>>entrys = data.entrySet();
            List<Map.Entry<String,ArrayList<Contact>>> list = new ArrayList<Map.Entry<String,ArrayList<Contact>>>(entrys);
            data.clear();
            Collections.sort(list, new Comparator<Map.Entry<String,ArrayList<Contact>>>() {
                @Override
                public int compare(Map.Entry<String,ArrayList<Contact>> o1, Map.Entry<String,ArrayList<Contact>> o2) {
                    String sortname1 = o1.getKey();
                    String sortname2 = o2.getKey();
                    if (sortname2 != null && sortname2 != null)
                        return sortname1.compareTo(sortname2);
                    return 1;
                }
            });
            for(Map.Entry<String,ArrayList<Contact>> entry : list){
                data.put(entry.getKey(), entry.getValue());
            }
            Object value = data.get(lastGroup);
            if(value!=null) {
                data.remove(lastGroup);
                data.put(lastGroup, (ArrayList<Contact>) value);
            }
        }
    }

}