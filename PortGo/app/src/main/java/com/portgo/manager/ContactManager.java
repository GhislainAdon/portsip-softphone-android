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
package com.portgo.manager;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;

import com.portgo.BuildConfig;
import com.portgo.R;

import com.portgo.util.NgnObservableHashMap;

import com.portgo.util.NgnStringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.portgo.manager.ContactDataAdapter.*;

public class ContactManager {
    protected NgnObservableHashMap<Integer, Contact> mContacts;
    private HashMap<Integer, HashMap<Integer, Integer>> mContactVersion = new HashMap<>();
    protected boolean mLoading=false;
    protected boolean mReady=false;

    protected Looper mLocalContactObserverLooper;

    protected ContentObserver mLocalContactObserver;
    protected ContentResolver mLocalContactResolver;

    Context mContext;
    Handler handler = null;

    static private ContactManager instance = new ContactManager();
    private ContactManager() {
    }

    static public ContactManager getInstance() {
        return instance;
    }

    synchronized public boolean start(Context context) {
        mContext = context.getApplicationContext();
        if (mContacts == null) {
            mContacts = getObservableContacts();
        }

        mimeTypeRes = new HashMap<String, LinkedHashMap>();
        loadMimeTypeRes(mContext);

        if (mLocalContactResolver == null) {
            mLocalContactResolver = mContext.getContentResolver();
        }
        if (mLocalContactObserver == null && mLocalContactObserverLooper == null) {
            new Thread(new Runnable() { // avoid locking calling thread
                @Override
                public void run() {
                    Looper.prepare();
                    mLocalContactObserverLooper = Looper.myLooper();
                    handler = new Handler();
                    handler.post(new Runnable() { // Observer thread. Will allow us to get notifications even if the application is on background
                        @Override
                        public void run() {
                            mLocalContactObserver = new ContentObserver(handler) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    super.onChange(selfChange);
                                    if (!PermissionManager.testDangercePermission(mContext, Manifest.permission.WRITE_CONTACTS)) {
                                        return ;
                                    }

                                    HashMap<Integer, HashMap<Integer, Integer>> changedContactVersion = new HashMap<Integer, HashMap<Integer, Integer>>();
                                    getRawContactVersion(mContext.getContentResolver(), ContactsContract.RawContacts.CONTENT_URI, changedContactVersion);

                                    Set<Integer> changedContactId = changedContactVersion.keySet();
                                    Set<Integer> contactId = mContactVersion.keySet();

                                    Set<Integer> newContacts = new HashSet<Integer>();
                                    Set<Integer> deleteContacts = new HashSet<Integer>();
                                    Set<Integer> updateContacts = new HashSet<Integer>();

                                    //获取新加的联系人
                                    newContacts.addAll(changedContactId);
                                    newContacts.removeAll(contactId);
                                    //获取删除的联系人
                                    deleteContacts.addAll(contactId);
                                    deleteContacts.removeAll(changedContactId);

                                    //获取可能更新的联系人//交集，有可能更新的联系人
                                    updateContacts.addAll(changedContactId);
                                    updateContacts.retainAll(contactId);

                                    Set<Integer> realUpdatedContacts = new HashSet<Integer>();
                                    realUpdatedContacts.addAll(updateContacts);

                                    for (int updateId : updateContacts) {

                                        HashMap<Integer, Integer> contactOld = mContactVersion.get(updateId);
                                        HashMap<Integer, Integer> contactNew = changedContactVersion.get(updateId);
                                        if (contactOld.size() != contactNew.size()) {//聚合成联系人的原始联系人变更。需要更新联系人
                                            continue;
                                        } else {

                                            Set<Integer> oldRawIds = contactOld.keySet();
                                            Set<Integer> newRawIds = contactNew.keySet();
                                            if (oldRawIds.containsAll(newRawIds)) {
                                                boolean equel = true;
                                                for (int rawid : oldRawIds) {
                                                    if (contactOld.get(rawid) != contactNew.get(rawid)) {
                                                        equel = false;
                                                        break;
                                                    }
                                                }
                                                if (equel) {
                                                    realUpdatedContacts.remove(updateId);//聚成成联系人的所有元素联系人未改变，此联系人未变，从更新中删除
                                                }
                                            } else {//聚合成联系人的原始联系人变更。需要更新联系人
                                                continue;
                                            }
                                        }

                                    }

                                    mContactVersion = changedContactVersion;//保留最新的联系人数据版本
                                    LinkedHashMap updateMap = null;
                                    //装载需要更新的和新加入的联系人
                                    realUpdatedContacts.addAll(newContacts);
                                    if (realUpdatedContacts.size() > 0) {
                                        updateMap = new LinkedHashMap<Integer, Contact>();
                                        ContentResolver resolver = mContext.getContentResolver();
                                        for (int updateId : realUpdatedContacts) {
                                            Contact contact = getContact(mContext,ContactsContract.Contacts.CONTENT_URI,updateId);
                                            if (contact!=null) {
                                                updateMap.put(updateId, contact);
                                            }
                                        }
                                    }

                                    synchronized (mContacts) {
                                        for (int deleteid : deleteContacts) {
                                            mContacts.remove(deleteid);
                                        }
                                        mContacts.addAll(updateMap);
                                    }

                                }

                            };
                            mLocalContactResolver.registerContentObserver(ContactsContract.RawContacts.CONTENT_URI,
                                    true, mLocalContactObserver);

                            load();
                        }
                    });
                    Looper.loop();// loop() until quit() is called
                }
            }).start();
        }

        return true;
    }


    static void loadMimeTypeRes(Context context) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }

        LinkedHashMap<Integer, String> sips = new LinkedHashMap<Integer, String>();
        for (int i = 0; i < SIP_TYPE.length; i++) {
            int typeRes = ContactsContract.CommonDataKinds.SipAddress.getTypeLabelResource(SIP_TYPE[i]);
            String value = context.getResources().getString(typeRes);
            sips.put(SIP_TYPE[i], value);
        }
        mimeTypeRes.put(SipAddress.CONTENT_ITEM_TYPE, sips);

        LinkedHashMap<Integer, String> phones = new LinkedHashMap<Integer, String>();
        for (int i = 0; i < PHONE_TYPE.length; i++) {
            int typeRes = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(PHONE_TYPE[i]);
            phones.put(PHONE_TYPE[i], context.getResources().getString(typeRes));
        }
        mimeTypeRes.put(CommonDataKinds.Phone.CONTENT_ITEM_TYPE, phones);
    }
//	public void notifyDataChange(){
//		if(mLocalContactObserver!=null)
//		{
//			NgnApplication.getContext().getContentResolver().notifyChange(CommonDataKinds.Phone.CONTENT_URI, mLocalContactObserver);
//		}updateContact
//	}

    synchronized public boolean stop() {

        try {
            mReady =false;
            if (mLocalContactObserver != null) {
                mContext.getContentResolver().unregisterContentObserver(mLocalContactObserver);
                mLocalContactObserver = null;
            }
            if (mLocalContactObserverLooper != null) {
                mLocalContactObserverLooper.quit();
                mLocalContactObserverLooper = null;
            }
        } catch (Exception e) {
            //PortApplication.getLogUtils().e(TAG, e.toString());
        }
        return true;
    }

    private void getRawContactVersion(ContentResolver resolver, Uri rawContactUri, HashMap<Integer, HashMap<Integer, Integer>> contactVersion) {

        String[] projection = null;
        if (rawContactUri.equals(com.portgo.androidcontacts.ContactsContract.RawContacts.CONTENT_URI)) {
            projection = new String[]{com.portgo.androidcontacts.ContactsContract.RawContacts.CONTACT_ID, com.portgo.androidcontacts.ContactsContract.RawContacts._ID,
                    com.portgo.androidcontacts.ContactsContract.RawContacts.VERSION};
        } else {
            projection = new String[]{ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts._ID,
                    ContactsContract.RawContacts.VERSION};
        }
        Cursor cursor = CursorHelper.resolverQuery(resolver,rawContactUri, projection, null, null, null);
        while (CursorHelper.moveCursorToNext(cursor)) {
            int contactID = cursor.getInt(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
            int rawID = cursor.getInt(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
            int version = cursor.getInt(cursor.getColumnIndex(ContactsContract.RawContacts.VERSION));
            HashMap<Integer, Integer> raws = contactVersion.get(contactID);
            if (raws == null) {
                HashMap<Integer, Integer> rawContactVersion = new HashMap<Integer, Integer>();
                rawContactVersion.put(rawID, version);
                contactVersion.put(contactID, rawContactVersion);
            } else {
                raws.put(rawID, version);
            }
        }
        CursorHelper.closeCursor(cursor);
    }

    public synchronized boolean load() {
        mLoading = true;
        boolean bOK = true;
        LinkedHashMap<Integer, Contact> contactsCopy = new LinkedHashMap<Integer, Contact>();
        if (!PermissionManager.testDangercePermission(mContext, Manifest.permission.WRITE_CONTACTS)) {
            return false;
        }
        if (load1(contactsCopy)) {
            bOK = true;
            getRawContactVersion(mContext.getContentResolver(), ContactsContract.RawContacts.CONTENT_URI, mContactVersion);

            synchronized (mContacts) {
                mContacts.clear();
                mContacts.addAll(contactsCopy);
                mContacts.notifyObservers();
            }

            mReady = true;
        } else {
            mReady = false;
            mLoading = false;
        }

        return bOK;
    }

    private boolean load1(LinkedHashMap<Integer, Contact> contacts) {
        boolean bOK = false;

        try {
            loadContacts(mContext, contacts);
            bOK = true;
        } catch (Exception e) {
            bOK = false;
        }

        return bOK;
    }

    static public void loadGroupsData(Context context,List<Group> groupsCopy) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }
        ContentResolver resolver = context.getContentResolver();

        final Resources res = context.getResources();
        Cursor groupCursor = CursorHelper.resolverQuery(resolver,com.portgo.androidcontacts.ContactsContract.Groups.CONTENT_URI,
                new String[]{ContactsContract.Groups.TITLE, ContactsContract.Groups._ID},
                null,
                null,
                null);

        while (CursorHelper.moveCursorToNext(groupCursor)) {
            String groupName = groupCursor.getString(groupCursor.getColumnIndex(ContactsContract.Groups.TITLE));
            int groupid = groupCursor.getInt(groupCursor.getColumnIndex(ContactsContract.Groups._ID));
            Group group = new Group(groupid, groupName);
            groupsCopy.add(group);
        }

        CursorHelper.closeCursor(groupCursor);


        Cursor groupContactCursor = CursorHelper.resolverQuery(resolver,com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI,
                new String[]{Data.RAW_CONTACT_ID},
                Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                        + "' AND " + ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = "
                        + com.portgo.androidcontacts.ContactsContract.Groups.subScribeGroupId,
                null,
                null);

// Second, query the corresponding name of the raw_contact_id
        while (CursorHelper.moveCursorToNext(groupContactCursor)) {
            Cursor contactCursor = CursorHelper.resolverQuery(resolver,com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI,
                    new String[]{Data.RAW_CONTACT_ID, StructuredName.FAMILY_NAME, StructuredName.GIVEN_NAME},
                    Data.MIMETYPE + "='" + StructuredName.CONTENT_ITEM_TYPE + "' AND " +
                            Data.RAW_CONTACT_ID + "=" + groupContactCursor.getInt(0),
                    null,
                    null);
            String name = null;
            while (CursorHelper.moveCursorToNext(contactCursor)) {
                name = contactCursor.getString(1) + " " + contactCursor.getString(2);
            }
//            PortApplication.getLogUtils().e("Test", "Member name is: " + name);
            CursorHelper.closeCursor(contactCursor);
        }
        CursorHelper.closeCursor(groupContactCursor);
    }

    public void loadContacts(Context context, LinkedHashMap<Integer, Contact> contactsCopy) {

        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }
        ContentResolver resolver = context.getContentResolver();

        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String Sort_Key_Lable = "phonebook_label";
        if (android.os.Build.VERSION.SDK_INT < 19) {
            Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        }

        Cursor cursorContact = CursorHelper.resolverQuery(resolver,uri, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                Sort_Key_Lable,
                ContactsContract.Contacts.SORT_KEY_ALTERNATIVE,
                ContactsContract.Contacts.LAST_TIME_CONTACTED,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.PHOTO_ID,}, null, null, Sort_Key_Lable + " ASC");

        Map<String, Object> ct = null;
        String displayName = null;
        String john_doe = mContext.getString(R.string.activity_main_contact_no_name);

        while (CursorHelper.moveCursorToNext(cursorContact)) {
            int id = cursorContact.getInt(0);
            displayName = cursorContact.getString(1);
            String sortKey = cursorContact.getString(2);
            String sortKeyAlt = cursorContact.getString(3);
            String lastTime = cursorContact.getString(4);
            int starred = cursorContact.getInt(5);
            int photoId = cursorContact.getInt(6);
            if (TextUtils.isEmpty(displayName)) {
                displayName = john_doe;
            }
            Contact contact = new Contact(id, displayName);
            contact.setStarrred(starred);
            contact.setSortKey(sortKey);
            contact.setAvatarId(photoId);
            Uri contactUri = ContentUris.withAppendedId(uri,id);
            if(photoId>0) {
                try {
                    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, contactUri);
                    Bitmap bmp_head = BitmapFactory.decodeStream(input);
                    if(bmp_head!=null){
                        contact.setAvatar(bmp_head);
                    }
                }catch (Exception e){

                }
            }
            contactsCopy.put(id, contact);
//            PortApplication.getLogUtils().d("Sortkey", sortKey + "||" + sortKeyAlt);
        }
        CursorHelper.closeCursor(cursorContact);

        //加载SipAddress，Phone，PortIM
        String selection = ContactsContract.Data.MIMETYPE +" =? OR "
                +ContactsContract.Data.MIMETYPE+" =? OR ("+ContactsContract.Data.MIMETYPE+" =? AND "+
                CommonDataKinds.Im.PROTOCOL +" =? AND "+CommonDataKinds.Im.CUSTOM_PROTOCOL+" =?) ";
        Cursor cursorData = CursorHelper.resolverQuery(resolver,ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data.MIMETYPE,ContactsContract.Data._ID,ContactsContract.Data.DATA1, ContactsContract.Data.DATA2,
                        ContactsContract.Data.DATA3, Data.CONTACT_ID},
                selection,
                new String[]{CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,CommonDataKinds.Phone.CONTENT_ITEM_TYPE,CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                        ""+CommonDataKinds.Im.PROTOCOL_CUSTOM,PORTSIP_IM_PROTOCAL}, null);

        while (CursorHelper.moveCursorToNext(cursorData)) {
            String mimeType =cursorData.getString(0);
            int dataID = cursorData.getInt(1);
            String data1 = cursorData.getString(2);
            int data2 = cursorData.getInt(3);
            String lable = null;
            if (data2 == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                lable = cursorData.getString(4);
            }
            int contactid = cursorData.getInt(5);
            Contact contact = contactsCopy.get(contactid);
            switch (mimeType) {
                case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE: {
                    if (contact != null) {
                        contact.addSipAddress(new Contact.ContactDataSipAddress(dataID, data1, data2, lable));
                    } else {
                    }
                }
                break;
                case CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                    if (contact != null) {
                        contact.addPhone(new Contact.ContactDataPhone(dataID, data1, data2, lable));
                    } else {
                    }
                }
                break;
                case CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                    if (contact != null) {
                        Contact.ContactDataIm im = new Contact.ContactDataIm(dataID, data1, data2, lable);
                        im.setProtocaltype(CommonDataKinds.Im.PROTOCOL_CUSTOM);
                        im.setProtocal(PORTSIP_IM_PROTOCAL);
                        contact.addIm(im);
                    }
                    break;
            }
        }
        CursorHelper.closeCursor(cursorData);
    }

    public static boolean getContactData(Context context, Contact contact) {
        return getContactData(context, contact, ContactsContract.Contacts.CONTENT_URI, Data.CONTENT_URI);
    }

    public static boolean getContactData(Context context, Contact contact, Uri contactUri, Uri dataUri) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return false;
        }
        ContentResolver resolver = context.getContentResolver();

        int contactId = contact.getId();
        Uri contactsUri = ContentUris.withAppendedId(contactUri, contactId);
//        Cursor cursorContact = resolver.query(contactsUri, null, null, null, null);

        Uri uri = Uri.withAppendedPath(contactsUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
        boolean loadData = false;
        //PHOTO_FILE_ID = DATA14; PHOTO = DATA15;
//            new String[] { Data._ID, Data.DATA1, Data.DATA15, Data.MIMETYPE }
        Cursor cursor = CursorHelper.resolverQuery(resolver,uri, new String[]{Data._ID, Data.DATA1, Data.DATA2,
                Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6,
                Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10, Data.DATA11, Data.DATA12, Data.DATA13
                , Data.DATA14, Data.DATA15, Data.MIMETYPE, StructuredName.PHONETIC_NAME}, null, null, null);

        //查询联系人个人信息
        while (CursorHelper.moveCursorToNext(cursor)) {
            loadData = true;
            int dataId = cursor.getInt(cursor.getColumnIndex(Data._ID));
            String mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
            String data1 = cursor.getString(cursor.getColumnIndex(Data.DATA1));
            int data2 = cursor.getInt(cursor.getColumnIndex(Data.DATA2));
            String data3 = cursor.getString(cursor.getColumnIndex(Data.DATA3));

            switch (mimeType) {
                case StructuredName.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataStructuredName structNameName = new Contact.ContactDataStructuredName(dataId, data1, data2, data3);
                    structNameName.mId = dataId;
                    structNameName.mData1 = data1;
                    structNameName.mData2 = data2;


                    //StructuredName.DISPLAY_NAME == data1
                    //structNameName.givenName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));==data2
                    //structNameName.familyName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));==data3

                    structNameName.givenName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));//
                    structNameName.middleName = cursor.getString(cursor.getColumnIndex(StructuredName.MIDDLE_NAME));
                    structNameName.prefix = cursor.getString(cursor.getColumnIndex(StructuredName.PREFIX));
                    structNameName.suffix = cursor.getString(cursor.getColumnIndex(StructuredName.SUFFIX));
                    structNameName.phoneticName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_NAME));
                    structNameName.phoneticFamilyName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_FAMILY_NAME));
                    structNameName.phoneticMiddleName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_MIDDLE_NAME));
                    structNameName.phoneticGivenName = cursor.getString(cursor.getColumnIndex(StructuredName.PHONETIC_GIVEN_NAME));

                    if (NgnStringUtils.isNullOrEmpty(data1)) {
                        data1 = "";
                    }
                    contact.addStructName(structNameName);
                }
                break;
                case CommonDataKinds.Nickname.CONTENT_ITEM_TYPE: {
                    //
                    Contact.ContactDataNickName nickName = new Contact.ContactDataNickName(dataId, data1, data2, data3);
                    contact.addNickName(nickName);
                }
                break;
                case CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                    //
                    Contact.ContactDataPhone contactPhone = new Contact.ContactDataPhone(dataId, data1, data2, data3);
                    contact.addPhone(contactPhone);
                }
                break;
                case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE: {
                    //Sip
                    Contact.ContactDataSipAddress sipAddress = new Contact.ContactDataSipAddress(dataId, data1, data2, data3);
                    contact.addSipAddress(sipAddress);
                }
                break;
                case CommonDataKinds.Email.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataEmail email = new Contact.ContactDataEmail(dataId, data1, data2, data3);
                    contact.addEmail(email);
                }
                break;
                case CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE: {
                    //
                    Contact.ContactDataStructuredPostal structuredPostal = new Contact.ContactDataStructuredPostal(dataId, data1, data2, data3);
                    contact.addStructuredPostal(structuredPostal);
                }
                break;
                case CommonDataKinds.Organization.CONTENT_ITEM_TYPE: {
                    String title = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.TITLE));
                    String department = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.DEPARTMENT));
                    String location = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Organization.OFFICE_LOCATION));
                    Contact.ContactDataOrgnization orgnization = new Contact.ContactDataOrgnization(dataId, data1, data2, data3, department, title);
                    orgnization.setLocation(location);
                    contact.addOgnization(orgnization);
                }
                break;
                case CommonDataKinds.Event.CONTENT_ITEM_TYPE: {
                    //
                    Contact.ContactDataEvent event = new Contact.ContactDataEvent(dataId, data1, data2, data3);
                    contact.addEvent(event);
                }
                break;
                case CommonDataKinds.Note.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataNote note = new Contact.ContactDataNote(dataId, data1, data2, data3);
                    contact.addNote(note);
                }
                break;
                case CommonDataKinds.Im.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataIm im = new Contact.ContactDataIm(dataId, data1, data2, data3);
                    im.protocal = cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Im.PROTOCOL));

                    if (im.protocal == CommonDataKinds.Im.PROTOCOL_CUSTOM) {
                        im.customProtocal = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Im.CUSTOM_PROTOCOL));
                    }
                    contact.addIm(im);
                }
                break;
                case CommonDataKinds.Website.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataWebsite website = new Contact.ContactDataWebsite(dataId, data1, data2, data3);
                    contact.addWebsite(website);
                }
                break;
                case CommonDataKinds.Photo.CONTENT_ITEM_TYPE: {
                    Contact.ContactDataPhoto photo = new Contact.ContactDataPhoto(dataId, data1, data2, data3);
                    contact.addPhoto(photo);
                }
                break;

            }
//			case Organization.CONTENT_ITEM_TYPE:{ }
        }
        CursorHelper.closeCursor(cursor);
        return loadData;
    }


    public boolean isLoading() {
        return mLoading;
    }

    public boolean isReady() {
        return mReady;
    }

    /**
     *
     */
    private static String getContactNumber(int contactId, Context context) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();

        Cursor phones = CursorHelper.resolverQuery(resolver,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        + " = " + contactId, null, null);
        String phoneNumber = "";
        if (CursorHelper.moveCursorToFirst(phones)) {
            phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }
        CursorHelper.closeCursor(phones);
        return phoneNumber;
    }

    public NgnObservableHashMap<Integer, Contact> getObservableContacts() {
        if (mContacts == null) {
            mContacts = new NgnObservableHashMap<Integer, Contact>(true);
        }
        return mContacts;
    }

    private Contact newContact(int id, String displayName) {
        return new Contact(id, displayName);
    }


    private Contact getContactByUri(Context context,String uri) {
//		final SipUri sipUri = new SipUri(uri,null);
        Contact contact = null;
//		if(sipUri.isValid()){
        contact = getContactByPhoneNumber(context,uri);
//		}
        return contact;
    }

    public static Contact getContactByPhoneNumber(Context context,String anyPhoneNumber) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();

        Contact contact = null;
        Uri contactUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath(Uri.encode(anyPhoneNumber)).
                appendQueryParameter(ContactsContract.PhoneLookup.QUERY_PARAMETER_SIP_ADDRESS, "true").build();

        Cursor cursor = CursorHelper.resolverQuery(resolver,contactUri, null, null, null, null);
        String johnDoe = context.getString(R.string.activity_main_contact_no_name);
        if (CursorHelper.moveCursorToFirst(cursor)) {
            int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID));
            String disName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            int starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED));
            if (TextUtils.isEmpty(disName)) {
                disName = johnDoe;
            }
            contact = new Contact(id, disName);
            contact.setStarrred(starred);
            ContactManager.getContactData(context, contact);

        }
        CursorHelper.closeCursor(cursor);
        return contact;
    }



    public static Contact getContact(Context context,Uri contactUri, int id) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();
        Contact contact = null;
        if (id > Contact.INVALIDE_ID) {
            String Sort_Key_Lable = "phonebook_label";
            if (android.os.Build.VERSION.SDK_INT < 19) {
                Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
            }

            Uri dataUri = ContactsContract.Data.CONTENT_URI;
            if (com.portgo.androidcontacts.ContactsContract.Contacts.CONTENT_URI.equals(contactUri)) {
                dataUri = com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI;
                Sort_Key_Lable = ContactsContract.Contacts.SORT_KEY_PRIMARY;
            }

            String johnDoe = context.getString(R.string.activity_main_contact_no_name);
            Uri uriWithId = ContentUris.withAppendedId(contactUri, id);

            Cursor cursor =CursorHelper.resolverQuery(resolver,uriWithId, new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    Sort_Key_Lable,
                    ContactsContract.Contacts.SORT_KEY_ALTERNATIVE,
                    ContactsContract.Contacts.LAST_TIME_CONTACTED,
                    ContactsContract.Contacts.STARRED,
                    ContactsContract.Contacts.PHOTO_ID}, null, null, null);

            if (CursorHelper.moveCursorToFirst(cursor)) {
                id = cursor.getInt(0);
                String displayName = cursor.getString(1);
                if (TextUtils.isEmpty(displayName)) {
                    displayName = johnDoe;
                }
                String sortKey = cursor.getString(2);
                String sortKeyAlt = cursor.getString(3);
                String lastTime = cursor.getString(4);
                int starred = cursor.getInt(5);
                int photoId = cursor.getInt(6);
                contact = new Contact(id, displayName);
                contact.setStarrred(starred);
                contact.setSortKey(sortKey);
                contact.setSortKeyAlt(sortKeyAlt);
                contact.setAvatarId(photoId);

                if(photoId>0) {
                    try {
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uriWithId);
                        Bitmap bmp_head = BitmapFactory.decodeStream(input);
                        if(bmp_head!=null){
                            contact.setAvatar(bmp_head);
                        }
                    }catch (Exception e){

                    }
                }

                ContactManager.getContactData(context, contact, contactUri, dataUri);
            }
            CursorHelper.closeCursor(cursor);
        }
        return contact;
    }

    public static Bitmap getPhoto(final Context context, String contactId, Uri dataUri) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();

        Bitmap photo = null;
        Cursor dataCursor = CursorHelper.resolverQuery(resolver,dataUri,
                new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO},
                ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'",
                new String[]{contactId}, null);
        if (CursorHelper.moveCursorToFirst(dataCursor)) {
                byte[] bytes = dataCursor.getBlob(dataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
                if (bytes != null) {
                    photo = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }

        CursorHelper.closeCursor(dataCursor);
        return photo;
    }




    public static long getRawContactId(Context context, Uri dataUri, long contactId) {
        Cursor rawContactIdCursor = null;
        long rawContactId = -1;
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return contactId;
        }
        ContentResolver resolver = context.getContentResolver();

        try {
            rawContactIdCursor = CursorHelper.resolverQuery(resolver,dataUri,
                    new String[]{ContactsContract.RawContacts._ID}, ContactsContract.RawContacts.CONTACT_ID
                            + "=" + contactId, null, null);
            if (CursorHelper.moveCursorToFirst(rawContactIdCursor)) {
                // Just return the first one.
                rawContactId = rawContactIdCursor.getLong(0);
            }
        } finally {
            CursorHelper.closeCursor(rawContactIdCursor);
        }
        return rawContactId;
    }

    public static long getContactId(Context context, Uri rawContacturi, long rawContactId) {
        long contactId = -1;
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return contactId;
        }
        ContentResolver resolver = context.getContentResolver();
        Cursor contactIdCursor = null;

        try {
            contactIdCursor =CursorHelper.resolverQuery(resolver,rawContacturi,
                    new String[]{ContactsContract.RawContacts.CONTACT_ID}, ContactsContract.RawContacts._ID
                            + "=" + rawContactId, null, null);
            if (CursorHelper.moveCursorToFirst(contactIdCursor)) {
                // Just return the first one.
                contactId = contactIdCursor.getLong(0);
            }
        } finally {
            CursorHelper.closeCursor(contactIdCursor);
        }
        return contactId;
    }

    class ContactMember {
        public String contact_name;
        public String contact_phone;
        public int contact_id;
        public String sortKey;
    }

//    Cursor c;

    public ArrayList<ContactMember> getContact(Context context) {
        ArrayList<ContactMember> listMembers = new ArrayList<ContactMember>();
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return listMembers;
        }

        Cursor cursor = null;
        try {

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            // 这里是获取联系人表的电话里的信息  包括：名字，名字拼音，联系人id,电话号码；
            // 然后在根据"sort-key"排序
            cursor = CursorHelper.resolverQuery(context.getContentResolver(),
                    uri,
                    new String[]{"display_name", "sort_key", "contact_id",
                            "data1"}, null, null, "sort_key");

            if (CursorHelper.moveCursorToFirst(cursor)){
                do {
                    ContactMember contact = new ContactMember();
                    String contact_phone = cursor
                            .getString(cursor
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(0);
                    String sortKey = getSortKey(cursor.getString(1));
                    int contact_id = cursor
                            .getInt(cursor
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    contact.contact_name = name;
                    contact.sortKey = sortKey;
                    contact.contact_phone = contact_phone;
                    contact.contact_id = contact_id;
                    if (name != null)
                        listMembers.add(contact);
                } while (CursorHelper.moveCursorToNext(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CursorHelper.closeCursor(cursor);
        }
        return listMembers;
    }

    private static String getSortKey(String sortKeyString) {
        String key = sortKeyString.substring(0, 1).toUpperCase();
        if (key.matches("[A-Z]")) {
            return key;
        }
        return "#";
    }

//    private ArrayList<ContactMember> getContact() {
//        ContentResolver resolver = mContext.getContentResolver();
//        ArrayList<ContactMember> listMembers = new ArrayList<ContactMember>();
//        // Phone里面的数据
//        Cursor phoneCursor = resolver.query(CommonDataKinds.Phone.CONTENT_URI,
//                new String[]{CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.DISPLAY_NAME,
//                        CommonDataKinds.Phone.CONTACT_ID}, null, null, null);
//        if (phoneCursor != null) {
//            while (phoneCursor.moveToNext()) {
//                // 读取联系人号码
//                ContactMember contactMember = new ContactMember();
//                int phoneNumberIndex = phoneCursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
//                contactMember.contact_phone = phoneCursor.getString(phoneNumberIndex);
//                if (TextUtils.isEmpty(contactMember.contact_phone))
//                    continue;
//                int contactNameIndex = phoneCursor
//                        .getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME);
//                contactMember.contact_name = phoneCursor.getString(contactNameIndex);
//
//                // 根据RAW_ID读取sort_key
//                int rawContactIdIndex = phoneCursor
//                        .getColumnIndex(CommonDataKinds.Phone.CONTACT_ID);
//                contactMember.contact_id = (int) phoneCursor.getLong(rawContactIdIndex);
//                contactMember.sortKey = getSortKeyString(contactMember.contact_id);
//                listMembers.add(contactMember);
//            }
//            phoneCursor.close();
//        }
//        return listMembers;
//    }

    public final static String PORTSIP_IM_PROTOCAL = "portgo_sip";

//    static private String getSortKeyString(Context context,long rawContactId) {
//
//        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
//            return null;
//        }
//
//        String Where = ContactsContract.RawContacts.CONTACT_ID + " ="
//                + rawContactId;
//        String[] projection = {"sort_key"};
//        Cursor cur =context.getContentResolver().query(
//                ContactsContract.RawContacts.CONTENT_URI, projection, Where,
//                null, null);
//        int sortIndex = cur.getColumnIndex("sort_key");
//        cur.moveToFirst();
//        String sortValue = cur.getString(sortIndex);
//        cur.close();
//        return sortValue;
//    }

    //尽量保证插入的数据值不能为空

    /**
     * @param context
     * @param contact
     * @return
     */

    static  public int insertContact(Context context, Contact contact, Bitmap bitmap) {
        int rawContactid = Contact.INVALIDE_ID;
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return rawContactid;
        }

        HashMap<String, Object> addResult = new HashMap<String, Object>();
        if (contact.isEmpty()) {
            addResult.put("result", "0");
            addResult.put("obj", "无效插入，联系人信息不完整！");
            return rawContactid;
        }
        //批量插入的内容集合
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation op = null;
        int rawIndex = 0;
        ContentProviderOperation.Builder builder = null;
        ContentResolver resolver = context.getContentResolver();


        //数据表 uri
        Uri uri = Data.CONTENT_URI;

        //Uri uri = RawContacts.CONTENT_URI; content://com.android.contacts/raw_contacts
        //此处.withValue("account_name", null)一定要加，不然会抛NullPointerException
        //withYieldAllowed(true)//为了避免这种死锁的数据库
        op = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .withYieldAllowed(true)
                .build();
        ops.add(op);
        HashMap<String, List<ContactDataAdapter>> contactData = contact.getContactData();
        Set<String> contentItems = contactData.keySet();
        ContentProviderOperation.Builder tempBuilder = null;
        for (String mimeType : contentItems) {

            //所有的记录,如：所有号码，所有email
            List<ContactDataAdapter> dataRows = contactData.get(mimeType);
            if (dataRows == null && !(dataRows.size() > 0))
                continue;
            switch (mimeType) {
                case StructuredName.CONTENT_ITEM_TYPE://姓名信息
                    for (Object obj : dataRows) {
                        if (obj != null && obj instanceof Contact.ContactDataStructuredName) {

                            Contact.ContactDataStructuredName structuredName = (Contact.ContactDataStructuredName) obj;
                            if (structuredName.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);
                                ContentValues values = structuredName.getContentValue();
                                tempBuilder.withValues(values);
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Nickname.CONTENT_ITEM_TYPE://昵称信息
                    break;
                case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE://SIP信息
                case CommonDataKinds.Phone.CONTENT_ITEM_TYPE://电话信息
                    for (ContactDataAdapter row : dataRows) {
                        if (row != null && row instanceof Contact.ContactDataNumber) {

                            Contact.ContactDataNumber contactNumber = (Contact.ContactDataNumber) row;
                            if (contactNumber.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);
                                tempBuilder.withValues(contactNumber.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;

                case CommonDataKinds.Email.CONTENT_ITEM_TYPE://email
                    break;
                case CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE://地址信息
                    break;
                case CommonDataKinds.Organization.CONTENT_ITEM_TYPE://组织信息
                    for (Object obj : dataRows) {
                        if (obj != null && obj instanceof Contact.ContactDataOrgnization) {
                            Contact.ContactDataOrgnization orgnization = (Contact.ContactDataOrgnization) obj;
                            if (orgnization.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);

                                tempBuilder.withValues(orgnization.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Event.CONTENT_ITEM_TYPE://事件提醒，生日，周年等
                    break;
                case CommonDataKinds.Note.CONTENT_ITEM_TYPE://备注信息
                    break;
                case CommonDataKinds.Im.CONTENT_ITEM_TYPE://im消息
                    for (ContactDataAdapter row : dataRows) {
                        if (row != null && row instanceof Contact.ContactDataIm) {
                            Contact.ContactDataIm contactIm = (Contact.ContactDataIm) row;
                            if (contactIm.dataAvailable()) {

                                if (contactIm.protocal != CommonDataKinds.Im.PROTOCOL_CUSTOM ||
                                        !PORTSIP_IM_PROTOCAL.equals(contactIm.customProtocal)) {
                                    continue;
                                }

                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                                        .withYieldAllowed(true);
                                tempBuilder.withValues(contactIm.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Website.CONTENT_ITEM_TYPE://网站
                    break;
                case CommonDataKinds.Photo.CONTENT_ITEM_TYPE://照片
//                    Iterator photeRow = dataRows.entrySet().iterator();
//                    if (photeRow.hasNext()) {
//                        Map.Entry row = (Map.Entry) photeRow.next();
//                        Integer rowId = (Integer) row.getKey();//charu 更新
//                        HashMap<String, Object> datas = (HashMap<String, Object>) row.getValue();
//                        Bitmap bmp = (Bitmap) datas.get(CommonDataKinds.Photo.PHOTO);
//                        if (bmp != null) {
//                            tempBuilder = ContentProviderOperation.newInsert(uri)
//                                    .withValue(Data.RAW_CONTACT_ID, rawIndex)
//                                    .withValue(Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
//                                    .withYieldAllowed(true);
//
//                            final ByteArrayOutputStream os = new ByteArrayOutputStream();
//                            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
//                            byte[] avatar = os.toByteArray();
//                            tempBuilder.withValue(CommonDataKinds.Photo.PHOTO, avatar);
//                            ops.add(tempBuilder.build());
//                        }
//                    }
                    break;
            }
        }


        //批量执行插入
        try {
            ContentProviderResult[] results = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            //插入成功返回的Uri集合

            if (results.length > rawIndex) {
                if (results[rawIndex].uri != null) {
                    rawContactid = (int) ContentUris.parseId(results[rawIndex].uri);
                }
            }
        } catch (Exception e) {
//					PortApplication.getLogUtils().i(Const.APPTAG, e.getMessage());
            addResult.put("result", "-1");
            addResult.put("obj", "插入失败:" + e.getMessage());
        }
//        }

        return rawContactid;
    }

    static public int insertFriend(Context context, Contact contact, Bitmap bitmap, boolean visable) {
        int contactid = -1;
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return contactid;
        }
        HashMap<String, Object> addResult = new HashMap<String, Object>();
        if (contact.isEmpty()) {
            addResult.put("result", "0");
            addResult.put("obj", "无效插入，联系人信息不完整！");
            return contactid;
        }
        //批量插入的内容集合
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ContentProviderOperation op = null;
        int rawIndex = 0;
        ContentResolver resolver = context.getContentResolver();


        //数据表 uri
        Uri uri = com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI;

        //Uri uri = RawContacts.CONTENT_URI; content://com.android.contacts/raw_contacts
        //此处.withValue("account_name", null)一定要加，不然会抛NullPointerException
        //withYieldAllowed(true)//为了避免这种死锁的数据库
        op = ContentProviderOperation.newInsert(com.portgo.androidcontacts.ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                        ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED)//不做聚合
                .withYieldAllowed(true)
                .build();
        ops.add(op);
        HashMap<String, List<ContactDataAdapter>> data = contact.getContactData();
        Set<String> contentItems = data.keySet();
        ContentProviderOperation.Builder tempBuilder = null;
        for (String mimeType : contentItems) {
            List<ContactDataAdapter> dataRows = data.get(mimeType);
            //所有的记录,如：所有号码，所有email
            if (dataRows == null || !(dataRows.size() > 0))
                continue;
            switch (mimeType) {
                case StructuredName.CONTENT_ITEM_TYPE://姓名信息
                    for (ContactDataAdapter row : dataRows) {
                        if (row != null && row instanceof Contact.ContactDataStructuredName) {
                            Contact.ContactDataStructuredName structuredName = (Contact.ContactDataStructuredName) row;
                            if (structuredName.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);
                                ContentValues values = structuredName.getContentValue();
                                tempBuilder.withValues(values);
                                ops.add(tempBuilder.build());
                            }
                        }
                    }

                    break;
                case CommonDataKinds.Nickname.CONTENT_ITEM_TYPE://昵称信息

                    break;
                case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE://电话信息
                    for (ContactDataAdapter row : dataRows) {
                        if (row != null && row instanceof Contact.ContactDataSipAddress) {
                            Contact.ContactDataSipAddress sipAddress = (Contact.ContactDataSipAddress) row;
                            if (sipAddress.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);
                                tempBuilder.withValues(sipAddress.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Phone.CONTENT_ITEM_TYPE://电话信息
                    for (Object object : dataRows) {
                        if (object != null && object instanceof Contact.ContactDataPhone) {
                            Contact.ContactDataPhone phone = (Contact.ContactDataPhone) object;

                            if (phone.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);
                                tempBuilder.withValues(phone.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Email.CONTENT_ITEM_TYPE://email
                    break;
                case CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE://地址信息
                    break;
                case CommonDataKinds.Organization.CONTENT_ITEM_TYPE://组织信息
                    for (Object object : dataRows) {
                        if (object != null && object instanceof Contact.ContactDataPhone) {
                            Contact.ContactDataOrgnization orgnization = (Contact.ContactDataOrgnization) object;
                            if (orgnization.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, mimeType)
                                        .withYieldAllowed(true);

                                tempBuilder.withValues(orgnization.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Event.CONTENT_ITEM_TYPE://事件提醒，生日，周年等
                    break;
                case CommonDataKinds.Note.CONTENT_ITEM_TYPE://备注信息
                    break;
                case CommonDataKinds.Im.CONTENT_ITEM_TYPE://im消息
                    for (Object object : dataRows) {
                        if (object != null && object instanceof Contact.ContactDataIm) {
                            Contact.ContactDataIm im = (Contact.ContactDataIm) object;

                            if (im.protocal != CommonDataKinds.Im.PROTOCOL_CUSTOM ||
                                    !PORTSIP_IM_PROTOCAL.equals(im.customProtocal)) {
                                continue;
                            }
                            if (im.dataAvailable()) {
                                tempBuilder = ContentProviderOperation.newInsert(uri)
                                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                        .withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                                        .withYieldAllowed(true);
                                tempBuilder.withValues(im.getContentValue());
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Website.CONTENT_ITEM_TYPE://网站
                    break;
                case CommonDataKinds.Photo.CONTENT_ITEM_TYPE://照片
                    break;
            }
        }

//        if (!visable) {
//            tempBuilder = ContentProviderOperation.newInsert(uri)
//                    .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
//                    .withValue(Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
//                    .withYieldAllowed(true);
//            tempBuilder.withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID,
//                    com.portgo.androidcontacts.ContactsContract.Groups.subScribeGroupId);
//            ops.add(tempBuilder.build());//加入subScribe组
//        }

        //批量执行插入
        try {
            ContentProviderResult[] results = resolver.applyBatch(com.portgo.androidcontacts.ContactsContract.AUTHORITY, ops);
            //插入成功返回的Uri集合

            if (results.length > rawIndex) {
                if (results[rawIndex].uri != null) {
                    contactid = (int) ContentUris.parseId(results[rawIndex].uri);
                }
            }

        } catch (Exception e) {
            contactid = -1;
            addResult.put("result", "-1");
            addResult.put("obj", "插入失败:" + e.getMessage());
        }
//        }

        if (addResult.size() == 0) {
            addResult.put("result", "0");
            contactid = -1;
            addResult.put("obj", "无效插入，联系人信息不完整！");
        }
        return contactid;
    }

    public static void deleteContacts(Context context, String authority, List<Integer> contacts, Uri contactUri, Uri rawContactUri) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }
        ContentResolver resolver = context.getContentResolver();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (Integer contact : contacts) {
            ContentProviderOperation op = ContentProviderOperation.newDelete(Uri.withAppendedPath(contactUri, "" + contact)).build();
            ops.add(op);
            op = ContentProviderOperation.newDelete(Uri.withAppendedPath(rawContactUri, "" + contact)).build();
            ops.add(op);
        }
        try {
            resolver.applyBatch(authority, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

    }

    public int insertFriend(Context context, List<Contact> contacts, Bitmap bitmap, boolean visable) {
        int contactid = -1;
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return contactid ;
        }

        HashMap<String, Object> addResult = new HashMap<String, Object>();
        //批量插入的内容集合
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentResolver resolver = context.getContentResolver();
        Set<Integer> allIndex = new HashSet<>();
        for (Contact contact : contacts) {
            if (contact.isEmpty()) {
                addResult.put("result", "0");
                addResult.put("obj", "无效插入，联系人信息不完整！");
                continue;
            }

            ContentProviderOperation op = null;
            int rawIndex = ops.size();
            allIndex.add(rawIndex);
            ContentProviderOperation.Builder builder = null;

            //数据表 uri
            Uri uri = com.portgo.androidcontacts.ContactsContract.Data.CONTENT_URI;

            //Uri uri = RawContacts.CONTENT_URI; content://com.android.contacts/raw_contacts
            //此处.withValue("account_name", null)一定要加，不然会抛NullPointerException
            //withYieldAllowed(true)//为了避免这种死锁的数据库
            op = ContentProviderOperation.newInsert(com.portgo.androidcontacts.ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                            ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED)//不做聚合
                    .withYieldAllowed(true)
                    .build();
            ops.add(op);
            HashMap<String, List<ContactDataAdapter>> contactData = contact.getContactData();
            Set<String> contentItems = contactData.keySet();
            ContentProviderOperation.Builder tempBuilder = null;
            for (String mimeType : contentItems) {
                List<ContactDataAdapter> rows = contactData.get(mimeType);
                if (rows == null || rows.size() <= 0) {
                    continue;
                }

                switch (mimeType) {
                    case StructuredName.CONTENT_ITEM_TYPE://姓名信息
                        for (ContactDataAdapter row : rows) {
                            if (row != null && row instanceof Contact.ContactDataStructuredName) {
                                Contact.ContactDataStructuredName structuredName = (Contact.ContactDataStructuredName) row;
                                if (structuredName.dataAvailable()) {
                                    tempBuilder = ContentProviderOperation.newInsert(uri)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                            .withValue(Data.MIMETYPE, mimeType)
                                            .withYieldAllowed(true);
                                    ContentValues values = structuredName.getContentValue();

                                    tempBuilder.withValues(values);
                                    ops.add(tempBuilder.build());
                                }
                            }
                        }
                        break;
                    case CommonDataKinds.Nickname.CONTENT_ITEM_TYPE://昵称信息
                        break;
                    case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE://电话信息
                    case CommonDataKinds.Phone.CONTENT_ITEM_TYPE://电话信息
                        for (Object obj : rows) {
                            if (obj != null && obj instanceof Contact.ContactDataNumber) {
                                Contact.ContactDataNumber contactNumber = (Contact.ContactDataNumber) obj;
                                if (contactNumber.dataAvailable()) {
                                    tempBuilder = ContentProviderOperation.newInsert(uri)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                            .withValue(Data.MIMETYPE, mimeType)
                                            .withYieldAllowed(true);
                                    tempBuilder.withValues(contactNumber.getContentValue());
                                    ops.add(tempBuilder.build());
                                }
                            }
                        }

                        break;
                    case CommonDataKinds.Email.CONTENT_ITEM_TYPE://email
                        break;
                    case CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE://地址信息
                        break;
                    case CommonDataKinds.Organization.CONTENT_ITEM_TYPE://组织信息
                        for (Object obj : rows) {
                            if (obj != null && obj instanceof Contact.ContactDataOrgnization) {
                                Contact.ContactDataOrgnization orgnization = (Contact.ContactDataOrgnization) obj;
                                if (orgnization.dataAvailable()) {
                                    tempBuilder = ContentProviderOperation.newInsert(uri)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                            .withValue(Data.MIMETYPE, mimeType)
                                            .withYieldAllowed(true);

                                    tempBuilder.withValues(orgnization.getContentValue());
                                    ops.add(tempBuilder.build());

                                }
                            }
                        }
                        break;
                    case CommonDataKinds.Event.CONTENT_ITEM_TYPE://事件提醒，生日，周年等
                        break;
                    case CommonDataKinds.Note.CONTENT_ITEM_TYPE://备注信息
                        break;
                    case CommonDataKinds.Im.CONTENT_ITEM_TYPE://im消息
                        for (Object obj : rows) {
                            if (obj != null && obj instanceof Contact.ContactDataIm) {
                                Contact.ContactDataIm contactIm = (Contact.ContactDataIm) obj;
                                if (contactIm.dataAvailable()) {
                                    //只处理定制协议
                                    if (contactIm.protocal != CommonDataKinds.Im.PROTOCOL_CUSTOM ||
                                            !PORTSIP_IM_PROTOCAL.equals(contactIm.customProtocal)) {
                                        continue;
                                    }

                                    tempBuilder = ContentProviderOperation.newInsert(uri)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                                            .withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                                            .withYieldAllowed(true);

                                    tempBuilder.withValues(contactIm.getContentValue());

                                    ops.add(tempBuilder.build());
                                }
                            }
                        }

                        break;
                    case CommonDataKinds.Website.CONTENT_ITEM_TYPE://网站
                        break;
                    case CommonDataKinds.Photo.CONTENT_ITEM_TYPE://照片
                        //                    Iterator photeRow = dataRows.entrySet().iterator();
                        //                    if (photeRow.hasNext()) {
                        //                        Map.Entry row = (Map.Entry) photeRow.next();
                        //                        Integer rowId = (Integer) row.getKey();//charu 更新
                        //                        HashMap<String, Object> datas = (HashMap<String, Object>) row.getValue();
                        //                        Bitmap bmp = (Bitmap) datas.get(CommonDataKinds.Photo.PHOTO);
                        //                        if (bmp != null) {
                        //                            tempBuilder = ContentProviderOperation.newInsert(uri)
                        //                                    .withValue(Data.RAW_CONTACT_ID, rawIndex)
                        //                                    .withValue(Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        //                                    .withYieldAllowed(true);
                        //
                        //                            final ByteArrayOutputStream os = new ByteArrayOutputStream();
                        //                            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        //                            byte[] avatar = os.toByteArray();
                        //                            tempBuilder.withValue(CommonDataKinds.Photo.PHOTO, avatar);
                        //                            ops.add(tempBuilder.build());
                        //                        }
                        //                    }
                        break;
                }
            }

            ops.add(ContentProviderOperation.newUpdate(com.portgo.androidcontacts.ContactsContract.Contacts.CONTENT_URI)
                    .withValueBackReference(com.portgo.androidcontacts.ContactsContract.Contacts._ID, rawIndex)
                    .withSelection(com.portgo.androidcontacts.ContactsContract.Contacts._ID + "=?", new String[1])
                    .withSelectionBackReference(0, rawIndex)
                    .withValue(com.portgo.androidcontacts.ContactsContract.Contacts.SEND_TO_VOICEMAIL, contact.getId())
                    .build());

            if (!visable) {
                tempBuilder = ContentProviderOperation.newInsert(uri)
                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                        .withValue(Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withYieldAllowed(true);
                tempBuilder.withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, com.portgo.androidcontacts.ContactsContract.Groups.subScribeGroupId);
                ops.add(tempBuilder.build());//加入subScribe组
            }

//            if (contact.getSipNumbers().size() > 0) {
//                Contact.ContactSipNumber sip = contact.getSipNumbers().get(0);
//                tempBuilder = ContentProviderOperation.newInsert(uri)
//                        .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
//                        .withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
//                        .withValue(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM)
//                        .withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, PORTSIP_IM_PROTOCAL)
//                        .withValue(CommonDataKinds.Im.DATA, sip.mNumberContent)
//                        .withValue(CommonDataKinds.Im.TYPE, CommonDataKinds.Im.TYPE_HOME)
//                        .withYieldAllowed(true);
//                ops.add(tempBuilder.build());
//            }
        }

        //批量执行插入
        try {
            ContentProviderResult[] results = resolver.applyBatch(com.portgo.androidcontacts.ContactsContract.AUTHORITY, ops);
            //插入成功返回的Uri集合

            Map<Long, String> uris = new HashMap<Long, String>();
            ArrayList<ContentProviderOperation> ops2 = new ArrayList<ContentProviderOperation>();
            int i = 0;
            for (Integer index : allIndex) {
                if (results.length > index && results[index].uri != null) {
                    long id = ContentUris.parseId(results[index].uri);
//                    long insertContactid =getContactId( mContext.getContentResolver(),
//                            com.portgo.androidcontacts.ContactsContract.RawContacts.CONTENT_URI,id);

//                    PortApplication.getLogUtils().d("ContentProviderResult","id"+id+"-"+insertContactid);
                    Contact contact = contacts.get(i++);
                    if (id > 0) {//SEND_TO_VOICEMAIL不等于0，意味着是从系统联系人加载的。
                        ops2.add(ContentProviderOperation.newUpdate(com.portgo.androidcontacts.ContactsContract.Contacts.CONTENT_URI)
                                .withSelection(com.portgo.androidcontacts.ContactsContract.Contacts._ID + "=?", new String[]{"" + id,})
                                .withValue(com.portgo.androidcontacts.ContactsContract.Contacts.SEND_TO_VOICEMAIL, contact.getId())
                                .build());
                    }


//                    try {
//                        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
//                    } catch (Exception e) {
//                        PortApplication.getLogUtils().e("Exception: ", e.getMessage());
//                    }

//                    uris.put(ContentUris.parseId(result.uri), result.uri.toString());
                }
            }
            if (ops2.size() > 0) {
                results = mContext.getContentResolver().applyBatch(com.portgo.androidcontacts.ContactsContract.AUTHORITY, ops2);
            }
            for (ContentProviderResult result : results) {
                if (result.uri != null) {
                    uris.put(ContentUris.parseId(result.uri), result.uri.toString());
                }
            }
        } catch (Exception e) {
            contactid = -1;
            addResult.put("result", "-1");
            addResult.put("obj", "插入失败:" + e.getMessage());
        }
//        }

//        if (addResult.size() == 0) {
//            addResult.put("result", "0");
//            contactid = -1;
//            addResult.put("obj", "无效插入，联系人信息不完整！");
//        }
        return contactid;
    }

//    static public void insertContactAvart(Context context, Bitmap bmp, long contactId, Uri datauri) {
//        if (bmp != null && contactId > 0) {
//            long rawContactId = ContactManager.getRawContactId(context,datauri,contactId);
//            ContentResolver resolver = context.getContentResolver();
//            ContentValues values = new ContentValues();
//            final ByteArrayOutputStream os = new ByteArrayOutputStream();
//            // 将Bitmap压缩成PNG编码，质量为100%存储
//            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
//            byte[] avatar = os.toByteArray();
//            values.put(Data.RAW_CONTACT_ID, rawContactId);
//            values.put(Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
//            values.put(CommonDataKinds.Photo.PHOTO, avatar);
//            resolver.insert(datauri, values);
//        }
//    }

    public static void setContactPhoto(Context context, Bitmap bmp,
                                       long contactid) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }
        ContentResolver resolver = context.getContentResolver();
        long personId = getRawContactId(context, ContactsContract.RawContacts.CONTENT_URI,contactid);
        byte[] avatar =null;
        ContentValues values = new ContentValues();
        int photoRow = -1;
        String where = ContactsContract.Data.RAW_CONTACT_ID + " = " + personId
                + " AND " + ContactsContract.Data.MIMETYPE + "=='"
                + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                + "'";

        if (bmp != null) {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            // 将Bitmap压缩成PNG编码，质量为100%存储
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
            avatar = os.toByteArray();
        }
        if(avatar==null){//将头像设置为空=删除头像
            resolver.delete(ContactsContract.Data.CONTENT_URI, where, null);
        }else {
            //查找当前的头像数据ID，
            Cursor cursor = CursorHelper.resolverQuery(resolver,ContactsContract.Data.CONTENT_URI, null, where,
                    null, null);
            int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
            if (CursorHelper.moveCursorToFirst(cursor)) {
                photoRow = cursor.getInt(idIdx);
            }
            CursorHelper.closeCursor(cursor);

            values.put(ContactsContract.Data.RAW_CONTACT_ID, personId);//插入一条数据，并将其设置为头像
            values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
            values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, avatar);
            values.put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

            if (photoRow >= 0) {//找到
                resolver.update(ContactsContract.Data.CONTENT_URI, values,
                        ContactsContract.Data._ID + " = " + photoRow, null);
            } else {//没找到，
                resolver.insert(ContactsContract.Data.CONTENT_URI, values);
            }
        }
    }

//    static public void updateContactAvart(Context context, Bitmap bmp, int photoId, long contactId, Uri datauri) {
//        if (photoId > 0 && contactId > 0) {
//            long rawContactId = ContactManager.getRawContactId(context,datauri,contactId);
//            ContentResolver resolver = context.getContentResolver();
//            ContentValues values = new ContentValues();
//            String where = Data.RAW_CONTACT_ID + "=? and " + Data.MIMETYPE + "=?";
//            String[] selection = new String[]{"" + rawContactId, CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
//            if (bmp != null) {
//                final ByteArrayOutputStream os = new ByteArrayOutputStream();
//                // 将Bitmap压缩成PNG编码，质量为100%存储
//                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
//                byte[] avatar = os.toByteArray();
//                values.put(CommonDataKinds.Photo.PHOTO, avatar);
//                resolver.update(datauri, values, where, selection);
//            } else {
//                resolver.delete(datauri, where, selection);
//            }
//        }
//    }

    static  public HashMap<String, Object> updateContact(Context context, Contact contact, Bitmap bitmap, Uri contactUri, Uri dataUri) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return null;
        }

        HashMap<String, Object> addResult = new HashMap<String, Object>();
        if (contact.isEmpty()) {
            addResult.put("result", "0");
            addResult.put("obj", "无效插入，联系人信息不完整！");
            return addResult;
        }
        //批量插入的内容集合
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentResolver resolver = context.getContentResolver();
        String rawContactId;
        if (ContactsContract.Contacts.CONTENT_URI.equals(contactUri)) {
            rawContactId = "" + getRawContactId(context, ContactsContract.RawContacts.CONTENT_URI, contact.getId());//
        } else {
            rawContactId = "" + getRawContactId(context, com.portgo.androidcontacts.ContactsContract.RawContacts.CONTENT_URI, contact.getId());//
        }

        ContentProviderOperation.Builder tempBuilder = null;
        HashMap<String, List<ContactDataAdapter>> contactData = contact.getContactData();
        Set<String> contentItems = contactData.keySet();

        for (String mimeType : contentItems) {
            List<ContactDataAdapter> rows = contactData.get(mimeType);

            if (rows == null && !(rows.size() > 0))
                continue;

            switch (mimeType) {
                case StructuredName.CONTENT_ITEM_TYPE://姓名信息
                    for (ContactDataAdapter row : rows) {
                        tempBuilder = null;
                        if (row != null && row instanceof Contact.ContactDataStructuredName) {
                            Contact.ContactDataStructuredName structuredName = (Contact.ContactDataStructuredName) row;
                            DATA_ACTION action = structuredName.getAction();
                            switch (action) {
                                case ACTION_ADD://增
                                    if (structuredName.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newInsert(dataUri)
                                                .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                                .withValue(Data.MIMETYPE, mimeType)
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(structuredName.getContentValue());
                                    }
                                    break;
                                case ACTION_DEL://删
                                    if(structuredName.mId>Contact.INVALIDE_ID) {
                                        tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                .withSelection(Data._ID + " =?", new String[]{"" + structuredName.mId})
                                                .withYieldAllowed(true);
                                    }
                                    break;
                                case ACTION_NONE://改
                                    if (structuredName.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newUpdate(dataUri)
                                                .withSelection(Data._ID +" =? and "+Data.RAW_CONTACT_ID + " =? and " + Data.MIMETYPE + " =?",
                                                        new String[]{""+structuredName.mId,"" + rawContactId, StructuredName.CONTENT_ITEM_TYPE})
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(structuredName.getContentValue());
                                    }else{
                                        if(structuredName.mId>Contact.INVALIDE_ID) {//当所有有意义的字段都置空时，删除此条记录
                                            tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                    .withSelection(Data._ID + " =?", new String[]{"" + structuredName.mId})
                                                    .withYieldAllowed(true);
                                        }
                                    }
                                    break;

                            }

                        }
                        if(tempBuilder!=null){
                            ops.add(tempBuilder.build());
                        }
                    }
                    break;
                case CommonDataKinds.Nickname.CONTENT_ITEM_TYPE://昵称信息
                    break;
                case CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE://电话信息
                case CommonDataKinds.Phone.CONTENT_ITEM_TYPE://电话信息

                    for (ContactDataAdapter row : rows) {
                        tempBuilder = null;
                        if (row != null && row instanceof Contact.ContactDataNumber) {
                            Contact.ContactDataNumber contactNumber = (Contact.ContactDataNumber) row;
                            DATA_ACTION action = contactNumber.getAction();
                            switch (action){
                                case ACTION_ADD://增
                                    if(contactNumber.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newInsert(dataUri)
                                                .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                                .withValue(Data.MIMETYPE, mimeType)
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(contactNumber.getContentValue());
                                    }
                                break;
                                case ACTION_DEL://删
                                    if(contactNumber.mId>Contact.INVALIDE_ID) {
                                        tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                .withSelection(Data._ID + " =?", new String[]{"" + contactNumber.mId})
                                                .withYieldAllowed(true);
                                        //tempBuilder.withValues(contactNumber.getContentValue());
                                    }
                                break;
                                case ACTION_NONE://改
                                    if(contactNumber.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newUpdate(dataUri)
                                                .withSelection(Data._ID + " =? and " + Data.RAW_CONTACT_ID + " =? and " + Data.MIMETYPE + " =?",
                                                        new String[]{"" + contactNumber.mId, "" + rawContactId, mimeType})
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(contactNumber.getContentValue());
                                    }else{
                                    if(contactNumber.mId>Contact.INVALIDE_ID) {//当所有有意义的字段都置空时，删除此条记录
                                        tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                .withSelection(Data._ID + " =?", new String[]{"" + contactNumber.mId})
                                                .withYieldAllowed(true);
                                    }
                                }
                                break;
                            }
                            if (tempBuilder != null) {
                                ops.add(tempBuilder.build());

                            }
                        }
                    }
                    break;
                case CommonDataKinds.Email.CONTENT_ITEM_TYPE://email
                    break;
                case CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE://地址信息
                    break;
                case CommonDataKinds.Organization.CONTENT_ITEM_TYPE://组织信息
                    for (ContactDataAdapter row : rows) {
                        tempBuilder = null;
                        if (row != null && row instanceof Contact.ContactDataOrgnization) {

                            Contact.ContactDataOrgnization orgnization = (Contact.ContactDataOrgnization) row;
                            DATA_ACTION action = orgnization.getAction();
                            switch (action) {
                                case ACTION_ADD:
                                    if(orgnization.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newInsert(dataUri)
                                                .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                                .withValue(Data.MIMETYPE, mimeType)
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(orgnization.getContentValue());
                                    }
                                    break;
                                case ACTION_DEL:
                                    if(orgnization.mId>Contact.INVALIDE_ID) {
                                        tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                .withSelection(Data._ID + " =? and " + Data.RAW_CONTACT_ID + " =? and " +
                                                                Data.MIMETYPE + " =? ",
                                                        new String[]{"" + orgnization.mId, rawContactId, mimeType}).withYieldAllowed(true);
                                    }
                                    break;
                                case ACTION_NONE://改
                                    if(orgnization.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newUpdate(dataUri)
                                                .withSelection(Data._ID + " =? and " + Data.RAW_CONTACT_ID + " =? and " +
                                                                Data.MIMETYPE + " =? ",
                                                        new String[]{"" + orgnization.mId, rawContactId, mimeType}).withYieldAllowed(true);
                                        tempBuilder.withValues(orgnization.getContentValue());
                                    }else{
                                        if(orgnization.mId>Contact.INVALIDE_ID) {//当修改后，此字段无效时，删除
                                            tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                    .withSelection(Data._ID + " =? and " + Data.RAW_CONTACT_ID + " =? and " +
                                                                    Data.MIMETYPE + " =? ",
                                                            new String[]{"" + orgnization.mId, rawContactId, mimeType}).withYieldAllowed(true);
                                        }
                                    }
                                    break;
                            }


                            if (tempBuilder != null) {
                                ops.add(tempBuilder.build());
                            }
                        }
                    }
                    break;
                case CommonDataKinds.Event.CONTENT_ITEM_TYPE://事件提醒，生日，周年等
                    break;
                case CommonDataKinds.Note.CONTENT_ITEM_TYPE://备注信息
                    break;
                case CommonDataKinds.Im.CONTENT_ITEM_TYPE://im消息
                    for (ContactDataAdapter row : rows) {
                        tempBuilder = null;
                        if (row != null && row instanceof Contact.ContactDataIm) {
                            Contact.ContactDataIm contactIm = (Contact.ContactDataIm) row;

                            DATA_ACTION action = contactIm.getAction();
                            //im协议只处理我们的
                            if (!contactIm.dataAvailable()||contactIm.protocal != com.portgo.androidcontacts.ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM ||
                                    !com.portgo.androidcontacts.ContactsContract.CommonDataKinds.Im.PORTSIP_IM_PROTOCAL.equals(contactIm.customProtocal)) {
                                continue;
                            }
                            switch(action) {
                                case ACTION_ADD:
                                    if(contactIm.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newInsert(dataUri)
                                                .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                                .withValue(Data.MIMETYPE, mimeType)
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(contactIm.getContentValue());
                                    }
                                    break;
                                case ACTION_DEL:
                                    if(contactIm.mId>0) {
                                        tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                .withSelection(Data._ID + " =?", new String[]{"" + contactIm.mId})
                                                .withYieldAllowed(true);

                                    }
                                    break;
                                case ACTION_NONE://改
                                    if(contactIm.dataAvailable()) {
                                        tempBuilder = ContentProviderOperation.newUpdate(dataUri)
                                                .withSelection(Data._ID + " =? and " + Data.RAW_CONTACT_ID + " =? and " + Data.MIMETYPE + " =?",
                                                        new String[]{"" + contactIm.mId, "" + rawContactId, mimeType})
                                                .withYieldAllowed(true);
                                        tempBuilder.withValues(contactIm.getContentValue());
                                    }else{
                                        if(contactIm.mId>0) {
                                            tempBuilder = ContentProviderOperation.newDelete(dataUri)
                                                    .withSelection(Data._ID + " =?", new String[]{"" + contactIm.mId})
                                                    .withYieldAllowed(true);

                                        }
                                    }
                                    break;
                            }

                            if (tempBuilder != null)
                                ops.add(tempBuilder.build());
                        }
                    }
                    break;
                case CommonDataKinds.Website.CONTENT_ITEM_TYPE://网站
                    break;
                case CommonDataKinds.Photo.CONTENT_ITEM_TYPE://照片
                    break;
            }
        }
        String authority = ContactsContract.AUTHORITY;
        if (contactUri.equals(com.portgo.androidcontacts.ContactsContract.Contacts.CONTENT_URI)) {
            authority = com.portgo.androidcontacts.ContactsContract.AUTHORITY;
        }

        //批量执行插入
        try {
            ContentProviderResult[] results = resolver.applyBatch(authority, ops);
            //插入成功返回的Uri集合
            Map<Long, String> uris = new HashMap<Long, String>();
            for (ContentProviderResult result : results) {
//						PortApplication.getLogUtils().i(Const.APPTAG, result.toString());
                if (result.uri != null) {
                    uris.put(ContentUris.parseId(result.uri), result.uri.toString());
                }
            }
            if (uris.size() > 0) {
                addResult.put("result", "1");
                addResult.put("obj", uris);
            }
        } catch (Exception e) {
//					PortApplication.getLogUtils().i(Const.APPTAG, e.getMessage());
            addResult.put("result", "-1");
            addResult.put("obj", "insert failed:" + e.getMessage());
        }
//        }

        return addResult;
    }

    public static String getTypeRes(Context context,String mimetype, int type) {
        if(mimeTypeRes == null){
            loadMimeTypeRes(context);
        }

        LinkedHashMap<Integer, String> typeRes = mimeTypeRes.get(mimetype);
        if (typeRes != null) {
            return typeRes.get(type);
        }
        return null;
    }

    public static String[] getAllTypeRes(Context context, String mimetype) {
        if (mimeTypeRes == null) {
            loadMimeTypeRes(context);
        }
        LinkedHashMap<Integer, String> types = mimeTypeRes.get(mimetype);
        String[] typeres = null;
        if (types.size() > 0) {
            typeres = new String[types.size()];
            int i = 0;
            for (Iterator it = types.keySet().iterator(); it.hasNext(); ) {
                Object key = it.next();
                typeres[i++] = types.get(key);
            }
        }
        return typeres;
    }

    //
    public static void deleteContact(Context context,Uri contactUri, Uri rawContactUri, int ContactId) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return ;
        }
        ContentResolver  contentResolver  = context.getContentResolver();
        contentResolver.delete(Uri.withAppendedPath(contactUri, "" + ContactId), null, null);
        contentResolver.delete(Uri.withAppendedPath(rawContactUri, "" + ContactId), null, null);
    }


    public static boolean starContacts(Context context, long contactId, boolean starred) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return false;
        }
        ContentResolver  contentResolver  = context.getContentResolver();
        ContentValues values = new ContentValues();
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "" + contactId);
        values.put(ContactsContract.Contacts.STARRED, starred ? 1 : 0);
        contentResolver.update(uri, values, null, null);
        return true;
    }

    public static boolean starContacts(Context context, String authority, Uri uri, List<Integer> contacts, boolean starred) {
        if (!PermissionManager.testDangercePermission(context, Manifest.permission.WRITE_CONTACTS)) {
            return false;
        }
        ContentResolver  contentResolver  = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (long contactId : contacts) {
            ContentValues values = new ContentValues();

            Uri contactUri = Uri.withAppendedPath(uri, "" + contactId);
            ContentProviderOperation.Builder op = ContentProviderOperation.newUpdate(contactUri);
            op.withValue(ContactsContract.Contacts.STARRED, starred ? 1 : 0);
            ops.add(op.build());
        }
        try {
            contentResolver.applyBatch(authority, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        return true;
    }

    static HashMap<String, LinkedHashMap> mimeTypeRes;
    final static int SIP_TYPE[] = {ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME, ContactsContract.CommonDataKinds.SipAddress.TYPE_WORK,
            ContactsContract.CommonDataKinds.SipAddress.TYPE_OTHER, ContactsContract.CommonDataKinds.SipAddress.TYPE_CUSTOM};
    final static int PHONE_TYPE[] = {ContactsContract.CommonDataKinds.Phone.TYPE_HOME, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
            , ContactsContract.CommonDataKinds.Phone.TYPE_PAGER, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
            ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK, ContactsContract.CommonDataKinds.Phone.TYPE_CAR,
            ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN, ContactsContract.CommonDataKinds.Phone.TYPE_ISDN,
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX,
            ContactsContract.CommonDataKinds.Phone.TYPE_RADIO, ContactsContract.CommonDataKinds.Phone.TYPE_TELEX,
            ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER, ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT,
            ContactsContract.CommonDataKinds.Phone.TYPE_MMS, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM};

}
