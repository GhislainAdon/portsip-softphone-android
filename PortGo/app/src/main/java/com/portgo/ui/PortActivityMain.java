package com.portgo.ui;

import android.app.ActionBar;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import androidx.annotation.IdRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.firebase.iid.FirebaseInstanceId;
import com.portgo.BuildConfig;
import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.androidcontacts.ContactsContract;
import com.portgo.database.CursorJoiner4Loader;
import com.portgo.database.CursorJoinerLoader;
import com.portgo.database.DBHelperBase;
import com.portgo.database.JoinIntegerIdCompare;
import com.portgo.database.PortCursorJoiner;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.NotificationUtils;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipEngine;
import com.portgo.manager.PortSipService;
import com.portgo.manager.UserAccount;
import com.portgo.view.TextDrawable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.HASIM;
import static com.portgo.BuildConfig.HASPRESENCE;
import static com.portgo.BuildConfig.PORT_ACTION_CALL;

public class PortActivityMain extends PortGoBaseActivity implements RadioGroup.OnCheckedChangeListener,LoaderManager.LoaderCallbacks<Cursor>, CompoundButton.OnCheckedChangeListener {
    private final int LOADER_UNREDAD_MESSAGES = 824;
    private final int LOADER_PRESENCE_MESSAGES = 13;
    private final int LOADER_UNREAD_CALL = 75;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!HASIM) {
            RadioGroup radioGroup = findViewById(R.id.activity_main_tabs);
            radioGroup.setWeightSum(4);
            findViewById(R.id.activity_main_tab_message).setVisibility(View.GONE);
        }
        findViewById(R.id.activity_main_tab_message).setEnabled(ENABLEIM);

        ((RadioGroup) findViewById(R.id.activity_main_tabs)).setOnCheckedChangeListener(this);
        PortBaseFragment fragment = null;
        if (PORT_ACTION_CALL.equals(getIntent().getAction())) {
            Bundle extra = new Bundle();
            extra.putParcelable(PORT_ACTION_CALL, getIntent().getData());
            fragment = getMainFragment(R.id.activity_main_tab_numbers, extra);
        } else {
            fragment = getMainFragment(R.id.activity_main_tab_numbers,null);
        }

        setFirstFragment(getIntent(), fragment);

        if (HASIM && ENABLEIM) {

            PortLoaderManager.initLoader(this,loadMgr,LOADER_UNREDAD_MESSAGES, null, this);

        }

        PortLoaderManager.initLoader(this,loadMgr, LOADER_UNREAD_CALL, null, this);

        ((RadioButton)findViewById(R.id.activity_main_tab_history)).setOnCheckedChangeListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private  void setFirstFragment(Intent intent, PortBaseFragment defaultFragment){
        Bundle bundle = intent.getExtras();
        if(bundle!=null){
            if(bundle.getInt("Entry")==1) {
//                          radiogroup的check会导致oncheckchange调用3次，程序崩溃
//                        ((RadioGroup)findViewById(R.id.activity_main_tabs)).check(R.id.activity_main_tab_message);
                ((RadioButton) findViewById(R.id.activity_main_tab_message)).setChecked(true);
                return;
            }
        }
        if(defaultFragment!=null) {
            defaultFragment.showFramegment(this, getFragmentManager(), getFragments(), R.id.activity_main_tabcontent, defaultFragment);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent!=null&&PORT_ACTION_CALL.equals(intent.getAction())){
            PortBaseFragment fragment = (PortBaseFragment) getFragments().get(R.id.activity_main_tab_numbers);
            Bundle extra = new Bundle();
            extra.putParcelable(PORT_ACTION_CALL,intent.getData());
            fragment.setArguments(-1, R.id.activity_main_tabcontent, R.id.activity_main_tab_numbers, false,extra);
            setFirstFragment(intent,fragment);
        }else {
            setFirstFragment(intent,null);
        }

    }

    int presenceSize =0;
    OnKeyBackListener onKeyBackListener = null;

    public void setOnKeyBackListener(OnKeyBackListener onKeyBackListener) {
        this.onKeyBackListener = onKeyBackListener;
    }


    PortBaseFragment getMainFragment(@IdRes int fragmentid,Bundle extra){
        PortBaseFragment fragment = (PortBaseFragment) getFragments().get(fragmentid);
        if(fragment==null) {
            switch (fragmentid) {
                case R.id.activity_main_tab_numbers:
                    fragment = new ActivityMainNumpadFragment();
                    break;
                case R.id.activity_main_tab_contacts:
                    fragment = new ActivityMainContactFavoriteFragment();
                    break;
                case R.id.activity_main_tab_message:
                    fragment = new ActivityMainMessageCursorFragment();
                    break;
                case R.id.activity_main_tab_setting:
                    fragment = new ActivityMainSettingFragment();
                    break;
                case R.id.activity_main_tab_history:
                    fragment = new ActivityMainHistoryCursorFragment();
                    break;
            }
            if(extra==null) {
                fragment.setArguments(-1, R.id.activity_main_tabcontent, fragmentid, false);
            }else{
                fragment.setArguments(-1, R.id.activity_main_tabcontent, fragmentid, false,extra);
            }
            getFragments().put(fragment.getFragmentId(), fragment);
        }
        return fragment;
    }
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        UserAccount account = AccountManager.getDefaultUser(this);
        String where;
        if (account == null) {
            return null;
        }
        switch (i) {
            case LOADER_UNREDAD_MESSAGES:
                where = DBHelperBase.ChatSessionColumns.SESSION_LOCAL_URI + "=? AND "
                        + DBHelperBase.ChatSessionColumns.SESSION_UNREAD + ">0 AND "
                        + DBHelperBase.ChatSessionColumns.SESSION_DELETE + "=0";
                return new CursorLoader(this, DBHelperBase.ChatSessionColumns.CONTENT_URI,
                        new String[]{"sum("+DBHelperBase.ChatSessionColumns.SESSION_UNREAD+")"}, where, new String[]{account.getFullAccountReamName()}, null);
            case LOADER_UNREAD_CALL:
                String local = account.getFullAccountReamName();
                final String constrain = DBHelperBase.HistoryColumns.HISTORY_CONNECTED+"=0 AND "
                        +DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=? AND "+DBHelperBase.HistoryColumns.HISTORY_SEEN+"=0 AND "
                        +DBHelperBase.HistoryColumns.HISTORY_CALLOUT+"=0";
                return new CursorLoader(this, DBHelperBase.ViewHistoryColumns.CONTENT_URI,
                        new String[]{"count(*)"}, constrain, new String[]{local}, DBHelperBase.ViewHistoryColumns.DEFAULT_ORDER);

             case  LOADER_PRESENCE_MESSAGES:
                 where = DBHelperBase.SubscribeColumns.SUBSCRIB_ACCTION + " =? AND "+
                         DBHelperBase.SubscribeColumns.SUBSCRIB_SEEN+"=?";
                 return new CursorLoader(this, DBHelperBase.SubscribeColumns.CONTENT_URI,
                         null, where, new String[]{"" + DBHelperBase.SubscribeColumns.ACTION_NONE
                         ,"" +DBHelperBase.SubscribeColumns.UN_SEEN}, null);
            default:
                break;
        }
        return null;
    }

    int unreadMessages = 0;
    int unreadCall = 0;
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int totalSize= 0;
        RadioButton tabMessage = null;
        switch (loader.getId()) {
            case LOADER_UNREDAD_MESSAGES:
                unreadMessages =0;
                if(CursorHelper.moveCursorToFirst(cursor)) {
                    unreadMessages += cursor.getInt(0);
                }
                tabMessage = (RadioButton) findViewById(R.id.activity_main_tab_message);
                totalSize = presenceSize + unreadMessages;
                updateTipsCount(tabMessage, getResources().getDrawable(R.drawable.tab_message_selector),totalSize);
                break;

            case LOADER_UNREAD_CALL:
                RadioButton tabHistory = (RadioButton) findViewById(R.id.activity_main_tab_history);
                unreadCall = 0;
                if(CursorHelper.moveCursorToFirst(cursor)) {
                    unreadCall = cursor.getInt(0);
                }
                updateTipsCount(tabHistory,getResources().getDrawable(R.drawable.tab_history_selector),unreadCall);
                break;
            case LOADER_PRESENCE_MESSAGES:

                presenceSize = CursorHelper.getCount(cursor);

                tabMessage = (RadioButton) findViewById(R.id.activity_main_tab_message);
                totalSize = presenceSize + unreadMessages;
                updateTipsCount(tabMessage,getResources().getDrawable(R.drawable.tab_message_selector), totalSize);

                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_UNREDAD_MESSAGES:
                unreadMessages = 0;
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
        Map<Integer,Fragment>  fragments = getFragments();
        PortBaseFragment fragment =getMainFragment(i,null);

        switch (i){
            case R.id.activity_main_tab_numbers:
                fragment.showFramegment(this,getFragmentManager(),fragments,R.id.activity_main_tabcontent,fragment);
                break;
            case R.id.activity_main_tab_contacts:
                fragment.showFramegment(this,getFragmentManager(),fragments,R.id.activity_main_tabcontent,fragment);
                break;
            case R.id.activity_main_tab_message:
                fragment.showFramegment(this,getFragmentManager(),fragments,R.id.activity_main_tabcontent,fragment);
                NotificationUtils.getInstance(this).cancelMessageNotification(this);
                break;
            case R.id.activity_main_tab_setting:
                fragment.showFramegment(this,getFragmentManager(),fragments,R.id.activity_main_tabcontent,fragment);
                break;
            case R.id.activity_main_tab_history:
                fragment.showFramegment(this,getFragmentManager(),fragments,R.id.activity_main_tabcontent,fragment);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Map<Integer,Fragment>  fragments = getFragments();
        int checkedId = ((RadioGroup)findViewById(R.id.activity_main_tabs)).getCheckedRadioButtonId();
        PortBaseFragment fragment = (PortBaseFragment) fragments.get(checkedId);
        if(fragment!=null){
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

//    LicenseChecker lvl;

    @Override
    protected void  onDestroy(){
        super.onDestroy();
        getFragments().clear();
    }
//
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Map<Integer,Fragment>  fragments = getFragments();
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            //if(event.getRepeatCount() == 0&&event.getAction()==KeyEvent.ACTION_DOWN) {
            if(event.getAction()==KeyEvent.ACTION_DOWN) {
                if (onKeyBackListener != null && onKeyBackListener.onKeyBackPressed(getFragmentManager(), fragments, null)) {
                    return true;
                }

                boolean agentSupportPush= mSipMgr.svrEngentsupportPush();
                if(mCallMgr.getCallsSize()>0){
                    PortSipCall call = mCallMgr.getDefaultCall();
                    if(call!=null) {
                        mApp.startAVActivity( call.getCallId());
                    }
                }else {
                    AccountManager accountManager = AccountManager.getInstance();
                    if (BuildConfig.SUPPORTPUSH&&agentSupportPush&&accountManager.isTokenRefresh()) {
                        mApp.closeActivitys();
                        String pakageName = getPackageName();
                        AccountManager.getInstance().unregister(mNetworkMgr, AccountManager.PUSH, pakageName);
                        stopService(new Intent(this, PortSipService.class));
                        NotificationUtils.getInstance(this).cancelAllNotification(this);
                        SharedPreferences sharedPreferences = getSharedPreferences("", Context.MODE_PRIVATE);
                        sharedPreferences.edit().putString("svrip","").commit();
                    } else {

                        if (!PermissionManager.testPowerSavePermissions(this)) {
                            PermissionManager.startPowerSavePermissions(this);
                        }else {
                            mApp.closeActivitys();
                            performHomeClick();
                        }
                        return true;
                    }
                }

            }
            return true;
        }
        return super.dispatchKeyEvent(event);

    }
    private void performHomeClick(){
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(!b&& unreadCall>0){
            ContentValues values = new ContentValues(1);
            values.put(DBHelperBase.HistoryColumns.HISTORY_SEEN,1);
            UserAccount account = AccountManager.getDefaultUser(this);
            if (account != null) {
                String local = account.getFullAccountReamName();
                final String selection = DBHelperBase.HistoryColumns.HISTORY_CONNECTED + "=0 AND "
                        + DBHelperBase.HistoryColumns.HISTORY_LOCAL + "=? AND " + DBHelperBase.HistoryColumns.HISTORY_SEEN + "=0 AND "
                        + DBHelperBase.HistoryColumns.HISTORY_CALLOUT+"=0";
                getContentResolver().update(DBHelperBase.HistoryColumns.CONTENT_URI, values, selection, new String[]{local});
            }
        }
    }

    interface OnKeyBackListener{
        boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result);
    }


    class PresenceCallback implements LoaderManager.LoaderCallbacks<CursorJoiner4Loader>{

        @Override
        public Loader<CursorJoiner4Loader> onCreateLoader(int i, Bundle bundle) {
            String where = android.provider.ContactsContract.Data.MIMETYPE + "='"
                    + android.provider.ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE +
                    "' AND (" + android.provider.ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
                    + " = " + ContactsContract.Groups.subScribeGroupId+
                    " OR " + android.provider.ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
                    + " = " + ContactsContract.Groups.sysDeleteGroupId+")";//尚未被处理

            String sortLeft =ContactsContract.Contacts._ID+" ASC",sortRight=ContactsContract.Data.CONTACT_ID+" ASC";
            return new CursorJoinerLoader(PortActivityMain.this,com.portgo.androidcontacts.ContactsContract.Contacts.CONTENT_URI,
                    null,null,null, sortLeft, com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI,
                    new String[]{android.provider.ContactsContract.Data.CONTACT_ID}, where, null, sortRight,new String[] { ContactsContract.Contacts._ID },
                    new String[] { ContactsContract.Data.CONTACT_ID },new JoinIntegerIdCompare());
        }

        @Override
        public void onLoadFinished(Loader<CursorJoiner4Loader> loader, CursorJoiner4Loader joiner4Loader) {
            PortCursorJoiner joiner = joiner4Loader.getJoiner();
//            Cursor left = joiner4Loader.getCursorLeft();
//            String local = baseActivity.mAccountMgr.getDefaultUser(baseActivity).getFullAccountReamName();
//            List<MessageEventForList> events = new ArrayList<>();
            presenceSize = 0;
//            for (CursorJoiner.Result joinerResult : joiner) {
            while (joiner.hasNext()){
                PortCursorJoiner.Result joinerResult = joiner.next();
                switch (joinerResult) {
                    case LEFT:
                        presenceSize++;
                        // handle case where a row in cursorA is unique
                        break;
                    case RIGHT:
//                        PortApplication.getLogUtils().d(String"");
                        // handle case where a row in cursorB is unique
                        break;
                    case BOTH:

                        break;
                }
            }

            RadioButton tabMessage = (RadioButton) findViewById(R.id.activity_main_tab_message);
            int totalSize=presenceSize+unreadMessages;
            updateTipsCount(tabMessage,getResources().getDrawable(R.drawable.tab_message_selector),totalSize);

        }

        @Override
        public void onLoaderReset(Loader<CursorJoiner4Loader> loader) {

        }
    }


    private void updateTipsCount(RadioButton tab,Drawable drawable ,int count){

        int paddingTop = (int) getResources().getDimension(R.dimen.tab_paddingTop);
        LayerDrawable layerDrawable;

        if(count>0) {
            Drawable[] layers = new Drawable[2];
            int textSize = (int) getResources().getDimension(R.dimen.textsize_unsee);
            int drawableWidth = (int) getResources().getDimension(R.dimen.message_num_tips_size);
            int drawableHeight = (int) getResources().getDimension(R.dimen.message_num_tips_size);
            TextDrawable unseeNumDrawable = TextDrawable.builder().beginConfig()
                    .textColor(Color.WHITE)
                    .useFont(Typeface.SERIF)
                    .fontSize(textSize)
                    .toUpperCase()
                    .height(drawableWidth)
                    .width(drawableHeight)
                    .endConfig()
                    .buildRound("" + (count>99?"..":count), getResources().getColor(R.color.portgo_color_red));
//            layers[0] = drawables[1];
            layers[0] = drawable;
            layers[1] = unseeNumDrawable;
            layerDrawable = new LayerDrawable(layers);


            int l_inset = (int) getResources().getDimension(R.dimen.message_num_tips_l);
            int t_inset = (int) getResources().getDimension(R.dimen.message_num_tips_t);
            int r_inset = (int) getResources().getDimension(R.dimen.message_num_tips_r);
            int b_inset = (int) getResources().getDimension(R.dimen.message_num_tips_b);
            layerDrawable.setLayerInset(1,l_inset,t_inset,r_inset,b_inset);
            layerDrawable.setLayerInset(0,0,paddingTop,0,0);
        }else{
            Drawable[] layers = new Drawable[1];
//            layers[0] = drawables[1];
            layers[0] = drawable;
            layerDrawable = new LayerDrawable(layers);
            layerDrawable.setLayerInset(0,0,paddingTop,0,0);
        }
        tab.setCompoundDrawablesWithIntrinsicBounds(null,layerDrawable,null,null);
    }
}