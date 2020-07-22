package com.portgo.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.ContactExpandAdapter;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PortLoaderManager;
import com.portgo.view.AllExpandListView;
import com.portgo.view.ContactSideBar;
import com.portgo.view.ViewHolder.ContactViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.xml.validation.Validator;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.HASIM;

public class ActivityMainFavoriteFragment extends PortBaseFragment implements View.OnClickListener
        ,ExpandableListView.OnChildClickListener, AdapterView.OnItemLongClickListener,
        Toolbar.OnMenuItemClickListener ,ContactSideBar.OnChooseLetterChangedListener,LoaderManager.LoaderCallbacks<Cursor>, Observer {
    private AllExpandListView mListView;

    ImageView view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.activity_main_favorite_fragment, null);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
        baseActivity.mContactMgr.getObservableContacts().deleteObserver(this);
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        Toolbar toolbar = (Toolbar) getParentFragment().getView().findViewById(R.id.toolBar);
        toolbar.setOnMenuItemClickListener(this);
        baseActivity.mContactMgr.getObservableContacts().addObserver(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);

        updateMenu();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                exitSelectStatus();
                break;
            case R.id.menu_contact_select:
                if (mAdapter != null && mAdapter.isAllSelect()) {
                    mAdapter.clearSelect();
                } else {
                    mAdapter.setSelectAll();
                }
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_contact_add:
                Intent intent = new Intent();
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,-1);
                intent.setClass(baseActivity,PortActivityContactEdit.class);
                baseActivity.startActivity(intent);
                break;
        }
        updateMenu();
        return true;
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments, Bundle result) {
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

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,FAVORITE_LOADER,null,this);
    }

    ContactExpandAdapter mAdapter = null;
    final int FAVORITE_LOADER = 0x742;
    private void initView(View view) {
        mListView = (AllExpandListView) view.findViewById(R.id.favorites_listView);
        mAdapter = new ContactExpandAdapter(baseActivity,favorites);
        mListView.setAdapter(mAdapter);
        mAdapter.registerDataSetObserver( new DataSetObserver(){
            public void onChanged() {
                if(mListView!=null){
                    mListView.expandAllGroup(mAdapter);
                }
            }
        });
        mListView.setOnChildClickListener(this);
        mListView.setOnItemLongClickListener(this);


    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case  R.id.bottombar_right:
                List<Integer> selectContact = mAdapter.getSelectContact();
                if(selectContact.size()>0) {
                    ContactManager.starContacts(baseActivity,ContactsContract.AUTHORITY,
                            ContactsContract.Contacts.CONTENT_URI, selectContact, false);
                }
                exitSelectStatus();
                break;
            case  R.id.bottombar_left:
                {
                    List<Integer> selectContacts = mAdapter.getSelectContact();
                    for (Integer contactid : selectContacts) {
                        Contact contact = ContactManager.getInstance().getObservableContacts().get(contactid);
                        if(contact!=null) {
                            List numbers = contact.getContactNumbers();
                            if (numbers != null) {
                                String[] strNumbers = new String[numbers.size()];
                                int index = 0;
                                for (Object obj : numbers) {
                                    if (obj != null && obj instanceof Contact.ContactDataNumber) {
                                        strNumbers[index++] = ((Contact.ContactDataNumber) obj).getNumber();
                                    }
                                }
                                if (strNumbers.length == 1) {
                                    String remote = strNumbers[0];
                                    PortActivityRecycleChat.beginChat(getActivity(), remote, contact.getId(), contact.getDisplayName());
                                } else if (numbers.size() > 1) {
                                    if (strNumbers.length > 1) {
                                        showNumberSelectDialog(strNumbers, contactid);
                                    }
                                }
                                exitSelectStatus();
                                break;
                            }
                        }
                    }
                    exitSelectStatus();
                }
                break;
            default:
                break;
        }
    }

    private void updateMenu(){
        View view = getParentFragment().getView();
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        int size  = toolbar.getMenu().size();
        for(int index=0;index<size;index++){
            updateMenuItem(toolbar.getMenu().getItem(index));
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

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
        ContactViewHolder holder = (ContactViewHolder) view.getTag();
        int contactid = (int) holder.contacts_item_radiobox.getTag();

        if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
            mAdapter.setContactCheck(view,contactid,!mAdapter.isContactChecked(contactid));
            updateMenu();
        }else {
            Intent intent = new Intent();
            intent.putExtra(PortActivityContactDetail.CONTACT_DETAIL_ID, contactid);
            intent.setClass(baseActivity, PortActivityContactDetail.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            baseActivity.startActivity(intent);
        }
        return true;
    }

    void updateMenuItem(MenuItem item){
        if(item==null)
            return;
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
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        long packedPosition = mListView.getExpandableListPosition(position);
        int itemType        = ExpandableListView.getPackedPositionType(packedPosition);
        int groupPosition   = ExpandableListView.getPackedPositionGroup(packedPosition);
        int childPosition   = ExpandableListView.getPackedPositionChild(packedPosition);

        if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
        }else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            enterSelectStatus();
            if(mListView.getChoiceMode()== AbsListView.CHOICE_MODE_MULTIPLE) {
                Contact contact = (Contact) mAdapter.getChild(groupPosition,childPosition);
                mAdapter.setContactCheck(view,contact.getId(), true);
            }
            return true;
        }
        return false;
    }


    private void enterSelectStatus(){
        View  bottomView = LayoutInflater.from(baseActivity).inflate(R.layout.view_bottombar,null);
        FrameLayout bottomBar = (FrameLayout) baseActivity.findViewById(R.id.activity_main_bottombar);
        bottomView.findViewById(R.id.bottombar_right).setOnClickListener(this);
        bottomView.findViewById(R.id.bottombar_left).setOnClickListener(this);
        if(!HASIM) {
            bottomView.findViewById(R.id.bottombar_left).setVisibility(View.INVISIBLE);
        }
        bottomView.findViewById(R.id.bottombar_left).setEnabled(ENABLEIM);
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
        if(mAdapter!=null){
            mAdapter.clearSelect();
            mAdapter.notifyDataSetChanged();
        }
    }

    final String QUERYSTRING = "CONSTRAIN";
    private  String mSearch = "";
    @Override
    synchronized public boolean onQueryTextChange(String newText) {
        Bundle bundle = new Bundle();
        mSearch = newText;
        bundle.putString(QUERYSTRING,newText);
        PortLoaderManager.restartLoader(baseActivity,loadMgr,FAVORITE_LOADER,bundle,this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return super.onQueryTextSubmit(query);
    }

    @Override
    public void onChooseLetter(String s) {
        int count = mAdapter.getGroupCount();
        if(s!=null){
            s = s.toUpperCase();
        }
        for (int index =0;index<count;index++){
            String text = (String) mAdapter.getGroup(index);
            if(text!=null&&text.toUpperCase().contains(s)){
                long pakedposition = ExpandableListView.getPackedPositionForChild(index,0);
                mListView.smoothScrollToPosition((int)pakedposition);
                break;
            }
        }
    }

    @Override
    public void onNoChooseLetter() {

    }
    int chatContactId = 0;
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i){
            case FAVORITE_LOADER:
                String selection = ContactsContract.Contacts.STARRED + " =  1 ";
                String[] selectionArgs= null;
                if(bundle!=null){
                    String queryString = bundle.getString(QUERYSTRING);
                    if(!TextUtils.isEmpty(queryString)) {
                        selection = selection + " AND "+ContactsContract.Contacts.DISPLAY_NAME
                                +" LIKE ?";
                        selectionArgs = new String[]{"%" + queryString + "%"};
                    }
                }

                String Sort_Key_Lable = "phonebook_label";
                if (android.os.Build.VERSION.SDK_INT < 19) {
                    Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
                }
                return new CursorLoader(getActivity(),ContactsContract.Contacts.CONTENT_URI,
                        new String[]{
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts.PHOTO_ID,
                                Sort_Key_Lable},
                        selection,selectionArgs,Sort_Key_Lable + " ASC");
        }
        return null;
    }
    List<Contact> favorites = new ArrayList<>();
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId())
        {
            case FAVORITE_LOADER:
                List<Contact> contactsCopy= new ArrayList<>();
                while (CursorHelper.moveCursorToNext(cursor)) {
                    int id = cursor.getInt(0);
                    Contact contact = baseActivity.mContactMgr.getObservableContacts().get(id);
                    if(contact!=null) {
                        contactsCopy.add(contact);
                    }
                }
                favorites.clear();
                favorites.addAll(contactsCopy);

                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        favorites.clear();
    }

    Dialog numberSelectDialog = null;
    private void showNumberSelectDialog(final String[] numbers,int contactId){
        chatContactId = contactId;
        if(numberSelectDialog!=null&&numberSelectDialog.isShowing()){
            numberSelectDialog.dismiss();
        }

        numberSelectDialog = new AlertDialog.Builder(getActivity()).setItems(numbers,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String displayname = "";
                        Contact contact = baseActivity.mContactMgr.getObservableContacts().get(chatContactId);
                        if(contact!=null) {
                            displayname = contact.getDisplayName();
                        }
                        PortActivityRecycleChat.beginChat(getActivity(),numbers[which],chatContactId,displayname);
                    }
                }).show();
    }

    @Override
    public void update(Observable observable, Object o) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERYSTRING,mSearch);

        PortLoaderManager.restartLoader(baseActivity,loadMgr,FAVORITE_LOADER,bundle,ActivityMainFavoriteFragment.this);

    }
}
