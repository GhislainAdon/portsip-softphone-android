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
package com.portgo.customwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.portgo.R;

public class CustomDialog {
	public static AlertDialog show(Context context, int icon, String title, String msg,
								   String positiveText, DialogInterface.OnClickListener positive,
								   String negativeText, DialogInterface.OnClickListener negative) {
		AlertDialog.Builder builder;

		builder = new AlertDialog.Builder(context, R.style.alterdialog);
//		builder.setView(layout);
		builder.setIcon(icon);
		builder.setTitle(title);
		builder.setMessage(msg);

		if (positive != null && positiveText != null) {
			builder.setPositiveButton(positiveText, positive);
		}
		if (negative != null && negativeText != null) {
			builder.setNegativeButton(negativeText, negative);
		}
		AlertDialog dialog = builder.create();

		dialog.show();
		return dialog;
	}

	public static Dialog showTransferDialog(Activity context, View.OnClickListener listener){

		Dialog tansferDlg =new
				Dialog(context, R.style.dialog_no_board);
		tansferDlg.setContentView(R.layout.view_transfer_dlg);
		tansferDlg.setCancelable(true);
		((TextView)tansferDlg.findViewById(R.id.dialog_title)).setText(R.string.please_inputorselect_transfer_dest);
		tansferDlg.findViewById(R.id.transfer_dest_selector).
				setOnClickListener(listener);
		tansferDlg.findViewById(R.id.transfer_ok).
				setOnClickListener(listener);
		tansferDlg.findViewById(R.id.transfer_cancel).
				setOnClickListener(listener);
		tansferDlg.show();
		return tansferDlg;
	}


	public static Dialog showTipsDialog(Activity context,@StringRes int title, @StringRes int tips){

		final Dialog tansferDlg =new
				Dialog(context, R.style.dialog_no_board);
		tansferDlg.setContentView(R.layout.custom_tips_dialog);
		tansferDlg.setCancelable(true);
		((TextView)tansferDlg.findViewById(R.id.dialog_title)).setText(context.getString(title));
		((TextView)tansferDlg.findViewById(R.id.dialog_tips)).setText(context.getString(tips));
		tansferDlg.findViewById(R.id.dialog_ok).
				setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						tansferDlg.dismiss();
					}
				});
		tansferDlg.show();
		return tansferDlg;
	}
}
