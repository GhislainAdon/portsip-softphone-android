package com.portgo.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
//import android.database.CursorJoiner;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.ContactPhoneAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.database.RemoteRecord;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CallManager;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactDataAdapter;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.NotificationUtils;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.UserAccount;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.SipTextFilter;
import com.portgo.view.CursorEndEditTextView;
import com.portgo.view.MyGridLayout;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASSIPTAILER;
import static com.portgo.BuildConfig.HASVIDEO;
import static com.portgo.BuildConfig.PORT_ACTION_CALL;

public class ActivityMainNumpadFragment extends PortBaseFragment implements View.OnClickListener, View.OnLongClickListener, TextWatcher,
        Toolbar.OnMenuItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	Context context = null;
    private Toolbar toolbar;
    Uri accountUri = null;

    final int VOICEMAIL_LOADER = 43987;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		context = getActivity();
        View view = inflater.inflate(R.layout.activity_main_numpad_fragment, null);
        toolbar = (Toolbar) view.findViewById(R.id.toolBar);
        UserAccount account = AccountManager.getDefaultUser(baseActivity);
        accountUri = ContentUris.withAppendedId(DBHelperBase.AccountColumns.CONTENT_URI,account.getId());
        PortLoaderManager.initLoader(baseActivity,loadMgr,VOICEMAIL_LOADER,null,this);
        ImageView videoview = view.findViewById(R.id.activity_main_fragment_dial_video);
        if(!HASVIDEO){
            videoview = view.findViewById(R.id.activity_main_fragment_dial_video);
            videoview.setVisibility(View.INVISIBLE);
        }
        videoview.setEnabled(ENABLEVIDEO);

        showToolBar(view,account.getDisplayDefaultAccount());
        toolbar.setBackgroundResource(R.color.portgo_color_dial);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.menu_dialpad);

		return view;
	}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    private void updateMessageCount(int mailSize){
        ImageView vmView = (ImageView) getView().findViewById(R.id.activity_main_fragment_dial_vm);
        Drawable messageDrawable = getResources().getDrawable(R.drawable.dial_voicemail_ico);
        Drawable newmessageDrawable = getResources().getDrawable(R.drawable.voicemail_msg_ico);
        LayerDrawable layerDrawable;

        if(mailSize>0) {
            Drawable[] layers = new Drawable[2];
            layers[0] = messageDrawable;
            layers[1] = newmessageDrawable;
            layerDrawable = new LayerDrawable(layers);

//            int vmTipsSize = (int) getResources().getDimension(R.dimen.vm_num_tips_size);
            int l_inset = (int) getResources().getDimension(R.dimen.vm_num_tips_l);
            int t_inset = (int) getResources().getDimension(R.dimen.vm_num_tips_t);
            int r_inset = (int) getResources().getDimension(R.dimen.vm_num_tips_r);
            int b_inset = (int) getResources().getDimension(R.dimen.vm_num_tips_b);
            layerDrawable.setLayerInset(1,l_inset,t_inset,r_inset,b_inset);
        }else{
            Drawable[] layers = new Drawable[1];
            layers[0] = messageDrawable;
            layerDrawable = new LayerDrawable(layers);
        }
        vmView.setImageDrawable(layerDrawable);
    }

    private void updateStatus(){
        UserAccount account = AccountManager.getDefaultUser(baseActivity);
        if(account!=null){
            if(account.isDistrbEnable()) {
                toolbar.getMenu().findItem(R.id.status_no_disturb).setVisible(true);
                toolbar.getMenu().findItem(R.id.status_forward).setVisible(false);
            }else {
                toolbar.getMenu().findItem(R.id.status_no_disturb).setVisible(false);
                if (account.getFowardMode() != UserAccount.FORWARD_CLOSE) {
                    toolbar.getMenu().findItem(R.id.status_forward).setVisible(false);
                } else {
                    toolbar.getMenu().findItem(R.id.status_forward).setVisible(true);
                }

                if (account.getFowardMode() == UserAccount.FORWARD_CLOSE || TextUtils.isEmpty(account.getFowardTo())) {
                    toolbar.getMenu().findItem(R.id.status_forward).setVisible(false);
                } else {
                    toolbar.getMenu().findItem(R.id.status_forward).setVisible(true);
                }
            }

        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
		initView(view);
        toolbar.inflateMenu(R.menu.menu_dialpad);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle argments = getArguments();
        Bundle extra = null;
        if(argments!=null) {
            extra = argments.getBundle("EXTRA_ARGS");
        }
        if(extra!=null) {
            Uri number = extra.getParcelable(PORT_ACTION_CALL);
            argments.putParcelable(PORT_ACTION_CALL,null);
            setArguments(argments);

            if (number != null) {
                String SSP = number.getSchemeSpecificPart();
                if (SSP != null) {
                    try {
                        SSP = URLDecoder.decode(SSP, "utf-8");
                        phoneNumber.setTextCursorEnd(SSP);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        updateStatus();
    }

    CursorEndEditTextView phoneNumber;
	private void initView(View view){
        initNumpad((MyGridLayout) view.findViewById(R.id.dialpad_digits));

        view.findViewById(R.id.activity_main_fragment_dial_audio_ll).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_dial_video).setOnClickListener(this);
        view.findViewById(R.id.activity_main_fragment_dial_vm_ll).setOnClickListener(this);

        view.findViewById(R.id.dialpad_deletenumber).setOnClickListener(this);
        view.findViewById(R.id.dialpad_deletenumber).setOnLongClickListener(this);
        view.findViewById(R.id.dialpad_selectnumber).setOnClickListener(this);

		phoneNumber = (CursorEndEditTextView) view.findViewById(R.id.dialpad_phonenumber);
		phoneNumber.setFilters(new InputFilter[]{new SipTextFilter()});
		phoneNumber.addTextChangedListener(this);

	}

    final int SELECT_CONTACT = 5435;//magic num
	@Override
	public void onClick(View view) {
        switch (view.getId()) {
           case R.id.activity_main_fragment_dial_1:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "1");
                break;
            case R.id.activity_main_fragment_dial_2:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "2");
                break;
            case R.id.activity_main_fragment_dial_3:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "3");
                break;
            case R.id.activity_main_fragment_dial_4:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "4");
                break;
            case R.id.activity_main_fragment_dial_5:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "5");
                break;
            case R.id.activity_main_fragment_dial_6:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "6");
                break;
            case R.id.activity_main_fragment_dial_7:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "7");
                break;
            case R.id.activity_main_fragment_dial_8:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "8");
                break;
            case R.id.activity_main_fragment_dial_9:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "9");
                break;
            case R.id.activity_main_fragment_dial_sharp:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "#");
                break;
            case R.id.activity_main_fragment_dial_star:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "*");
                break;
            case R.id.activity_main_fragment_dial_0:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString() + "0");
                break;
            case R.id.dialpad_deletenumber:
                if (phoneNumber.getText().length() > 1) {
                    phoneNumber.setTextCursorEnd(phoneNumber.getText().toString().substring(0, phoneNumber.length() - 1));
                } else {
                    phoneNumber.setTextCursorEnd("");
                }
                break;

            case R.id.dialpad_selectnumber:
                Intent intent = new Intent();
                intent.setClass(baseActivity, PortActivityPhoneNumberSelect.class);
                baseActivity.startActivityForResult(intent, SELECT_CONTACT);
                break;
            case R.id.activity_main_fragment_dial_audio_ll: {
                UserAccount account = AccountManager.getDefaultUser(baseActivity);
                if (AccountManager.getInstance().getLoginState() != UserAccount.STATE_ONLINE) {
                    Toast.makeText(baseActivity, R.string.please_login_tips, Toast.LENGTH_SHORT).show();
                    return;
                }
                String number = phoneNumber.getText().toString();
                RemoteRecord remoteRecord  =getRemote();
                //contactid
                if(TextUtils.isEmpty(number)){
                    HistoryAVCallEvent lastcall= CallManager.getLatestHistory(baseActivity,account.getFullAccountReamName());
                    if(lastcall!=null){
                        number = lastcall.getRemoteUri();
                        if(!HASSIPTAILER){
                            number = NgnUriUtils.getUserName(number);
                        }
                        phoneNumber.setText(number);
                        break;
                    }

                }
                baseActivity.makeCall((int) remoteRecord.getRowID(), number, PortSipCall.MEDIATYPE_AUDIO);
                phoneNumber.setText("");
            }
            break;
            case R.id.activity_main_fragment_dial_vm_ll: {
                    if (AccountManager.getInstance().getLoginState() != UserAccount.STATE_ONLINE) {
                        Toast.makeText(baseActivity, R.string.please_login_tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserAccount account = AccountManager.getDefaultUser(baseActivity);
                    if (account != null) {
                        String voiceMail = account.getVoiceMail();
                        if (TextUtils.isEmpty(voiceMail)) {
                            Toast.makeText(baseActivity,R.string.empty_voicemail_tips,Toast.LENGTH_SHORT).show();
                        }else {
                            RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), voiceMail,
                                    getString(R.string.string_voicemail), -1);
                            baseActivity.makeCall((int) remoteRecord.getRowID(), voiceMail, PortSipCall.MEDIATYPE_AUDIO);
                        }
                    }
                }

                break;
            case R.id.activity_main_fragment_dial_video: {
                if (AccountManager.getInstance().getLoginState() != UserAccount.STATE_ONLINE) {
                    Toast.makeText(baseActivity, R.string.please_login_tips, Toast.LENGTH_SHORT).show();
                    return;
                }
                UserAccount account = AccountManager.getDefaultUser(baseActivity);
                String number = phoneNumber.getText().toString();

                RemoteRecord remoteRecord = getRemote();
                if(TextUtils.isEmpty(number)){
                    HistoryAVCallEvent lastcall= CallManager.getLatestHistory(baseActivity,account.getFullAccountReamName());
                    if(lastcall!=null){
                        number = lastcall.getRemoteUri();
                        if(!HASSIPTAILER){
                            number = NgnUriUtils.getUserName(number);
                        }else{
                            number = lastcall.getRemoteUri();
                        }
                        phoneNumber.setText(number);
                        break;
                    }

                }

                if(HASVIDEO&&ENABLEVIDEO) {
                    baseActivity.makeCall((int) remoteRecord.getRowID(), number, PortSipCall.MEDIATYPE_AUDIOVIDEO);
                }else{
                    baseActivity.makeCall((int) remoteRecord.getRowID(), number, PortSipCall.MEDIATYPE_AUDIO);
                }

                phoneNumber.setText("");
            }
            break;

        }
    }

    private RemoteRecord getRemote(){
        RemoteRecord remoteRecord = null;
        UserAccount account = AccountManager.getDefaultUser(baseActivity);
        String number = phoneNumber.getText().toString();
        Object tag = phoneNumber.getTag();
        String remote = NgnUriUtils.getFormatUrif4Msg(number, account.getDomain());

        if (tag != null) {
            if (number.equals(tag)) {
                if (TextUtils.isEmpty(disName)) {
                    disName = number;
                }
                remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, disName, contactId);
            }
        }
        if (remoteRecord ==null) {
            remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, number);
        }
        return  remoteRecord;
    }
	@Override
	public boolean onLongClick(View view) {
		switch (view.getId()){
			case R.id.activity_main_fragment_dial_0:
                phoneNumber.setTextCursorEnd(phoneNumber.getText().toString()+"+");
				break;
            case R.id.dialpad_deletenumber:
                phoneNumber.setTextCursorEnd("");
                break;
		}
		return true;
	}

	@Override
	public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	}

	@Override
	public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	}

	@Override
	public void afterTextChanged(Editable editable) {

	}

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return true;
    }

    int contactId=0;
    String disName ="";
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_CONTACT && resultCode == Activity.RESULT_OK) {
            String select = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_NUMBER);
            if (!TextUtils.isEmpty(select)){
                contactId  = data.getIntExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_ID,0);
                disName = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_DISNAME);
                phoneNumber.setTextCursorEnd(select);
                phoneNumber.setTag(select);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initNumpad(MyGridLayout view){
        final int KEY_SIZE = 12;
        String[] numbers= getResources().getStringArray(R.array.keynumber_values);
        String[] letters= getResources().getStringArray(R.array.keyletter_values);
        for(int i=0;i<KEY_SIZE;i++){
            View diallayout = view.getChildAt(i);
            diallayout.setOnClickListener(this);
            TextView numberView = (TextView) diallayout.findViewById(R.id.keynumber);
            TextView letterView = (TextView) diallayout.findViewById(R.id.keyletter);
            numberView.setText(numbers[i]);
            letterView.setText(letters[i]);
        }

        view.findViewById(R.id.activity_main_fragment_dial_0).setOnLongClickListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(baseActivity,accountUri,null,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(CursorHelper.moveCursorToFirst(data)) {
            int mailSize= data.getInt(data.getColumnIndex(DBHelperBase.AccountColumns.ACCOUNT_MAILSIZE));
            updateMessageCount(mailSize);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
