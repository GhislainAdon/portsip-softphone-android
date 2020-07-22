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

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.portgo.manager.Contact;
import com.portgo.manager.MessageEvent;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NgnStringUtils {
	private static MessageDigest sMD5Digest;

	public static String emptyValue() {
		return "";
	}

	public static String nullValue() {
		return "(null)";
	}

	static HashMap<Character, Integer> l2NHashMap = new HashMap<Character, Integer>() {
		{
			put('a', 2);
			put('b', 2);
			put('c', 2);
			put('d', 3);
			put('e', 3);
			put('f', 3);
			put('g', 4);
			put('h', 4);
			put('i', 4);
			put('j', 5);
			put('k', 5);
			put('l', 5);
			put('m', 6);
			put('n', 6);
			put('o', 6);
			put('p', 7);
			put('q', 7);
			put('r', 7);
			put('s', 7);
			put('t', 8);
			put('u', 8);
			put('v', 8);
			put('w', 9);
			put('x', 9);
			put('y', 9);
			put('z', 9);
		}
	};

//	public static boolean isNullOrEmpty(String s){
//		return ((s == null) || ("".equals(s)));
//	}


	public static boolean containsIgnoreUpCase(String s1, String s2) {
		if (TextUtils.isEmpty(s1)) {
			return false;
		} else {
			return s1.toUpperCase().contains(s2);
		}
	}

	public static boolean firstCharactIsletter(String s) {
		char c = s.charAt(0);
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
	}

	public static boolean isNullOrEmpty(Object ob) {
		if (ob == null)
			return true;
		if (ob instanceof String) {
			return ((String) ob).isEmpty();
		}
		if (ob instanceof Collection) {
			return ((Collection) ob).isEmpty();
		}

		return false;
	}

	public static String getReadAbleSize(long size) {
		String result;
		float fSize = size;
		if (size < 1024) {//B
			if (size <= 0) {
				result = "Empty File";
			} else {
				result = String.format(Locale.getDefault(), "%.2f %s", fSize, "B");
			}
		} else if (size >> 10 < 1024) {//KB
			fSize /= 1024;
			result = String.format(Locale.getDefault(), "%.2f %s", fSize, "KB");
		} else {//MB
			fSize = size >> 10;
			fSize /= 1024;
			result = String.format(Locale.getDefault(), "%.2f %s", fSize, "MB");
		}
		return result;
	}

	public static String getAvatarText(String disName) {
		String avatarText;
		if (disName != null) {
			if (disName.length() > 1) {
				avatarText = disName.substring(0, 2);
				if (avatarText.getBytes().length > 2) {
					avatarText = avatarText.substring(0, 1);
				}
			} else {
				avatarText = disName;
			}
		} else {
			avatarText = "";
		}

		return avatarText.toLowerCase();
	}

	public static boolean startsWith(String s, String prefix, boolean ignoreCase) {
		if (s != null && prefix != null) {
			if (ignoreCase) {
				return s.toLowerCase().startsWith(prefix.toLowerCase());
			} else {
				return s.startsWith(prefix);
			}
		}
		return s == null && prefix == null;
	}

	public static boolean equals(String s1, String s2, boolean ignoreCase) {
		if (s1 != null && s2 != null) {
			if (ignoreCase) {
				return s1.equalsIgnoreCase(s2);
			} else {
				return s1.equals(s2);
			}
		} else {
			return ((s1 == null && s2 == null));
		}
	}

	public static String unquote(String s, String quote) {
		if (!NgnStringUtils.isNullOrEmpty(s) && !NgnStringUtils.isNullOrEmpty(quote)) {
			if (s.startsWith(quote) && s.endsWith(quote)) {
				return s.substring(1, s.length() - quote.length());
			}
		}
		return s;
	}

	public static String quote(String s, String quote) {
		if (!NgnStringUtils.isNullOrEmpty(s) && !NgnStringUtils.isNullOrEmpty(quote)) {
			return quote.concat(s).concat(quote);
		}
		return s;
	}

	public static long parseLong(String value, long defaultValue) {
		try {
			if (NgnStringUtils.isNullOrEmpty(value)) {
				return defaultValue;
			}
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return defaultValue;
	}

	public static boolean isNumber(String string) {
		try {
			Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static int parseInt(String value, int defaultValue) {
		try {
			if (NgnStringUtils.isNullOrEmpty(value)) {
				return defaultValue;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return defaultValue;
	}

	public static String getMD5(String str) {
		if (str != null) {
			try {
				final BigInteger bigInt;
				if (sMD5Digest == null) {
					sMD5Digest = MessageDigest.getInstance("MD5");
				}
				synchronized (sMD5Digest) {
					sMD5Digest.reset();
					bigInt = new BigInteger(1, sMD5Digest.digest(str.getBytes("UTF-8")));
				}
				String hash = bigInt.toString(16);
				while (hash.length() < 32) {
					hash = "0" + hash;
				}
				return hash;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;
	}

	public static byte[] getMD5Digest(String str) {
		if (str != null) {
			try {
				if (sMD5Digest == null) {
					sMD5Digest = MessageDigest.getInstance("MD5");
				}
				synchronized (sMD5Digest) {
					sMD5Digest.reset();
					return sMD5Digest.digest(str.getBytes("UTF-8"));
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;
	}

//	public static String getPingYin(String inputString) {
//		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
//		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
//		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//		format.setVCharType(HanyuPinyinVCharType.WITH_V);
//		char[] input = inputString.trim().toCharArray();
//		String output = "";
//		try {
//			for (char curchar : input) {
//				if (java.lang.Character.toString(curchar).matches("[\\u4E00-\\u9FA5]+")) {
//					String[] temp = PinyinHelper.toHanyuPinyinStringArray(curchar, format);
//					output += temp[0];
//				} else
//					output += java.lang.Character.toString(curchar);
//			}
//		} catch (BadHanyuPinyinOutputFormatCombination e) {
//			e.printStackTrace();
//		}
//		return output;
//	}

//	public static String getPingYinFirstChar(String inputString) {
//		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
//		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
//		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//		format.setVCharType(HanyuPinyinVCharType.WITH_V);
////		char[] input = null;
//		char curchar = inputString.charAt(0);
//		String output = "";
//		try {
////			for (char curchar : input) {
//				if (java.lang.Character.toString(curchar).matches("[\\u4E00-\\u9FA5]+")) {
//					String[] temp = PinyinHelper.toHanyuPinyinStringArray(curchar, format);
//					output += temp[0];
//				} else
//					output += java.lang.Character.toString(curchar);
////			}
//		} catch (BadHanyuPinyinOutputFormatCombination e) {
//			e.printStackTrace();
//		}
//		return output.substring(0,1);
//	}

	private final static int[] li_SecPosValue = {1601, 1637, 1833, 2078, 2274,
			2302, 2433, 2594, 2787, 3106, 3212, 3472, 3635, 3722, 3730, 3858,
			4027, 4086, 4390, 4558, 4684, 4925, 5249, 5590};
	private final static String[] lc_FirstLetter = {"a", "b", "c", "d", "e",
			"f", "g", "h", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
			"t", "w", "x", "y", "z"};

	/**
	 * 取得给定汉字串的首字母串,即声母串
	 *
	 * @param str 给定汉字串
	 * @return 声母串
	 */
	static public String getAllFirstLetter(String str) {
		if (str == null || str.trim().length() == 0) {
			return "";
		}

		String _str = "";
		for (int i = 0; i < str.length(); i++) {
//            _str = _str + getFirstLetter(str.substring(i, i + 1));
			_str = _str + getHanziFirstLetter(str.substring(i, i + 1));
		}

		return _str;
	}

	/**
	 * 取得给定汉字的首字母,即声母
	 *
	 * @param chinese 给定的汉字
	 * @return 给定汉字的声母
	 */
	static public String getFirstLetter(String chinese) {
		if (chinese == null || chinese.trim().length() == 0) {
			return "";
		}
		chinese = conversionStr(chinese.substring(0, 1), "GB2312", "ISO8859-1");

		if (chinese.length() > 1) // 判断是不是汉字
		{
			int li_SectorCode = (int) chinese.charAt(0); // 汉字区码
			int li_PositionCode = (int) chinese.charAt(1); // 汉字位码
			li_SectorCode = li_SectorCode - 160;
			li_PositionCode = li_PositionCode - 160;
			int li_SecPosCode = li_SectorCode * 100 + li_PositionCode; // 汉字区位码
			if (li_SecPosCode > 1600 && li_SecPosCode < 5590) {
				for (int i = 0; i < 23; i++) {
					if (li_SecPosCode >= li_SecPosValue[i]
							&& li_SecPosCode < li_SecPosValue[i + 1]) {
						chinese = lc_FirstLetter[i];
						break;
					}
				}
			} else // 非汉字字符,如图形符号或ASCII码
			{
				chinese = conversionStr(chinese, "ISO8859-1", "GB2312");
				chinese = chinese.substring(0, 1);
			}
		}

		return chinese;
	}

	static HashMap<String, String> map = new HashMap<String, String>() {
		{
			put("啊", "A");
			put("芭", "B");
			put("擦", "C");
			put("搭", "D");
			put("蛾", "E");
			put("发", "F");
			put("哈", "H");
			put("乁", "I");
			put("击", "J");
			put("喀", "K");
			put("垃", "L");
			put("妈", "M");
			put("拿", "N");
			put("哦", "O");
			put("啪", "P");
			put("期", "Q");
			put("然", "R");
			put("撒", "S");
			put("塌", "T");
			put("挖", "W");
			put("昔", "X");
			put("压", "Y");
			put("铡", "Z");
			put("座", "Z");
		}
	};

	static public synchronized String getHanziFirstLetter(String str) {
		if (!NgnStringUtils.isNullOrEmpty(str)) {
			String sub = str.substring(0, 1);
			String letter = map.get(sub);
			if (letter == null) {
				map.put(sub, sub);
				List<String> list = new ArrayList<>(map.keySet());
				Collator compare = Collator.getInstance(Locale.CHINESE);
				Collections.sort(list, compare);
				String prekey = sub;
				if (list.get(0).equals(sub) || list.get(list.size() - 1).equals(sub)) {//超出可排序汉字范围
					map.remove(sub);
					return sub;
				}

				for (String key : list) {
					if (key.equals(sub)) {
						map.remove(sub);
						return map.get(prekey);
					}
					prekey = key;
				}
			}
			return letter;
		}
		return null;
	}

	public static String getDescString(String description){
		final int MAX_DESC = 28;
		int desLen = description.length();
		int descSize = MAX_DESC;
		if(desLen>MAX_DESC){
				// 编译正则表达式
			description = description.substring(0,description.length()> MAX_DESC*2? MAX_DESC*2: description.length());
			Pattern pattern = Pattern.compile("\\[:.+?\\]");
			String[] strings = pattern.split(description);
			Matcher matcher = pattern.matcher(description);
			while (matcher.find()){
				if(matcher.start()<MAX_DESC&&matcher.end()>MAX_DESC){
					descSize = matcher.start();
					break;
				}
			}
			if(descSize>0){
				description = description.substring(0, descSize);
			}

			description += "...";
		}
		return description;
	}
	/**
	 * 将字符串转换成9宫格对应的数字
	 */

	public static String letter2NumberIn9Path(String str) {
		String result = "";
		if (isNullOrEmpty(str))
			return result;
		str = str.toLowerCase();
		for (char character : str.toCharArray()) {
			Integer num = l2NHashMap.get(character);
			if (num != null) {
				result += (int) num;
			} else {
				result += character;
			}
		}

		return result;
	}

	/**
	 * 字符串编码转换
	 *
	 * @param str           要转换编码的字符串
	 * @param charsetName   原来的编码
	 * @param toCharsetName 转换后的编码
	 * @return 经过编码转换后的字符串
	 */
	static private String conversionStr(String str, String charsetName, String toCharsetName) {
		try {
			str = new String(str.getBytes(charsetName), toCharsetName);
		} catch (UnsupportedEncodingException ex) {
			System.out.println("字符串编码转换异常：" + ex.getMessage());
		}
		return str;
	}

	public static boolean isValidMD5String(String md5String) {
		if (md5String != null) {
			return md5String.length() == 32;
		}
		return false;
	}

	//获取可写的文件副本，如果文件不存在，返回本身fileName.xxx，如果已经存在，fileName+i.xxx
	public static File getFileFromOriginal(final File file) throws IOException {
		if(!file.getParentFile().exists()){
			file.mkdirs();
			return file;
		}
		if(file.isDirectory()){
			throw new IOException("getFileFromOriginal");
		}
		if (!file.exists()) {
			return file;
		} else {
			File fileCopy =null;
			String name = file.getName();
			String extName ="";
			int index = name.lastIndexOf('.');
			if(0<index&&index<name.length()) {
				extName = name.substring(index,name.length());
				name = name.substring(0,index);
			}
			int number=1;
			do {
				fileCopy = new File(file.getParent(),name+number+extName);
				number++;
			}while (fileCopy.exists());
			return fileCopy;
		}
	}
}