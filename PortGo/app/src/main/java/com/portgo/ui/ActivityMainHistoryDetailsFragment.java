package com.portgo.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.adapter.ContactDetailAdapter;
import com.portgo.adapter.HistoryDetailAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;
import com.portgo.manager.AccountManager;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactDataAdapter;
import com.portgo.manager.ContactManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.HistoryAVCallEvent;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.UserAccount;
import com.portgo.util.ContactQueryTask;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.view.ExpandListView;
import com.portgo.view.MeasureExpandListView;
import com.portgo.view.RoundedImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.ENABLEVIDEO;
import static com.portgo.BuildConfig.HASIM;
import static com.portgo.BuildConfig.HASVIDEO;


public class ActivityMainHistoryDetailsFragment extends PortBaseFragment implements View.OnClickListener
        , LoaderManager.LoaderCallbacks<Cursor>, CompoundButton.OnCheckedChangeListener, ExpandableListView.OnChildClickListener {
    ExpandListView phoneListView;


    List<Contact.ContactDataNumber> numbers  = new ArrayList<>();
    ArrayList<HistoryAVCallEvent> mCallHistory = new ArrayList<HistoryAVCallEvent>();

    final int LOAD_CONTACT_ID = 3245;
    final int LOAD_CALL_ID = LOAD_CONTACT_ID+1;
//    Uri uri;
    HistoryDetailAdapter mHistoryAdapter;
    ContactDetailAdapter mPhoneAdpter;

    int mRemoteId,mRemoteContactId;
    String mRemoteUri,mRemoteDisname;
    int count =1,eventID;
    boolean getMissed;
    boolean needShowmore = false;
    final int LESS_COUNT = 4;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PortLoaderManager.initLoader(baseActivity,loadMgr,LOAD_CONTACT_ID,null,this);
        PortLoaderManager.initLoader(baseActivity,loadMgr,LOAD_CALL_ID,null,this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mRemoteId = getArguments().getBundle(PortBaseFragment.EXTRA_ARGS).getInt(PortActivityHistoryDetail.REMOTE);
        count= getArguments().getBundle(PortBaseFragment.EXTRA_ARGS).getInt(PortActivityHistoryDetail.COUNT);
        eventID= getArguments().getBundle(PortBaseFragment.EXTRA_ARGS).getInt(PortActivityHistoryDetail.EVENT_ID);
        getMissed= getArguments().getBundle(PortBaseFragment.EXTRA_ARGS).getBoolean(PortActivityHistoryDetail.MISS);
        if(count>LESS_COUNT){
            needShowmore =true;
        }
        orderBy = String.format(getString(R.string.history_fragment_detail_limit),count>LESS_COUNT?LESS_COUNT:count);

        return inflater.inflate(R.layout.activity_main_history_fragment_detail, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                baseActivity.finish();
                break;
        }
        return true;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        showToolBarAsActionBar(view,getString(R.string.title_historydetail),true);
    }

    @Override
    public boolean onKeyBackPressed(FragmentManager manager, Map<Integer,Fragment> fragments, Bundle result) {
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
        phoneListView = (ExpandListView) view.findViewById(R.id.activity_main_history_fragment_phones);
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
        textAvatar.setOnClickListener(this);
        textAvatar.setTextSize(TypedValue.COMPLEX_UNIT_SP,getResources().getInteger(R.integer.detail_avatar_textsize));
        userAvatar.setOnClickListener(this);

        mPhoneAdpter = new ContactDetailAdapter(baseActivity,numbers);
        mPhoneAdpter.setActionListener(this);

        callList = (MeasureExpandListView) view.findViewById(R.id.activity_main_history_fragment_call_detail);
        mHistoryAdapter = new HistoryDetailAdapter(baseActivity,groupData);

        showmore = LayoutInflater.from(baseActivity).inflate(R.layout.activity_main_history_fragment_detail_foot,null);
        if(!needShowmore){
            showmore.findViewById(R.id.history_detail_show_more).setVisibility(View.GONE);
            showmore.findViewById(R.id.history_detail_show_less).setVisibility(View.GONE);
        }else {
            showmore.findViewById(R.id.history_detail_show_more).setOnClickListener(this);
            showmore.findViewById(R.id.history_detail_show_less).setOnClickListener(this);
        }
        showmore.findViewById(R.id.hisroty_fragment_detail_newcontact).setOnClickListener(this);
        showmore.findViewById(R.id.hisroty_fragment_detail_addtocontact).setOnClickListener(this);

        callList.addFooterView(showmore);

        callList.setAdapter(mHistoryAdapter,true,true);
        footerView = getFooterView();
        phoneListView.addFooterView(footerView);
        phoneListView.setAdapter(mPhoneAdpter,true);
    }

    MeasureExpandListView callList;
    View showmore;

    final int SELECT_CONTACT = 843;
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.hisroty_fragment_detail_newcontact:
                Intent intent = new Intent();
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,-1);
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_NAME,NgnUriUtils.getUserName(mRemoteUri));
                intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_PHONE,mRemoteUri);
                intent.setClass(baseActivity,PortActivityContactEdit.class);
                baseActivity.startActivity(intent);
                break;
            case R.id.hisroty_fragment_detail_addtocontact:
                Intent trasferintent = new Intent();
                trasferintent.setClass(baseActivity, PortActivityContactSelect.class);
                baseActivity.startActivityForResult(trasferintent, SELECT_CONTACT);
                break;
            case R.id.history_detail_show_more:
                orderBy = String.format(getString(R.string.history_fragment_detail_limit),count);
                view.setVisibility(View.GONE);
                showmore.findViewById(R.id.history_detail_show_less).setVisibility(View.VISIBLE);

                PortLoaderManager.restartLoader(baseActivity,loadMgr,LOAD_CALL_ID,null,this);

                break;

            case R.id.history_detail_show_less:
                orderBy = String.format(getString(R.string.history_fragment_detail_limit),count>LESS_COUNT?LESS_COUNT:count);
                view.setVisibility(View.GONE);
                showmore.findViewById(R.id.history_detail_show_more).setVisibility(View.VISIBLE);

                PortLoaderManager.restartLoader(baseActivity,loadMgr,LOAD_CALL_ID,null,this);

                break;
            case R.id.user_avatar_text:
                break;
            case R.id.user_avatar_image:
                break;
            case R.id.fragment_detail_audio:
                if(mHistoryAdapter.getGroupCount()>0) {
                    baseActivity.makeCall(mRemoteId,mRemoteUri,PortSipCall.MEDIATYPE_AUDIO);
                }
                break;
            case R.id.fragment_detail_video:
                if(mHistoryAdapter.getGroupCount()>0) {
                    baseActivity.makeCall(mRemoteId,mRemoteUri,PortSipCall.MEDIATYPE_AUDIOVIDEO);
                }
                break;
            case R.id.fragment_detail_message:
                if(mHistoryAdapter.getGroupCount()>0) {
                    HistoryAVCallEvent event = (HistoryAVCallEvent) mHistoryAdapter.getChild(0,0);
                    PortActivityRecycleChat.beginChat(baseActivity, mRemoteUri,mRemoteContactId,mRemoteDisname);
                }
                break;
            case R.id.activity_main_contact_fragment_detail_audiocall:
                String audioNumber = (String) view.getTag();
                baseActivity.makeCall(mRemoteId,audioNumber,PortSipCall.MEDIATYPE_AUDIO);
                break;
            case R.id.activity_main_contact_fragment_detail_videocall:
                String videoNumber = (String) view.getTag();
                baseActivity.makeCall(mRemoteId,videoNumber,PortSipCall.MEDIATYPE_AUDIOVIDEO);
                break;
            case R.id.activity_main_contact_fragment_detail_sendmsg:
                String mesageNumber = (String) view.getTag();
                PortActivityRecycleChat.beginChat(baseActivity, mesageNumber,mRemoteContactId,mRemoteDisname);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case SELECT_CONTACT:
                int contactid= Contact.INVALIDE_ID;
                if(resultCode==Activity.RESULT_OK){
                    contactid = data.getIntExtra(PortActivityContactSelect.CONTACT_SELECT_REUSLT,Contact.INVALIDE_ID);
                    Intent intent = new Intent();
                    intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_ID,contactid);
                    intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_NAME,NgnUriUtils.getUserName(mRemoteUri));
                    intent.putExtra(PortActivityContactEdit.CONTACT_EDIT_PHONE,mRemoteUri);
                    intent.setClass(baseActivity,PortActivityContactEdit.class);
                    baseActivity.startActivity(intent);
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    View footerView;
    String orderBy = "";
    void updateView(Contact contact){
        String displayName = null;
        if(contact==null||contact.getId()==Contact.INVALIDE_ID) {
            displayName = NgnUriUtils.getUserName(mRemoteUri);
            if(showmore!=null) {
                showmore.findViewById(R.id.hisroty_fragment_detail_newcontact).setVisibility(View.VISIBLE);
                showmore.findViewById(R.id.hisroty_fragment_detail_addtocontact).setVisibility(View.VISIBLE);
            }
            getView().findViewById(R.id.user_avatar_image).setVisibility(View.GONE);
            TextView useravatar_text = (TextView) getView().findViewById(R.id.user_avatar_text);
            useravatar_text.setVisibility(View.VISIBLE);

            useravatar_text.setText(NgnStringUtils.getAvatarText(displayName));

            TextView username = (TextView) getView().findViewById(R.id.activity_main_contact_fragment_detail_username);
            username.setText(displayName);
        }else {

            displayName = contact.getDisplayName();
            if(showmore!=null) {
                showmore.findViewById(R.id.hisroty_fragment_detail_newcontact).setVisibility(View.GONE);
                showmore.findViewById(R.id.hisroty_fragment_detail_addtocontact).setVisibility(View.GONE);
            }

            numbers.clear();
            List<ContactDataAdapter>  contactNumbers = contact.getContactNumbers();
            if(contactNumbers!=null){
               for(ContactDataAdapter item:contactNumbers){
                    if(item!=null&&item instanceof Contact.ContactDataNumber){
                        numbers.add((Contact.ContactDataNumber) item);
                    }
                }
            }
            mPhoneAdpter.notifyDataSetChanged();

            Bitmap bitmap = contact.getAvatar();

            if (bitmap != null) {
                ImageView avataView = (ImageView) getView().findViewById(R.id.user_avatar_image);
                getView().findViewById(R.id.user_avatar_text).setVisibility(View.GONE);
                avataView.setVisibility(View.VISIBLE);
                avataView.setImageBitmap(bitmap);
            } else {
                getView().findViewById(R.id.user_avatar_image).setVisibility(View.GONE);
                TextView useravatar_text = (TextView) getView().findViewById(R.id.user_avatar_text);
                useravatar_text.setVisibility(View.VISIBLE);
                useravatar_text.setText(contact.getAvatarText());
            }
            if (displayName != null) {
                TextView username = (TextView) getView().findViewById(R.id.activity_main_contact_fragment_detail_username);
                username.setText(displayName);
            }

        }
        updateFooterView(contact);
    }

    private View getFooterView(){
        LayoutInflater inflater = (LayoutInflater) baseActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        footerView = inflater.inflate(R.layout.activity_main_contact_fragment_detail_footer, null, false);
        return footerView;
    }

    private void updateFooterView(Contact contact){

        if(contact!=null&&contact.getId()!=Contact.INVALIDE_ID) {
            if(footerView==null){
                footerView = getFooterView();
                phoneListView.addFooterView(footerView);
            }
            CheckBox checkBox = (CheckBox) footerView.findViewById(R.id.fragment_detail_favorite);
            checkBox.setEnabled(false);

            if (contact.getSTARRED() == 1) {
                setTextViewText(footerView, R.id.fragment_detail_favorite_description,
                        getString(R.string.history_fragment_detail_in_favorite));
                checkBox.setChecked(true);
            } else {
                setTextViewText(footerView, R.id.fragment_detail_favorite_description,
                        getString(R.string.history_fragment_detail_not_favorite));
                checkBox.setChecked(false);
            }

            List<ContactDataAdapter> orgnizations = contact.getOrgnizationInfos();
            if(orgnizations!=null) {
                for(ContactDataAdapter item:orgnizations){
                    if (item != null && item instanceof Contact.ContactDataOrgnization) {
                        Contact.ContactDataOrgnization orgnization = (Contact.ContactDataOrgnization) item;
                        setTextViewText(footerView, R.id.fragment_detail_department, orgnization.getDepartment());
                        setTextViewText(footerView, R.id.fragment_detail_compony, orgnization.getCompony());
                        setTextViewText(footerView, R.id.fragment_detail_job, orgnization.getTitle());
                        break;
                    }
                }
            }
            List<ContactDataAdapter> nameInfos = contact.getContactStructuredNames();
            if(nameInfos!=null){
                for(ContactDataAdapter item:nameInfos){
                    if(item!=null&&item instanceof Contact.ContactDataStructuredName){
                        Contact.ContactDataStructuredName structuredName = (Contact.ContactDataStructuredName) item;
                        setTextViewText(footerView, R.id.fragment_detail_familyname, structuredName.getFamilyName());
                        setTextViewText(footerView, R.id.fragment_detail_givingname, structuredName.getGivenName());
                        break;
                    }
                }
            }


        }else {
            if(phoneListView.getFooterViewsCount()>0) {
                phoneListView.removeFooterView(footerView);
            }
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String where =null;
        String args[]= null;
        switch (i){
            case LOAD_CONTACT_ID:
                where = DBHelperBase.RemoteColumns._ID + "=?";
                args = new String[]{""+mRemoteId};
                return new CursorLoader(baseActivity, DBHelperBase.RemoteColumns.CONTENT_URI, null, where, args, null);
            case LOAD_CALL_ID:
                UserAccount account = AccountManager.getDefaultUser(baseActivity);
                if(account!=null) {
                    String local = account.getFullAccountReamName();
                    where ="";
                    where = DBHelperBase.HistoryColumns.HISTORY_REMOTE_ID + "=?" + " AND " +DBHelperBase.HistoryColumns.HISTORY_LOCAL+ "=?"
                            + " AND "+ DBHelperBase.HistoryColumns._ID + " <= " + eventID ;
                    if (getMissed == true) {
                        where += " AND " + DBHelperBase.HistoryColumns.HISTORY_CONNECTED + "=0";
                    }
                    args =new String[]{""+mRemoteId, local};
                   // DataBaseManager.upDataHistorySeenStatus(baseActivity,local,mRemoteId,eventID,true);
                    return new CursorLoader(baseActivity, DBHelperBase.HistoryColumns.CONTENT_URI, null,
                            where, args, orderBy);
                }
        }
        return null;
    }

    Contact contact = null;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOAD_CONTACT_ID: {
                while (CursorHelper.moveCursorToFirst(cursor)) {
                    mRemoteContactId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID));
                    mRemoteId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns._ID));
                    mRemoteUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI));
                    break;
                }
                Contact contact= null;
                if(mRemoteContactId>0){
                    contact = ContactManager.getInstance().getObservableContacts().get(mRemoteContactId);
                }

                new ContactQueryTask().execute(baseActivity,mRemoteId,mRemoteUri);
                updateView(contact);
            }
            break;
            case LOAD_CALL_ID:
                mCallHistory.clear();
                while (cursor.moveToNext()) {
                    HistoryAVCallEvent event = HistoryAVCallEvent.historyAVCallEventFromCursor(cursor);
                    mCallHistory.add(event);
                }
                syncGroupData(mCallHistory);

                callList.setAdapter(mHistoryAdapter,true,true);
                mHistoryAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()){
            case LOAD_CALL_ID:
                mCallHistory.clear();
                mHistoryAdapter.notifyDataSetChanged();
                break;
            case LOAD_CONTACT_ID:
                numbers.clear();
                mPhoneAdpter.notifyDataSetChanged();
                break;
        }
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


    SimpleDateFormat dateYMDFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat dateMDFormat = new SimpleDateFormat("MM-dd");

    HashMap<String,List<HistoryAVCallEvent>>groupData= new HashMap<String,List<HistoryAVCallEvent>>();
    private  void syncGroupData(List<HistoryAVCallEvent> callHistroys){
        Date today = new Date();
        groupData.clear();
        for (HistoryAVCallEvent history:callHistroys){
            long startTime = history.getStartTime();
            String day;
            Date date = new Date(startTime);
            if(date.getYear()==today.getYear()) {
                day = dateMDFormat.format(date);
            }else {
                day = dateYMDFormat.format(date);
            }
            List<HistoryAVCallEvent> list = groupData.get(day);
            if(list==null){
                list = new ArrayList<HistoryAVCallEvent>();
                list.add(history);
                groupData.put(day,list);
            }else{
                list.add(history);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }
    final String AUDIO_FORMAT = ".wav";
    final String VIDEO_FORMAT= ".avi";
    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPostion, int childPostion, long l) {
        HistoryAVCallEvent event = (HistoryAVCallEvent) mHistoryAdapter.getChild(groupPostion,childPostion);
        if(event.getHasRecord())
        {
            String where = DBHelperBase.RecordColumns.RECORD_CALLID+"="+event.getCallid();
            ArrayList<String> recordFiles = new ArrayList<>();
            Cursor cursor = CursorHelper.resolverQuery(getActivity().getContentResolver(),DBHelperBase.RecordColumns.CONTENT_URI,
                    null,where,null,null);
            while (CursorHelper.moveCursorToNext(cursor)) {
                int indexName = cursor.getColumnIndex(DBHelperBase.RecordColumns.RECORD_FILE_NAME);
                if(indexName>-1){
                    String path = cursor.getString(indexName);
                    File file = new File(path+AUDIO_FORMAT);
                    if(file.exists()){
                        recordFiles.add(file.getAbsolutePath());
                    }else{
                        file = new File(path+VIDEO_FORMAT);
                        if(file.exists()){
                            recordFiles.add(file.getAbsolutePath());
                        }
                    }
                }
            }
           CursorHelper.closeCursor(cursor);

            if(recordFiles.size()>0) {
                Intent palyer = new Intent(getActivity(), PortActivityRecordPlayer.class);
                palyer.putExtra(PortActivityRecordPlayer.PLAY_SET, recordFiles);

                startActivity(palyer);
            }else{
                Toast.makeText(getActivity(),R.string.record_cannot_find,Toast.LENGTH_LONG).show();
            }
        }
        return false;
    }
}