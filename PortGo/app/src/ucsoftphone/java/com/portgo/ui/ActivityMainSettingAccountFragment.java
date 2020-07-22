package com.portgo.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;
import com.portgo.manager.AccountManager;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipService;
import com.portgo.manager.UserAccount;
import com.portgo.util.OkHttpHelper;
import com.portgo.util.SpecialCharTrimFilter;
import com.portgo.view.CursorEndEditTextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


public class ActivityMainSettingAccountFragment extends PortBaseFragment implements View.OnClickListener,
        Observer,View.OnFocusChangeListener, CompoundButton.OnCheckedChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.activity_main_setting_fragment_account, null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showToolBarAsActionBar(view, getString(R.string.title_account), true);
        EditText voicemail = (EditText) view.findViewById(R.id.activity_main_fragment_setting_account_voicemail);
        voicemail.setFilters(new InputFilter[]{new SpecialCharTrimFilter("\r\n")});
        view.findViewById(R.id.activity_main_fragment_setting_account_unregister).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_setting_account_forward).setOnClickListener(this);
        ((ToggleButton)view.findViewById(R.id.fragment_account_nodistrub_switch)).setOnCheckedChangeListener(this);

        AccountManager.getInstance().addObserver(this);
        updateView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AccountManager.getInstance().deleteObserver(this);
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer, Fragment> fragments, Bundle result) {
        if (mNeedRemoveFormList) {
            fragments.remove(mFragmentId);
        }
        EditText voicemail = (EditText) getView().findViewById(R.id.activity_main_fragment_setting_account_voicemail);
        voicemail.clearFocus();
        //清除焦点，使输入失效

//		Fragment fragment = fragments.get(mFragmentId);
        manager.beginTransaction().remove(this).commit();
        PortBaseFragment backFragment = (PortBaseFragment) fragments.get(mBackFragmentId);
        if (mBackFragmentId != -1 && backFragment != null && mFragmentResId != -1) {
            showFramegment(getActivity(), manager, fragments, mFragmentResId, backFragment);
            return true;
        }
        return false;
    }

    private void updateView() {

        UserAccount userAccount = AccountManager.getDefaultUser(baseActivity);
        if (userAccount != null) {
            String sipUri = userAccount.getFullAccountReamName();
            if (userAccount.getPort() != 5060) {
                sipUri += ":" + userAccount.getPort();
            }
            setTextViewText(R.id.activity_main_fragment_setting_account_uri, userAccount.getFullAccountReamName());
            setTextViewText(R.id.activity_main_fragment_setting_account_disname, userAccount.getDisplayDefaultAccount());
            String[] transtype =getResources().getStringArray(R.array.transports_type);
            if(transtype.length>userAccount.getTransType()) {
                setTextViewText(R.id.activity_main_fragment_setting_account_transtype, transtype[userAccount.getTransType()]);
            }
            ((ToggleButton)getView().findViewById(R.id.fragment_account_nodistrub_switch)).setChecked(userAccount.isDistrbEnable());
            getView().findViewById(R.id.activity_main_fragment_setting_account_callrule_rl).setOnClickListener(this);

            String voiceMail = userAccount.getVoiceMail();
            CursorEndEditTextView voicemail = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_fragment_setting_account_voicemail);
            voicemail.setTextCursorEnd(voiceMail);
            voicemail.setOnFocusChangeListener(this);
        }
    }

    private void setTextViewText(int viewId, String text) {
        TextView textView = (TextView) getView().findViewById(viewId);
        if(TextUtils.isEmpty(text)){
            text="";
        }
        textView.setText(text);
    }

    @Override
    public void onClick(View view) {
        AccountManager accountManager = AccountManager.getInstance();
        Intent unregistItent;
        switch (view.getId()) {
            case R.id.activity_main_fragment_setting_account_unregister:
                switch (accountManager.getLoginState()) {
                    case UserAccount.STATE_NOTONLINE:
//                        baseActivity.mAccountMgr.register(baseActivity,baseActivity.mNetworkMgr,
//                                baseActivity.mSipMgr,baseActivity.mConfigurationService);
                        break;
                    case UserAccount.STATE_ONLINE:
                        HashMap<Integer, Long> map = baseActivity.mCallMgr.getSubscrib();
                        Iterator iter = map.entrySet().iterator();//终止所有的订阅
                        while (iter.hasNext()) {
                            Map.Entry entry = (Map.Entry) iter.next();
                            Integer key = (Integer) entry.getKey();
                            Long val = (Long) entry.getValue();
                            baseActivity.mSipMgr.presenceTerminateSubscribe(val);
                        }
                        unregistItent = new Intent(baseActivity,PortSipService.class);
                        unregistItent.setAction(BuildConfig.PORT_ACTION_UNREGIEST);
                        baseActivity.startService(unregistItent);//
                        break;
                    case UserAccount.STATE_LOGIN:
                        unregistItent = new Intent(baseActivity,PortSipService.class);
                        unregistItent.setAction(BuildConfig.PORT_ACTION_UNREGIEST);
                        baseActivity.startService(unregistItent);
                        break;
                }
                baseActivity.mApp.closeActivitys();
                Intent intent = new Intent(getActivity(), PortActivityLogin.class);
                intent.setAction(BuildConfig.PORT_ACTION_UNREGIEST);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                OkHttpHelper.cancellAll();
                //程序退出时，将所有的尚在处理中的消息跟新为失败状态
                DataBaseManager.upDataProcessingMessagetoFail(getActivity());

                getActivity().startActivity(intent);
                break;

            case R.id.activity_main_fragment_setting_account_callrule_rl: {
                Intent ruleIntent = new Intent(baseActivity, PortActivityCallRules.class);
                UserAccount userAccount = AccountManager.getDefaultUser(baseActivity);
                if (userAccount != null) {
                    ruleIntent.putExtra(PortActivityCallRules.ACCOUNT_ID, userAccount.getId());
                    startActivity(ruleIntent);
                }
            }
            break;
            case R.id.activity_main_fragment_setting_account_forward: {
                Intent forwardIntent = new Intent(baseActivity, PortActivityFoward.class);
                UserAccount userAccount = AccountManager.getDefaultUser(baseActivity);
                if (userAccount != null) {
                    forwardIntent.putExtra(PortActivityFoward.FORWARD_ACCOUNT_ID, userAccount.getId());
                    startActivity(forwardIntent);
                }
            }
            default:
                break;
        }
    }

    @Override
    public void update(Observable observable, Object o) {
//        switch (baseActivity.mAccountMgr.getLoginState()){
//            case UserAccount.STATE_NOTONLINE:
//                setTextViewText(R.id.activity_main_fragment_setting_account_unregister,getString(R.string.str_login));
//                getView().findViewById(R.id.activity_main_fragment_setting_account_unregister).setEnabled(true);
//                break;
//            case UserAccount.STATE_ONLINE:
//                getView().findViewById(R.id.activity_main_fragment_setting_account_unregister).setEnabled(true);
//                setTextViewText(R.id.activity_main_fragment_setting_account_unregister,
//                        getString(R.string.activity_main_fragment_setting_account_unregister));
//                break;
//            case UserAccount.STATE_LOGIN:
//                getView().findViewById(R.id.activity_main_fragment_setting_account_unregister).setEnabled(false);
//                setTextViewText(R.id.activity_main_fragment_setting_account_unregister,getString(R.string.str_login));
//                break;
//        }
    }



    @Override
    public void onFocusChange(View view, boolean b) {
        UserAccount account = AccountManager.getDefaultUser(baseActivity);
        if (account == null) {
            return;
        }
        Uri userUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI, account.getId());
        ContentValues values = new ContentValues();
        switch (view.getId()) {
            case R.id.activity_main_fragment_setting_account_voicemail:
                if (!b) {
                    String text = ((EditText) view).getText().toString();

                    if (!text.equals(account.getVoiceMail())) {
                        account.setVoiceMail(text);
                        values.put(DBHelperBase.AccountColumns.ACCOUNT_VOICEMAIL, text);
                        baseActivity.getContentResolver().update(userUri, values, null, null);
                    }
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.fragment_account_nodistrub_switch:
                if(b){
                    Toast.makeText(getActivity(),R.string.nodisturb_tips,Toast.LENGTH_SHORT).show();
                }
                baseActivity.mSipMgr.setDistrubMode(b);

                UserAccount account = AccountManager.getDefaultUser(baseActivity);
                Uri userUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI, account.getId());
                ContentValues values = new ContentValues();

                values.put(DBHelperBase.AccountColumns.ACCOUNT_DISTRUB_MODE, b?1:0);
                baseActivity.getContentResolver().update(userUri, values, null, null);

                break;
            default:
                break;
        }
    }
}
