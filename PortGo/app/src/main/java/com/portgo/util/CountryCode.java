package com.portgo.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.portgo.R;

public class CountryCode {

    public static String GetCountryZipCode(Context Context) {
        String CountryID = "";
        String CountryZipCode = "";
        TelephonyManager manager = (TelephonyManager) Context.getSystemService(Context.TELEPHONY_SERVICE);
        CountryID = manager.getSimCountryIso().toUpperCase();
        String[] rl = Context.getResources().getStringArray(R.array.countrycodes);
        for (int i = 0; i < rl.length; i++) {
            String[] g = rl[i].split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    public static String GetCountryZipCode(Context Context,String phoneNumber) {//   HashMap<Integer, String> countryCodeMap = new HashMap<>();
        if(TextUtils.isEmpty(phoneNumber))
            return null;
        String CountryZipCode = "";
        String[] rl = Context.getResources().getStringArray(R.array.countrycodes);
        for (int i = 0; i < rl.length; i++) {
            String[] g = rl[i].split(",");
            if (phoneNumber.startsWith(g[0])) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }
}
