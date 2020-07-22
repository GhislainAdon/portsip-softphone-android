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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactPhoneSelectAdapter extends BaseExpandableListAdapter {
    private final LayoutInflater mInflater;
    private List<Contact> mContacts = new ArrayList<>();
    private List<Contact> mContactsCopy;
    private Context mContext;
    int mAvartaTextsize = 16;

    private final Map<Integer, Bitmap> avatrarCache = new HashMap<>();//

    HashMap<Long,Boolean> selectChild= new HashMap();
    final int FAKE_CONTACT_ID = Integer.MAX_VALUE -1000;

    public ContactPhoneSelectAdapter(Context context, List<Contact> contacts) {
        mContext = context;
        mContactsCopy=contacts;

        mInflater = LayoutInflater.from(mContext);
        mAvartaTextsize = mContext.getResources().getInteger(R.integer.contact_avatar_textsize);
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
        return mContacts.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Contact group = (Contact) getGroup(groupPosition);
        if(group.getId()>FAKE_CONTACT_ID) {
            return 0;
        }else {
            return group.getContactNumbers().size();
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mContacts.get(groupPosition);
    }

    final int VIEWTYPE_FAKE_GROUP =0;
    final int VIEWTYPE_REAL_GROUP =1;

    @Override
    public int getGroupType(int groupPosition) {
        Contact contact = (Contact) getGroup(groupPosition);
        if(contact.getId()>FAKE_CONTACT_ID){
            return VIEWTYPE_FAKE_GROUP;
        }else{
            return VIEWTYPE_REAL_GROUP;
        }
    }

    @Override
    public int getGroupTypeCount() {
        return 2;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Contact contact = (Contact) getGroup(groupPosition);
        if(contact.getId()>FAKE_CONTACT_ID){
            return null;
        }else {
            return contact.getContactNumbers().get(childPosition);
        }
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        Contact.ContactDataNumber number = (Contact.ContactDataNumber) getChild(groupPosition,childPosition);
        return  number.getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View view =null;
        Contact contact = (Contact) getGroup(groupPosition);
        if(getGroupType(groupPosition)==VIEWTYPE_FAKE_GROUP) {
            view = mInflater.inflate(R.layout.view_contact_group, parent, false);
            TextView tvDisplayName = (TextView) view.findViewById(R.id.view_list_header_title);
            ImageView mGroupIndicator = (ImageView) view.findViewById(R.id.group_indicator);
            String groupName = contact.getDisplayName();
            SpannableString spanString = new SpannableString(groupName);
            RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(0.7f);

            spanString.setSpan(relativeSizeSpan, groupName.indexOf("("), groupName.indexOf(")") + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            tvDisplayName.setText(spanString);
        }else{
            ViewHolder.ContactViewHolder holder = null;
            if (view == null) {
                holder = new ViewHolder.ContactViewHolder();
                view = mInflater.inflate(R.layout.activity_contacts_lvitem, null);
                holder.contacts_item_radiobox = (CheckBox) view.findViewById(R.id.contacts_item_radiobox);
                holder.contacts_item_textView_displayname = (TextView) view.findViewById(R.id.contacts_item_textView_displayname);
                holder.contacts_item_txavatar= (TextView) view.findViewById(R.id.user_avatar_text);
                holder.contacts_item_txavatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,mAvartaTextsize);
                holder.contacts_item_avatar = (RoundedImageView) view.findViewById(R.id.user_avatar_image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder.ContactViewHolder) view.getTag();
            }

            updateContactView(holder,contact, (ListView) parent);
            return view;
        }
        return view;
    }

    private void updateContactView(ViewHolder.ContactViewHolder holder, Contact contact,ListView listView) {
        String disPlayname = contact.getDisplayName();
        int contactid = contact.getId();
        holder.contacts_item_radiobox.setVisibility(View.GONE);
        holder.contacts_item_textView_displayname.setText(disPlayname);

        Bitmap avatar = null;
        int photoid = contact.getContactAvatarId();
        if(photoid>0) {
            avatar = avatrarCache.get(photoid);//
            if(avatar==null) {//
                Contact.ContactDataPhoto contactPhoto = contact.getPhoto(mContext, ContactsContract.Contacts.CONTENT_URI, photoid, false);
                if (contactPhoto != null) {
                    avatrarCache.put(contactPhoto.getId(), contactPhoto.bitmap);
                    avatar = contactPhoto.bitmap;
                }
            }

        }
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

    public boolean childItemChecked(long childid) {
        Object value = selectChild.get(childid);
        return  value==null?false:(Boolean)value;
    }


    public boolean setChildItemChecked(long childid, boolean check) {
        selectChild.clear();
        selectChild.put(childid,check);
        return check;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolder.PhoneNumberViewHolder holder = null;
        if(convertView==null){
            convertView = mInflater.inflate(R.layout.activity_main_phone_select_fragment_childitem,null);
            holder = new ViewHolder.PhoneNumberViewHolder();
            holder.tvPhoneNumber = (TextView) convertView.findViewById(R.id.child_item_title);
            holder.tvCheck = (CheckBox) convertView.findViewById(R.id.phone_item_check);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder.PhoneNumberViewHolder) convertView.getTag();
        }
        Contact.ContactDataNumber number = (Contact.ContactDataNumber) getChild(groupPosition,childPosition);
        holder.tvPhoneNumber.setText( number.getNumber());
        holder.tvCheck.setChecked(childItemChecked(number.getId()));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private void updateSections() {
        LinkedHashMap<String,ArrayList<Contact>> data = new LinkedHashMap();
        mContacts.clear();
        synchronized (mContactsCopy) {
            List<Contact> contacts = mContactsCopy;
            final String lastGroup = "#";

            for (Contact contact : contacts) {//
                String sortKey = contact.getSortKey();
                if (NgnStringUtils.isNullOrEmpty(sortKey)) {
                    continue;
                }

                String group = sortKey.substring(0,1);
                if(!NgnStringUtils.firstCharactIsletter(sortKey)){//
                    group = lastGroup;
                }

                ArrayList<Contact> children = data.get(group);
                if (children == null) {
                    children = new ArrayList<Contact>();
                    data.put(group, children);
                }
                children.add(contact);

            }

            Object value = data.get(lastGroup);
            if(value!=null) {
                data.remove(lastGroup);
                data.put(lastGroup, (ArrayList<Contact>) value);
            }

            Set<Map.Entry<String,ArrayList<Contact>>> entrys = data.entrySet();
            List<Map.Entry<String,ArrayList<Contact>>> list = new ArrayList<Map.Entry<String,ArrayList<Contact>>>(entrys);
            data.clear();

            int fakeContactNum=0;
            for(Map.Entry<String,ArrayList<Contact>> entry : list){
                ArrayList<Contact> values = entry.getValue();
                if(values.size()>0) {
                    Contact fackerContact = new Contact(FAKE_CONTACT_ID + (++fakeContactNum),
                            entry.getKey() + "(" +values.size()+ ")");
                    fackerContact.setSortKey(entry.getKey());
                    mContacts.add(fackerContact);
                    mContacts.addAll(values);
                }
            }

        }
    }

}