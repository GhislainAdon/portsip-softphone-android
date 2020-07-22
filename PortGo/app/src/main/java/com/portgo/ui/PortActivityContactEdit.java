package com.portgo.ui;
//

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.portgo.R;

//
public class PortActivityContactEdit extends PortGoBaseActivity{
    static final String CONTACT_EDIT_FRIEND = "friend";
    static final String CONTACT_EDIT_ID = "contactid";
    static final String CONTACT_EDIT_NAME = "contactname";
    static final String CONTACT_EDIT_PHONE = "contactphone";
    int contactId;
    String name,phone;//sipnumber
    boolean friend;
    Uri contactUri= ContactsContract.Contacts.CONTENT_URI;;
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        contactId = getIntent().getIntExtra(CONTACT_EDIT_ID, -1);
        name = getIntent().getStringExtra(CONTACT_EDIT_NAME);
        phone = getIntent().getStringExtra(CONTACT_EDIT_PHONE);
        friend= getIntent().getBooleanExtra(CONTACT_EDIT_FRIEND,false);

        setContentView(R.layout.activity_chat);
        initView();
    }

    ActivityMainContactsEditFragment contactsEditFragment;
    private  void initView(){
        contactsEditFragment = new ActivityMainContactsEditFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CONTACT_EDIT_ID,contactId);
        bundle.putString(CONTACT_EDIT_NAME,name);
        bundle.putString(CONTACT_EDIT_PHONE,phone);
        bundle.putBoolean(CONTACT_EDIT_FRIEND,friend);
        Bundle extraBundle = new Bundle();
        extraBundle.putBundle(PortBaseFragment.EXTRA_ARGS,bundle);
        contactsEditFragment.setArguments(extraBundle);
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,contactsEditFragment).show(contactsEditFragment).commit();
    }
}