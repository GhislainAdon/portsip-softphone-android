package com.portgo.manager;

import android.content.Context;

public interface ICustomUserInfo {
	boolean getOptionalUserInfo(AccountManager accountManager, UserAccount user);
	boolean getRequestUserInfo(Context context, UserAccount user);
	boolean stopGetInfo(UserAccount user);
	
}
