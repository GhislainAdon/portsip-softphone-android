package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactDataAdapter;
import com.portgo.manager.ContactManager;
import com.portgo.view.CursorEndEditTextView;
import com.portgo.view.PhoneItemView;
import com.portgo.view.SlideDeleteListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.portgo.manager.Contact.INVALIDE_ID;
import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;


public class ActivityMainContactsEditFragment extends PortBaseFragment implements View.OnClickListener
        ,SlideDeleteListView.OnResumeListener,PhoneItemView.SIPNumberChangerWatcher {
    com.portgo.view.SlideDeleteListView activity_main_contact_fragment_phones;
    com.portgo.view.SlideDeleteListView activity_main_contact_fragment_sips;


    List<ContactDataAdapter> mSipsNumber = new ArrayList<ContactDataAdapter>();
    List<ContactDataAdapter> mPhonesNumber = new ArrayList<ContactDataAdapter>();

    List<Contact.ContactDataNumber> mRemove = new ArrayList<>();
    PhoneItemAdpter mPhonsListAdapter = null;
    PhoneItemAdpter mSipsListAdapter = null;
//    Contact.ContactDataIm portIm;
    int mContactid = -1;
    private  Contact mContact=null;
    boolean mFriend;
    String mName = "";
    String mSipNumber = "";
    
    Uri argUri = ContactsContract.Contacts.CONTENT_URI;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        Bundle bundle = getArguments().getBundle(PortBaseFragment.EXTRA_ARGS);
        if(bundle!=null) {
            mContactid = bundle.getInt(PortActivityContactEdit.CONTACT_EDIT_ID);
            mName = bundle.getString(PortActivityContactEdit.CONTACT_EDIT_NAME);
            mSipNumber = bundle.getString(PortActivityContactEdit.CONTACT_EDIT_PHONE);
            mFriend = bundle.getBoolean(PortActivityContactEdit.CONTACT_EDIT_FRIEND);
        }
        return inflater.inflate(R.layout.activity_main_contact_fragment_edit, null);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        int titleRes= R.string.edit_fragment_addfriend;
        if(mFriend) {
            if(mContactid>0) {
                titleRes = R.string.edit_fragment_editfriend;
            }
        }else{
            if(mContactid>0) {
                titleRes = R.string.edit_fragment_editcontact;
            }else {
                titleRes = R.string.edit_fragment_addcontact;
            }
        }
        showToolBarAsActionBar(view,getString(titleRes),true);
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        loadData();
        showToolBar(view,null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contactedit,menu);
    }

    void loadData(){

        if(mContactid >0) {
            mContact = ContactManager.getContact(baseActivity,argUri,mContactid);
        }
        if(mContact !=null){
            List sips = mContact.getContactSipNumbers();
            if(sips!=null) {
                mSipsNumber.addAll(sips);
            }
            Contact.ContactDataSipAddress sipAddress = new Contact.ContactDataSipAddress(contactNumberid--,mSipNumber, SipAddress.TYPE_HOME,  "");
            sipAddress.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
            mSipsNumber.add(sipAddress);
            List phones = mContact.getContactPhones();
            if(phones!=null) {
                mPhonesNumber.addAll(phones);
            }

            List<ContactDataAdapter> contactNameInfos = mContact.getContactStructuredNames();
            Contact.ContactDataStructuredName contactName = null;
            for(ContactDataAdapter item:contactNameInfos) {
                if (item != null && item instanceof Contact.ContactDataStructuredName) {
                    contactName = (Contact.ContactDataStructuredName) item;
                    break;
                }
            }
            if(contactName!=null) {
                CursorEndEditTextView editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_familyname);
                editText.setTextCursorEnd(contactName.getFamilyName());
                editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_givingname);
                editText.setTextCursorEnd(contactName.getGivenName());
            }

            Contact.ContactDataIm contactDataIm = mContact.getContactPortImAddress();
            TextView imview = (TextView) getView().findViewById(R.id.contact_fragment_edit_im);
            if(contactDataIm!=null) {
                imview.setText(contactDataIm.getImAccount());
                for(ContactDataAdapter item:mSipsNumber){
                    Contact.ContactDataSipAddress sip = null;
                    if(item!=null&&item instanceof Contact.ContactDataSipAddress){
                        sip = (Contact.ContactDataSipAddress) item;
                        if(sip.getNumber()!=null&&sip.getNumber().equals(contactDataIm.getImAccount())) {
                            imAttachItem = sip.getId();
                        }
                    }
                }
            }
            List<ContactDataAdapter> orgnizations =  mContact.getOrgnizationInfos();
            if(orgnizations!=null) {
                for (ContactDataAdapter item : orgnizations) {
                    Contact.ContactDataOrgnization orgnization = null;
                    if (item != null && item instanceof Contact.ContactDataOrgnization) {
                        orgnization = (Contact.ContactDataOrgnization) item;
                        CursorEndEditTextView editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_usercompany);
                        editText.setTextCursorEnd(orgnization.getCompony());
                        editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_userdepartment);
                        editText.setTextCursorEnd(orgnization.getDepartment());
                        editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_usertitle);
                        editText.setTextCursorEnd(orgnization.getTitle());
                    }

                    break;
                }
            }

        }else{
            CursorEndEditTextView editText = (CursorEndEditTextView) getView().findViewById(R.id.activity_main_contact_fragment_edit_givingname);
            editText.setTextCursorEnd(mName);
            mSipsNumber.add(new Contact.ContactDataSipAddress(contactNumberid--,mSipNumber, SipAddress.TYPE_HOME, ""));
        }

        mPhonsListAdapter.notifyDataSetChanged();
        mSipsListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                baseActivity.finish();
                break;
            case R.id.menu_contact_editfinish:
                Contact contact = getData();
                if(contact.isEmpty()){
                    Toast.makeText(getActivity(),R.string.invalidate_contact_data,Toast.LENGTH_SHORT).show();
                }else {
                    if (mContactid > 0) {
                        ContactManager.updateContact(baseActivity, contact, null, argUri, ContactsContract.Data.CONTENT_URI);
                    } else {
                        Contact.ContactDataIm friendIm = contact.getContactPortImAddress();
                        if (mFriend) {
                            if (friendIm == null || TextUtils.isEmpty(friendIm.getImAccount())) {
                                Toast.makeText(getActivity(), R.string.request_friend_im, Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                        int rawcontactid = ContactManager.insertContact(baseActivity, contact, null);
                        if (rawcontactid > 0 && friendIm != null) {

                            String handle = friendIm.getImAccount();
                            if(handle!=null) {
                                String[] array = handle.split("@");
                                if (array.length > 0) {
                                    baseActivity.mSipMgr.presenceSubscribe(array[0], "Hello me");
                                }
                            }
                            baseActivity.mCallMgr.putsubScribedTime(rawcontactid, System.currentTimeMillis());
                        }

                    }
                    getActivity().finish();
                }
                
               break;
        }
        return true;
    }

    @Override
	public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments,Bundle result) {
		if (mNeedRemoveFormList) {
			fragments.remove(mFragmentId);
		}
		manager.beginTransaction().remove(this).commit();
		PortBaseFragment backFragment = (PortBaseFragment) fragments.get(mBackFragmentId);
		if (mBackFragmentId != -1 && backFragment != null && mFragmentResId != -1) {
			showFramegment(getActivity(),manager, fragments, mFragmentResId, backFragment);
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
        super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

    private Contact getData(){
        Contact newContact = new Contact(mContactid);
        EditText editText = (EditText) getView().findViewById(R.id.activity_main_contact_fragment_edit_familyname);
        String familyname = editText.getText().toString();
        editText = (EditText) getView().findViewById(R.id.activity_main_contact_fragment_edit_givingname);
        String givingname = editText.getText().toString();

        editText = (EditText) getView().findViewById(R.id.activity_main_contact_fragment_edit_usercompany);
        String company = editText.getText().toString();
        editText = (EditText) getView().findViewById(R.id.activity_main_contact_fragment_edit_userdepartment);
        String department = editText.getText().toString();
        editText = (EditText) getView().findViewById(R.id.activity_main_contact_fragment_edit_usertitle);
        String title = editText.getText().toString();

        TextView txIm = (TextView) getView().findViewById(R.id.contact_fragment_edit_im);
        String imAdress = txIm .getText().toString();

        Contact.ContactDataIm im = null;//只处理我们的协议项
        if(mContact!=null){
            im = mContact.getContactPortImAddress();
        }
        if(im!=null){
            if(TextUtils.isEmpty(imAdress)){
                im.setAction(ContactDataAdapter.DATA_ACTION.ACTION_DEL);
            }
            im.setImAccount(imAdress);
        }else{
            if(!TextUtils.isEmpty(imAdress)) {
                im = new Contact.ContactDataIm(INVALIDE_ID,imAdress,ContactsContract.CommonDataKinds.Im.TYPE_HOME,null);
                im.setProtocaltype( ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
                im.setProtocal(PORTSIP_IM_PROTOCAL);
                im.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
            }
        }
        if(im!=null) {
            newContact.addIm(im);
        }
        Contact.ContactDataStructuredName structuredName = null;
        if(mContact!=null){
            List<ContactDataAdapter> names = mContact.getContactStructuredNames();
            if(names!=null) {
                for (ContactDataAdapter item : names) {
                    structuredName = (Contact.ContactDataStructuredName) item;
                    break;
                }
            }
        }
        if(structuredName==null) {
            structuredName = new Contact.ContactDataStructuredName(INVALIDE_ID, null, 0, null);
            structuredName.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
        }
        structuredName.setFamilyName(familyname);
        structuredName.setGivenName(givingname);

        newContact.addStructName(structuredName);

        Contact.ContactDataOrgnization org = null;
        if(mContact!=null){
            List<ContactDataAdapter> orgnizations =  mContact.getOrgnizationInfos();
            for(ContactDataAdapter item:orgnizations) {
                if (item != null && item instanceof Contact.ContactDataOrgnization) {
                    org = (Contact.ContactDataOrgnization) item;
                    break;
                }
            }
            if(org!=null){
                org.setCompany(company);
                org.setDepartment(department);
                org.setTitle(title);
            }else{
                org = new Contact.ContactDataOrgnization(INVALIDE_ID,company,ContactsContract.CommonDataKinds.Organization.TYPE_WORK,null,department,title);
                org.setDepartment(department);
                org.setTitle(title);
                org.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
            }
        }else{
            org = new Contact.ContactDataOrgnization(INVALIDE_ID,company,ContactsContract.CommonDataKinds.Organization.TYPE_WORK,null,department,title);
            org.setDepartment(department);
            org.setTitle(title);
            org.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
        }
        newContact.addOgnization(org);

      for(ContactDataAdapter item:mSipsNumber){
            if(item!=null&&item instanceof Contact.ContactDataSipAddress) {
                Contact.ContactDataSipAddress sipAddress = (Contact.ContactDataSipAddress) item;
                if(sipAddress.getId()<=0){
                    sipAddress.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
                }
                newContact.addSipAddress(sipAddress);
            }
        }

        for(ContactDataAdapter item:mPhonesNumber){
            if(item!=null&&item instanceof Contact.ContactDataPhone) {
                Contact.ContactDataPhone phone = (Contact.ContactDataPhone) item;
                if(phone.getId()<=0){
                    phone.setAction(ContactDataAdapter.DATA_ACTION.ACTION_ADD);
                }
                newContact.addPhone(phone);
            }
        }

        for(Contact.ContactDataNumber number:mRemove)
        {
            if(number.getId()>INVALIDE_ID) {
                number.setAction(ContactDataAdapter.DATA_ACTION.ACTION_DEL);
                if(number instanceof Contact.ContactDataSipAddress) {
                    newContact.addSipAddress((Contact.ContactDataSipAddress) number);
                }else {
                    newContact.addPhone((Contact.ContactDataPhone) number);
                }
            }
        }
        return newContact;
    }

    private void initView(View view) {
        activity_main_contact_fragment_phones = (SlideDeleteListView) view.findViewById(R.id.activity_main_contact_fragment_phones);
        activity_main_contact_fragment_sips = (SlideDeleteListView) view.findViewById(R.id.activity_main_contact_fragment_sips);

        view.findViewById(R.id.activity_main_contact_fragment_delete).setOnClickListener(this);
        view.findViewById(R.id.ll_contact_fragment_edit_im).setOnClickListener(this);

        View sipsfooter = baseActivity.getLayoutInflater().inflate(R.layout.activity_main_contact_fragment_edit_footer,null);
        View phonesfooter = baseActivity.getLayoutInflater().inflate(R.layout.activity_main_contact_fragment_edit_footer,null);

        sipsfooter.findViewById (R.id.contact_linkman_editor_add_phone).setOnClickListener(new ActionAddListener(TYPE_SIP));
        TextView lable = (TextView) sipsfooter.findViewById (R.id.contact_linkman_editor_lable_add_phone);
        lable.setText(R.string.activity_main_contact_fragment_edit_addsip);

        phonesfooter.findViewById (R.id.contact_linkman_editor_add_phone).setOnClickListener(new ActionAddListener(TYPE_PHONE));
        lable = (TextView) phonesfooter.findViewById (R.id.contact_linkman_editor_lable_add_phone);
        lable.setText(R.string.activity_main_contact_fragment_edit_addphone);

        activity_main_contact_fragment_sips.addFooterView(sipsfooter);
        activity_main_contact_fragment_phones.addFooterView(phonesfooter);


        mSipsListAdapter = new PhoneItemAdpter(baseActivity, mSipsNumber);
        mPhonsListAdapter  = new PhoneItemAdpter(baseActivity, mPhonesNumber);

        activity_main_contact_fragment_sips.setAdapter(mSipsListAdapter,true);
        activity_main_contact_fragment_phones.setAdapter(mPhonsListAdapter,true);

	}

	boolean deletestatus = false;
	@Override
	public void onClick(View view) {
        switch (view.getId()){

            case R.id.activity_main_contact_fragment_lvitem_action:
                Contact.ContactDataNumber number = (Contact.ContactDataNumber) view.getTag();
                if(number.getId()==imAttachItem){//
                    imAttachItem = INVALIDE_ATTACH_NUMBER;
                    TextView imview = (TextView) getView().findViewById(R.id.contact_fragment_edit_im);
                    imview.setText("");
                }
                if(number.getId()>0)
                    mRemove.add(number);
                if(number instanceof Contact.ContactDataSipAddress) {
                    mSipsNumber.remove(number);
                    mSipsListAdapter.notifyDataSetChanged();
                }else{
                    mPhonesNumber.remove(number);
                    mPhonsListAdapter.notifyDataSetChanged();
                }
                break;

            case R.id.activity_main_contact_fragment_delete:
                if(mContactid!=-1) {
                    Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;
                   ContactManager.deleteContact(baseActivity,argUri,rawContactUri,mContactid);

                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }else{
                    goback();
                }
                break;

            case R.id.ll_contact_fragment_edit_im:
                TextView imview = (TextView) getView().findViewById(R.id.contact_fragment_edit_im);
                Intent imSelector = new Intent(baseActivity,PortActivityImSelect.class);
                ArrayList<String> selector = new ArrayList<>();
                selector.add(getString(R.string.select_no_number));
                String portim = imview.getText().toString();
                int defaultIm=-1;
                int index = 0;
                for(ContactDataAdapter item:mSipsNumber){
                    Contact.ContactDataNumber sipNumber = (Contact.ContactDataNumber) item;
                    if(!TextUtils.isEmpty(sipNumber.getNumber())){
                        if(sipNumber.getNumber().equals(portim)){
                                defaultIm = index;
                        }
                            selector.add(sipNumber.getNumber());
                    }
                    index++;
                }

                if(defaultIm<=-1) {//
                    if(!TextUtils.isEmpty(portim)) {
                        selector.add(portim);
                        defaultIm = selector.size()-1;
                    }
                }
                imSelector.putExtra(PortActivityImSelect.IM_SELECT_DEFAULT, defaultIm);
                imSelector.putStringArrayListExtra(PortActivityImSelect.IM_SELECT,selector);
                imSelector.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(imSelector,REQUEST_IM);
                break;
		}
	}
    final int REQUEST_IM = 324;

    final int INVALIDE_ATTACH_NUMBER = -4329;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode== Activity.RESULT_OK){
            switch (requestCode){
                case REQUEST_IM:
                    String imaddress = data.getStringExtra(PortActivityImSelect.IM_SELECT_RESULT);
                    TextView imview = (TextView) getView().findViewById(R.id.contact_fragment_edit_im);
                    imview.setText(imaddress);
                    if(TextUtils.isEmpty(imaddress)){
                        imAttachItem =INVALIDE_ATTACH_NUMBER;
                    }else {
                        for(ContactDataAdapter item:mSipsNumber){
                            if (item != null && item instanceof Contact.ContactDataSipAddress) {
                                Contact.ContactDataNumber sip = (Contact.ContactDataNumber) item;
                                if (!TextUtils.isEmpty(sip.getNumber()) && sip.getNumber().equals(imaddress)) {
                                    imAttachItem = sip.getId();
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void resumeItem(View view) {
        deletestatus = false;
    }
    KeyListener keyListener = null;
    class PhoneItemAdpter extends BaseAdapter {
        List<ContactDataAdapter> data;
        Context mContext;
        LayoutInflater mInflater;
        PhoneItemAdpter(Context context,List<ContactDataAdapter> numbers){
            data = numbers;
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            int count = 0;
            if(data!=null)
                count = data.size();

            return count;
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            PhoneItemView itemView;
            if(view == null) {
                view = new PhoneItemView(baseActivity);
            }

            itemView = (PhoneItemView) view;

            Contact.ContactDataNumber phoneNumber = (Contact.ContactDataNumber) getItem(i);
            itemView.setPhoneNumber(phoneNumber);
            itemView.setIMTextWatcher(ActivityMainContactsEditFragment.this);
            itemView.setOnActionClick(ActivityMainContactsEditFragment.this);
            itemView.setOnDeleteClick(ActivityMainContactsEditFragment.this);

            return view;
        }
    }

    @Override
    public void onSipNumberChanger(int id, String number) {
        if(imAttachItem==id){
            TextView imview = (TextView)getView().findViewById(R.id.contact_fragment_edit_im);
            if(number!=null) {
                imview.setText(number);
            }else{
                imview.setText("");
            }
        }
    }

    int imAttachItem =INVALIDE_ATTACH_NUMBER;
    final int TYPE_SIP = 0,TYPE_PHONE = 1;
    int contactNumberid = -1;
    class  ActionAddListener implements View.OnClickListener{
        final int actionType;
        ActionAddListener(int type){
            actionType = type;
        }

        @Override
        public void onClick(View view) {
            int type = 0;
            switch (actionType){
                case TYPE_SIP:
                    type = SipAddress.TYPE_HOME;
                    mSipsNumber.add(new Contact.ContactDataSipAddress(contactNumberid--,"",type,  ""));
                    mSipsListAdapter.notifyDataSetChanged();
                    break;

                case TYPE_PHONE:
                    type = Phone.TYPE_HOME;
                    mPhonesNumber.add(new Contact.ContactDataPhone(contactNumberid--,"", type,""));
                    mPhonsListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

}
