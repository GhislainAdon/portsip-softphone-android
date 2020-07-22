package com.portgo.ui;
//
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.portgo.R;
import com.portgo.manager.UserAccount;

import java.util.List;

//
public class PortActivityContactDetail extends PortGoBaseActivity{
    int contactId;
    Uri contactUri;
    static final String CONTACT_DETAIL_ID = "contact_id";
    private List<UserAccount> userAccounts = null;
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        contactId = getIntent().getIntExtra(CONTACT_DETAIL_ID, -1);
        setContentView(R.layout.activity_chat);
        initView();
    }

    ActivityMainContactsDetailFragment contactsDetailFragment;
    private  void initView(){
        contactsDetailFragment = new ActivityMainContactsDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CONTACT_DETAIL_ID,contactId);
        Bundle extraBundle = new Bundle();
        extraBundle.putBundle(PortBaseFragment.EXTRA_ARGS,bundle);
        contactsDetailFragment.setArguments(extraBundle);
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,contactsDetailFragment).show(contactsDetailFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(contactsDetailFragment!=null){
            contactsDetailFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}