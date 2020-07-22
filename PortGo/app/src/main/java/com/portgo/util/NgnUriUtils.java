/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.util;


import android.content.Context;
import android.text.TextUtils;

import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NgnUriUtils {
	private String mHead;
	private String mUserName;
	private String mRealm;
	private String mPort;

	private final static long MAX_PHONE_NUMBER = 1000000000000L;
	private final static String INVALID_SIP_URI = "sip:invalid@open-ims.test";
	static final String SIP_ADDRRE_PATTERN = "(^(sip+(s)?+:))?(\\+)?[a-zA-Z0-9]+([_\\.-][a-zA-Z0-9]+)*@([a-zA-Z0-9]+([\\.-][a-zA-Z0-9]+)*)+(:[0-9]{2,5})?$";
    static final String SIP_NAME_PATTERN = "(^(sip[s]?:))?.+(@([a-zA-Z0-9]+([\\.-][a-zA-Z0-9]+)*)+(:[0-9]{2,5})?$)";
	static final String SIP_ADDRRE_PATTERN2 = "(^(sip+(s)?+:))?(\\+)?[a-zA-Z0-9]+([_\\.-][a-zA-Z0-9]+)*";
	static final String SIP_MSG_PATTERN = "^(\\+)?[a-z0-9]+([_\\.-][a-zA-Z0-9]+)@([a-zA-Z0-9]+([\\.-][a-zA-Z0-9]+)*)$";
	static final String SIP_SDDRRE_REALM = "@([a-zA-Z0-9]+([\\.-][a-zA-Z0-9]+)*)+";
    static final String SIP_PHONE_REALM = "[0-9\\+\\-\\(\\)]+";
    public static final String SIP_HEADER = "(^(sip+(s)?+:))";
    static final String SIP_TAIL = "@([a-zA-Z0-9]+([\\.-][a-zA-Z0-9]+)*)+(:[0-9]{2,5})?$";
	static final String WEBURI = "[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62}|(:([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]{1}|6553[0-5])$))+\\.?";
    public static final String SIP_CHAR = "[a-zA-Z0-9\\@\\:\\.\\+\\*\\#\\-]";

	boolean parseUriString(String uri){
		mHead =null;
		mPort = "";
		if(isValidUriString(uri)){
			if(uri.matches(SIP_HEADER)){
				mHead = uri.substring(0,uri.indexOf(":"));
				uri =uri.replace(mHead,"");
			}
			String[] group= uri.split("@");
			mUserName = group[0];
			if(group.length>1) {
				uri = group[1];
				group = uri.split(":");
				mRealm = group[0];
				if (group.length > 1) {
					mPort = group[1];
				}
			}
			return true;
		}
		return false;
	}

	public static URL getUrl(String remoteLoginUrl){
        URL url = null;
        if(Pattern.matches(WEBURI,remoteLoginUrl)){
            try {
                url = new URL("http://"+remoteLoginUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }
		return url;
	}

	public static String getDisplayName(String uri,Context context) {

		String displayname = null;

		if (uri != null) {
            String userName= getUserName(uri);
			Contact contact = ContactManager.getContactByPhoneNumber(context,uri);
            if(contact==null&&!uri.equals(userName)){
                contact = ContactManager.getContactByPhoneNumber(context,userName);
            }
			if (contact != null
					&& !NgnStringUtils.isNullOrEmpty(contact.getDisplayName())) {
				displayname = contact.getDisplayName();
			}
			return displayname == null ? userName : displayname;
		}
		return uri;
	}
	

	
	public static String getRealmName(String validUri){
		if(validUri==null)
			return null;

		if(Pattern.matches(SIP_NAME_PATTERN, validUri)){				//(head?)+user name+realm(+?port)
			Pattern pattern = Pattern.compile(SIP_SDDRRE_REALM);
			Matcher matches = pattern.matcher(validUri);
			if(matches.find()){
				String remoteString = matches.group();
				remoteString = remoteString.replaceAll("@", "");
				return remoteString;
			}
		}else{
            return null;
		}
		return null;//illegal
	}

    public static String getUserName(String validUri){
        String result = validUri;
        if(Pattern.matches(SIP_NAME_PATTERN, validUri)){
            result = validUri.replaceFirst(SIP_HEADER,"").replaceAll(SIP_TAIL,"");
        }
        return result;
    }

	public static String getFormatUrif4Msg(String str_uri,String realm){// format the uri like 101@realm
		if(str_uri==null)
			return null;

		if(Pattern.matches(SIP_NAME_PATTERN,str_uri)){
			return str_uri;
		}else{
            return str_uri+"@"+realm;
		}
	}
	
	public static boolean isValidUriString(String uri){
		if(TextUtils.isEmpty(uri))
			return false;
        return Pattern.matches(SIP_ADDRRE_PATTERN, uri) || Pattern.matches(SIP_ADDRRE_PATTERN2, uri);
    }



    public static boolean isValidUriChar(String uri){
        if(uri==null)
            return false;
        return Pattern.matches(SIP_CHAR, uri);
    }

    public static String replaceInvalidUriString(String uri){
        String validateUri="";
        validateUri = String.copyValueOf(uri.toCharArray());


        if(Pattern.matches(SIP_PHONE_REALM,uri)){
            uri= uri.replaceAll("[\\-\\(\\)]","");
        }

        return uri.trim();
    }

	public String getPort() {
		return mPort;
	}

	public String getRealm() {
		return mRealm;
	}

	public String getUserName() {
		return mUserName;
	}


	public String getHead() {
		return mHead;
	}
		
}
