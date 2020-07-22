package com.portgo.ui;
//

import android.app.Activity;
import android.app.AlertDialog;

import android.app.AppOpsManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.portgo.R;
import com.portgo.manager.AccountManager;
import com.portgo.manager.PermissionManager;
import com.portgo.manager.PortSipService;

//
public class PortActivityPermission extends AppCompatActivity {
    public static final int PERMISSIONS_GRANTED = 0;
    public static final int PERMISSIONS_DENIED = 1; //

    private static final int PERMISSION_REQUEST_CODE = 0; //
    private static final String EXTRA_PERMISSIONS =
            "extra_permission"; //
    private static final String PACKAGE_URL_SCHEME = "package:"; //

    public static void startActivityForResult(Activity activity, int requestCode, String... permissions) {
        Intent intent = new Intent(activity, PortActivityPermission.class);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || !getIntent().hasExtra(EXTRA_PERMISSIONS)) {
            throw new RuntimeException("PermissionsActivity!");
        }
        setContentView(R.layout.activity_permissions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override protected void onResume() {
        super.onResume();
        if(!AccountManager.allPermissionGranted(this)){
            showMissingPermissionDialog();
        }
    }


    final int OVERLAY_REQ = 0x944;
    private String[] getPermissions() {
        return getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
    }

    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void allPermissionsGranted() {
        setResult(PERMISSIONS_GRANTED);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllPermissionsGranted(grantResults)) {
            allPermissionsGranted();
        } else {
            showMissingPermissionDialog();
        }
    }

    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    static  AlertDialog permissionDialog = null;
    private void showMissingPermissionDialog() {
        if(permissionDialog==null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PortActivityPermission.this);
            builder.setTitle(R.string.permission_help_title);
            builder.setMessage(String.format(getString(R.string.permission_help_content),getString(R.string.app_name)));

            builder.setNegativeButton(R.string.permission_help_quit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setResult(PERMISSIONS_DENIED);
                    finish();
                    stopService(new Intent(PortActivityPermission.this,PortSipService.class));
                    permissionDialog= null;
                }
            });

            builder.setPositiveButton(R.string.permission_help_setting, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String[] permissions = getPermissions();

                    permissionDialog.dismiss();
                    permissionDialog= null;
                    requestPermissions(permissions);
                }
            });

            permissionDialog = builder.create();
            permissionDialog.show();
            permissionDialog.setCancelable(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(permissionDialog!=null) {
            permissionDialog.dismiss();
        }
        permissionDialog=null;
    }
}