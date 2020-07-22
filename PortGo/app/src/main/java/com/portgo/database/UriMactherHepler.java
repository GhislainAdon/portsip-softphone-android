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
package com.portgo.database;

import android.content.UriMatcher;
import android.os.Build;

import com.portgo.BuildConfig;

public  class UriMactherHepler extends Object {
    private static UriMatcher uriMatcher;
    public static final String AUTHORITY = BuildConfig.PROVIDER_DATA;

    protected static final int MATCH_ACCOUNT = 0X3432;
    protected static final int MATCH_ACCOUNT_ALL = MATCH_ACCOUNT + 1;
    protected static final int MATCH_HISTORY = MATCH_ACCOUNT + 2;
    protected static final int MATCH_HISTORY_ALL = MATCH_ACCOUNT + 3;
    protected static final int MATCH_MESSAGE = MATCH_ACCOUNT + 4;
    protected static final int MATCH_MESSAGE_ALL = MATCH_ACCOUNT + 5;
    protected static final int MATCH_CONTACT_VERSION= MATCH_ACCOUNT + 6;
    protected static final int MATCH_CONTACT_VERSION_ALL = MATCH_ACCOUNT + 7;

    protected static final int MATCH_REMOTE= MATCH_ACCOUNT + 8;
    protected static final int MATCH_REMOTE_ALL = MATCH_ACCOUNT + 9;
    protected static final int MATCH_CHAT_SESSION = MATCH_ACCOUNT + 10;
    protected static final int MATCH_CHAT_SESSION_ALL = MATCH_ACCOUNT + 11;
    protected static final int MATCH_VIEW_CHAT_SESSION = MATCH_ACCOUNT + 12;
    protected static final int MATCH_VIEW_CHAT_SESSION_ALL = MATCH_ACCOUNT + 13;
    protected static final int MATCH_VIEW_HISTORY= MATCH_ACCOUNT + 14;
    protected static final int MATCH_VIEW_HISTORY_ALL= MATCH_ACCOUNT + 15;
    protected static final int MATCH_CALL_RULE= MATCH_ACCOUNT + 16;
    protected static final int MATCH_CALL_RULE_ALL= MATCH_ACCOUNT + 17;
    protected static final int MATCH_RECORD= MATCH_ACCOUNT + 18;
    protected static final int MATCH_RECORD_ALL= MATCH_ACCOUNT + 19;
    protected static final int MATCH_SUBSCRIBE= MATCH_ACCOUNT + 20;
    protected static final int MATCH_SUBSCRIBE_ALL= MATCH_ACCOUNT + 21;

    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_ACCOUNT + "/#", MATCH_ACCOUNT);//
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_ACCOUNT + "/", MATCH_ACCOUNT_ALL);//
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_HISTORY + "/#", MATCH_HISTORY);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_HISTORY + "/", MATCH_HISTORY_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_MESSAGE + "/#", MATCH_MESSAGE);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_MESSAGE + "/", MATCH_MESSAGE_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_MESSAGE + "/", MATCH_MESSAGE_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_REMOTE + "/#", MATCH_REMOTE);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_REMOTE + "/", MATCH_REMOTE_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CHATSESSION + "/#",MATCH_CHAT_SESSION);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CHATSESSION+ "/", MATCH_CHAT_SESSION_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.VIEW_CHATSESSION+ "/#",MATCH_VIEW_CHAT_SESSION);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.VIEW_CHATSESSION+ "/", MATCH_VIEW_CHAT_SESSION_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.VIEW_HISTORY + "/#",MATCH_VIEW_HISTORY);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.VIEW_HISTORY+ "/", MATCH_VIEW_HISTORY_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CONTACT_VERSION + "/#",MATCH_CONTACT_VERSION);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CONTACT_VERSION+ "/", MATCH_CONTACT_VERSION_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CALLRULE + "/#",MATCH_CALL_RULE);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_CALLRULE+ "/", MATCH_CALL_RULE_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_RECORD+ "/#",MATCH_RECORD);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_RECORD+ "/", MATCH_RECORD_ALL);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_SUBSCRIB+ "/#",MATCH_SUBSCRIBE);
        uriMatcher.addURI(AUTHORITY, DBHelperBase.TABLE_SUBSCRIB+ "/", MATCH_SUBSCRIBE_ALL);
    }

    static public UriMatcher getUriMatcher(){
        return uriMatcher;
    }
}
