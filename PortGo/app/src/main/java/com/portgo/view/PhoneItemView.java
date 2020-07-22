package com.portgo.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.util.SipTextFilter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by huacai on 2017/4/26.
 */

public class PhoneItemView extends LinearLayout implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, TextWatcher,DialogInterface.OnClickListener {
    ImageView activity_main_contact_fragment_lvitem_action;
    TextView activity_main_contact_fragment_lvitem_type;
    EditText activity_main_contact_fragment_lvitem_phone;
    View.OnClickListener phoneActionListener = null, phoneDeleteListener = null;
    Contact.ContactDataNumber phoneNumber;
    Drawable actionSrc;
    SIPNumberChangerWatcher imWatcher;

    static List<String> sipselector =new ArrayList<>();

    public PhoneItemView(Context context) {
        this(context, null);
    }

    public PhoneItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
        initSelectors();
        LayoutInflater mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.activity_main_contact_fragment_edit_phone_item, this);
        intView();
    }

    public void setOnActionClick(View.OnClickListener listener) {
        phoneActionListener = listener;
    }

    public void setOnDeleteClick(View.OnClickListener listener) {
        phoneDeleteListener = listener;
    }

    public void setIMTextWatcher(SIPNumberChangerWatcher watcher){
        imWatcher = watcher;
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    private void intView(){
        activity_main_contact_fragment_lvitem_action = (ImageView) findViewById(R.id.activity_main_contact_fragment_lvitem_action);
//        activity_main_contact_fragment_lvitem_delete = (ImageView) findViewById(R.id.activity_main_contact_fragment_lvitem_action);
        activity_main_contact_fragment_lvitem_type = (TextView) findViewById(R.id.activity_main_contact_fragment_lvitem_type);
        activity_main_contact_fragment_lvitem_phone = (EditText) findViewById(R.id.activity_main_contact_fragment_lvitem_phone);

        activity_main_contact_fragment_lvitem_action.setOnClickListener(this);
//        activity_main_contact_fragment_lvitem_delete.setOnClickListener(this);
        activity_main_contact_fragment_lvitem_phone.addTextChangedListener(this);
        if (actionSrc != null)
            activity_main_contact_fragment_lvitem_action.setImageDrawable(actionSrc);
        activity_main_contact_fragment_lvitem_type.setOnClickListener(this);
        if(phoneNumber!=null)
            setPhoneNumber(phoneNumber);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_main_contact_fragment_lvitem_action:

                if (phoneDeleteListener != null)
                    phoneDeleteListener.onClick(view);
                break;
            case R.id.activity_main_contact_fragment_lvitem_type:
//                showCommonListDialog(phoneNumber.getMIMETYPE());
                if(phoneNumber instanceof Contact.ContactDataSipAddress) {
                    showCommonListDialog(ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE);
                }else{
                    showCommonListDialog(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                }
            default:

                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {

        phoneNumber.setNumber(editable.toString());
        if(imWatcher!=null){
            imWatcher.onSipNumberChanger(phoneNumber.getId(),phoneNumber.getNumber());
        }
    }

    private void init(AttributeSet paramAttributeSet, int paramInt) {
        TypedArray localTypedArray = getContext().obtainStyledAttributes(paramAttributeSet, R.styleable.PhoneItemView, paramInt, 0);
        actionSrc = localTypedArray.getDrawable(R.styleable.PhoneItemView_actionSrc);
        localTypedArray.recycle();
    }

    public void setPhoneNumber(Contact.ContactDataNumber number) {
        phoneNumber = number;
        int type = phoneNumber.getType();
        String lable = "";
        String mimetype = "";
        if(number instanceof Contact.ContactDataSipAddress){//
            mimetype = ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE;
        }else {
            mimetype = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
        }

        final String[] items = ContactManager.getAllTypeRes(getContext(), mimetype);

        if(type == Contact.TYPE_CUSTOM) {
            lable = phoneNumber.getLabel();
        }else {
            if(items!=null) {
                lable = items[phoneNumber.getType()];
            }
        }

        if(activity_main_contact_fragment_lvitem_type!=null) {
            activity_main_contact_fragment_lvitem_type.setText(lable);
        }

        if(activity_main_contact_fragment_lvitem_phone!=null) {
            activity_main_contact_fragment_lvitem_phone.setText(phoneNumber.getNumber());
        }
        if(activity_main_contact_fragment_lvitem_action!=null) {
            activity_main_contact_fragment_lvitem_action.setTag(phoneNumber);
        }

        if(number instanceof Contact.ContactDataSipAddress){//
            activity_main_contact_fragment_lvitem_phone.setFilters(new InputFilter[]{new SipTextFilter()});
        }else {
            activity_main_contact_fragment_lvitem_phone.setInputType(InputType.TYPE_CLASS_PHONE);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void initSelectors() {
        synchronized (sipselector) {
            if(sipselector.size()>0)
                return;

        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i){
            case DialogInterface.BUTTON_POSITIVE:
                if(customTypeView!=null) {
                    String text = customTypeView.getText().toString();
                    if(text.length()>0){
                        phoneNumber.setLable(text);
                        phoneNumber.setType(Contact.TYPE_CUSTOM);//custom
                        activity_main_contact_fragment_lvitem_type.setText(text);
                    }
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

    EditText customTypeView = null;
    AlertDialog customdialog = null;
    private void showCustomDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.loading_dialog_style);
        alert.setNegativeButton(android.R.string.cancel,this);
        alert.setPositiveButton(android.R.string.ok,this);
        customTypeView = new EditText(getContext(),null);
        alert.setView(customTypeView);
        alert.setTitle(R.string.activity_main_contact_fragment_edit_newType);
        customdialog =alert.show();
    }

    public interface SIPNumberChangerWatcher{
        public void onSipNumberChanger(int id,String number);
    }

    private void showCommonListDialog(final String mimetype) {
        final String[] items = ContactManager.getAllTypeRes(getContext(), mimetype);
        if(items==null)
            return;
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(getResources().getString(R.string.contact_fragment_edit_numtype));
        alert.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(items[which].equals(ContactManager.getTypeRes(getContext(),mimetype,0))) {//equal customType
                    showCustomDialog();
                }else {
                    phoneNumber.setType(which);
                    activity_main_contact_fragment_lvitem_type.setText(items[which]);
                }
            }
        }).create();
        alert.show();
    }
}
