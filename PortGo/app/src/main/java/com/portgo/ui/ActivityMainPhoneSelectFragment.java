package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;

import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
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
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.ContactPhoneSelectAdapter;
import com.portgo.manager.Contact;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PortLoaderManager;
import com.portgo.view.ContactSideBar;
import com.portgo.view.HintSideBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivityMainPhoneSelectFragment extends PortBaseFragment implements ExpandableListView.OnChildClickListener
        , ContactSideBar.OnChooseLetterChangedListener, LoaderManager.LoaderCallbacks<Cursor> {
    private ExpandableListView mListView;
    final int CONTACT_LOADER = 0x98;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.activity_main_phoneselect_fragment, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_phone_select, menu);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,CONTACT_LOADER, null, this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

        showToolBar(view, getString(R.string.phone_selected));
        Toolbar bar = (Toolbar) view.findViewById(R.id.toolBar);
        bar.setNavigationIcon(R.drawable.nav_back_ico);
        baseActivity.setSupportActionBar(bar);
        baseActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer, Fragment> fragments, Bundle result) {
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

    ContactPhoneSelectAdapter mAdapter = null;

    private void initView(View view) {
        mListView = (ExpandableListView) view.findViewById(R.id.contacts_listView);
        contacts.clear();
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mAdapter = new ContactPhoneSelectAdapter(baseActivity, contacts);
        mListView.setAdapter(mAdapter);
        mListView.setOnChildClickListener(this);
        HintSideBar quickSearch = (HintSideBar) view.findViewById(R.id.quick_searchbar);
        quickSearch.setOnChooseLetterChangedListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent result = new Intent();
        switch (item.getItemId()) {
            case android.R.id.home:
                baseActivity.setResult(Activity.RESULT_CANCELED, result);
                baseActivity.finish();
                break;
            case R.id.menu_phone_select_finish:
                if(setctContact>0&&!TextUtils.isEmpty(selectPhoneNumber)) {
                    result.putExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_ID, setctContact);
                    result.putExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_DISNAME, selectDisName);
                    result.putExtra(PortActivityPhoneNumberSelect.PHONE_NUMBER, selectPhoneNumber);
                    baseActivity.setResult(Activity.RESULT_OK, result);
                    baseActivity.finish();
                }else{
                    Toast.makeText(baseActivity,R.string.invalidate_phone_number,Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return true;
    }

    String selectPhoneNumber;
    String selectDisName;
    int setctContact;
    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
        if(mAdapter.childItemChecked(id)){
            mAdapter.setChildItemChecked(id,false);
            selectPhoneNumber =null;
            selectDisName=null;
            setctContact=0;
        }else{
            mAdapter.setChildItemChecked(id,true);
            Contact contact = (Contact) mAdapter.getGroup(groupPosition);
            Contact.ContactDataNumber contactNumber = (Contact.ContactDataNumber) mAdapter.getChild(groupPosition,childPosition);

            selectPhoneNumber =contactNumber.getNumber();
            selectDisName=contact.getDisplayName();
            setctContact= contact.getId();
        }

        mAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    synchronized public boolean onQueryTextChange(String newText) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERYSTRING, newText);

        PortLoaderManager.restartLoader(baseActivity,loadMgr,CONTACT_LOADER, bundle, this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return super.onQueryTextSubmit(query);
    }

    @Override
    public void onChooseLetter(String s) {
        int count = mAdapter.getGroupCount();
        for (int index = 0; index < count; index++) {
            Contact contact = (Contact) mAdapter.getGroup(index);
            String sortName =  contact.getSortKey();
            if (!TextUtils.isEmpty(sortName)&&sortName.startsWith(s)) {
                mListView.setSelectedGroup(index);
                break;
            }
        }
    }

    @Override
    public void onNoChooseLetter() {

    }

    final String QUERYSTRING = "Constrain";

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case CONTACT_LOADER:
                String selection = null;
                String[] selectionArgs = null;
                if (bundle != null) {
                    String queryString = bundle.getString(QUERYSTRING);
                    if (!TextUtils.isEmpty(queryString)) {
                        selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
                        selectionArgs = new String[]{"%" + queryString + "%"};
                    }
                }
                String Sort_Key_Lable = "phonebook_label";
                if (android.os.Build.VERSION.SDK_INT < 19) {
                    Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
                }
                return new CursorLoader(getActivity(), ContactsContract.Contacts.CONTENT_URI, new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_ID,
                        Sort_Key_Lable,
                        ContactsContract.Contacts.NAME_RAW_CONTACT_ID}, selection, selectionArgs,
                        Sort_Key_Lable + " ASC");
            default:
                break;
        }
        return null;
    }

    List<Contact> contacts = new ArrayList<>();

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case CONTACT_LOADER:
                HashMap<Integer, Contact> contactsCopy = new LinkedHashMap<>();
                ;

                String displayName = null;
                String johnDoe = getString(R.string.activity_main_contact_no_name);
                while (CursorHelper.moveCursorToNext(cursor)) {
                    int id = cursor.getInt(0);
                    displayName = cursor.getString(1);
                    int photoId = cursor.getInt(2);
                    String sortKey = cursor.getString(3);
                    int rawContactid = cursor.getInt(4);
                    if (TextUtils.isEmpty(displayName)) {
                        displayName = johnDoe;
                    }
                    Contact contact = new Contact(id, displayName);
                    contact.setSortKey(sortKey);

                    if (photoId > 0) {
                        contact.setAvatarId(photoId);
                    }
                    contactsCopy.put(rawContactid, contact);
                }

                String selection = ContactsContract.Data.MIMETYPE + " =? ";
                Cursor cursor1 = CursorHelper.resolverQuery(getActivity().getContentResolver(),ContactsContract.Data.CONTENT_URI,
                        new String[]{ContactsContract.Data._ID, ContactsContract.Data.DATA1, ContactsContract.Data.DATA2,
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
                        lable = cursor1.getString(cursor1.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL));
                    }

                    Contact contact = contactsCopy.get(rawContactid);
                    if (contact != null) {
                        contact.addSipAddress(new Contact.ContactDataSipAddress(sipPhoneId, sipPhoneNumber, phoneType, lable));
                    } else {
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
                    String phoneNumber = cursor2.getString(2);
                    String lable = cursor2.getString(3);
                    int rawContactid = cursor2.getInt(4);

                    Contact contact = contactsCopy.get(rawContactid);
                    if (contact != null) {
                        contact.addPhone(new Contact.ContactDataPhone(phoneID, phoneNumber, phoneType, lable));
                    } else {

                    }
                }
                CursorHelper.closeCursor(cursor2);
                contacts.clear();
                contacts.addAll(contactsCopy.values());
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        contacts.clear();
    }


}

