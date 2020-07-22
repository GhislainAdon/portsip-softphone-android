package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.ContactExpandAdapter;
import com.portgo.manager.Contact;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PortLoaderManager;
import com.portgo.view.AllExpandListView;
import com.portgo.view.ContactSideBar;
import com.portgo.view.HintSideBar;
import com.portgo.view.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivityMainContactSelectFragment extends PortBaseFragment implements ExpandableListView.OnChildClickListener,
        ContactSideBar.OnChooseLetterChangedListener,
        LoaderManager.LoaderCallbacks<Cursor>{
    private AllExpandListView mListView;
    final int CONTACT_SELECT_LOADER = 0x445;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,CONTACT_SELECT_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        return inflater.inflate(R.layout.activity_contact_select_fragment, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_contact_select_finish:
                getActivity().finishActivity(Activity.RESULT_CANCELED);
                break;
            case android.R.id.home:
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
                break;
        }
        return true;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        showToolBarAsActionBar(view,getString(R.string.contact_select_contact),true);

    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result) {
        if(mListView.getChoiceMode()==AbsListView.CHOICE_MODE_MULTIPLE)
        {
            return true;
        }
        if (mNeedRemoveFormList) {
            fragments.remove(mFragmentId);
        }
//		Fragment fragment = fragments.get(mFragmentId);
        manager.beginTransaction().remove(this).commit();
        PortBaseFragment backFragment = (PortBaseFragment) fragments.get(mBackFragmentId);
        if (mBackFragmentId != -1 && backFragment != null && mFragmentResId != -1) {
            showFramegment(getActivity(),manager, fragments, mFragmentResId, backFragment);
            return true;
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

    ContactExpandAdapter mAdapter = null;

    private void initView(View view) {
        mListView = (AllExpandListView) view.findViewById(R.id.contacts_listView);

        mAdapter = new ContactExpandAdapter(baseActivity,contacts);
        mAdapter.registerDataSetObserver( new DataSetObserver(){
            public void onChanged() {
                if(mListView!=null){
                    mListView.expandAllGroup(mAdapter);
                }
            }
        });
        mListView.setAdapter(mAdapter);
        mListView.setOnChildClickListener(this);
        HintSideBar quickSearch = (HintSideBar) view.findViewById(R.id.quick_searchbar);
        quickSearch.setOnChooseLetterChangedListener(this);
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
        ViewHolder.ContactViewHolder holder = (ViewHolder.ContactViewHolder) view.getTag();
        int contactid = (int) holder.contacts_item_radiobox.getTag();
        Intent data = new Intent();
        data.putExtra(PortActivityContactSelect.CONTACT_SELECT_REUSLT,contactid);
        getActivity().setResult(Activity.RESULT_OK,data);
        getActivity().finish();
        return true;
    }

    final String QUERYSTRING = "Constrain";
    @Override
    public boolean onQueryTextChange(String newText) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERYSTRING,newText);
        try {
            PortLoaderManager.restartLoader(baseActivity,loadMgr,CONTACT_SELECT_LOADER, bundle, this);
        }catch (IllegalStateException e){

        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return super.onQueryTextSubmit(query);
    }

    @Override
    public void onChooseLetter(String s) {
        int count = mAdapter.getGroupCount();
        for (int index =0;index<count;index++){
            String text = (String) mAdapter.getGroup(index);
            if(text.contains(s)){
//                long pakedposition = ExpandableListView.getPackedPositionForChild(index,0);
//                mListView.smoothScrollToPosition((int)pakedposition);
                mListView.setSelectedGroup(index);
                break;
            }
        }
    }

    @Override
    public void onNoChooseLetter() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String Sort_Key_Lable = "phonebook_label";

        String selection = null;
        String[] selectionArgs= null;
        if(bundle!=null){
            String queryString = bundle.getString(QUERYSTRING);
            if(!TextUtils.isEmpty(queryString)) {
                selection =  ContactsContract.Contacts.DISPLAY_NAME +" LIKE ?";
                selectionArgs = new String[]{"%" + queryString + "%"};
            }
        }
        if (android.os.Build.VERSION.SDK_INT < 19) {
            Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        }
        return new CursorLoader(getActivity(),ContactsContract.Contacts.CONTENT_URI, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                Sort_Key_Lable,
                ContactsContract.Contacts.SORT_KEY_ALTERNATIVE,
                ContactsContract.Contacts.PHOTO_ID,
                ContactsContract.Contacts.NAME_RAW_CONTACT_ID}, selection, selectionArgs,
                Sort_Key_Lable + " ASC");
    }

    List<Contact> contacts = new ArrayList<>();
    HashMap<Integer,Contact> contactsFinder = new LinkedHashMap<>();
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        List<Contact> contactsCopy= new ArrayList<>();

        String displayName = null;
        String johnDoe = getString(R.string.activity_main_contact_no_name);
        while (CursorHelper.moveCursorToNext(cursor)) {
            int id = cursor.getInt(0);
            displayName = cursor.getString(1);
            String sortKey = cursor.getString(2);
            String sortKeyAlt = cursor.getString(3);
            int photoId = cursor.getInt(4);
            int rawContactid = cursor.getInt(5);
            if(TextUtils.isEmpty(displayName)){
                displayName = johnDoe;
            }
            Contact contact = new Contact(id, displayName);
            if(photoId>0){
                contact.setAvatarId(photoId);
            }
            contact.setSortKey(sortKey);
            contactsCopy.add(contact);
        }

        String selection = ContactsContract.Data.MIMETYPE +" =? ";
        Cursor cursor1 = CursorHelper.resolverQuery(getActivity().getContentResolver(),ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.DATA1, ContactsContract.Data.DATA2,
                        ContactsContract.Data.DATA3, ContactsContract.Data.RAW_CONTACT_ID},
                selection,
                new String[]{ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE}, null);

        while (CursorHelper.moveCursorToNext(cursor1)) {
            int sipPhoneId = cursor1.getInt(cursor1.getColumnIndex(ContactsContract.Data._ID));
            String sipPhoneNumber = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Data.DATA1));
            int phoneType = cursor1.getInt(cursor1.getColumnIndex(ContactsContract.Data.DATA2));
            int rawContactid = cursor1.getInt(cursor1.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
            String lable = null;
            if (phoneType == ContactsContract.CommonDataKinds.SipAddress.TYPE_CUSTOM) {
                try {
                    lable = cursor.getString(cursor1.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL));
                }catch (Exception e){
                    lable ="";
                }

            }

            Contact contact = contactsFinder.get(rawContactid);
            if(contact!=null) {
                contact.addSipAddress(new Contact.ContactDataSipAddress(sipPhoneId, sipPhoneNumber, phoneType, lable));
            }else{
            }
        }
        CursorHelper.closeCursor(cursor1);
        Cursor cursor2 = CursorHelper.resolverQuery(getActivity().getContentResolver(),ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID}, null, null, null);
        while (CursorHelper.moveCursorToNext(cursor2)) {
            int phoneID = cursor2.getInt(0);
            int phoneType = cursor2.getInt(1);
            String  phoneNumber = cursor2.getString(2);
            String  lable = cursor2.getString(3);
            int rawContactid = cursor2.getInt(4);

            Contact contact = contactsFinder.get(rawContactid);
            if(contact!=null) {
                contact.addPhone(new Contact.ContactDataPhone(phoneID, phoneNumber, phoneType, lable));
            }else{

            }
        }
        CursorHelper.closeCursor(cursor2);
        contactsFinder.clear();


        synchronized (contacts) {
            contacts.clear();
            contacts.addAll(contactsCopy);
        }


        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        contacts.clear();
    }

}
