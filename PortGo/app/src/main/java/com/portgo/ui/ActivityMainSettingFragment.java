package com.portgo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.AccountManager;
import com.portgo.manager.UserAccount;
import com.portgo.util.CropTakePictrueUtils;
import com.portgo.util.ImagePathUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.view.RoundedDrawable;
import com.portgo.view.RoundedImageView;

import java.io.File;
import java.util.Observable;
import java.util.Observer;


public class ActivityMainSettingFragment extends PortBaseFragment implements View.OnClickListener, Observer {
	int[] icons;
    CropTakePictrueUtils cropTakePictrueUtils;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        cropTakePictrueUtils = new CropTakePictrueUtils(baseActivity);
		return inflater.inflate(R.layout.activity_main_setting_fragment, null);
	}

	Toolbar toolbar;
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		initView();
        AccountManager.getInstance().addObserver(this);
		toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        showToolBar(view,getString(R.string.portgo_title_setting));

        setAccountStatus();
	}


    @Override
    public void onDestroyView() {
        AccountManager.getInstance().deleteObserver(this);
        super.onDestroyView();
    }

    RoundedImageView iguseravatar;
    TextView txuseravatar,txBalance;
	void initView() {
        TextView txusername = (TextView) getView().findViewById(R.id.activity_main_fragment_setting_username);
        TextView txdescription = (TextView) getView().findViewById(R.id.fragment_setting_status_description);
        iguseravatar = (RoundedImageView) getView().findViewById(R.id.user_avatar_image);
        txuseravatar = (TextView) getView().findViewById(R.id.user_avatar_text);
        txuseravatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,getResources().getInteger(R.integer.setting_avatar_textsize));
        setViewClickListener(R.id.line_set_about,this);

        setViewClickListener(R.id.line_set_code,this);
        setViewClickListener(R.id.line_set_user,this);
        setViewClickListener(R.id.line_set_set,this);
        setViewClickListener(R.id.line_set_balance,this);
        setViewClickListener(R.id.fragment_setting_status,this);

        txuseravatar.setOnClickListener(this);
        iguseravatar.setOnClickListener(this);

        txBalance = (TextView) getView().findViewById(R.id.tx_set_balance);
        txBalance.setText("$:10");
        UserAccount defAccount = AccountManager.getDefaultUser(baseActivity);
        Bitmap bitmap = null;
        if (defAccount != null) {
            String disName = defAccount.getDisplayDefaultAccount();
            txusername.setText(disName);
            bitmap =  AccountManager.getAccountAvarta(baseActivity,defAccount.getId());
            txuseravatar.setText(NgnStringUtils.getAvatarText(disName));
            txdescription.setText(defAccount.getFullAccountReamName());
        }
        if(!BuildConfig.HASSIPTAILER) {
            txdescription.setVisibility(View.GONE);
        }

        if(bitmap!=null) {
            iguseravatar.setImageBitmap(bitmap);
            txuseravatar.setVisibility(View.GONE);
            iguseravatar.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
	}

    @Override
    public void onResume() {
        super.onResume();
        setAccountStatus();

    }

    private void setAccountStatus(){
        int resIcon = R.drawable.mid_content_status_offline_ico,
                resLable = R.string.status_offline;
        if(AccountManager.getInstance().getLoginState()==UserAccount.STATE_ONLINE) {
            //
            UserAccount defAccount = AccountManager.getDefaultUser(baseActivity);

            if (defAccount != null) {
                int presence = defAccount.getPresence();
                switch (presence) {
                    case ContactsContract.StatusUpdates.AVAILABLE:
                        resIcon = R.drawable.mid_content_status_online_ico;
                        resLable = R.string.status_online;
                        break;
                    case ContactsContract.StatusUpdates.AWAY:
                        resIcon = R.drawable.mid_content_status_away_ico;
                        resLable = R.string.status_away;
                        break;
                    case ContactsContract.StatusUpdates.DO_NOT_DISTURB:
                        resIcon = R.drawable.mid_content_status_nodisturb_ico;
                        resLable = R.string.status_nodistrub;
                        break;
                    case ContactsContract.StatusUpdates.INVISIBLE:
                        resIcon = R.drawable.mid_content_status_busy_ico;
                        resLable = R.string.status_busy;
                        break;
                    case ContactsContract.StatusUpdates.OFFLINE:
                        resIcon = R.drawable.mid_content_status_offline_ico;
                        resLable = R.string.status_offline;
                        break;
                }

            }
        }else if(AccountManager.getInstance().getLoginState()==UserAccount.STATE_LOGIN) {
            resLable = R.string.app_login;
        }
        TextView status = (TextView) getView().findViewById(R.id.fragment_setting_status);
        status.setText(getString(resLable));
        status.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(resIcon),null,null,null);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.user_avatar_image:
                showSetEditPhoto(R.array.avatar_setandedit);
                break;
            case R.id.user_avatar_text:
                showSetEditPhoto(R.array.avatar_set);
                break;
            default:
                break;
            case R.id.line_set_user:
                startActivity(new Intent(baseActivity, PortActivityAccount.class));
                break;
            case R.id.line_set_set:
                startActivity(new Intent(baseActivity, PortActivityPrefrence.class));
                break;
            case R.id.line_set_code:
                startActivity(new Intent(baseActivity, PortActivityCodecs.class));
                break;
            case R.id.line_set_balance:
                //startActivity(new Intent(this,PortActivityAbout.class));
                break;

            case R.id.line_set_about:
                startActivity(new Intent(baseActivity,PortActivityAbout.class));
//                startActivity(new Intent(baseActivity,PortActivityPurchaseActivity.class));
                break;
            case R.id.fragment_setting_status:
                startActivity(new Intent(baseActivity,PortActivityStatus.class));
                break;
        }
    }
    private void showSetEditPhoto(int arrayres){
        new AlertDialog.Builder(getActivity()).setItems(arrayres,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0://拍照
                                cropTakePictrueUtils.capturPicture(baseActivity);
                                break;
                            case 1://选择
                                cropTakePictrueUtils.seletPicture(baseActivity);
                                break;
                            case 2://编辑

                                RoundedDrawable drawable = (RoundedDrawable)iguseravatar.getDrawable();
                                if(drawable!=null&&drawable.getSourceBitmap()!=null) {
                                    Uri source = cropTakePictrueUtils.saveBitmapforCrop(baseActivity,drawable.getSourceBitmap());
                                    cropTakePictrueUtils.startPhotoZoom(baseActivity,source);
                                }
                                break;
                            case 3://删除

                                iguseravatar.setImageBitmap(null);
                                iguseravatar.setVisibility(View.GONE);
                                txuseravatar.setVisibility(View.VISIBLE);

                                UserAccount defAccount = AccountManager.getDefaultUser(baseActivity);
                                if(defAccount!=null) {
                                    AccountManager.saveAccountAvarta(baseActivity, defAccount.getId(), null);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }).setTitle(R.string.str_set_avarta).show();
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CropTakePictrueUtils.TAKE_PHOTO:
                    Uri uri = cropTakePictrueUtils.getCamareUri(baseActivity);
                    if(uri !=null) {
                        cropTakePictrueUtils.startPhotoZoom(baseActivity, uri );
                    }else{
                        Toast.makeText(baseActivity,"no sd",Toast.LENGTH_LONG).show();
                    }
//                }
                break;

            case CropTakePictrueUtils.CHOOSE_PHOTO://<7.0
                if (resultCode == Activity.RESULT_OK) {
                    cropTakePictrueUtils.startPhotoZoom(baseActivity, data.getData());
                }
                break;
            case CropTakePictrueUtils.CHOOSE_PHOTO_KITKAT:
                if (resultCode == Activity.RESULT_OK&&data!=null) {

                    String filePath = ImagePathUtils.getPath(baseActivity,data.getData());
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    if (bitmap != null) {
                        Uri source = cropTakePictrueUtils.saveBitmapforCrop(baseActivity,bitmap);
                        cropTakePictrueUtils.startPhotoZoom(baseActivity,source);
                    }
                }
            break;

            case CropTakePictrueUtils.CROP_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap bitmap = cropTakePictrueUtils.getCropedBitmap(baseActivity);
                    if(bitmap!=null) {
                        iguseravatar.setImageBitmap(bitmap);
                        txuseravatar.setVisibility(View.GONE);
                        iguseravatar.setVisibility(View.VISIBLE);
                    } else{
                        txuseravatar.setVisibility(View.VISIBLE);
                        iguseravatar.setVisibility(View.GONE);
                    }
                    UserAccount defAccount = AccountManager.getDefaultUser(baseActivity);
                    if(defAccount!=null) {
                        AccountManager.saveAccountAvarta(baseActivity, defAccount.getId(), bitmap);
                    }
                }
                break;

            default:
                break;
        }
    }


    @Override
    public void update(Observable observable, Object o) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setAccountStatus();
            }
        });
    }
}
