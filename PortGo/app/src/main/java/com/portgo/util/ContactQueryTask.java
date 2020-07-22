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


import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.database.RemoteRecord;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactDataAdapter;
import com.portgo.manager.ContactManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PermissionManager;

import java.util.ArrayList;
import java.util.List;


public class ContactQueryTask extends AsyncTask {
    @Override
    protected Object doInBackground(Object[] objects) {
        Context context = null;
        Integer remoteId = -14321;//magic
        String remoteUri = null;
        if(objects!=null&&objects.length>=3) {
            if(objects[0] instanceof Context) {
                context = (Context) objects[0];
            }
            if(objects[1] instanceof Integer) {
                remoteId = (Integer) objects[1];
            }
            if(objects[2] instanceof String) {
                remoteUri = (String) objects[2];
            }
        }
        if(context==null||remoteId==-14321||remoteUri==null){
            return null;
        }
        RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(context.getContentResolver(),remoteId);
        ContentResolver resolver = context.getContentResolver();
        Contact contact = null;

        contact = queryFromLocalRemoteDb(context, remoteUri);
        String jonedoe = context.getString(R.string.activity_main_contact_no_name);
        if (contact == null) {
            NgnUriUtils userparse = new NgnUriUtils();
            String userName;
            if (userparse.parseUriString(remoteUri)) {
                userName =userparse.getUserName();//userparse.getUserName();
            } else {
                userName = remoteUri;
            }
            try {
                Long.parseLong(userName);
            }catch (Exception e){

            }

            contact = queryFromSysCONTACTDb(context, remoteUri, jonedoe);//SysCONTACT
            int contactid ;
            if (contact == null) {
                contact = queryFromSysDATADb(context, remoteUri, jonedoe);//SysDATA
            }
            if(contact==null){
                contactid =0;
            }else{
                contactid = contact.getId();
            }
            if (remoteRecord!=null&&contactid!=remoteRecord.getContactId()){
                ContentValues values = new ContentValues();
                values.put(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID, contactid);
                resolver.update(DBHelperBase.RemoteColumns.CONTENT_URI, values, DBHelperBase.RemoteColumns._ID + "=" + remoteId , null);//关联remoteID与联系人。
            }
        }

        return null;
    }

    private Contact queryFromLocalRemoteDb(Context context,@NonNull String remoteUri) {
        Contact contact = null;
        int contactID = 0;
        int remoteID = 0;
        if(!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)){
            return  null;
        }
        ContentResolver resolver = context.getContentResolver();

        String where = DBHelperBase.RemoteColumns.REMOTE_URI + "=? AND " + DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID + ">0";
        Cursor cursor = CursorHelper.resolverQuery(resolver,DBHelperBase.RemoteColumns.CONTENT_URI, null, where, new String[]{remoteUri}, null);
        if (CursorHelper.moveCursorToFirst(cursor)) {
            contactID = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID));
            remoteID = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns._ID));
        }
        CursorHelper.closeCursor(cursor);
        if (contactID > 0) {
            String userNumber = NgnUriUtils.getUserName(remoteUri);
            String numberContrycode="";
            if(userNumber.startsWith("+")) {
                String contrycode="";
                contrycode = CountryCode.GetCountryZipCode(context,userNumber.substring(1));
                if(contrycode!=null){
                    String phoneNumber = userNumber.substring(1+contrycode.length());
                    numberContrycode = "((\\+"+contrycode+")|"+"("+contrycode+"))?";
                    numberContrycode += phoneNumber;
                }
            }

            contact = new Contact(contactID);
            boolean find = false;
            if (ContactManager.getContactData(context, contact)) {
                List<ContactDataAdapter> numbers = contact.getContactNumbers();
                for(Object obj:numbers){
                    if (obj != null && obj instanceof Contact.ContactDataNumber) {
                        Contact.ContactDataNumber number = (Contact.ContactDataNumber) obj;
                        if (!TextUtils.isEmpty(number.getNumber()) &&( number.getNumber().contains(userNumber)||number.getNumber().matches(numberContrycode))) {
                            find = true;
                            break;
                        }
                    }
                }

            }
            if (!find) {
                contact = null;//
            }
        }
        return contact;
    }

    private Contact queryFromSysCONTACTDb(Context context, String remoteUri, String defaultDisName) {
        Uri contactUri = null;
        Contact contact = null;
        if(!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)){
            return  null;
        };

        ContentResolver resolver = context.getContentResolver();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {//5.0
            contactUri = ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI.buildUpon().appendPath(remoteUri)
                    .appendQueryParameter(ContactsContract.PhoneLookup.QUERY_PARAMETER_SIP_ADDRESS, "true")
                    .build();
        } else {
            contactUri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath(Uri.encode(remoteUri)).
                    appendQueryParameter(android.provider.ContactsContract.PhoneLookup.QUERY_PARAMETER_SIP_ADDRESS, "true").build();
        }

        Cursor cursor = CursorHelper.resolverQuery(resolver,contactUri, null, null, null, null);
        while (CursorHelper.moveCursorToNext(cursor)) {
            int id = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.CONTACT_ID));
            String disName = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME));
//			int starred = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.STARRED));
//			String number = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.NUMBER));
//			int type = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.TYPE));
            if (TextUtils.isEmpty(disName)) {
                disName = defaultDisName;
            }
            contact = new Contact(id, disName);
//			contact.setStarrred(starred);
            ContactManager.getContactData(context, contact);

            break;
        }
        CursorHelper.closeCursor(cursor);
        return contact;
    }


    private Contact queryFromSysDATADb(Context context, String remoteUri, String defaultDisName) {

        if(!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)){
            return  null;
        }
        if(TextUtils.isEmpty(remoteUri)){
            return null;
        }
        ContentResolver resolver = context.getContentResolver();
        Contact contact = null;
        String head = null, userName = null, realmName = null, port = null,userName334;
        int matchFactor = 0;
        int contactid = 0;//
        final int FACTOR_HEAD = 1, FACTOR_NAME = 10, FACTOR_REALMNAME = 4, FACTOR_PORT = 2;


        NgnUriUtils userparse = new NgnUriUtils();

        String userNameOrder;
        if (userparse.parseUriString(remoteUri)) {
            head = userparse.getHead();
            userName =userparse.getUserName();//userparse.getUserName();
            realmName = userparse.getRealm();
            port = userparse.getPort();
            userNameOrder = userName;

        } else {
            userName = remoteUri;
            userNameOrder = userName;
        }

        if(userName.length()>2){
            userNameOrder = userName.substring(0,1);
            for(int chindex=1;chindex<userName.length();chindex++){
                userNameOrder += "%"+userName.charAt(chindex);
            }
        }


        String where = ContactsContract.CommonDataKinds.Phone.NUMBER + " like ? AND (" + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?)";


        final String START = "^";
        final String END = "$";
        String contrycode;
        String phoneNumber=userNameOrder;
        if(userName.startsWith("+")){
            contrycode = CountryCode.GetCountryZipCode(context,userName.substring(1));
            if(contrycode!=null){
                phoneNumber = userName.substring(1+contrycode.length());
                userName="((\\+"+contrycode+")|"+"("+contrycode+"))?";
                userName+=phoneNumber;
            }
        }
        String[] args = new String[]{"%" + phoneNumber + "%", ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE};

        final String Patern10 = NgnUriUtils.SIP_HEADER + "?" + userName + END;
        final String Patern14 = NgnUriUtils.SIP_HEADER + "?" + userName + "@" + realmName + END;
        final String Patern16 = NgnUriUtils.SIP_HEADER + "?" + userName + "@" + realmName + ":" + port + END;
        final String Patern15 = START + head + userName + "@" + realmName + END;
        final String Patern17 = START + head + ":" + userName + "@" + realmName + END + ":" + port + END;
        final int MAX_FACTOR = FACTOR_NAME + (TextUtils.isEmpty(head) ? FACTOR_HEAD : 0)
                + (TextUtils.isEmpty(realmName) ? FACTOR_REALMNAME : 0) + (TextUtils.isEmpty(port) ? FACTOR_PORT : 0);

        Cursor cursor = CursorHelper.resolverQuery(resolver,ContactsContract.Data.CONTENT_URI, null, where, args, null);
        while (CursorHelper.moveCursorToNext(cursor)) {
            int tempfactor = 0;
            int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
            String type = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
            String numberNoSpace = number.replace(" ","");
            numberNoSpace = numberNoSpace.replace("-","");

            boolean typePhone = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(type);
            //计算匹配因子

            if (MAX_FACTOR >= 17) {
                if (number.matches(Patern17)||(typePhone&&numberNoSpace.matches(Patern17))) {
                    tempfactor = 17;
                    if (MAX_FACTOR == tempfactor) {//找到了最匹配的项，不再继续
                        matchFactor = tempfactor;
                        contactid = id;
                        break;
                    }
                }
            }
            if (MAX_FACTOR >= 16) {
                if (number.matches(Patern16)||(typePhone&&numberNoSpace.matches(Patern16))) {//找到了最匹配的项，不再继续
                    tempfactor = 16;
                    if (MAX_FACTOR == tempfactor) {
                        matchFactor = tempfactor;
                        contactid = id;
                        break;
                    }
                }
            }
            if (MAX_FACTOR >= 15) {
                if (number.matches(Patern15)||(typePhone&&numberNoSpace.matches(Patern15))) {//找到了最匹配的项，不再继续
                    tempfactor = 15;
                    if (MAX_FACTOR == tempfactor) {
                        matchFactor = tempfactor;
                        contactid = id;
                        break;
                    }
                }
            } else if (MAX_FACTOR >= 14) {
                if (number.matches(Patern14)||(typePhone&&numberNoSpace.matches(Patern14))) {//找到了最匹配的项，不再继续
                    tempfactor = 14;
                    if (MAX_FACTOR == tempfactor) {
                        matchFactor = tempfactor;
                        contactid = id;
                        break;
                    }
                }
            } else if (MAX_FACTOR >= 10) {
                if (number.matches(Patern10)||(typePhone&&numberNoSpace.matches(Patern10))) {//找到了最匹配的项，不再继续
                    tempfactor = 10;
                    if (MAX_FACTOR == tempfactor) {
                        matchFactor = tempfactor;
                        contactid = id;
                        break;
                    }
                }
            }
            if (tempfactor > matchFactor) {
                matchFactor = tempfactor;
                contactid = id;
            }
        }
        CursorHelper.closeCursor(cursor);
        if (contactid > 0) {
            contactid = (int) ContactManager.getContactId(context, ContactsContract.RawContacts.CONTENT_URI, contactid);
            contact = new Contact(contactid);
            ContactManager.getContactData(context, contact);
        }
        return contact;
    }

}
