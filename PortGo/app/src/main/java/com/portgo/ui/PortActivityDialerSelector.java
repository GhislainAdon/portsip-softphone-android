package com.portgo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.PhoneStateReceiver;
public class PortActivityDialerSelector extends Activity implements DialogInterface.OnDismissListener {

    String phoneNumber;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(BuildConfig.PORT_ACTION_DIALERVIEW.equals(intent.getAction())) {
            phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }
		dialogList();
	}

	private void dialogList() {
		final String items[] = {"Phone", getString(R.string.app_name)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this,3);
		builder.setTitle(phoneNumber);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case 0:
						Intent sysDialer = new Intent();
						sysDialer.setAction(Intent.ACTION_CALL);
						sysDialer.setData(Uri.parse("tel:" + phoneNumber+ PhoneStateReceiver.INTERNAL_CALL));
						startActivity(sysDialer);
						break;
					case 1:
						Uri uri = Uri.fromParts("tel", phoneNumber, null);
						Intent appDialer = new Intent(PortActivityDialerSelector.this,PortActivityLogin.class);
						appDialer.setAction(Intent.ACTION_CALL);
						appDialer.setData(uri);
						startActivity(appDialer);
                        break;
				}
                dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(this);
        dialog .show();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.finish();
    }
}
