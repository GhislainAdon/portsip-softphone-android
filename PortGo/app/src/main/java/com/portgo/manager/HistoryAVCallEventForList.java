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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.portgo.database.DBHelperBase;
import com.portgo.util.DateTimeUtils;
import com.portgo.util.NgnMediaType;
import com.portgo.util.NgnUriUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class HistoryAVCallEventForList {
    HistoryAVCallEvent event;
    Contact attachContact;
    int count;
    public HistoryAVCallEvent getEvent(){
        return event;
    }
    public HistoryAVCallEventForList(HistoryAVCallEvent event){
        this.event = event;
    }

    public void setAttachContact(Contact contact){
        attachContact = contact;
    }
    public Contact getAttachContact(){
        return attachContact;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
