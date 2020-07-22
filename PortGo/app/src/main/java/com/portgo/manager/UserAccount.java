package com.portgo.manager;


import android.content.ContentValues;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.portgo.BuildConfig;
import com.portgo.R;

import com.portgo.database.DBHelperBase;
import com.portgo.util.NgnPredicate;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portsip.PortSipEnumDefine;

import java.io.Serializable;
import java.net.URL;

public class UserAccount implements Comparable<UserAccount>,Cloneable,Serializable{
	public static final int INVALIDATE_ID = -1;

	int mId = INVALIDATE_ID;
	String mAccountNumber = "";
	String mPassword= "";
	String mRealm= "";
    int mTransType= PortSipEnumDefine.ENUM_TRANSPORT_UDP;
	private int mPort= 5060;
	String mDisplayName= "";
	String mAuthor= "";
	String mDomain= "";

	String mEmailAddr= "";
	String mEmailPwd = "";

	int mDfault = 0;
    int mRemberMe = 1;

	String fowardto = null;
	int fowardMode = 0;
	int noAnswerTime = 10;
	int mDistrubMode = 0;
    String mVoiceMail= "";

    int mEnableLog= 0;
    int mEnableStun = 0;
    String mStunServer = "";
    int mStunPort = 3478;

	int mPresence;
	String mPresenceStatus;

	public final static int STATE_NOTONLINE = 0;
	public final static int STATE_ONLINE = 1;
	public final static int STATE_LOGIN = 2;

	public final static int FORWARD_CLOSE = 0;
	public final static int FORWARD_BUSY= 1;
	public final static int FORWARD_NOANSWER = 2;
	public final static int FORWARD_ALL = 3;
	int mState = STATE_NOTONLINE;


	public String getUserEmail(){return mEmailAddr;}
    public String getUserEmailPWD(){return mEmailPwd;}


    public void setUserEmail(String email){mEmailAddr = email;}
    public void setUserEmailPWD(String emailPwd){mEmailPwd = emailPwd;}

    public void setEnableLog(boolean enablelog) {
        this.mEnableLog = (enablelog==true?1:0);
    }

    public boolean getEnableLog(){
        return  mEnableLog==1;
    }

    public void setRemberMe(boolean remberMe) {
        this.mRemberMe = (remberMe==true?1:0);
    }

    public boolean getRemberme(){
        return  mRemberMe==1;
    }

    public String getPassword() {
		return mPassword;
	}
	
	public void setPassword(String password) {
		this.mPassword = password;
	}
	
	public String getRealm() {

		return mRealm;
	}
	public String getRealmDefaultDomain() {
		if(!NgnStringUtils.isNullOrEmpty(mDomain))
			return mDomain;
		return mRealm;
	}
	public void setFowardto(String fowardto) {
		this.fowardto = fowardto;
	}
    public void setVoiceMail(String voiceMail) {
        this.mVoiceMail = voiceMail;
    }
	public void setDistrubMode(int distrubMode) {
		this.mDistrubMode = distrubMode;
	}

	public boolean isDistrbEnable(){
		return this.mDistrubMode>0;
	}
    public String getVoiceMail() {
        return mVoiceMail;
    }

    public int getFowardMode() {
		return fowardMode;
	}

	public int getFowardNoAnswerTime() {
		return noAnswerTime;
	}

	public void setFowardNoAnswerTime(int noAnswerTime) {
		this.noAnswerTime =noAnswerTime;
	}
	public void setFowardMode(int fowardMode) {
		this.fowardMode = fowardMode;
	}

	public void setRealm(String realm) {
		URL urlSipserver=null;
		if(!NgnStringUtils.isNullOrEmpty(realm.trim()))
			urlSipserver = NgnUriUtils.getUrl(realm.trim());
		String sipServer="";
        int port=-1;
        if(urlSipserver!=null) {
            sipServer = urlSipserver.getHost();
            port = urlSipserver.getPort();
        }

		this.mRealm = sipServer;
		if(port!=-1){
			this.mPort=port;
		}else{
			this.mPort=5060;
		}
	}
 
	public String getAccountNumber() {
		return mAccountNumber;
	}
	
	public void setAccountNumber(String accountNumber) {
		this.mAccountNumber = accountNumber;
	}
	
	public String getDisplayDefaultAccount() {
		if(TextUtils.isEmpty(mDisplayName)) {
			mDisplayName = mAccountNumber;
		}
		return this.mDisplayName;
	}

	public String getDisplayName() {
		return this.mDisplayName;
	}

	public void setDisplayName(String DisplayName) {
		this.mDisplayName = DisplayName;
	}

	public String getAuthor() {
		return this.mAuthor;
	}
	
	public void setAuthor(String Author) {
		this.mAuthor = Author;
	}
	
	public int getPort() {
		return this.mPort;
	}
    public int getTransType() {  return this.mTransType;}

	public int getPresence() {
		return mPresence;
	}

	public void setPresence(int presence) {
		this.mPresence = presence;
	}

	public String getPresenceStatus() {
		return mPresenceStatus;
	}

	public void setPresenceStatus(String status) {
		this.mPresenceStatus= status;
	}

	private void setPort(int Port) {
		this.mPort = Port;
	}

    public void setTransType(int transType) {
        this.mTransType = transType;
    }

	public int getId() {
		return this.mId;
	}
	
	public void setID(int id) {
		this.mId = id;
	}
	
	public boolean getDefalt() {
		return this.mDfault>0;
	}
	
	public void setDefault(boolean state) {
		this.mDfault= state?1:0;			
	}
	
	public String getDomain() {
		return this.mDomain;
	}
		
	public void setDomain(String Domain) {

        URL urlDomain=null;
        if(!NgnStringUtils.isNullOrEmpty(Domain.trim()))
            urlDomain = NgnUriUtils.getUrl(Domain.trim());
        String sipDomain="";
        int port=-1;
        if(urlDomain!=null) {
            sipDomain = urlDomain.getHost();
            port = urlDomain.getPort();
        }

        this.mDomain = sipDomain;
        if(port!=-1){
            this.mPort=port;
        }
	}

    //like:101@192.168.1.98:5060
    public String getFullAccountReamName() {
        return getAccountNumber()+"@"+getRealmDefaultDomain();
    }

//	public int getFowardMode() {
//		return this.fowardMode;
//	}

	public String getFowardTo() {
		return this.fowardto;
	}

    public int getStunPort(){
        return mStunPort;
    }

    public String getStunServer(){
        return mStunServer;
    }

    public void setEnableStun(boolean enableStun) {
        this.mEnableStun = enableStun?1:0;
    }

    public void setStunPort(int stunPort){
        mStunPort =stunPort;
    }

    public void setStunServer(String  StunServer){
        mStunServer =StunServer;
    }

	protected int setState() {
		return this.mState;
	}

    public int getState() {
        return this.mState;
    }

    public boolean isStunEnable(){
        return mEnableStun==1;
    }

	public boolean isValidate(){
        return !(NgnStringUtils.isNullOrEmpty(mAccountNumber) || NgnStringUtils.isNullOrEmpty(mPassword) || NgnStringUtils.isNullOrEmpty(mDomain) || mPort <= 0 || mPort >= 65535);
    }

	@Override
	public String toString(){
		return mAccountNumber;
	}

	@Override
	public int compareTo(UserAccount another) {
		if (another.mAccountNumber.equals(mAccountNumber)||another.mRealm.equals(mRealm)) {
			return 1;
		}
		return 0;
	}
		
	
	@Override
	public UserAccount clone(){
		UserAccount account = null;
		try {
			account = (UserAccount) super.clone();
			if(this.mAccountNumber!=null) {
				account.setAccountNumber(String.copyValueOf(this.getAccountNumber().toCharArray()));
			}
			if(this.mAuthor!=null)
				account.setAuthor(String.copyValueOf(this.getAuthor().toCharArray()));
			if(this.mDisplayName!=null)
				account.setDisplayName(String.copyValueOf(this.getDisplayName().toCharArray()));
			if(this.mDomain!=null)
				account.setDomain(String.copyValueOf(this.getDomain().toCharArray()));
			if(this.mPassword!=null)
				account.setPassword(String.copyValueOf(this.getPassword().toCharArray()));
            if(this.mStunServer!=null)
                account.setStunServer(String.copyValueOf(this.getStunServer().toCharArray()));
			if(this.mRealm!=null)
				account.setRealm(String.copyValueOf(this.getRealm().toCharArray()));
            if(this.mEmailAddr!=null)
                account.mEmailAddr = String.copyValueOf(this.mEmailAddr.toCharArray());
            if(this.mEmailPwd!=null)
                account.mEmailPwd = String.copyValueOf(this.mEmailPwd.toCharArray());
        } catch (Exception e) {
		}
		return account;
	}
	
	void update(UserAccount user){
		if(user == null||user.getId()!=mId||this==user)
			return;
		if(user.mAccountNumber!=null) {
			setAccountNumber(String.copyValueOf(user.getAccountNumber().toCharArray()));
		}
		if(user.mAuthor!=null)
			setAuthor(String.copyValueOf(user.getAuthor().toCharArray()));
		if(user.mDisplayName!=null)
			setDisplayName(String.copyValueOf(user.getDisplayName().toCharArray()));
		if(user.mDomain!=null)
			setDomain(String.copyValueOf(user.getDomain().toCharArray()));
		if(user.mPassword!=null)
			setPassword(String.copyValueOf(user.getPassword().toCharArray()));
		if(user.mRealm!=null)
			setRealm(String.copyValueOf(user.getRealm().toCharArray()));
        if(user.mStunServer!=null)
            setStunServer(String.copyValueOf(user.getStunServer().toCharArray()));
		this.mDfault = user.mDfault;
        this.mTransType = user.mTransType;
        this.mStunPort = user.mStunPort;
        this.mPort = user.mPort;
        this.mEnableStun = user.mEnableStun;
        this.mEnableLog = user.mEnableLog;
		this.mRemberMe = user.mRemberMe;
	
	}
	
	public int getPresenceCommandRes(){
		int codeRes = R.string.cmd_presence_offline;
		switch (mPresence){
			case ContactsContract.StatusUpdates.AVAILABLE:
				codeRes = R.string.cmd_presence_online;
				break;
			case ContactsContract.StatusUpdates.AWAY:
				codeRes = R.string.cmd_presence_away;
				break;
			case ContactsContract.StatusUpdates.DO_NOT_DISTURB:
				codeRes = R.string.cmd_presence_nodisturb;
				break;
			case ContactsContract.StatusUpdates.INVISIBLE:
				codeRes = R.string.cmd_presence_busy;
				break;
			case ContactsContract.StatusUpdates.OFFLINE:
				codeRes = R.string.cmd_presence_offline;
				break;
		}
		return codeRes;
	}

	public ContentValues getContentValue(){
		ContentValues values = new ContentValues();
		values.put(DBHelperBase.AccountColumns.ACCOUNT_NAME, this.getAccountNumber());            //dengluming ,zhang hao
		values.put(DBHelperBase.AccountColumns.ACCOUNT_DISPLAYNAME, this.getDisplayName());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_PASSWORD,this.getPassword());//
		values.put(DBHelperBase.AccountColumns.ACCOUNT_REALM,this.getRealm());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_TRANS_TYPE, this.getTransType());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_PORT,  this.getPort());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_AUTHOR, this.getAuthor());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_DOMAIN, this.getDomain());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_DEFAULT,this.getDefalt());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_REMBER_PASSWORD, this.getRemberme()?1:0);
		values.put(DBHelperBase.AccountColumns.ACCOUNT_LOG_ENABLE,this.getEnableLog()?1:0);
		values.put(DBHelperBase.AccountColumns.ACCOUNT_STUN_ENABLE,this.isStunEnable()?1:0);
		values.put(DBHelperBase.AccountColumns.ACCOUNT_STUN_SERVER,this.getStunServer());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_STUN_PORT,this.getStunPort());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_MODE,this.getFowardMode());
		values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_TIME,this.getFowardNoAnswerTime());


		values.put(DBHelperBase.AccountColumns.ACCOUNT_FOWARDTO,this.getFowardTo());
        values.put(DBHelperBase.AccountColumns.ACCOUNT_VOICEMAIL,this.getVoiceMail());

        values.put(DBHelperBase.AccountColumns.ACCOUNT_EMAIL,this.mEmailAddr);
        values.put(DBHelperBase.AccountColumns.ACCOUNT_EMAIL_PWD,this.mEmailPwd);
		return values;
	}
	public static UserAccount userAccountFromCursor(Cursor cursor){
			UserAccount user = new UserAccount();

            int ACCOUNT_ID = cursor.getColumnIndex(DBHelperBase.AccountColumns._ID);
			int ACCOUNT_NAME = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_NAME);
			int ACCOUNT_PASSWORD = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_PASSWORD);
			int ACCOUNT_DISPLAYNAME = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_DISPLAYNAME);
			int ACCOUNT_REALM = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_REALM);
			int ACCOUNT_TRANS_TYPE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_TRANS_TYPE);
			int ACCOUNT_PORT = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_PORT);
			int ACCOUNT_STUN_SERVER = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_STUN_SERVER);
			int ACCOUNT_STUN_PORT = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_STUN_PORT);
			int ACCOUNT_STUN_ENABLE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_STUN_ENABLE);
			int ACCOUNT_LOG_ENABLE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_LOG_ENABLE);
			int ACCOUNT_DOMAIN = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_DOMAIN);
			int ACCOUNT_REMBER_PASSWORD = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_REMBER_PASSWORD);
			int ACCOUNT_AUTHOR = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_AUTHOR);
			int ACCOUNT_DEFAULT = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_DEFAULT);

            int ACCOUNT_PRESENCE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE);
            int ACCOUNT_PRESENCE_STATUS = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_PRESENCE_STATUS);

			int ACCOUNT_FOWARD_MODE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_MODE);
			int ACCOUNT_FOWARD_TIME = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_FOWARD_TIME);
			int ACCOUNT_FOWARDTO = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_FOWARDTO);
			int ACCOUNT_DISTRUB_MODE = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_DISTRUB_MODE);
            int ACCOUNT_VOICEMAIL = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_VOICEMAIL);


        int ACCOUNT_EMAIL = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_EMAIL);
        int ACCOUNT_EMAIL_PWD = cursor.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_EMAIL_PWD);

			String name="",disname="",pwd="",realm="",stunsvr="",domain="",author="";
			int remeberme=0,logenable=0,stunenable=0,port=5060,stunport=5060,transtype=0,id=INVALIDATE_ID,defaultUser =0;
			if(ACCOUNT_NAME>=0)
				name = cursor.getString(ACCOUNT_NAME);
			if(ACCOUNT_PASSWORD>=0)
				pwd = cursor.getString(ACCOUNT_PASSWORD);
			if(ACCOUNT_DISPLAYNAME>=0)
				disname = cursor.getString(ACCOUNT_DISPLAYNAME);
			if(ACCOUNT_REALM>=0)
				realm = cursor.getString(ACCOUNT_REALM);
			if(ACCOUNT_TRANS_TYPE>=0)
				transtype =cursor.getInt(ACCOUNT_TRANS_TYPE);
			if(ACCOUNT_PORT>=0)
				port = cursor.getInt(ACCOUNT_PORT);
			if(ACCOUNT_STUN_SERVER>=0)
				stunsvr = cursor.getString(ACCOUNT_STUN_SERVER);
			if(ACCOUNT_STUN_PORT>=0)
				stunport = cursor.getInt(ACCOUNT_STUN_PORT);
			if(ACCOUNT_STUN_ENABLE>=0)
				stunenable = cursor.getInt(ACCOUNT_STUN_ENABLE);
			if(ACCOUNT_LOG_ENABLE>=0)
				logenable = cursor.getInt(ACCOUNT_LOG_ENABLE);
			if(ACCOUNT_DOMAIN>=0)
				domain = cursor.getString(ACCOUNT_DOMAIN);
			if(ACCOUNT_REMBER_PASSWORD>=0)
				remeberme = cursor.getInt(ACCOUNT_REMBER_PASSWORD);
			if(ACCOUNT_AUTHOR>=0)
				author = cursor.getString(ACCOUNT_AUTHOR);
            if(ACCOUNT_ID>=0)
                id = cursor.getInt(ACCOUNT_ID);
			if(ACCOUNT_ID>=0)
				defaultUser= cursor.getInt(ACCOUNT_DEFAULT);
            if(ACCOUNT_PRESENCE>=0)
                user.setPresence(cursor.getInt(ACCOUNT_PRESENCE));
            if(ACCOUNT_PRESENCE_STATUS>=0)
                user.setPresenceStatus(cursor.getString(ACCOUNT_PRESENCE_STATUS));

        if(ACCOUNT_EMAIL>=0)
            user.mEmailAddr = cursor.getString(ACCOUNT_EMAIL);

        if(ACCOUNT_EMAIL_PWD>=0)
            user.mEmailPwd = cursor.getString(ACCOUNT_EMAIL_PWD);

		if(ACCOUNT_FOWARD_MODE>=0)
			user.setFowardMode(cursor.getInt(ACCOUNT_FOWARD_MODE));

		if(ACCOUNT_FOWARD_TIME>=0)
			user.setFowardNoAnswerTime(cursor.getInt(ACCOUNT_FOWARD_TIME));
		if(ACCOUNT_FOWARDTO>=0)
			user.setFowardto(cursor.getString(ACCOUNT_FOWARDTO));
        if(ACCOUNT_VOICEMAIL>=0)
            user.setVoiceMail(cursor.getString(ACCOUNT_VOICEMAIL));

		if(ACCOUNT_DISTRUB_MODE>=0)
			user.setDistrubMode(cursor.getInt(ACCOUNT_DISTRUB_MODE));

            user.setID(id);
			user.setAccountNumber(name);//
			user.setPassword(pwd);
			user.setDisplayName(disname);//
			user.setRealm(realm);
			user.setTransType(transtype);
			user.setPort(port);
			user.setStunServer(stunsvr);
			user.setStunPort(stunport);
			user.setEnableStun(stunenable>0);
			user.setEnableLog(logenable>0);
			user.setDomain(domain);

			user.setRemberMe(remeberme>0);
			user.setAuthor(author);
			user.setDefault(defaultUser>0);//
		return user;
	}
}
