package com.portgo.ui;

import android.os.Bundle;

import com.portgo.R;

public class PortActivityContactSelect extends PortGoBaseActivity{
	public static final String CONTACT_SELECT_REUSLT = "contact_select";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_select);
        initView();

	}

	ActivityMainContactSelectFragment contactSelectFragment;
	void initView(){
		contactSelectFragment = new ActivityMainContactSelectFragment();
        getFragmentManager().beginTransaction().add(R.id.conten_fragment,contactSelectFragment).show(contactSelectFragment).commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}
