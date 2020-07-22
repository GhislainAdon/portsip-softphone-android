package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
//import android.database.CursorJoiner;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.portgo.R;
import com.portgo.adapter.SubscribeAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.database.UriMactherHepler;
import com.portgo.manager.AccountManager;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;

import com.portgo.manager.CursorHelper;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PresenseMessage;
import com.portgo.manager.UserAccount;
import com.portgo.view.SlideMenuListView;
import com.portsip.PortSipErrorcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.portgo.manager.Contact.INVALIDE_ID;
import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;

public class ActivitySubscribeCursorFragment extends PortBaseFragment implements SubscribeAdapter.OnSubscribeActionClick,
        LoaderManager.LoaderCallbacks<Cursor>,AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener, View.OnLongClickListener {
    private SlideMenuListView mListView;
    private SubscribeAdapter mAdapter;
    private Toolbar toolbar;
    View bottomBar = null;

    private final int LOADER_PRESENCE = 0x24;
    HashMap<Long,PresenseMessage> allMessages = new HashMap();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,LOADER_PRESENCE, null, this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.activity_main_subscribe_fragment, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bottomBar  =view.findViewById(R.id.bottom_toolbar);
        bottomBar.findViewById(R.id.bottombar_left).setVisibility(View.GONE);
        bottomBar.findViewById(R.id.bottombar_right).setOnClickListener(this);
        initView(view);
        toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        toolbar.inflateMenu(R.menu.menu_subscribe);
        toolbar.setOnMenuItemClickListener(this);
        showToolBar(view,getString(R.string.subscribe_title));
    }
    View tips = null;
    void initView(View view) {
        mAdapter = new SubscribeAdapter(baseActivity, allMessages, this);
        mListView = (SlideMenuListView) view.findViewById(R.id.subscribe_listView);
        tips =  view.findViewById(R.id.subscrib_no_friend);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_subscribe_select:
                if(isAllSelect()){
                    setAllCheck(false);
                    item.setTitle(R.string.select_all);
                }else{
                    setAllCheck(true);
                    item.setTitle(R.string.clear_all);
                }
                mAdapter.notifyDataSetChanged();
                break;
            case android.R.id.home:
                getActivity().finish();
                break;
        }
        return true;
    }

    private boolean isAllSelect(){
        Iterator iterator = allMessages.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Long,PresenseMessage> entry = (Map.Entry<Long, PresenseMessage>) iterator.next();
            PresenseMessage message = entry.getValue();
            if(!message.isChecked()){
                return false;
            }
        }

        return true;
    }

    private void setAllCheck(boolean check){
        Iterator iterator = allMessages.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Long,PresenseMessage> entry = (Map.Entry<Long, PresenseMessage>) iterator.next();
            PresenseMessage message = entry.getValue();
            message.setChecked(check);
        }
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer, Fragment> fragments, Bundle result) {
        if(mListView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE){
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            bottomBar.setVisibility(View.GONE);
            toolbar.getMenu().findItem(R.id.menu_subscribe_select).setVisible(false);
            setAllCheck(false);
            return true;
        }else {
            return super.onKeyBackPressed(manager, fragments, result);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    //本地联系人，用来存储订阅信息。
    // 1：在订阅组（已接受订阅）
    // 2：在删除组（已经被拒绝）
    //3：不再任何组（未处理）
    @Override
    public void onClick(long id, SubscribeAdapter.ACTION_CODE code) {
        ContentResolver resolver = baseActivity.getContentResolver();
        PresenseMessage message = allMessages.get(id);
        int presenceId = message.getPresenceId();
        String from = message.getRemoteParty();
        String displayName = message.getDisplayName();

        Uri uri = ContentUris.withAppendedId(DBHelperBase.SubscribeColumns.CONTENT_URI,presenceId);
        ContentValues values = new ContentValues(1);
        if (code == SubscribeAdapter.ACTION_CODE.REJECT) {//拒绝，移除订阅组，但是并不删除。下次订阅不会在提示
            Long subID = baseActivity.mCallMgr.getSubscribId(presenceId+5000);
            if (subID != null) {
                baseActivity.mSipMgr.presenceRejectSubscribe(subID);
                baseActivity.mCallMgr.removeSubScribeIdByContactid(presenceId);
            }
            if (presenceId > 0) {
                values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION, DBHelperBase.SubscribeColumns.ACTION_REJECTED);
                resolver.update(uri,values,null,null);
            }

        } else {//接受订阅，从本地联系人删除，添加到系统联系人。

            //插入一个名字字段
            Contact contact = new Contact(Contact.INVALIDE_ID,displayName);
            Contact.ContactDataStructuredName structuredName  = new Contact.ContactDataStructuredName(INVALIDE_ID,null,0,null);
            structuredName.givenName =displayName;
            contact.addStructName(structuredName);
            //插入一个sip地址
            Contact.ContactDataSipAddress sipAddress = new Contact.ContactDataSipAddress(Contact.INVALIDE_ID,from,
                    android.provider.ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME,null);
            contact.addSipAddress(sipAddress);
            //插入一个im地址
            Contact.ContactDataIm contactIm = new Contact.ContactDataIm(Contact.INVALIDE_ID,from, android.provider.ContactsContract.CommonDataKinds.Im.TYPE_HOME,null);
            contactIm.setProtocaltype(android.provider.ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
            contactIm.setProtocal(PORTSIP_IM_PROTOCAL);
            contact.addIm(contactIm);

            int rawContactid = ContactManager.insertContact(baseActivity, contact, null);//将联系人加入到系统联系人

//            Intent intent = new Intent();
//            intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,-1);
//            intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_NAME, displayName);
//            intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_PHONE,sipAddress);
//            intent.setClass(baseActivity,PortActivityContactEdit.class);
//            baseActivity.startActivity(intent);
            if (presenceId > 0) {
                values.put(DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION, DBHelperBase.SubscribeColumns.ACTION_ACCEPTED);
                resolver.update(uri,values,null,null);
            }
            Long subID = baseActivity.mCallMgr.getSubscribId(presenceId+5000);

            if (subID != null) {
                //更新关联的rawContactid
                baseActivity.mCallMgr.removeSubScribeIdByContactid(presenceId);
                baseActivity.mCallMgr.putsubScribeId(subID, rawContactid);

                int result = baseActivity.mSipMgr.presenceAcceptSubscribe(subID);//接受对方订阅
                if (PortSipErrorcode.ECoreErrorNone == result) {


                    Contact.ContactDataIm portIm = contact.getContactPortImAddress();//多个号码怎么办，什么格式号码，应该订阅
                    if (portIm != null) {

                        UserAccount defAccount = AccountManager.getDefaultUser(baseActivity);
                        int resLable = R.string.cmd_presence_offline;
                        if (defAccount != null) {
                            resLable = defAccount.getPresenceCommandRes();
                        }
                        String handle = portIm.getImAccount();
                        if(handle!=null) {
                            String[] array = handle.split("@");
                            if (array.length > 0) {
                                baseActivity.mSipMgr.presenceSubscribe(array[0],getString(resLable));//订阅对方
                            }
                        }

                        baseActivity.mSipMgr.setPresenceStatus(subID, getString(resLable));//将自己的状态推送给对方
                    }
                }

            }
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOADER_PRESENCE:
                return new CursorLoader(getActivity(), DBHelperBase.SubscribeColumns.CONTENT_URI,
                        null, null, null
                        , DBHelperBase.SubscribeColumns.DEFAULT_ORDER);// DBHelperBase.SubscribeColumns.DEFAULT_ORDER);
            default:
                break;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        String local = AccountManager.getDefaultUser(baseActivity).getFullAccountReamName();
        HashMap<Long,PresenseMessage> synMessages =new HashMap<>();

        while (CursorHelper.moveCursorToNext(cursor)){
            PresenseMessage message = getPresenceMessage(cursor,local);
            PresenseMessage checkableMessage = allMessages.get((long) message.getPresenceId());
            if(checkableMessage!=null){//在选择过程中，数据改变，需要同步选择状态。
                message.setChecked(checkableMessage.isChecked());
            }
            synMessages.put((long) message.getPresenceId(),message);
        }

        allMessages.clear();
        allMessages.putAll(synMessages);
        synMessages.clear();
        if(allMessages.size()>0){
            tips.setVisibility(View.GONE);
        }else{
            tips.setVisibility(View.VISIBLE);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private PresenseMessage getPresenceMessage(Cursor cursor, String local) {
        String displayName = cursor.getString(cursor.getColumnIndex(DBHelperBase.SubscribeColumns.SUBSCRIB_NAME));
        String remote = cursor.getString(cursor.getColumnIndex(DBHelperBase.SubscribeColumns.SUBSCRIB_REMOTE));
        String status = cursor.getString(cursor.getColumnIndex(DBHelperBase.SubscribeColumns.SUBSCRIB_DESC));
        int action = cursor.getInt(cursor.getColumnIndex(DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION));
        long time = cursor.getLong(cursor.getColumnIndex(DBHelperBase.SubscribeColumns.SUBSCRIB_TIME));
        int id = cursor.getInt(cursor.getColumnIndex(DBHelperBase.SubscribeColumns._ID));

        PresenseMessage message = new PresenseMessage(local, remote, displayName, status, time, id);
        message.setAction(action);
        return message;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(mListView.getChoiceMode()== ListView.CHOICE_MODE_MULTIPLE){
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
            bottomBar.setVisibility(View.INVISIBLE);
            toolbar.getMenu().findItem(R.id.menu_subscribe_select).setVisible(false);
            setAllCheck(false);
        }else{
            toolbar.getMenu().findItem(R.id.menu_subscribe_select).setVisible(true);
            bottomBar.setVisibility(View.VISIBLE);
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }
        mAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        PresenseMessage message = (PresenseMessage) mAdapter.getItem(i);
        if(mListView.getChoiceMode()==ListView.CHOICE_MODE_MULTIPLE){
            message.toggle();
            MenuItem item = toolbar.getMenu().findItem(R.id.menu_subscribe_select);
            if(isAllSelect()){
                item.setTitle(R.string.clear_all);
            }else{
                item.setTitle(R.string.select_all);
            }
        }else{

        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bottombar_right:
                Iterator iterator = allMessages.entrySet().iterator();
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                while (iterator.hasNext()){
                    Map.Entry<Long,PresenseMessage> entry = (Map.Entry<Long, PresenseMessage>) iterator.next();
                    PresenseMessage message = entry.getValue();
                    if(message.isChecked()){
                        ContentProviderOperation op = ContentProviderOperation.newDelete(Uri.withAppendedPath(DBHelperBase.SubscribeColumns.CONTENT_URI,
                                "" + message.getPresenceId())).build();
                        ops.add(op);
                    }
                }
                toolbar.getMenu().findItem(R.id.menu_subscribe_select).setVisible(false);
                bottomBar.setVisibility(View.GONE);
                mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                if(ops.size()>0) {
                    try {
                        getActivity().getContentResolver().applyBatch(UriMactherHepler.AUTHORITY, ops);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    }
                }

        }
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }
}
