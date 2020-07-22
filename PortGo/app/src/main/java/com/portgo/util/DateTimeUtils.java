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

import com.portgo.R.string;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils extends NgnDateTimeUtils{

	static final DateFormat sDateFormat = DateFormat.getInstance();
	static final DateFormat shortFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	static final DateFormat shortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
	static final DateFormat sDateTimeFormat = DateFormat.getDateTimeInstance();
    static final DateFormat sDateFormatSHORT = DateFormat.getDateInstance(DateFormat.SHORT);
	static final DateFormat sTimeFormat = DateFormat.getTimeInstance();
	public static final SimpleDateFormat sFormat8601ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static final SimpleDateFormat sFormat8601ymd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static String sTodayName;
	static String sYesterdayName;
	
	public static String getTodayName(Context context){
		if(sTodayName == null){
			sTodayName = context.getResources().getString(string.day_today);
		}
		return sTodayName;
	}
	
	public static String getYesterdayName(Context context){
		if(sYesterdayName == null){
			sYesterdayName = context.getResources().getString(string.day_yesterday);
		}
		return sYesterdayName;
	}
	
	public static String getFriendlyDateString(final Date date,Context context){
		final Date today = new Date();
        if (DateTimeUtils.isSameDay(date, today)){
            return String.format("%s %s", getTodayName(context), sTimeFormat.format(date));
        }
        else if (DateTimeUtils.isYesterdayDay(date,today)){
            return String.format("%s %s", getYesterdayName(context), sTimeFormat.format(date));
        }
        else{
            return sDateTimeFormat.format(date);
        }
	}

	public static String getFriendlyDateStringShort(final Date date,Context context){
		final Date today = new Date();
		if (DateTimeUtils.isSameDay(date, today)){
			return String.format("%s %s", getTodayName(context), shortTimeFormat.format(date));
		}
		else if (DateTimeUtils.isYesterdayDay(date,today)){
			return String.format("%s %s", getYesterdayName(context), shortTimeFormat.format(date));
		}
		else{
			return shortFormat.format(date);
		}
	}

	public static String getFriendlyDayString(final Date date,Context context){
		final Date today = new Date();
		if (DateTimeUtils.isSameDay(date, today)){
			return getTodayName(context);
		}
		else if (DateTimeUtils.isYesterdayDay(date,today)){
			return getYesterdayName(context);
		}
		else{
			return sDateFormatSHORT.format(date);
		}
	}

	public static String dateLong2StringGTM(long time){
		return new Date(time).toGMTString();
	}

	public static String dateLong2String8601(long time){
		return  sFormat8601ymdhms.format(new Date(time)).toString();
	}

	public static String dateLong2String8601ymd(long time){
		return  sFormat8601ymd.format(new Date(time)).toString();
	}

	public static String dateGMT2String8601(String time){
		return  sFormat8601ymdhms.format(new Date(time)).toString();
	}

    public static long dateString86012long(String time){
        try {
            return sFormat8601ymdhms.parse(time).getTime();
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return 0;
    }
	public static long dateString2Long(String time){
		if(time!=null)
		return Date.parse(time);
		return 0;
	}
}
