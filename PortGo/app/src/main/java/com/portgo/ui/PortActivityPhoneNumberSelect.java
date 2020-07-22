package com.portgo.ui;

import android.os.Bundle;

import com.portgo.R;

public class PortActivityPhoneNumberSelect extends PortGoBaseActivity{

	public static final String PHONE_NUMBER = "number";
	public static final String PHONE_CONTACT_ID = "contactid";
	public static final String PHONE_CONTACT_DISNAME= "displayname";
	public boolean isResume;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();

	}

	ActivityMainPhoneSelectFragment phoneSelectFragment;
	void initView(){
		phoneSelectFragment = new ActivityMainPhoneSelectFragment();
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,phoneSelectFragment).show(phoneSelectFragment).commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		isResume = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		isResume = false;
	}
}
