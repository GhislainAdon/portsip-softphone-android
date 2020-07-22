package com.portgo.manager;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;

import java.util.ArrayList;

import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;

public class StatusUpdatesHelper extends Object {

    static public Uri insertStatusUpdate(Context context,long dataId, int presence, String status, Long timestamp,String[] handles,int labelResId,int iconResid,String res_package)
            throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (String handle:handles) {
            ops.add(ContentProviderOperation.newInsert(StatusUpdates.CONTENT_URI)
                    .withValue(StatusUpdates.PRESENCE, presence)
                    .withValue(StatusUpdates.STATUS, status)
                    .withValue(StatusUpdates.STATUS_TIMESTAMP, timestamp)
                    .withValue(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM)
                    .withValue(StatusUpdates.CUSTOM_PROTOCOL, PORTSIP_IM_PROTOCAL)
                    .withValue(StatusUpdates.IM_HANDLE, handle)
                    .withValue(StatusUpdates.STATUS_LABEL,labelResId)
                    .withValue(StatusUpdates.STATUS_ICON,iconResid)
                    .withValue(StatusUpdates.STATUS_RES_PACKAGE,res_package)
                    .build());
        }
        ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        return results[0].uri;
    }
}
