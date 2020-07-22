package com.portgo.adapter;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by huacai on 2017/5/24.
 */

public class ContactPhoneAdapter extends BaseAdapter implements Filterable{

    private final LayoutInflater mInflater;
    private final Context mContext;

    private List<Item> mSourceItem;
    private List<Item> mContactItem = new ArrayList<>();
    private List<Item> mContactItemCopy = new ArrayList<>();
    int selectTextColor;
    ContactPhoneFilter mFilter;

    View.OnClickListener moreInfoListener = null;

    public ContactPhoneAdapter(Context context,  List<Item> phoneItems) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mSourceItem = phoneItems;
        mContactItem.addAll(mSourceItem);
        mContactItemCopy.addAll(mSourceItem);
        this.moreInfoListener = moreInfoListener;
        selectTextColor = mContext.getResources().getColor(R.color.portgo_color_blue);
    }

    @Override
    public void notifyDataSetChanged() {//
        mContactItemCopy.clear();
        mContactItemCopy.addAll(mSourceItem);
        mContactItem.clear();
        mContactItem.addAll(mSourceItem);
        super.notifyDataSetChanged();
    }
    private  void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        if(mFilter==null) {
            mFilter = new ContactPhoneFilter();
        }
        return  mFilter;
    }

    @Override
    public int getCount() {
        return mContactItem.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactItem.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder.ContactNumberViewHolder holder = null;

        if (convertView == null) {
            holder = new ViewHolder.ContactNumberViewHolder();
            convertView = mInflater.inflate(R.layout.activity_main_numpad_fragment_item, null);
            holder.tvContactName = (TextView) convertView.findViewById(R.id.activity_main_numpad_fragment_contact_name);
            holder.tvPhoneNumber = (TextView) convertView.findViewById(R.id.activity_main_numpad_fragment_phone_number);
            holder.tvPhoneType = (TextView) convertView.findViewById(R.id.activity_main_numpad_fragment_phone_type);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder.ContactNumberViewHolder) convertView.getTag();
        }

        updateContactView(holder,position, (Item) getItem(position), (ListView) parent);
        return convertView;
    }

    String searchContent = null;
    private void updateContactView(ViewHolder.ContactNumberViewHolder holder,int i, Item item,ListView listView) {

        if (searchContent != null) {
            String name = item.displayName;
            int disNameindex = item.displayName.toUpperCase().indexOf(searchContent);
            int sortNameindex =  item.sortName.toUpperCase().indexOf(searchContent);
            int sortNameForNine = item.sortName4Nine.toUpperCase().indexOf(searchContent);
            if (disNameindex != -1||sortNameindex != -1||sortNameForNine!=-1) {
                if(disNameindex!=-1){
                    name =item.displayName.toUpperCase();
                }else if(sortNameindex!=-1){
                    name =item.sortName.toUpperCase();
                }else {
                    name =item.sortName4Nine.toUpperCase();
                }
                SpannableStringBuilder style = new SpannableStringBuilder(item.displayName);
                style.setSpan(
                        new ForegroundColorSpan(selectTextColor),
                        name.indexOf(searchContent),
                        name.indexOf(searchContent) + searchContent.length(),
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                holder.tvContactName.setText(style);
            }else{
                holder.tvContactName.setText(item.displayName);
            }
            int  numberNineindex =-1;
            String number = item.phone.getNumber();
            if(number==null){
                number="";
            }
            if(item.phone instanceof Contact.ContactDataSipAddress)
            {

                number =((Contact.ContactDataSipAddress) item.phone).mNumberFor9Path.toUpperCase();
                numberNineindex = number.indexOf(searchContent);
            }
            int  numberindex = item.phone.getNumber().toUpperCase().indexOf(searchContent);
            if (numberindex != -1||numberNineindex!=-1) {
                if(numberindex!=-1){
                    number = item.phone.getNumber().toUpperCase();
                }

                SpannableStringBuilder style = new SpannableStringBuilder(item.phone.getNumber());
                style.setSpan(
                        new ForegroundColorSpan(selectTextColor),
                        number.toUpperCase().indexOf(searchContent),
                        number.toUpperCase().indexOf(searchContent) + searchContent.length(),
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                holder.tvPhoneNumber.setText(style);
            }else{
                holder.tvPhoneNumber.setText(item.getPhoneNumber());
            }
        }else{
            holder.tvContactName.setText(item.displayName);
            holder.tvPhoneNumber.setText(item.getPhoneNumber());
        }
    }

    CharSequence mConstraint = null;
    public class ContactPhoneFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            mConstraint = constraint;
            List<Item> fiterItems = new ArrayList<>();

            try {
                if (constraint == null || constraint.length() == 0) {
                    searchContent = null;
                    fiterItems.clear();
                    fiterItems.addAll(mContactItemCopy);
                    results.values = fiterItems;
                    results.count = fiterItems.size();
                } else {

                    String upcasConstraint = constraint.toString().toUpperCase();
                    searchContent = upcasConstraint;
                    Iterator<Item> iterator = mContactItemCopy.iterator();
                    while (iterator.hasNext()) {
                        Item item = iterator.next();
                        String for9Path = null;
                        if (item.phone instanceof Contact.ContactDataSipAddress) {
                            for9Path = ((Contact.ContactDataSipAddress) item.phone).mNumberFor9Path;
                        }
                        if (NgnStringUtils.containsIgnoreUpCase(item.displayName,upcasConstraint) || NgnStringUtils.containsIgnoreUpCase(item.sortName,upcasConstraint)
                                || NgnStringUtils.containsIgnoreUpCase(item.sortName4Nine,upcasConstraint) || NgnStringUtils.containsIgnoreUpCase(item.phone.getNumber(),upcasConstraint) ||
                                NgnStringUtils.containsIgnoreUpCase(for9Path,upcasConstraint)) {
                            fiterItems.add(item);
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            results.values = fiterItems;
            results.count = fiterItems.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mContactItem.clear();
            if (results.count > 0) {
                mContactItem.addAll((ArrayList<Item>) results.values);
                superNotifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
    
   static public class Item {
        String displayName ="";
        String sortName="";
        Contact.ContactDataNumber phone;
        String sortName4Nine="";
       int contactid;

       public Item(int contactid,String displayName,String sortName,String sortName4Nine, Contact.ContactDataNumber phone){
           if(!TextUtils.isEmpty(displayName))
               this.displayName=displayName;
           if(!TextUtils.isEmpty(sortName))
               this.sortName=sortName;
           if(!TextUtils.isEmpty(sortName4Nine))
               this.sortName4Nine = sortName4Nine;

            this.phone=phone;
        }

        public String getPhoneNumber(){
            if(phone ==null||TextUtils.isEmpty(phone.getNumber()))
                return "";
            return phone.getNumber();
        }

       public String getDisplayName() {
           return displayName;
       }

       public int getContactid() {
           return contactid;
       }
   }
}
