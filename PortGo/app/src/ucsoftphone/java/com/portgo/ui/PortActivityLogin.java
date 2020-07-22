package com.portgo.ui;
//

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.AccountCursorAdapter;
import com.portgo.customwidget.CustomDialog;
import com.portgo.database.DBHelperBase;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipService;
import com.portgo.manager.UserAccount;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.view.TextDrawable;
import com.portsip.PortSipEnumDefine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import static com.portgo.manager.AccountManager.KEY_DIS;
import static com.portgo.manager.AccountManager.KEY_DOMAIN;
import static com.portgo.manager.AccountManager.KEY_NAME;
import static com.portgo.manager.AccountManager.KEY_PWD;
import static com.portgo.manager.AccountManager.KEY_SVR_OUTBOUND;
import static com.portgo.manager.AccountManager.KEY_SVR_PUBLIC;
import static com.portgo.manager.AccountManager.KEY_TRANSPORT;
import static com.portgo.manager.AccountManager.KEY_TRANS_PORT;
import static com.portgo.manager.AccountManager.KEY_TRANS_PROTOCOL;
import static com.portgo.manager.AccountManager.KEY_VOICE_MAIL;
import static com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP;

//
public class PortActivityLogin extends Activity implements Observer,View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, LoaderManager.LoaderCallbacks<Cursor>, FilterQueryProvider, DialogInterface.OnCancelListener {
    UserAccount mUserAccount;
    ProgressDialog login_progress;
    AutoCompleteTextView etUserName;
    EditText etUserPwd,etUserDomain;
    CheckBox ckRemeber;
    ImageView scanner;
    TextWatcher mUserNameWatcher,mUserPwdWatcher,mDomainWatcher;
    boolean autoLogin;
    private AccountCursorAdapter mAdapter = null;
    final int LOADER_ID = 0x384;
    LoaderManager loadMgr;

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        loadMgr = getLoaderManager();
        Intent intent = getIntent();
        if(intent!=null&&BuildConfig.PORT_ACTION_UNREGIEST.equals(intent.getAction())){
            autoLogin = false;
        }else{
            autoLogin= true;
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    autoLogin();
                }
            });
        }

        setContentView(R.layout.activity_login_scanner);

        initView();
        mUserAccount = AccountManager.getDefaultUser(this);
        setAccoutInfo(mUserAccount,true);

        PortLoaderManager.initLoader(this,loadMgr,LOADER_ID, null, this);

        HashMap explans = new HashMap();
        explans.put(PermissionManager.REQUEST_WRITE_CONTACTS,"REQUEST_WRITE_CONTACTS");
        explans.put(PermissionManager.REQUEST_EXTERNAL_STORAGE,"REQUEST_EXTERNAL_STORAGE");
        explans.put(PermissionManager.REQUEST_MICPHONE,"REQUEST_MICPHONE");
        explans.put(PermissionManager.REQUEST_CAMERA,"REQUEST_CAMERA");

        AccountManager.getInstance().addObserver(this);

        if(autoLogin&&mUserAccount!=null){
            findViewById(R.id.login_input_dlg).setVisibility(View.INVISIBLE);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!AccountManager.allPermissionGranted(this)) {
            startPermissionsActivity();
        }
    }
    final int REQUEST_PERMISSION =0x33;
    private void startPermissionsActivity() {
        PortActivityPermission.startActivityForResult(this, REQUEST_PERMISSION, PermissionManager.PORTGO_MUST_PERMISSION);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setAccoutInfo(UserAccount account,boolean dismisDropdown) {
        etUserName.removeTextChangedListener(mUserNameWatcher);
        etUserPwd.removeTextChangedListener(mUserPwdWatcher);
        etUserDomain.removeTextChangedListener(mDomainWatcher);
        if (account != null) {
            String content = account.getAccountNumber();
            if (content != null) {
                if (!etUserName.getText().toString().equals(content)) {
                    etUserName.setText(content);
                    etUserName.setSelection(content.length());
                }
            }

            content = account.getPassword();
            if (content != null && account.getRemberme()) {
                etUserPwd.setText(content);
            }else{
                etUserPwd.setText("");
            }

            if(account.getPort()!=5060&&account.getPort()>0&&TextUtils.isEmpty(account.getRealm())){
                etUserDomain.setText(account.getDomain()+":"+account.getPort());
            }else{
                etUserDomain.setText(account.getDomain());
            }
            ckRemeber.setChecked(account.getRemberme());
        }
        if (dismisDropdown){
            etUserName.dismissDropDown();
        }

        etUserName.addTextChangedListener(mUserNameWatcher);
        etUserPwd.addTextChangedListener(mUserPwdWatcher);
        etUserDomain.addTextChangedListener(mDomainWatcher);
    }

    private void getAccoutInfo(UserAccount account){
        if(account!=null){
            account.setAccountNumber(etUserName.getText().toString());
            account.setPassword(etUserPwd.getText().toString());
            account.setDomain(etUserDomain.getText().toString());
        }
    }

    protected void onLoginClick(){

        if(!PermissionManager.testDangercePermissions(this,PermissionManager.PORTGO_MUST_PERMISSION))
        {
            return;
        }

        if(mUserAccount==null||mUserAccount.isValidate()==false){
            CustomDialog.showTipsDialog(this,android.R.string.dialog_alert_title,R.string.login_input_tips);
            return;
        }

        final AccountManager accountManager = AccountManager.getInstance();
        switch (accountManager.getLoginState()){
            case UserAccount.STATE_NOTONLINE://----------->

                mUserAccount.setDefault(true);
                Cursor cursor = getContentResolver().query(DBHelperBase.AccountColumns.CONTENT_URI,
                        new String[]{DBHelperBase.AccountColumns._ID,},DBHelperBase.AccountColumns.ACCOUNT_NAME+" =? AND "+
                                DBHelperBase.AccountColumns.ACCOUNT_DOMAIN+" =?",
                        new String[]{mUserAccount.getAccountNumber(),mUserAccount.getDomain()},null,null);

                if(CursorHelper.moveCursorToFirst(cursor)) {
                    int id = cursor.getInt(cursor.getColumnIndex(DBHelperBase.AccountColumns._ID));
                    mUserAccount.setID(id);
                    AccountManager.updateUser(PortActivityLogin.this, mUserAccount,false);
                }else {
                    AccountManager.insertUser(PortActivityLogin.this, mUserAccount);
                }
                CursorHelper.closeCursor(cursor);
                Intent registItent = new Intent(this,PortSipService.class);
                registItent.setAction(BuildConfig.PORT_ACTION_REGIEST);
                startService(registItent);
                break;
            case UserAccount.STATE_ONLINE:
                dismissLoginProgress();
                enterMainActivity();
                this.finish();
                break;
            case UserAccount.STATE_LOGIN:
                showLoginProgress(autoLogin);
                break;
            default:
                break;
        }

    }

    private void showLoginProgress(boolean autoLogin){
        if(autoLogin){//自动登录，显示小的菊花进度条
            findViewById(R.id.loading_process_dialog).setVisibility(View.VISIBLE);
            if (login_progress != null) {
                login_progress.dismiss();
                login_progress = null;
            }
        }else {//非自动登录，显示带文字进度条
            if (login_progress == null) {
                login_progress = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
                login_progress.setOnCancelListener(this);
            }
            if (!login_progress.isShowing()) {
                login_progress.setMessage(getString(R.string.login_login_tips));
                login_progress.setCancelable(true);
                login_progress.setIndeterminate(false);
                login_progress.setCanceledOnTouchOutside(false);

                login_progress.show();
            }
        }
    }

    private void dismissLoginProgress(){
        if(login_progress!=null){
            login_progress.dismiss();
        }
        findViewById(R.id.loading_process_dialog).setVisibility(View.INVISIBLE);
    }

    @Override
    public void update(Observable observable, Object data) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                refreshLoginState();
            }
        });
    }


    void refreshLoginState(){
        AccountManager accountManager = AccountManager.getInstance();
        switch (accountManager.getLoginState()) {
            case UserAccount.STATE_NOTONLINE:
                String reason = accountManager.getLoginStateDetail();
                if(!TextUtils.isEmpty(reason)){
                    Toast.makeText(this,reason,Toast.LENGTH_LONG).show();
                }
                dismissLoginProgress();
                findViewById(R.id.login_input_dlg).setVisibility(View.VISIBLE);
                break;
            case UserAccount.STATE_ONLINE:
                dismissLoginProgress();

                enterMainActivity();
                this.finish();
                break;
            case UserAccount.STATE_LOGIN:

                showLoginProgress(autoLogin);
                break;
            default:
                break;
        }
    }

    final int REQUEST_ADVANCE  =0x2134;
    final int REQUEST_OVERLAYS  =0x534;
    final int REQUEST_QR_RCODE=0x535;
    private void enterMainActivity(){
        Intent intentFrom = getIntent();

        Intent intent = new Intent(this,PortActivityMain.class);
        if(intentFrom!=null&&Intent.ACTION_CALL.equals(intentFrom.getAction())){
            intent.setAction(BuildConfig.PORT_ACTION_CALL);
            intent.setData(intentFrom.getData());
        }

        Bundle bundle = getIntent().getExtras();
        if(bundle!=null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    private void startQrCode() {

        new IntentIntegrator(this).setOrientationLocked(false).setCaptureActivity(ActivityQRScanner .class).setRequestCode(REQUEST_QR_RCODE).initiateScan();
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.activity_login_scanner: {
                startQrCode();
            }
            break;
            case R.id.activity_login_advance:{
                Intent intent= new Intent(this,PortActivityAdvance.class);
                if(mUserAccount==null){
                    mUserAccount = new UserAccount();
                }
                getAccoutInfo(mUserAccount);
                intent.putExtra("User",mUserAccount);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent,REQUEST_ADVANCE);
            }
            break;
            case R.id.activity_login_delusername: {
                etUserName.setText("");
                etUserName.requestFocus();
            }
            break;
            case R.id.activity_login_delpwd: {
                etUserPwd.setText("");
                etUserPwd.requestFocus();
            }
            break;
            case R.id.activity_login_deldomain: {
                etUserDomain.setText("");
                etUserDomain.requestFocus();
            }
            break;
            case R.id.activity_login_login:{
                if(mUserAccount==null){
                    mUserAccount = new UserAccount();
                }
                getAccoutInfo(mUserAccount);
                onLoginClick();
            }
            break;
            case R.id.activity_login_username:
                etUserName.showDropDown();
                break;
            case R.id.activity_login_item_user_del: {
                UserAccount account = (UserAccount) view.getTag();
                if(mUserAccount!=null&mUserAccount.getId()==account.getId()){
                    setAccoutInfo(null,false);
                }
                AccountManager.deleteUser(this,account);

                Bundle bundle = null;
                String strName =etUserName.getEditableText().toString();
                if(!TextUtils.isEmpty(strName)) {
                    bundle = new Bundle();
                    bundle.putString("filter",strName );
                }
                PortLoaderManager.restartLoader(this,loadMgr,LOADER_ID,bundle,this);

            }
            break;
            case R.id.activity_login_item_user: {
                mUserAccount = (UserAccount) view.findViewById(R.id.activity_login_item_user_del).getTag();
                mUserAccount.setDefault(true);
                AccountManager.updateUser(PortActivityLogin.this, mUserAccount,false);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
                setAccoutInfo(mUserAccount,true);

            }
            break;
            default:
                break;
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_QR_RCODE:
                if(resultCode == RESULT_OK) {
                    IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
                    String scanResult =result.getContents();
                    JSONObject user;
                    UserAccount userAccount;
                    try {
                        userAccount = new UserAccount();
                        user = new JSONObject(scanResult);
                        userAccount.setAccountNumber(user.getString(KEY_NAME));//账号名
                        userAccount.setPassword(user.getString(KEY_PWD));//账号密码
                        userAccount.setDomain(user.getString(KEY_DOMAIN));
                    }catch (JSONException e) {
                        userAccount = null;
                        user = null;
                    }
                    if(userAccount!=null&&user!=null){
                        int transType = PortSipEnumDefine.ENUM_TRANSPORT_UDP, transPort = 5060;

                        try {
                            JSONArray transport = user.getJSONArray(KEY_TRANSPORT);
                            HashMap<String, Integer> hashTrans = new HashMap<>();
                            for (int i = 0; i < transport.length(); i++) {
                                JSONObject trans = transport.getJSONObject(i);
                                String key = trans.getString(KEY_TRANS_PROTOCOL);
                                int port = trans.getInt(KEY_TRANS_PORT);
                                if(!TextUtils.isEmpty(key)) {
                                    hashTrans.put(key, port);
                                }
                            }
                            Set<String> transTypeSet = hashTrans.keySet();
                            if (transTypeSet.contains("UDP")) {
                                transType = PortSipEnumDefine.ENUM_TRANSPORT_UDP;
                                transPort = hashTrans.get("UDP");
                            } else if (transTypeSet.contains("TCP")) {
                                transType = PortSipEnumDefine.ENUM_TRANSPORT_TCP;
                                transPort = hashTrans.get("TCP");
                            } else if (transTypeSet.contains("TLS")) {
                                transType = PortSipEnumDefine.ENUM_TRANSPORT_TLS;
                                transPort = hashTrans.get("TLS");
                            } else if (transTypeSet.contains("PERS_UDP")) {
                                transType = PortSipEnumDefine.ENUM_TRANSPORT_PERS_UDP;
                                transPort = hashTrans.get("PERS_UDP");
                            } else if (transTypeSet.contains("PERS_TCP")) {
                                transType = PortSipEnumDefine.ENUM_TRANSPORT_PERS_TCP;
                                transPort = hashTrans.get("PERS_TCP");
                            }

                        } catch (JSONException e) {
                            transType = PortSipEnumDefine.ENUM_TRANSPORT_UDP;
                            transPort = 5060;
                        }

                        userAccount.setTransType(transType);

                        String svr = null;
                        try {
                            svr = user.getString(KEY_SVR_PUBLIC);
                        } catch (JSONException e) {
                            svr = null;
                        }

                        if (svr == null) {
                            try {
                                svr = user.getString(KEY_SVR_OUTBOUND);
                            } catch (JSONException e) {
                                svr = null;
                            }
                        }

                        userAccount.setRealm(transPort==5060?svr:svr+":"+transPort);
                        try {
                            String disname =  user.getString(KEY_DIS);
                            if(disname!=null){
                                disname = disname.trim();
                            }
                            userAccount.setDisplayName(disname);
                        } catch (JSONException e) {
                        }
                        try {
                            userAccount.setVoiceMail(user.getString(KEY_VOICE_MAIL));
                        } catch (JSONException e) {
                        }

                    }

                    if(userAccount==null||user==null){
                       Toast.makeText(this,R.string.invalidate_qrcode,Toast.LENGTH_LONG).show();
                    }else{
                        userAccount.setDefault(true);
                        mUserAccount = userAccount;
                        setAccoutInfo(mUserAccount,true);
                        AccountManager.insertUser(PortActivityLogin.this, userAccount);
                        //autoLogin();
                        onLoginClick();
                    }
                }
                break;
            case REQUEST_ADVANCE:
                if(resultCode == RESULT_OK) {
                    mUserAccount = (UserAccount) data.getSerializableExtra("User");
                }

                break;
            case REQUEST_PERMISSION:
                if (requestCode ==  REQUEST_PERMISSION && resultCode == PortActivityPermission.PERMISSIONS_DENIED) {
                    this.finish();
                }
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        for (int i=0;i<permissions.length;i++){
            if(grantResults[i]==PackageManager.PERMISSION_GRANTED){

            }else{
                Toast.makeText(this,permissions[i]+"failed",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId())
        {
            case R.id.activity_login_rememberme:
                if(mUserAccount!=null) {
                    mUserAccount.setRemberMe(b);
                    if (mUserAccount.isValidate()) {
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (login_progress == null || !login_progress.isShowing()) {
            this.stopService(new Intent(this, PortSipService.class));
            super.onBackPressed();
            this.finish();
        }
    }

    @Override
    protected void  onDestroy(){
        super.onDestroy();
        dismissLoginProgress();
        AccountManager.getInstance().deleteObserver(PortActivityLogin.this);
    }

    private  void initView(){
        etUserName= (AutoCompleteTextView) findViewById(R.id.activity_login_username);
        etUserName.setOnClickListener(this);
        etUserName.setDropDownAnchor(R.id.activity_login_username_rl);
//        etUserName.setOnDismissListener();
        etUserPwd = (EditText)findViewById(R.id.activity_login_pwd);
        etUserDomain = (EditText)findViewById(R.id.activity_login_domain);
        ckRemeber = ((CheckBox)findViewById(R.id.activity_login_rememberme));
        ckRemeber.setOnCheckedChangeListener(this);
        ImageView delname,delPwd,delDomain;
        delname = (ImageView) findViewById(R.id.activity_login_delusername);
        delPwd = (ImageView)findViewById(R.id.activity_login_delpwd);
        delDomain = (ImageView)findViewById(R.id.activity_login_deldomain);
        delname.setOnClickListener(this);
        delPwd.setOnClickListener(this);
        delDomain.setOnClickListener(this);

        mAdapter = new AccountCursorAdapter(this,null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER,this,this);
        mAdapter.setFilterQueryProvider(this);
        scanner = (ImageView)findViewById(R.id.activity_login_scanner);
        scanner.setOnClickListener(this);
        etUserName.setAdapter(mAdapter);
        ((ImageView)findViewById(R.id.activity_login_logo)).setImageBitmap(null);
        //.fontSize(60)
        TextDrawable drawable = TextDrawable.builder().beginConfig().textColor(getColor(R.color.portgo_color_blue)).bold().useFont(Typeface.SANS_SERIF).endConfig().buildRect(getString(R.string.app_name),getColor(android.R.color.transparent));

        ((ImageView)findViewById(R.id.activity_login_logo)).setImageDrawable(drawable);
        mUserNameWatcher = new MyTextWatcher(findViewById(R.id.activity_login_username_rl));
        mUserPwdWatcher = new MyTextWatcher(findViewById(R.id.activity_login_pwd_rl));
        mDomainWatcher = new MyTextWatcher(findViewById(R.id.rl_domain));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] filter=null;
        String where =null;
        if(bundle!=null){
            where = DBHelperBase.AccountColumns.ACCOUNT_NAME+" like ?";
            filter = new String[]{bundle.getString("filter")};
        }
        return new CursorLoader(this, DBHelperBase.AccountColumns.CONTENT_URI,
                 null, where,filter,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ID:
                mAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public Cursor runQuery(CharSequence charSequence) {
        Bundle  bundle = new Bundle();
        bundle.putString("filter","%"+charSequence+"%");
        PortLoaderManager.restartLoader(this,loadMgr,LOADER_ID, bundle, this);

        return mAdapter.getCursor();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Intent unregistItent = new Intent(this,PortSipService.class);
        unregistItent.setAction(BuildConfig.PORT_ACTION_UNREGIEST);
        startService(unregistItent);
    }

    class MyTextWatcher implements TextWatcher {
        final View rl;
        MyTextWatcher(View parent){
            rl = parent;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            ImageView del= findViewById(R.id.activity_login_delusername);;
            switch (rl.getId()){
                case R.id.activity_login_username_rl:
                    del = findViewById(R.id.activity_login_delusername);
                    etUserPwd.setText("");
                    break;
                case R.id.activity_login_pwd_rl:
                    del = findViewById(R.id.activity_login_delpwd);
                    break;
                case R.id.rl_domain:
                    del = findViewById(R.id.activity_login_deldomain);
                    if(mUserAccount!=null){
                        UserAccount account = new UserAccount();
                        account.setAccountNumber(mUserAccount.getAccountNumber());
                        account.setDomain(editable.toString());
                        account.setPassword(mUserAccount.getPassword());

                        mUserAccount = account;
                    }
                    break;
            }

            if (editable.toString().length() > 0) {
                del.setVisibility(View.VISIBLE);
            }else{
                del.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void autoLogin() {
        if (!AccountManager.allPermissionGranted(this)) {
            return;
        }
        AccountManager accountManager = AccountManager.getInstance();
        mUserAccount = AccountManager.getDefaultUser(PortActivityLogin.this);//默认使用
        if(accountManager.getLoginState()==UserAccount.STATE_ONLINE){
            enterMainActivity();
            PortActivityLogin.this.finish();
        }else {
            if (mUserAccount == null) {
                mUserAccount = new UserAccount();
                setAccoutInfo(mUserAccount, true);
            } else {
                setAccoutInfo(mUserAccount, true);
                if (mUserAccount.isValidate() && mUserAccount.getRemberme() && autoLogin) {
                    onLoginClick();
                }
            }
        }
    }

}