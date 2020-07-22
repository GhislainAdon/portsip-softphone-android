package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.portgo.R;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ActivityMainSettingAdvanceSelectFragment extends PortBaseFragment implements AdapterView.OnItemClickListener {
	int mSelectid= 0;
	List<String>data = Arrays.asList("AAAAA");
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View myView;
		super.onCreateView(inflater, container, savedInstanceState);
		myView = inflater.inflate(R.layout.activity_main_setting_fragment_advance_select, null);
		Bundle bundle = getArguments().getBundle(PortBaseFragment.EXTRA_ARGS);
        String title = "";
		if(bundle!=null) {
			data = Arrays.asList(bundle.getStringArray(PortActivityPrefrenceSelector.CONTENT_DATA));
			mSelectid = bundle.getInt(PortActivityPrefrenceSelector.SELECT_ID);
            title = bundle.getString(PortActivityPrefrenceSelector.SELECT_TITLE);
		}
		initListView(myView);
        showToolBarAsActionBar(myView,title,true);
		return myView;

	}

	@Override
	public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result) {
		if (mNeedRemoveFormList) {
			fragments.remove(mFragmentId);
		}
		manager.beginTransaction().remove(this).commit();
		PortBaseFragment backFragment = (PortBaseFragment) fragments.get(mBackFragmentId);
		if (mBackFragmentId != -1 && backFragment != null && mFragmentResId != -1) {
			showFramegment(getActivity(),manager, fragments, mFragmentResId, backFragment);
			return true;
		}
		return false;
	}

	private  void  initListView(View view){
		ListView listView = (ListView) view.findViewById(R.id.activity_main_fragment_setting_advance_select_list);
		ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(),R.layout.view_im_lv_item, data);
		listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

		listView.setAdapter(adapter);
        if((data!=null)&&(data.size()>mSelectid)) {
            listView.setItemChecked(mSelectid, true);
        }
        listView.setOnItemClickListener(this);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		Intent result = new Intent();
		result.putExtra(PortActivityPrefrenceSelector.SELECT_ID,i);
		baseActivity.setResult(Activity.RESULT_OK,result);
		baseActivity.finish();

    }
}
