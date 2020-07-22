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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class NgnDateTimeUtils {
	static final DateFormat sDefaultDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final DateFormat sTimeFormat = new SimpleDateFormat("HH:mm:ss");

	public static String now(String dateFormat) {
	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	    return sdf.format(cal.getTime());
	}
	
	public static String now() {
	    Calendar cal = Calendar.getInstance();
	    return sDefaultDateFormat.format(cal.getTime());
	}
	
	public static Date parseDate(String date, DateFormat format){
		if(!NgnStringUtils.isNullOrEmpty(date)){
			try {
				return format == null ? sDefaultDateFormat.parse(date) : format.parse(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return new Date();
	}
	
	public static Date parseDate(String date){
		return parseDate(date, null);
	}
	
	public static boolean isSameDay(Date d1, Date today){
		return d1.getDay() == today.getDay() && d1.getMonth() == today.getMonth() && d1.getYear() == today.getYear();
	}

	public static boolean isSameDay(Calendar calDateA, Calendar calDateB) {

		return calDateA.get(Calendar.YEAR) == calDateB.get(Calendar.YEAR)
				&& calDateA.get(Calendar.MONTH) == calDateB.get(Calendar.MONTH)
				&& calDateA.get(Calendar.DAY_OF_MONTH) == calDateB
				.get(Calendar.DAY_OF_MONTH);
	}
	public static boolean isYesterdayDay(Date d1, Date today){
		return (today.getDay()-d1.getDay()==1) && d1.getMonth() == today.getMonth() && d1.getYear() == today.getYear();
	}
}
