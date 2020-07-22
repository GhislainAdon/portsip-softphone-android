package com.portgo.util;

import androidx.annotation.NonNull;
import android.text.InputFilter;
import android.text.Spanned;

public class SpecialCharTrimFilter implements InputFilter {
	final String SpecialChar;

	public SpecialCharTrimFilter(@NonNull String specialChar) {
		SpecialChar = specialChar;
	}

	//spanned
	@Override
	public CharSequence filter(CharSequence source, int start, int stop, Spanned spanned, int i2, int i3) {
		String input = "";
		for (int i = 0; i < source.length(); i++) {
			if (SpecialChar.indexOf(source.charAt(i)) <0) {
				input+=source.charAt(i);
			}
		}
		if(input.length()==source.length()){
			return null;
		}
		return input;
	}
}