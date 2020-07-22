package com.portgo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.adapter.ContactDetailAdapter;
import com.portgo.database.RemoteRecord;
import com.portgo.manager.AccountManager;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.UserAccount;
import com.portgo.util.CropTakePictrueUtils;
import com.portgo.util.ImagePathUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.view.RoundedDrawable;
import com.portgo.view.RoundedImageView;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASIM;
import static com.portgo.BuildConfig.HASVIDEO;


public class ActivityMainContactsDetailFragment extends PortBaseFragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, LoaderManager.LoaderCallbacks<Cursor> {

    private int mContactid;
    private ListView mPhoneListView;
    private ContactDetailAdapter mAdapter;
    private String mDisplayName;
    private List<Contact.ContactDataNumber> mNumbers  = new ArrayList<>();
    private final int LOAD_ID = 3241;
    private Uri mUri,mContactsUri =  ContactsContract.Contacts.CONTENT_URI,
            mDataUri= ContactsContract.Data.CONTENT_URI;;

    private CropTakePictrueUtils cropTakePictrueUtils;
    
    
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,LOAD_ID,null,this);
        cropTakePictrueUtils = new CropTakePictrueUtils(baseActivity);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        ContentValues values = new ContentValues();
        mContactid = getArguments().getBundle(PortBaseFragment.EXTRA_ARGS).getInt(PortActivityContactDetail.CONTACT_DETAIL_ID);
        mUri = ContentUris.withAppendedId(mContactsUri,mContactid);

        return inflater.inflate(R.layout.activity_main_contact_fragment_detail, null);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_contactdetail,menu);
    }

    final int REQ_EDIT = 32741;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                baseActivity.finish();
                break;
            case R.id.menu_contact_detail_edit:
                Intent intent = new Intent();
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,mContactid);
                intent.setClass(baseActivity,PortActivityContactEdit.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                baseActivity.startActivityForResult(intent,REQ_EDIT);
                break;
        }
        return true;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        showToolBarAsActionBar(view, getString(R.string.contact_detail), true);
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

	private void initView(View view) {
        mPhoneListView = (ListView) view.findViewById(R.id.activity_main_contact_fragment_detail_phone);
        userAvatar = (RoundedImageView) view.findViewById(R.id.user_avatar_image);
        view.findViewById(R.id.fragment_detail_audio).setOnClickListener(this);
        view.findViewById(R.id.fragment_detail_video).setOnClickListener(this);
        view.findViewById(R.id.fragment_detail_message).setOnClickListener(this);
        LinearLayout linearLayout = view.findViewById(R.id.llquicklink);
        int weightsum = 3;
        if(!HASIM) {
            weightsum-=1;
            view.findViewById(R.id.fragment_detail_message).setVisibility(View.GONE);
        }
        view.findViewById(R.id.fragment_detail_message).setEnabled(ENABLEIM);
        if(!HASVIDEO){
            weightsum-=1;
            view.findViewById(R.id.fragment_detail_video).setVisibility(View.GONE);
        }
        view.findViewById(R.id.fragment_detail_video).setEnabled(ENABLEVIDEO);

        linearLayout.setWeightSum(weightsum);
        textAvatar = (TextView) view.findViewById(R.id.user_avatar_text);
        textAvatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,getResources().getInteger(R.integer.detail_avatar_textsize));
        textAvatar.setOnClickListener(this);
        userAvatar.setOnClickListener(this);

        mAdapter = new ContactDetailAdapter(baseActivity,mNumbers);
        mAdapter.setActionListener(this);
        mPhoneListView.setAdapter(mAdapter);

        footerView = getFooterView();
        mPhoneListView.addFooterView(footerView);
	}

	@Override
	public void onClick(View view) {

		switch (view.getId()){
            case R.id.user_avatar_text:
                showSetEditPhoto(R.array.avatar_set);
                break;
            case R.id.user_avatar_image:
                showSetEditPhoto(R.array.avatar_setandedit);
                break;
            case R.id.fragment_detail_audio:
                if(mNumbers.size()>0) {
                    UserAccount account = AccountManager.getDefaultUser(baseActivity);
                    String number = mNumbers.get(0).getNumber();
                    String remote = NgnUriUtils.getFormatUrif4Msg(number, account.getDomain());
                    RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, number,mContactid);//mContactid
                    baseActivity.makeCall((int) remoteRecord.getRowID(),remote, PortSipCall.MEDIATYPE_AUDIO);
                }
                break;
            case R.id.fragment_detail_video:
                if(mNumbers.size()>0) {
                    UserAccount account = AccountManager.getDefaultUser(baseActivity);
                    String number = mNumbers.get(0).getNumber();
                    String remote = NgnUriUtils.getFormatUrif4Msg(number, account.getDomain());
                    RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, number,mContactid);//mContactid
                    baseActivity.makeCall((int) remoteRecord.getRowID(),remote, PortSipCall.MEDIATYPE_AUDIOVIDEO);
                }
                break;
            case R.id.fragment_detail_message:
                if(mNumbers.size()>0) {
                    PortActivityRecycleChat.beginChat(baseActivity, mNumbers.get(0).getNumber(),mContactid,mDisplayName);
                }
                break;
            
            case R.id.activity_main_contact_fragment_detail_audiocall:
                String audioNumber = (String) view.getTag();
                {
                    UserAccount account = AccountManager.getDefaultUser(baseActivity);
                    String remote = NgnUriUtils.getFormatUrif4Msg(audioNumber, account.getDomain());
                    RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, audioNumber,mContactid);//mContactid
                    baseActivity.makeCall((int) remoteRecord.getRowID(),remote, PortSipCall.MEDIATYPE_AUDIO);
                }
                break;
            case R.id.activity_main_contact_fragment_detail_videocall:
                String videoNumber = (String) view.getTag();
                {
                    UserAccount account = AccountManager.getDefaultUser(baseActivity);
                    String remote = NgnUriUtils.getFormatUrif4Msg(videoNumber, account.getDomain());
                    RemoteRecord remoteRecord = RemoteRecord.getRemoteRecord(baseActivity.getContentResolver(), remote, videoNumber,mContactid);//mContactid

                    baseActivity.makeCall((int) remoteRecord.getRowID(),remote, PortSipCall.MEDIATYPE_VIDEO);
                }

                break;
            case R.id.activity_main_contact_fragment_detail_sendmsg:
                String mesageNumber = (String) view.getTag();
                PortActivityRecycleChat.beginChat(baseActivity, mesageNumber,mContactid,mDisplayName);
                break;
			default:
				break;
		}
	}

    View footerView;

    void updateView(Contact contact){
        if(contact==null)
            return;
        mNumbers.clear();
        List contactNumbers =  contact.getContactNumbers();
        if(contactNumbers !=null){
            for(Object obj:contactNumbers){
                if(obj!=null&&obj instanceof Contact.ContactDataNumber)
                mNumbers.add((Contact.ContactDataNumber) obj);
            }
        }

        mAdapter.notifyDataSetChanged();


        Bitmap bitmap = ContactManager.getPhoto(baseActivity,""+mContactid,mDataUri);
        if(bitmap!=null){
            ImageView avataView = (ImageView) getView().findViewById(R.id.user_avatar_image);
            getView().findViewById(R.id.user_avatar_text).setVisibility(View.GONE);
            avataView.setVisibility(View.VISIBLE);
            avataView.setImageBitmap(bitmap);
        }else {
            getView().findViewById(R.id.user_avatar_image).setVisibility(View.GONE);
            TextView useravatar_text = (TextView) getView().findViewById(R.id.user_avatar_text);
            useravatar_text.setVisibility(View.VISIBLE);
            useravatar_text.setText(contact.getAvatarText());
        }

        mDisplayName= contact.getDisplayName();
        if(mDisplayName!=null)
        {
            TextView username = (TextView) getView().findViewById(R.id.activity_main_contact_fragment_detail_username);
            username.setText(mDisplayName);
        }
        TextView status = (TextView) getView().findViewById(R.id.activity_main_contact_fragment_detail_status);
        Contact.ContactDataIm portIm = contact.getContactPortImAddress();
        if(portIm ==null||TextUtils.isEmpty(portIm .getImAccount())){
            status.setVisibility(View.INVISIBLE);
        }else{
            status.setVisibility(View.VISIBLE);
            int mode = contact.getPresence_mode();
            int resLable = contact.getPresence_resLable();
            int resIcon = contact.getPresence_resicon();
            if (resLable <= 0 || resIcon <= 0) {
                resLable = R.string.status_offline;
                resIcon = R.drawable.mid_content_status_offline_ico;
            }

            status.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(resIcon), null, null, null);
            status.setText(getString(resLable));
        }
        updateFooterView(contact);
    }

    private View getFooterView(){

        LayoutInflater inflater = LayoutInflater.from(baseActivity);
        footerView = inflater.inflate(R.layout.activity_main_contact_fragment_detail_footer, null, false);
        return footerView;
    }

    private void updateFooterView(Contact contact) {
        if (footerView == null) {
            return;
        }
        CheckBox checkBox = (CheckBox) footerView.findViewById(R.id.fragment_detail_favorite);
        checkBox.setOnCheckedChangeListener(this);
        if (contact.getSTARRED() == 1) {
            setTextViewText(footerView, R.id.fragment_detail_favorite_description,
                    getString(R.string.activity_main_contact_fragment_detail_rmfavorite));
            checkBox.setChecked(true);
        } else {
            setTextViewText(footerView, R.id.fragment_detail_favorite_description,
                    getString(R.string.activity_main_contact_fragment_detail_addfavorite));
            checkBox.setChecked(false);
        }

        List orgnizations = contact.getOrgnizationInfos();
        Contact.ContactDataOrgnization orgnization =null;
        if (orgnizations != null) {
            for(Object obj:orgnizations){
                if (obj != null & obj instanceof Contact.ContactDataOrgnization) {
                    orgnization = (Contact.ContactDataOrgnization) obj;
                    break;
                }
            }
        }
        if(orgnization!=null){
            setTextViewText(footerView, R.id.fragment_detail_department, orgnization.getDepartment());
            setTextViewText(footerView, R.id.fragment_detail_compony, orgnization.getCompony());
            setTextViewText(footerView, R.id.fragment_detail_job, orgnization.getTitle());
        }else{
            setTextViewText(footerView, R.id.fragment_detail_department, "");
            setTextViewText(footerView, R.id.fragment_detail_compony, "");
            setTextViewText(footerView, R.id.fragment_detail_job, "");
        }

        List contactNameInfos = contact.getContactStructuredNames();
        Contact.ContactDataStructuredName structuredName =null;
        if (contactNameInfos != null) {
            for(Object obj :contactNameInfos){
                if (obj != null & obj instanceof Contact.ContactDataStructuredName) {
                    structuredName = (Contact.ContactDataStructuredName) obj;
                    break;
                }
            }
        }
        if(structuredName !=null) {
            setTextViewText(footerView, R.id.fragment_detail_familyname, structuredName.getFamilyName());
            setTextViewText(footerView, R.id.fragment_detail_givingname, structuredName.getGivenName());
        }else{
            setTextViewText(footerView, R.id.fragment_detail_familyname, "");
            setTextViewText(footerView, R.id.fragment_detail_givingname, "");
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        ContactManager.starContacts(baseActivity,mContactid,b);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(baseActivity,mUri,null,null,null,null);
    }
    Contact contact = null;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOAD_ID: {
                String johnDoe = getString(R.string.activity_main_contact_no_name);
                if(CursorHelper.moveCursorToFirst(cursor)) {
                    int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String disName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    int starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED));
                    if(TextUtils.isEmpty(disName)){
                        disName=johnDoe;
                    }
                    contact = new Contact(id, disName);
                    contact.setStarrred(starred);
                    ContactManager.getContactData(baseActivity, contact,mContactsUri,mDataUri);

                    Contact.ContactDataIm portIm =contact.getContactPortImAddress();
                    if(portIm!=null) {
                        String selecton = ContactsContract.StatusUpdates.DATA_ID + "=?";
                        Cursor cursorStatus = CursorHelper.resolverQuery(getActivity().getContentResolver(),ContactsContract.StatusUpdates.CONTENT_URI,
                                null, selecton,new String[]{""+portIm.getId()} , null);
                        if(CursorHelper.moveCursorToFirst(cursorStatus)) {
                            String status = cursorStatus.getString(cursorStatus.getColumnIndex(ContactsContract.StatusUpdates.STATUS));
                            long time = cursorStatus.getLong(cursorStatus.getColumnIndex(ContactsContract.StatusUpdates.STATUS_TIMESTAMP));
                            int presesece = cursorStatus.getInt(cursorStatus.getColumnIndex(ContactsContract.StatusUpdates.PRESENCE));
                            int lable = cursorStatus.getInt(cursorStatus.getColumnIndex(ContactsContract.StatusUpdates.STATUS_LABEL));
                            int icon = cursorStatus.getInt(cursorStatus.getColumnIndex(ContactsContract.StatusUpdates.STATUS_ICON));
                            contact.setPresence_mode(presesece);
                            contact.setPresence_resicon(icon);
                            contact.setPresence_resLable(lable);
                            contact.setPresence_status(status);
                        }
                        CursorHelper.closeCursor(cursorStatus);
                    }

                }
                updateView(contact);
            }
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void setTextViewText(View parentview ,int viewid,String text){
        if(parentview==null) {
            return;
        }
        TextView textView = (TextView) parentview.findViewById(viewid);
        if(textView!=null) {
            if(TextUtils.isEmpty(text))
                text = "";
            textView.setText(text);
        }
    }
    RoundedImageView userAvatar = null;
    TextView textAvatar = null;

    private void showSetEditPhoto(int arrayres){
        new AlertDialog.Builder(getActivity()).setItems(arrayres,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0://拍照
                                cropTakePictrueUtils.capturPicture(baseActivity);
                                break;
                            case 1:
                                cropTakePictrueUtils.seletPicture(baseActivity);
                                break;
                            case 2:
                                RoundedDrawable drawable = (RoundedDrawable)userAvatar.getDrawable();
                                if(drawable!=null&&drawable.getSourceBitmap()!=null) {
                                    Uri source = cropTakePictrueUtils.saveBitmapforCrop(baseActivity,drawable.getSourceBitmap());
                                    cropTakePictrueUtils.startPhotoZoom(baseActivity,source);
                                }
                                break;
                            case 3:
                                if(contact!=null) {
                                    ContactManager.setContactPhoto(baseActivity, null, mContactid);
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
            case REQ_EDIT:
                if(resultCode==Activity.RESULT_OK){
                    getActivity().finish();
                }
                break;
            case CropTakePictrueUtils.TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri mUri = cropTakePictrueUtils.getCamareUri(baseActivity);
                    if(mUri !=null) {
                        cropTakePictrueUtils.startPhotoZoom(baseActivity, mUri );
                    }else{
                        Toast.makeText(baseActivity,"no sd",Toast.LENGTH_LONG).show();
                    }
                }
                break;

            case CropTakePictrueUtils.CHOOSE_PHOTO_KITKAT://>==7.0
                if (resultCode == Activity.RESULT_OK) {
                    String filePath = ImagePathUtils.getPath(baseActivity,data.getData());
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    if (bitmap != null) {
                        Uri source = cropTakePictrueUtils.saveBitmapforCrop(baseActivity,bitmap);
                        cropTakePictrueUtils.startPhotoZoom(baseActivity,source);
                    }
                }
                break;
            case CropTakePictrueUtils.CHOOSE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri mDataUri = ImagePathUtils.getProviderUri(baseActivity,data.getData());
                    if(data.getData()!=null) {
                        cropTakePictrueUtils.startPhotoZoom(baseActivity, data.getData());
                    }
                }

            break;

            case CropTakePictrueUtils.CROP_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap bitmap = cropTakePictrueUtils.getCropedBitmap(baseActivity);
                    if (contact != null && data != null) {
                        ContactManager.setContactPhoto(baseActivity, bitmap, mContactid);
                    }
                }
                break;
            default:
                break;
        }
    }

}
