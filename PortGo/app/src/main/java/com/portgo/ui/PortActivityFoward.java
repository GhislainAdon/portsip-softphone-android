package com.portgo.ui;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.appcompat.widget.Toolbar;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.database.DBHelperBase;

import com.portgo.manager.AccountManager;
import com.portgo.manager.UserAccount;

import com.portgo.util.NgnStringUtils;
import com.portgo.util.SpecialCharTrimFilter;
import com.portgo.view.CursorEndEditTextView;

public class PortActivityFoward extends PortGoBaseActivity implements RadioGroup.OnCheckedChangeListener,
        View.OnFocusChangeListener {
    public final static String FORWARD_ACCOUNT_ID = "fowrad_account_id";
    private int mAccountId = -1;
    RadioGroup rgForward =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_setting_fragment_account_foward);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(R.string.title_forward);
            toolbar.setTitleTextAppearance(this, R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setTitleMarginStart(0);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

//        mAccountId = getIntent().getIntExtra(FORWARD_ACCOUNT_ID, -1);

        rgForward = (RadioGroup) findViewById(R.id.fragment_account_foward);
        rgForward.setOnCheckedChangeListener(this);

        CursorEndEditTextView editForward = (CursorEndEditTextView) findViewById(R.id.fragment_setting_account_forward_editor);
        editForward.setOnFocusChangeListener(this);
        CursorEndEditTextView editRedirect = (CursorEndEditTextView) findViewById(R.id.fragment_setting_account_redirect_editor);
        editRedirect.setOnFocusChangeListener(this);

        UserAccount account = AccountManager.getDefaultUser(this);
        if(account!=null){
            int forwardMode = account.getFowardMode();
            switch (forwardMode){
                case UserAccount.FORWARD_BUSY:
                    rgForward.check(R.id.fragment_account_foward_busy);
                    break;
                case UserAccount.FORWARD_NOANSWER:
                    rgForward.check(R.id.fragment_account_foward_noanswer);
                    break;
                case UserAccount.FORWARD_CLOSE:
                    rgForward.check(R.id.fragment_account_foward_close);
                    break;
                case UserAccount.FORWARD_ALL:
                    rgForward.check(R.id.fragment_account_foward_all);
                    break;
            }
            editRedirect.setTextCursorEnd(""+account.getFowardNoAnswerTime());
            String forwardTo = account.getFowardTo();

            if(TextUtils.isEmpty(forwardTo)){
                forwardTo = "";
            }
            editForward.setTextCursorEnd(forwardTo);
            editForward.setFilters(new InputFilter[]{new SpecialCharTrimFilter("\r\n")});
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                findViewById(R.id.fragment_setting_account_forward_editor).clearFocus();
                findViewById(R.id.fragment_setting_account_redirect_editor).clearFocus();
                this.finish();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        findViewById(R.id.fragment_setting_account_forward_editor).clearFocus();
        findViewById(R.id.fragment_setting_account_redirect_editor).clearFocus();
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
        int forwordMode = 0;
        UserAccount account = AccountManager.getDefaultUser(this);
        if (account == null) {
            return;
        }
        Uri userUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI, account.getId());
        switch (i){
            case R.id.fragment_account_foward_busy:
                findViewById(R.id.fragment_setting_account_redirect_editorll).setVisibility(View.GONE);
                forwordMode= UserAccount.FORWARD_BUSY;
                break;
            case R.id.fragment_account_foward_noanswer:
                findViewById(R.id.fragment_setting_account_redirect_editorll).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_setting_account_redirect_editor).requestFocus();
                forwordMode= UserAccount.FORWARD_NOANSWER;
                break;
            case R.id.fragment_account_foward_close:
                findViewById(R.id.fragment_setting_account_redirect_editorll).setVisibility(View.GONE);
                forwordMode= UserAccount.FORWARD_CLOSE;
                break;
            case R.id.fragment_account_foward_all:
                findViewById(R.id.fragment_setting_account_redirect_editorll).setVisibility(View.GONE);
                forwordMode= UserAccount.FORWARD_ALL;
                break;
        }
        if(account.getFowardMode()!=forwordMode) {
            ContentValues values = new ContentValues();
            values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_MODE, forwordMode);
            getContentResolver().update(userUri, values, null, null);
            account.setFowardMode(forwordMode);
            forwardProcess(account);
        }

    }

    @Override
    public void onFocusChange(View view, boolean b) {
        UserAccount account = AccountManager.getDefaultUser(this);
        if (account == null) {
            return;
        }
        Uri userUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI, account.getId());
        ContentValues values = new ContentValues();
        switch (view.getId()) {
            case R.id.fragment_setting_account_forward_editor:
                if (!b) {
                    String text = ((EditText) view).getText().toString();

                    if (!text.equals(account.getFowardTo())) {
                        account.setFowardto(text);
                        values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARDTO, text);
                        getContentResolver().update(userUri, values, null, null);
                    }
                    forwardProcess(account);
                }
                break;
            case R.id.fragment_setting_account_redirect_editor:
                if (!b) {
                    String text = ((EditText) view).getText().toString();
                    int minSecond = getResources().getInteger(R.integer.forward_time_noanswer_min);
                    int time = NgnStringUtils.parseInt(text,getResources().getInteger(R.integer.forward_time_noanswer_default));
                    if(time<minSecond){
                        time=minSecond;
                        Toast.makeText(this,R.string.hint_min_sencod,Toast.LENGTH_SHORT).show();
                    }
                    if (time!=account.getFowardNoAnswerTime()) {
                        account.setFowardNoAnswerTime(time);
                        values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_TIME, time);
                        getContentResolver().update(userUri, values, null, null);
                    }
                    forwardProcess(account);
                }
                break;
        }
    }

    private  void forwardProcess(UserAccount account){
        int mode = account.getFowardMode();
        String forwordto = account.getFowardTo();
        int time = account.getFowardNoAnswerTime();
        switch (mode){
            case UserAccount.FORWARD_ALL:
                if(TextUtils.isEmpty(forwordto)){
                    mSipMgr.disableCallForward();
                }else{
                    mSipMgr.enableCallForward(false,forwordto);
                }
                break;
            case UserAccount.FORWARD_CLOSE:
                mSipMgr.disableCallForward();
                break;
            case UserAccount.FORWARD_BUSY:
                if(TextUtils.isEmpty(forwordto)){
                    mSipMgr.disableCallForward();
                }else{
                    mSipMgr.enableCallForward(true,forwordto);
                }
                break;
            case UserAccount.FORWARD_NOANSWER:
                mSipMgr.disableCallForward();
                break;
        }
    }

}
