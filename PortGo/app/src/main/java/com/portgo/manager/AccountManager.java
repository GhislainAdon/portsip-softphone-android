package com.portgo.manager;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.util.NgnObservableList;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.UUID;

public class AccountManager extends Observable{


	public static String KEY_DIS="display_name";
	public static String KEY_NAME="extension_number";
	public static String KEY_PWD="extension_password";
	public static String KEY_WEB_PWD="web_access_password";
	public static String KEY_DOMAIN="sip_domain";
	public static String KEY_TRANSPORT="transports";
	public static String KEY_VOICE_MAIL="voicemail_number";
	public static String KEY_MAIL="email";
	public static String KEY_SVR_PUBLIC ="pbx_public_ip";
	public static String KEY_SVR_PRIVATE ="pbx_private_ip";
	public static String KEY_TRANS_PORT="port";
	public static String KEY_TRANS_PROTOCOL ="protocol";

	public static String KEY_SVR_OUTBOUND ="outbound_proxy";

	UserAccountList mAccountList = new UserAccountList();
	static int mLocalPort = new Random().nextInt(7000)+8000;
	String mStatus;
    public static final int NETWORK_UNAVAILABLE = 0;//

    public static final int ACCOUNT_UNAVAILABLE= 1;//
    public static final int PUSH= 2;//
    public static final int CORE_ERROR=3;//init
    public static final int NETWORK_FORBIDDEN = 4;//
	public static final int PERSSION_ERROR = 5;//
    public static final int EXIT = 10;//


	static  AccountManager instance = new AccountManager();
	volatile int mState;
    private AccountManager(){
    }
    static public AccountManager getInstance(){
    	return instance;
	}

    public int getOfflineReason() {
        return mOffline_reason;
    }

	public	boolean isAutoUnregiest(){
		return (mOffline_reason == NETWORK_UNAVAILABLE);
	}

	static public UserAccount getDefaultUser(Context context) {
		UserAccount user = null;
		ContentResolver resolver = context.getContentResolver();
		String selection = DBHelperBase.AccountColumns.ACCOUNT_DEFAULT +"=1";
		Cursor cursor = CursorHelper.resolverQuery(resolver,DBHelperBase.AccountColumns.CONTENT_URI,null,selection,null,null);
		if(CursorHelper.moveCursorToFirst(cursor)){
			user = UserAccount.userAccountFromCursor(cursor);
		}

		CursorHelper.closeCursor(cursor);
		return user;
	}

	static public boolean insertUser(Context context,UserAccount userAccount)
	{
		if(userAccount==null)
			return false;
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = userAccount.getContentValue();
//        values.put(DBHelperBase.AccountColumns.ACCOUNT_LAST_TIME_LOGIN,new .currentTimeMillis());
		UserAccount user = null;

		String selection = DBHelperBase.AccountColumns.ACCOUNT_NAME +"=? AND "+DBHelperBase.AccountColumns.ACCOUNT_DOMAIN
				+"=? AND "+DBHelperBase.AccountColumns.ACCOUNT_REALM +"=? AND "+DBHelperBase.AccountColumns.ACCOUNT_PORT +"=? ";
		String[] Args= new String[]{userAccount.getAccountNumber(),userAccount.getDomain(),userAccount.getRealm(),""+userAccount.getPort()};
		Cursor cursor = CursorHelper.resolverQuery(resolver,DBHelperBase.AccountColumns.CONTENT_URI,null,selection,Args,null);
		if(CursorHelper.moveCursorToFirst(cursor)){
			user = UserAccount.userAccountFromCursor(cursor);
		}
		CursorHelper.closeCursor(cursor);


		try {
            if(user!=null){
                userAccount.setID(user.getId());
                updateUser(context,userAccount,false);
            }else {
                Uri inserturi = resolver.insert(DBHelperBase.AccountColumns.CONTENT_URI, values);
                long id = ContentUris.parseId(inserturi);
                userAccount.setID((int)id);
            }

		}catch (Exception ex)
		{
			return false;
		}
		return true;
	}
	static public Bitmap getAccountAvarta(Context context,int accountId){
		Bitmap bitmap = null;
		ContentResolver resolver = context.getContentResolver();
		Cursor cursor = CursorHelper.resolverQuery(resolver,DBHelperBase.AccountColumns.CONTENT_URI,new String[]{DBHelperBase.AccountColumns.ACCOUNT_AVATAR},
				DBHelperBase.AccountColumns._ID+" = "+accountId,null,null);
		while (CursorHelper.moveCursorToNext(cursor)){
			byte[] avatarData = cursor.getBlob(cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_AVATAR));
			if(avatarData!=null){
				bitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, null);
				break;
			}
		}
		CursorHelper.closeCursor(cursor);
		return bitmap;
	}

	static public void updateMailSize(Context context,long accountID,int mailSize){
		Uri userUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,accountID);
		ContentValues contentValues = new ContentValues(1);
		contentValues.put(DBHelperBase.AccountColumns.ACCOUNT_MAILSIZE,mailSize);
		context.getContentResolver().update(userUri,contentValues,null,null);
	}

	static public void saveAccountAvarta(Context context,int accountId, Bitmap bitmap){
		ContentResolver resolver = context.getContentResolver();
		byte[] avatarData =null;
		if(bitmap!=null){
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
			avatarData = os.toByteArray();
		}
		ContentValues values = new ContentValues();
		values.put(DBHelperBase.AccountColumns.ACCOUNT_AVATAR,avatarData);
		Uri uri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,accountId);
		int count = resolver.update(uri,values,null,null);

	}

	static public boolean updateUser(Context context,UserAccount userAccount,boolean withPresence){
		if(userAccount==null||userAccount.getId() == UserAccount.INVALIDATE_ID)
			return false;
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = userAccount.getContentValue();
		if(withPresence){
			values.put(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE,userAccount.getPresence());
			values.put(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE_STATUS,userAccount.getPresenceStatus());
		}
		try {
			Uri uri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,userAccount.getId());
			resolver.update(uri,values,null,null);
		}catch (Exception ex)
		{
			return false;
		}
		return true;
	}

	static public boolean updateUser(Context context, UserAccount userAccount, JSONObject jsonObject,boolean withPresence) throws JSONException,NullPointerException{
		if(userAccount==null||userAccount.getId() == UserAccount.INVALIDATE_ID)
			return false;
		String access_token="";
		String extension_number="";
		String extension_password="";
		String api_version="";
		String domain="";
		String role="";
		int expires=30;

		extension_number = jsonObject.getString("Extension");
		extension_password = jsonObject.getString("SipPassword");

//		domain = jsonObject.getString("domian");

		role = jsonObject.getString("Role");
//		expires = jsonObject.getInt("expires");
//		access_token = jsonObject.getString("access_token");
//		api_version = jsonObject.getString("api_version");

		userAccount.setAccountNumber(extension_number);
		userAccount.setPassword(extension_password);
		userAccount.setDomain(domain);


		ContentResolver resolver = context.getContentResolver();
		ContentValues values = userAccount.getContentValue();
		if(withPresence){
			values.put(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE,userAccount.getPresence());
			values.put(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE_STATUS,userAccount.getPresenceStatus());
		}
		try {
			Uri uri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,userAccount.getId());
			resolver.update(uri,values,null,null);
		}catch (Exception ex)
		{
			return false;
		}
		return true;
	}

	static public boolean deleteUser(Context context,UserAccount userAccount)
	{
		ContentResolver resolver = context.getContentResolver();
		if(userAccount.getId() != UserAccount.INVALIDATE_ID) {
			Uri uri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,userAccount.getId());
			try {
				resolver.delete(uri,null,null);
			}catch (Exception ex){
				return false;
			}
		}
		return true;
	}

	public List<UserAccount> getAccounts() {
		NgnObservableList<UserAccount> obList = getObservableUsers();
		if(obList!=null){
			return obList.getList();
		}
		return null;
	}

	public NgnObservableList<UserAccount> getObservableUsers() {
		if(mAccountList!=null){
			return mAccountList.getList();
		}
		return null;
	}
	
	synchronized public long register(Context context,SipManager sipManager){
		return autoRegister(context,sipManager);
	}

	static public boolean allPermissionGranted(Context context){
		if (!PermissionManager.testDangercePermissions(context,PermissionManager.PORTGO_MUST_PERMISSION)){
			return false;
		}
		return true;
	}

    String pushtoken="";
    public void setTokenRefresh(String token){
		pushtoken = token;
    }


	public boolean isTokenRefresh(){
		return !TextUtils.isEmpty(pushtoken);
	}


	public void enablePBXPush(String appname,boolean enable){
		if(!BuildConfig.SUPPORTPUSH) {
			return;
		}
		PortSipSdkWrapper sdk =PortSipSdkWrapper.getInstance();
		if(TextUtils.isEmpty(pushtoken)){
			enable =false;
			pushtoken ="";
		}

		if (appname.equals("com.portgo")) {
			appname = "com.portsip.portgo";
		}
		String pushMessage;
		if(TextUtils.isEmpty(pushtoken)){
			pushMessage = "device-os=android";
		}else {
			pushMessage = "device-os=android;device-uid=" + pushtoken +
				";allow-call-push=" + enable + ";allow-message-push=" + enable + ";app-id=" + appname;
		}
		sdk.clearAddedSipMessageHeaders();

		//long result = sdk.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage);//old pbx version
		long result = sdk.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage);//new pbx version

	}

	public long autoRegister(Context context,SipManager sipManager){

		UserAccount account = getDefaultUser(context);
		if(!allPermissionGranted(context)){
			setLoginState(UserAccount.STATE_NOTONLINE,PERSSION_ERROR, context.getString(R.string.permission_help_content),true);
			return PortSipErrorcode.ECoreNotRegistered;
		}
		if(account==null||!account.isValidate())
		{
            setLoginState(UserAccount.STATE_NOTONLINE,ACCOUNT_UNAVAILABLE, null,true);
			return PortSipErrorcode.ECoreNotRegistered;
		}

        PortSipSdkWrapper sdk =PortSipSdkWrapper.getInstance();

		sdk.unRegisterServer();
		setLoginState(UserAccount.STATE_LOGIN,null);

		String userDomain = account.getDomain();
		String sipServer = account.getRealm();
        int port = account.getPort();
        URL urlDomain = null;

        if(!NgnStringUtils.isNullOrEmpty(userDomain))
            urlDomain = NgnUriUtils.getUrl(userDomain);

        if(urlDomain!=null) {
            userDomain = urlDomain.getHost();
			if(!TextUtils.isEmpty(sipServer)){//
				port = account.getPort();
			}else{//
				sipServer = userDomain;
				if(port==5060||port<0) {
                    int domainPort = urlDomain.getPort();
                    port = domainPort == -1 ? 5060 : domainPort;
                }
			}
        }

        ConfigurationManager configurationManager  = ConfigurationManager.getInstance();
		NetworkManager networkMgr = NetworkManager.getNetWorkmanager();
		if(networkMgr.acquire(configurationManager)){
			int result = PortSipErrorcode.ECoreNotInitialized;

			result = sipManager.setUserInfo(account.getAccountNumber(), account.getDisplayName(), account.getAuthor(),
				account.getPassword(), userDomain, sipServer, port, account.getStunServer(), account.getStunPort());
			String instance = configurationManager.getStringValue(context,ConfigurationManager.INSTANCE_ID,null);
			if(instance==null){
				instance = UUID.randomUUID().toString();
				configurationManager.setStringValue(context,ConfigurationManager.INSTANCE_ID,instance);
			}
            sdk.setInstanceId(instance);

			boolean presenceAgent= configurationManager.getBooleanValue(context,ConfigurationManager.PRESENCE_AGENT,
					context.getResources().getBoolean(R.bool.presence_agent));
            int pubTime= configurationManager.getIntergerValue(context,ConfigurationManager.PRESENCE_PUB_REFRESH,
                    context.getResources().getInteger(R.integer.presence_pub_time));
            int subTime= configurationManager.getIntergerValue(context,ConfigurationManager.PRESENCE_SUB_REFRESH,
                    context.getResources().getInteger(R.integer.presence_sub_time));
            switch (result) {
                case PortSipErrorcode.ECoreErrorNone: {
                    sdk.setPresenceMode(presenceAgent ? 1 : 0);//0 - P2P mode; 1 - Presence Agent mode.
                    sdk.setDefaultSubscriptionTime(pubTime);
                    sdk.setDefaultPublicationTime(subTime);
                    enablePBXPush(context.getPackageName(), true);
                    result = sdk.registerServer(90, 0);

                }
                break;
                case PortSipErrorcode.ECoreCreateTransportFailure://
                    mLocalPort = new Random().nextInt(7000)+8000;//
                    break;
                case PortSipErrorcode.ECoreNotInitialized:
                    break;
                    default:
                        break;
            }

            sdk.setDoNotDisturb(account.isDistrbEnable());
			if(result!=PortSipErrorcode.ECoreErrorNone){
				setLoginState(UserAccount.STATE_NOTONLINE,CORE_ERROR,null,true);
				return PortSipErrorcode.ECoreNotRegistered; 
			}

			sdk.enableAutoCheckMwi(true);

			int fowardMode= account.getFowardMode();
			String callee = account.getFowardTo();
			if((fowardMode==UserAccount.FORWARD_BUSY||fowardMode==UserAccount.FORWARD_ALL)
					&&!NgnStringUtils.isNullOrEmpty(callee)&& NgnUriUtils.isValidUriString(callee)){
				sdk.enableCallForward(fowardMode==UserAccount.FORWARD_BUSY,callee);
			}else {
				sdk.disableCallForward();
			}
			return result;
		}else {
			setLoginState(UserAccount.STATE_NOTONLINE,NETWORK_UNAVAILABLE,context.getString(R.string.network_not_availabe),true);
			return PortSipErrorcode.ECoreNotRegistered;
		}
	}
	
	public void unregister(NetworkManager networkMgr,int reason,String appName){
        PortSipSdkWrapper sdk = PortSipSdkWrapper.getInstance();
        if(reason==EXIT) {//
			enablePBXPush(appName,false);
        }

        setLoginState(UserAccount.STATE_NOTONLINE,reason,null,true);
        sdk.unRegisterServer();
        networkMgr.release();
	}

	int mOffline_reason = EXIT;
	public void setLoginState(int state,int reason,String detail,boolean notify){
		mState = state;
		mStatus = detail;
        mOffline_reason = reason;
		if(notify){
			setChanged();
			notifyObservers(mStatus);
		}
	}

	public void setLoginState(int state,String detail){
        mState = state;
        mStatus = detail;

        setChanged();
        notifyObservers(mStatus);
    }
	public int getLoginState() {
		return mState;
	}

	public String getLoginStateDetail() {
		return mStatus;
	}

}

