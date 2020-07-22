package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.PortSipSdkWrapper;
import com.portgo.util.NgnStringUtils;
import com.portsip.PortSipSdk;

import java.util.List;
import java.util.Map;


public class ActivityMainSettingAboutFragment extends Fragment implements View.OnClickListener{
	AppCompatActivity activity = null;
	String mTitle="";
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View myView;
		super.onCreateView(inflater, container, savedInstanceState);
		myView = inflater.inflate(R.layout.activity_main_setting_fragment_about, null);
		TextView version = (TextView) myView.findViewById(R.id.activity_about_ver);
		TextView compony = (TextView) myView.findViewById(R.id.activity_about_company);
        compony.getPaint().setFlags(Paint. UNDERLINE_TEXT_FLAG );
        compony.setOnClickListener(this);
		TextView sdkversion = (TextView) myView.findViewById(R.id.activity_about_sdk_ver);

		version.setText(getVersion());
		sdkversion.setText(getSdkVersion());
		activity = (AppCompatActivity)getActivity();
		return myView;
	}
	/**
	 * 获取版本号
	 * @return 当前应用的版本号
	 */
	public String getVersion() {
		String version = null;
		try {
			PackageManager manager = getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
			version = getString(R.string.app_name)+" "+ info.versionName;

		} catch (Exception e) {
//			e.printStackTrace();
		}
		return version;
	}
	/**
	 * 获取版本号
	 * @return 当前应用的版本号
	 */
	public String getSdkVersion() {
	    String versiontips = getString(R.string.activity_about_sdk_ver);
        String sdkversion = PortSipSdkWrapper.getInstance().getVersion();
        return versiontips+sdkversion;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		showToolBarAsActionBar(view,getString(R.string.title_about),true);
	}

	private void showToolBarAsActionBar(View view,String title,boolean homeup) {
		mTitle = title;
		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolBar);

		if(toolbar!=null) {
			toolbar.setBackgroundResource(R.color.portgo_color_toobar_gray);
			setHasOptionsMenu(true);
			toolbar.setTitleTextAppearance(getActivity(),R.style.ToolBarTextAppearance);
			if(homeup) {
				toolbar.setNavigationIcon(R.drawable.nav_back_ico);
			}
			toolbar.setTitleMarginStart(0);
			if(!NgnStringUtils.isNullOrEmpty(title)) {
				toolbar.setTitle(title);
			}
			activity.setSupportActionBar(toolbar);
			if(homeup){
				activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}
	}
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case android.R.id.home:
				getActivity().finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_about_company:
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse(getString(R.string.activity_about_homeurl));
                intent.setData(content_url);
                if (isIntentAvailable(getActivity(), intent)) {
                    startActivity(intent);
                }
                break;
            default:
                break;

        }
    }

    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.size() > 0;
    }
}
