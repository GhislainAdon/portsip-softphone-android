package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
//import android.database.CursorJoiner;
import com.portgo.BuildConfig;
import com.portgo.database.JoinIntegerIdCompare;
import com.portgo.database.PortCursorJoiner;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.portgo.R;
import com.portgo.adapter.FriendAdapter;
import com.portgo.manager.Contact;
import com.portgo.database.CursorJoiner4Loader;
import com.portgo.database.CursorJoinerLoader;
import com.portgo.manager.PortLoaderManager;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static android.provider.BaseColumns._ID;
import static com.portgo.androidcontacts.ContactsContract.CommonDataKinds.Im.PORTSIP_IM_PROTOCAL;

public class ActivityMainFriendsFragment extends PortBaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,Toolbar.OnMenuItemClickListener, Observer ,LoaderManager.LoaderCallbacks<CursorJoiner4Loader>{
    private ListView mListView;
    final int FRIEND_LOADER = 4835;

    List mFriends = new ArrayList<Contact>();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PortLoaderManager.initLoader(baseActivity,loadMgr,FRIEND_LOADER,null,this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);


        return inflater.inflate(R.layout.activity_main_friend_fragment, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        baseActivity.mContactMgr.getObservableContacts().addObserver(this);

        Toolbar toolbar = (Toolbar) getParentFragment().getView().findViewById(R.id.toolBar);
        toolbar.setOnMenuItemClickListener(this);
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_select:
                if (mAdapter.isAllSelect()) {
                    mAdapter.clearSelect();
                } else {
                    mAdapter.setSelectAll();
                }
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_contact_add:
                Intent intent = new Intent();
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,-1);
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_FRIEND,true);
                intent.setClass(baseActivity,PortActivityContactEdit.class);
                baseActivity.startActivity(intent);
                break;
        }
        updateMenu();
        return true;
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result) {
        if(mListView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE)
        {
            exitSelectStatus();
            return true;
        }

        if (mNeedRemoveFormList) {
            fragments.remove(mFragmentId);
        }

        return false;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    FriendAdapter mAdapter = null;
    List<Contact> friends = new ArrayList<>();
    private void initView(View view) {
        mListView = (ListView) view.findViewById(R.id.friend_listView);

        mAdapter = new FriendAdapter(baseActivity,friends);
        mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

    }

    @Override
    public void onClick(View view) {
        PortBaseFragment baseFragment ;
        switch (view.getId()){
            case  R.id.bottombar_right:
            {
                List<Integer> selectContact = mAdapter.getSelectContact();

                if(selectContact.size()>0) {
                    baseActivity.mContactMgr.deleteContacts(getActivity(),
                            ContactsContract.AUTHORITY, selectContact,
                            ContactsContract.Contacts.CONTENT_URI,
                            ContactsContract.RawContacts.CONTENT_URI);
                }

                exitSelectStatus();
            }
                break;
            case  R.id.bottombar_left:
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroyView() {
        baseActivity.mContactMgr.getObservableContacts().deleteObserver(this);
        super.onDestroyView();
    }

    void updateMenu(){
        View view = getParentFragment().getView();
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        int size  = toolbar.getMenu().size();
        for(int index=0;index<size;index++){
            updateItem(toolbar.getMenu().getItem(index));
        }
        if(mListView.getChoiceMode()!=AbsListView.CHOICE_MODE_MULTIPLE)
            return;
        int selectSize= mAdapter.getSelectContact().size();
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);

        if(mAdapter.getSelectContact().size()>0){
            toolbar.setTitle(String.format(getString(R.string.contact_selected_contact),selectSize));
            if(bottomView!=null) {
                bottomView.findViewById(R.id.bottombar_right).setEnabled(true);
            }
        }else{
            if(bottomView!=null) {
                bottomView.findViewById(R.id.bottombar_right).setEnabled(false);
            }
            toolbar.setTitle(R.string.contact_select_contact);
        }
        if(mAdapter.getSelectContact().size()==1) {
            if (bottomView != null){
                bottomView.findViewById(R.id.bottombar_left).setEnabled(true);
            }
        }else {
            if(bottomView!=null) {
                bottomView.findViewById(R.id.bottombar_left).setEnabled(false);
            }
        }
    }

    private  void updateItem(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_contact_select:
                if(mAdapter.isAllSelect()){
                    item.setTitle(getString(R.string.clear_all));
                }else {
                    item.setTitle(getString(R.string.select_all));
                }
                break;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ViewHolder.FriendViewHolder holder = (ViewHolder.FriendViewHolder) view.getTag();
        int contactid = (int) holder.friend_item_radiobox.getTag();

        if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setContactCheck(contactid,!mAdapter.isContactChecked(contactid));
            holder.friend_item_radiobox.setChecked(mAdapter.isContactChecked(contactid));
            updateMenu();
        }else {
            Intent intent = new Intent();
            intent.putExtra(PortActivityContactDetail.CONTACT_DETAIL_ID, contactid);
            intent.setClass(baseActivity, PortActivityContactDetail.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            baseActivity.startActivity(intent);
        }
        return;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        enterSelectStatus();
        ViewHolder.FriendViewHolder holder = (ViewHolder.FriendViewHolder) view.getTag();
        int contactid = (int) holder.friend_item_radiobox.getTag();

        if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setContactCheck(contactid, true);
        }
        return true;
    }

    private void enterSelectStatus(){
        View  bottomView = LayoutInflater.from(baseActivity).inflate(R.layout.view_bottombar,null);
        bottomView.findViewById(R.id.bottombar_right).setOnClickListener(this);
        bottomView.findViewById(R.id.bottombar_left).setVisibility(View.INVISIBLE);
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.INVISIBLE);
        bottomBar.addView(bottomView);
        Toolbar toolbar = (Toolbar) getParentFragment().getView().findViewById(R.id.toolBar);

        if(toolbar!=null) {
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    exitSelectStatus();
                }
            });
            toolbar.setOnMenuItemClickListener(this);
            toolbar.getMenu().setGroupVisible(R.id.group_normal,false);
            toolbar.getMenu().setGroupVisible(R.id.group_select,true);
            toolbar.invalidate();
            toolbar.setTitle(getString(R.string.contact_select_contact));
        }

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        if(mAdapter!=null)
            mAdapter.notifyDataSetChanged();
        TabLayout tabLayout = (TabLayout) getParentFragment().getView().findViewById(R.id.tabLayout);
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tabView = tabStrip.getChildAt(i);
            if (tabView != null) {
                tabView.setClickable(false);
            }
        }
    }

    private void exitSelectStatus(){
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        View bottomView = bottomBar.findViewById(R.id.bottom_toolbar);
        if(bottomView!=null){
            bottomBar.removeView(bottomView);
        }
        bottomBar.findViewById(R.id.activity_main_tabs).setVisibility(View.VISIBLE);
        Toolbar toolbar = (Toolbar) getParentFragment().getView().findViewById(R.id.toolBar);

        if(toolbar!=null) {
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
            toolbar.getMenu().setGroupVisible(R.id.group_normal,true);
            toolbar.getMenu().setGroupVisible(R.id.group_select,false);
            toolbar.invalidate();
            toolbar.setTitle(R.string.portgo_title_contact);
        }

        TabLayout tabLayout = (TabLayout) getParentFragment().getView().findViewById(R.id.tabLayout);
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tabView = tabStrip.getChildAt(i);
            if (tabView != null) {
                tabView.setClickable(true);
            }
        }

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        if(mAdapter!=null) {
            mAdapter.clearSelect();
            mAdapter.notifyDataSetChanged();
        }
    }

    private  String mSearch = "";
    @Override
    public boolean onQueryTextChange(String newText) {
        Bundle bundle = new Bundle();
        mSearch = newText;
        bundle.putString(QUERYSTRING,newText);

        PortLoaderManager.restartLoader(baseActivity,loadMgr,FRIEND_LOADER,bundle,this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return super.onQueryTextSubmit(query);
    }

    final String QUERYSTRING = "Constrain";

    @Override
    public void update(Observable observable, Object o) {

        Bundle bundle = new Bundle();
        bundle.putString(QUERYSTRING,mSearch);

        PortLoaderManager.restartLoader(baseActivity,loadMgr,FRIEND_LOADER,bundle,this);
    }



        @Override
        public Loader<CursorJoiner4Loader> onCreateLoader(int i, Bundle bundle) {
            Uri uriLeft,uriRight;
            String selectionLeft,selectionRight,sortLeft,sortRight;
            String[] projectionLeft,projectionRight,selectionArgLeft,selectionArgRight,joinLeft,joinRight;
            uriLeft = ContactsContract.StatusUpdates.CONTENT_URI;
            uriRight= ContactsContract.Data.CONTENT_URI;

            projectionLeft = null;
            selectionLeft =null;
            selectionArgLeft =null;

            sortLeft =ContactsContract.StatusUpdates.DATA_ID+" ASC";
            projectionRight =new String[]{_ID,ContactsContract.Data.CONTACT_ID,ContactsContract.Data.DATA1};
            selectionRight = ContactsContract.Data.MIMETYPE + "=?" + " AND "
                    + ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL+"=?"+" AND "
                    + ContactsContract.CommonDataKinds.Im.PROTOCOL+"=?";
            selectionArgRight = new String[]{ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,PORTSIP_IM_PROTOCAL,""+ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM};
//            sortRight =null;
            sortRight =ContactsContract.Data._ID+" ASC";
            joinLeft = new String[]{ContactsContract.StatusUpdates.DATA_ID};
            joinRight = new String[]{ContactsContract.Data._ID};

            return new CursorJoinerLoader(baseActivity,uriLeft,
                    projectionLeft,selectionLeft,selectionArgLeft, sortLeft, uriRight,
                    projectionRight, selectionRight,selectionArgRight,sortRight,joinLeft,
                    joinRight,new JoinIntegerIdCompare());
        }

        int presenceSize = 0;
        @Override
        public void onLoadFinished(Loader<CursorJoiner4Loader> loader, CursorJoiner4Loader joiner4Loader) {
            PortCursorJoiner joiner = joiner4Loader.getJoiner();
            Cursor left = joiner4Loader.getCursorLeft();
            Cursor right = joiner4Loader.getCursorRight();
            String Sort_Key_Lable = "phonebook_label";
            if (android.os.Build.VERSION.SDK_INT < 19) {
                Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
            }
            List<Contact> friendsCopy = new ArrayList<>();

            while (joiner.hasNext()){
                PortCursorJoiner.Result joinerResult = joiner.next();
                int contactId = 0;
                Contact contact = null;
                switch (joinerResult) {
                    case LEFT:
                        break;
                    case RIGHT:
                        contactId = left.getInt(left.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                        contact = baseActivity.mContactMgr.getObservableContacts().get(contactId);
                        if(contact!=null&&(TextUtils.isEmpty(mSearch)||
                                ((contact.getDisplayName()!=null)&&contact.getDisplayName().contains(mSearch)))) {
                            friendsCopy.add(contact);
                        }
                        break;
                    case BOTH:
                        String johnDoe = getString(R.string.activity_main_contact_no_name);
                        int dataid = left.getInt(left.getColumnIndex(ContactsContract.StatusUpdates.DATA_ID));

                        String status = left.getString(left.getColumnIndex(ContactsContract.StatusUpdates.STATUS));
                        int presesece = left.getInt(left.getColumnIndex(ContactsContract.StatusUpdates.PRESENCE));
                        int lable = left.getInt(left.getColumnIndex(ContactsContract.StatusUpdates.STATUS_LABEL));
                        int icon = left.getInt(left.getColumnIndex(ContactsContract.StatusUpdates.STATUS_ICON));

                        String data1 = right.getString(right.getColumnIndex(ContactsContract.Data.DATA1));
                        contactId = right.getInt(right.getColumnIndex(ContactsContract.Data.CONTACT_ID));

                        if(TextUtils.isEmpty(data1)){
                            String displayName = data1;
                        }

                        contact = baseActivity.mContactMgr.getObservableContacts().get(contactId);
                        if(contact!=null&&(TextUtils.isEmpty(mSearch)||
                                ((contact.getDisplayName()!=null)&&contact.getDisplayName().contains(mSearch)))) {
                            contact.setPresence_mode(presesece);
                            contact.setPresence_resicon(icon);
                            contact.setPresence_resLable(lable);
                            contact.setPresence_status(status);
                            friendsCopy.add(contact);
                        }
                        break;
                }
            }

            Collections.sort(friendsCopy, new Comparator<Contact>() {
                @Override
                public int compare(Contact o1, Contact o2) {
                    if (o1 != null && o2 != null) {
                        return o2.getPresence_mode()-o1.getPresence_mode();
                    }
                    return 1;
                }
            });

            friends.clear();
            friends.addAll(friendsCopy);
            mAdapter.notifyDataSetChanged();

        }

        @Override
        public void onLoaderReset(Loader<CursorJoiner4Loader> loader) {
            friends.clear();
        }

}
