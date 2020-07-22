package com.portgo.util;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import java.util.Map;
import java.util.TreeMap;
public  class SipTextFilter implements InputFilter {

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String filteString="";
        for(int i=0;i<source.length();i++){
            if(NgnUriUtils.isValidUriChar(""+source.charAt(i))){
                filteString+=source.charAt(i);
            }
        }
        return filteString;
    }
}