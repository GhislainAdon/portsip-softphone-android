package com.portgo.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.UserAccount;
import com.portgo.view.ViewHolder;

public class AccountCursorAdapter extends CursorAdapter {
    Context mContext;
    private final LayoutInflater mInflater;
    private View.OnClickListener deleteClick,itemClick;
    public AccountCursorAdapter(Context context, Cursor c, int flag,View.OnClickListener deleteClick,View.OnClickListener itemClick){
        super(context, c, flag);
        mContext = context;
        mInflater= LayoutInflater.from(context);
        this.deleteClick = deleteClick;
        this.itemClick = itemClick;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        ViewHolder.LoginViewHolder holder = new ViewHolder.LoginViewHolder();
        View view = mInflater.inflate(R.layout.view_activity_login_user_item,null);
        view.findViewById(R.id.activity_login_item_user);
        holder.user_item = view.findViewById(R.id.activity_login_item_user);
        holder.user_name = (TextView) view.findViewById(R.id.activity_login_item_user_name);
        holder.user_del = (ImageView) view.findViewById(R.id.activity_login_item_user_del);
        view.setTag(holder);
        updateView(holder,cursor);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder.LoginViewHolder holder = (ViewHolder.LoginViewHolder) view.getTag();
        updateView(holder,cursor);
    }
    
    @Override
    public String convertToString(Cursor cursor) {
        UserAccount userAccount = UserAccount.userAccountFromCursor(cursor);
        return userAccount.getFullAccountReamName();
    }

    private void updateView(ViewHolder.LoginViewHolder holder ,Cursor cursor){
        UserAccount userAccount = UserAccount.userAccountFromCursor(cursor);
        holder.user_name.setText(userAccount.getFullAccountReamName());
        holder.user_del.setTag(userAccount);
        holder.user_del.setOnClickListener(deleteClick);
        holder.user_item.setOnClickListener(itemClick);
    }

}