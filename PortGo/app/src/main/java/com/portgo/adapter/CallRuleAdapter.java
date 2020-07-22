package com.portgo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.util.CallRule;
import com.portgo.view.DragListViewAdapter;
import com.portgo.view.RoundedImageView;
import com.portgo.view.ViewHolder;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huacai on 2017/5/24.
 */

public class CallRuleAdapter extends DragListViewAdapter<CallRule> {
    private final LayoutInflater mInflater;
    private final Context mContext;
    private View.OnClickListener mdeleteRuleListener = null;
    private List<Contact> mContacts ;
    HashMap<Integer,Boolean> selectItems = new HashMap();
    int avartaTextsize  = 16;

    public CallRuleAdapter(Context context, List<CallRule> rules,View.OnClickListener deleteRuleListener) {
        super(context,rules);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mdeleteRuleListener = deleteRuleListener;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getRuleId();
    }

    @Override
    public View getItemView(int position, View convertView, ViewGroup parent) {
        ViewHolder.CallRuleViewHolder holder = null;
        if(convertView==null){
            convertView = mInflater.inflate(R.layout.view_callrule_item,null);
            holder =new ViewHolder.CallRuleViewHolder();
            convertView.setTag(holder);
            holder.ruleDel = (ImageView) convertView.findViewById(R.id.callrule_item_del);
            holder.ruleMover = (ImageView) convertView.findViewById(R.id.callrule_item_mover);
            holder.ruleName = (TextView) convertView.findViewById(R.id.callrule_item_name);
            holder.ruleDel.setOnClickListener(mdeleteRuleListener);
        }else{
            holder = (ViewHolder.CallRuleViewHolder) convertView.getTag();
        }

        CallRule rule = mDragDatas.get(position);
        holder.ruleDel.setTag(rule.getRuleId());

        holder.ruleName.setText(rule.getName());
        return convertView;
    }
}