package com.portgo.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.adapter.ChatRecyclerCursoAdapter;
import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;
import com.portgo.manager.AccountManager;
import com.portgo.manager.CallManager;
import com.portgo.manager.ChatSession;
import com.portgo.manager.ConfigurationManager;
import com.portgo.manager.Contact;
import com.portgo.manager.ContactManager;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.MessageEvent;
import com.portgo.manager.PortLoaderManager;
import com.portgo.manager.PortSipCall;
import com.portgo.manager.PortSipService;
import com.portgo.manager.SipManager;
import com.portgo.manager.UserAccount;
import com.portgo.util.ContactQueryTask;
import com.portgo.util.ImagePathUtils;
import com.portgo.util.MIMEType;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.media.AudioPlayer;
import com.portgo.util.media.MediaCodecInfo;
import com.portgo.view.BottomBar;
import com.portgo.view.CursorEndEditTextView;
import com.portgo.view.InputChat;
import com.portgo.view.KeyboardDetectorRelativeLayout;
import com.portgo.view.RadiusBackgroundSpan;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.portgo.manager.MessageEvent.KEY_TEXT_CONTENT;

public class PortActivityRecycleChat extends PortGoBaseActivity implements View.OnClickListener
        , TextWatcher, LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener, ChatRecyclerCursoAdapter.OnMessageViewClickLister, MediaPlayer.OnCompletionListener {
    static public String CHAT_TO_URI = "chat_uri";
    static public String CHAT_TO_CONTACT = "chat_contactid";
    static public String CHAT_TO_DISNAME = "chat_dest";

    static public String CHAT_SESSION = "chat_session";
    static public String CHAT_POSITION = "chat_position";

    BottomBar bottomBar;
    InputChat inputChat;
    static long timepre = 0;
    private RecyclerView mRecycleView;
    LinearLayoutManager layoutManager;
    ChatRecyclerCursoAdapter mAdapter;

    private final int LOADER_MESSAGE_ID = 0x472;
    private final int LOADER_CONTACT_ID = LOADER_MESSAGE_ID + 1;


    long positionId = -1;
    Toolbar toolbar;
    ArrayList<Long> transferMessageId = new ArrayList<>();

    String mRemoteSip = "";//与发送者输入框的内容是同步的
    int mRemoteContactid;
    String mRemoteDisName;

    String localReaml = "";
    String localUri = "";
    String localDisname = "";
    ChatSession chatSession;
    AudioPlayer player = new AudioPlayer();

    public PortActivityRecycleChat() {
        super();
    }

    //如果会话不存在，显示接收者输入栏。发送消息时，创建会话，隐藏接收者输入栏
    static public void beginChat(Context context, String remote, int contactID, String displayName) {
        if (context == null)
            return;
        Intent intent = new Intent();
        intent.putExtra(CHAT_TO_URI, remote);
        intent.putExtra(CHAT_TO_CONTACT, contactID);
        intent.putExtra(CHAT_TO_DISNAME, displayName);

        intent.setClass(context, PortActivityRecycleChat.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
//        timepre =System.currentTimeMillis();
//        PortApplication.getLogUtils().d("xxxxxxxxxx","beginChat "+System.currentTimeMillis());
    }

    //如果回话已经存在，隐藏接收者输入栏。使用当前会话收发
    static public void beginChat(Context context, int sessionId, long smsid) {
        if (context == null)
            return;
        Intent intent = new Intent();
        intent.putExtra(CHAT_SESSION, sessionId);
        intent.putExtra(CHAT_POSITION, smsid);

        intent.setClass(context, PortActivityRecycleChat.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
//        timepre =System.currentTimeMillis();
//        PortApplication.getLogUtils().d("xxxxxxxxxx","beginChat "+System.currentTimeMillis());
    }

    PortSipService.PortsipBinder binder = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (PortSipService.PortsipBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_message_fragment_avchat_recycle);
        Intent intent = getIntent();
        //
        long sessionID = intent.getIntExtra(CHAT_SESSION, -1);
        positionId = intent.getLongExtra(CHAT_POSITION, -1);
        UserAccount defaultUser = AccountManager.getDefaultUser(this);

        if (defaultUser != null) {
            localReaml = defaultUser.getDomain();
            localUri = defaultUser.getFullAccountReamName();
            localDisname = defaultUser.getDisplayDefaultAccount();
        }

        if(sessionID>0){//beginChat(Context context, int sessionId, long smsid)
            chatSession = ChatSession.ChatSessionById(this,sessionID);
        }else{
            mRemoteSip = intent.getStringExtra(CHAT_TO_URI);
            mRemoteContactid = intent.getIntExtra(CHAT_TO_CONTACT, 0);
            mRemoteDisName = intent.getStringExtra(CHAT_TO_DISNAME);
        }

        //绑定服务，让活动与服务之间实现通信
        Intent svr = new Intent(this,PortSipService.class);
        bindService(svr, connection, BIND_AUTO_CREATE);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        showToolsbarAsHomeUp("");

        inputChat = new InputChat();
        Bundle args = new Bundle();
        args.putBoolean("TEXT", !SipManager.getSipManager().pbxSupportFileTransfer());
//        args.putBoolean("TEXT", false);
        inputChat.setArguments(args);

        bottomBar = new BottomBar();
//        PortApplication.getLogUtils().d("xxxxxxxxxx","onCreate2 "+(System.currentTimeMillis()-timepre));
//        timepre =System.currentTimeMillis();
        getFragmentManager().beginTransaction().replace(R.id.input_container, inputChat).commit();
        inputChat.setInputChatListener(new InputChat.InputChatListener() {
            @Override
            public boolean onSendMessage(String msg) {
                if(canStartSession()) {
                    sendTextMessage(msg);
                    return true;
                }
                return false;
            }

            @Override
            public void onSendVoiceMessage(String path, int duration, String description) {//会先判读canStartRecord ，loadSessionID肯定成功
                loadSessionID();
                JSONObject content = MessageEvent.constructAudioMessage(path,"",MIMEType.MIMETYPE_audioamr,0,duration);
                MessageEvent message = constructStreamMessage(chatSession.getId(),chatSession.getRemoteUri(),content, duration);
                if(message!=null) {
                    sendStreamMessage(message);
                }
            }

            @Override
            public boolean canStartSession(){//判断是否有接收者，是否可以开始录制音视频，选择照片
                if(chatSession!=null) {//会话已创建
                    return true;
                }else {
                    if (!NgnStringUtils.isNullOrEmpty(mRemoteSip)) {//已经输入接收者
                        return true;
                    }
                    Toast.makeText(PortActivityRecycleChat.this,R.string.input_number_tips,Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override
            public boolean canStartRecord(){//判断是否有正在进行的通话，有通话时，不让发音视频消息
                if(CallManager.getInstance().getCallsSize()>0) {
                    Toast.makeText(PortActivityRecycleChat.this,R.string.string_incall_error,Toast.LENGTH_SHORT).show();
                    return false;
                }else {
                    return true;
                }
            }
            @Override
            public void onAudioCall() {//会先判读canStartRecord ，loadSessionID肯定成功
                loadSessionID();
                makeCall(chatSession.getRemoteId(), chatSession.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIO);
            }

            @Override
            public void onVideoCall() {//会先判读canStartRecord ，loadSessionID肯定成功
                loadSessionID();
                makeCall(chatSession.getRemoteId(), chatSession.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIOVIDEO);
            }

            @Override
            public void onPickPhotoMessage(Intent data) {//会先判读canStartRecord ，loadSessionID肯定成功
                List<String> results = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                if (results != null && results.size() > 0) {
                    for (String path : results) {
                        File file = new File(path);
                        if (file.exists()) {
                            loadSessionID();
                            JSONObject content = MessageEvent.constructImageMessage(path,"", MIMEType.MIMETYPE_imagejpeg,0,0,0);
                            MessageEvent message = constructStreamMessage(chatSession.getId(), chatSession.getRemoteUri(), content,0);
                            if (message != null) {
                                sendStreamMessage(message);
                            }

                        }
                    }
                }
            }

            @Override
            public void onTakePhotoMessage(Intent data) {//会先判读canStartRecord ，loadSessionID肯定成功
            }

            @Override
            public void onPickLocMessage(Intent data) {//会先判读canStartRecord ，loadSessionID肯定成功
            }

            @Override
            public void onSendFileMessage(Intent data) {
                if(data!=null){
                    List<String> results = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                    if(results!=null&&results.size()>0){
                        for (String path:results){
                            File file = new File(path);
                            if(file.exists()) {
                                loadSessionID();
                                String mime = MIMEType.getMIMEType(file);
                                JSONObject content = MessageEvent.constructFileMessage(file.getName(),file.getAbsolutePath(),"",mime,file.length());
                                MessageEvent message = constructStreamMessage(chatSession.getId(),chatSession.getRemoteUri(),content, 0);
                                if(message!=null) {
                                    sendStreamMessage(message);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onRecordVideoMessage(Intent data) {//会先判读canStartRecord ，loadSessionID肯定成功
                String filename = data.getStringExtra(PortActivityVideoReorder.RECORDER_NAME);
                File file = new File(filename);
                int duration = MediaCodecInfo.getFilePlayTime(file);

                MessageEvent message = null;
                loadSessionID();
                if(filename.endsWith(".mp4")){
                    JSONObject content = MessageEvent.constructVideoMessage(file.getAbsolutePath(),"",MIMEType.MIMETYPE_videomp4,file.length(),duration);
                    message =constructStreamMessage(chatSession.getId(),chatSession.getRemoteUri(),content, duration);
                }else if(filename.endsWith(".jpg")){
                    JSONObject content = MessageEvent.constructImageMessage(file.getAbsolutePath(),"",MIMEType.MIMETYPE_imagejpeg,file.length(),0,0);
                    message =constructStreamMessage(chatSession.getId(),chatSession.getRemoteUri(),content, duration);
                }
                if(message!=null) {
                    sendStreamMessage(message);
                }
            }
        });

        KeyboardDetectorRelativeLayout root = (KeyboardDetectorRelativeLayout) findViewById(R.id.activity_main_message_fragment_avchat);
        root.setOnSoftKeyboardListener(new KeyboardDetectorRelativeLayout.OnSoftKeyboardListener() {
            @Override
            public void onShown(int keyboardHeight) {
                inputChat.onKeyboardShow(keyboardHeight);
            }

            @Override
            public void onHidden() {
                inputChat.onKeyboardDismiss();
            }

            @Override
            public void onMeasureFinished() {
//                if (inputChat.isBoxShow()) {
//                } else {
//                }
            }
        });
        initView();
    }

    /*
        @description 文本消息的一小段，文本消息的文件名
        */

    private MessageEvent constructStreamMessage(int sessionRowid, String remoteUri, JSONObject content, int duration){
        MessageEvent message = null;

        if (sessionRowid > 0 && checkCallCondition(remoteUri)) {
            String name = NgnUriUtils.getDisplayName(remoteUri, PortActivityRecycleChat.this);
            message = new MessageEvent(sessionRowid, name, MessageEvent.MessageStatus.PROCESSING, true);

            message.setContent(MIMEType.MIMETYPE_appJson, content);//file.getName().getBytes());

            long messgeid = new Random().nextLong();
            message.setMessageDuration(duration);
            message.setSMSId(messgeid);
            DataBaseManager.insertMessage(PortActivityRecycleChat.this, message);
        }
        return message;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN) {
            if(inputChat.isBoxShow()){
                inputChat.hideAll();
                return true;
            }
//            if (mListView.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE) {
//                exitSelectStatus();
//                return true;
//            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_single, menu);
        if (!NgnStringUtils.isNullOrEmpty(mRemoteSip)) {
            menu.setGroupVisible(R.id.chat_group_normal, false);
            if (!BuildConfig.HASVIDEO) {
                menu.findItem(R.id.menu_chat_video).setVisible(false);
            }
            menu.findItem(R.id.menu_chat_video).setEnabled(BuildConfig.ENABLEVIDEO);
        } else {
            menu.setGroupVisible(R.id.chat_group_none, false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                if (mListView != null && mListView.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE) {
//                    exitSelectStatus();
//                } else {
//                    super.onOptionsItemSelected(item);
//                }
                goback();
                break;
            case R.id.menu_chat_add:
                goback();
                break;
            case R.id.menu_chat_clearall:
                mAdapter.setSelectNone();
                updateSelectStatus();
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_chat_selectall:
                mAdapter.setSelectALL();
                updateSelectStatus();
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.menu_chat_video://
                loadSessionID();
                if(chatSession!=null) {
                    makeCall(chatSession.getRemoteId(), chatSession.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIOVIDEO);
                }
                break;
            case R.id.menu_chat_audio://
                loadSessionID();
                if(chatSession!=null) {
                    makeCall(chatSession.getRemoteId(), chatSession.getRemoteUri(), PortSipCall.MEDIATYPE_AUDIO);
                }
                break;
        }

        updateMenu();
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = new MenuInflater(this);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        MessageEvent event = DataBaseManager.findMessageByMessageRowId(this,info.id);
        if(event!=null) {
            JSONObject content = event.getJsonContent();
            String messageType;
            try {
                messageType = content.getString(MessageEvent.KEY_MESSAGE_TYPE);
            } catch (JSONException e) {
                e.printStackTrace();
                messageType = "";
            }

            if(TextUtils.isEmpty(messageType))
                return;
            inflater.inflate(R.menu.menu_chat_context, menu);
            menu.findItem(R.id.menu_chat_context_delete).setVisible(true);

            if(event.getSendOut()){
                if(event.getMessageStatus()!= MessageEvent.MessageStatus.SUCCESS) {//发出去的，失败重发
                    menu.findItem(R.id.menu_chat_context_send).setVisible(true);
                }
            }
            if(MessageEvent.MESSAGE_TYPE_TEXT.equals(messageType)){//文字消息，允许拷贝
                menu.findItem(R.id.menu_chat_context_copy).setVisible(true);
                menu.findItem(R.id.menu_chat_context_transfer).setVisible(true);

            }else if(MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)){//音频消息，可以重新下载，重发，转发不能拷贝
                menu.findItem(R.id.menu_chat_context_transfer).setVisible(false);
                if(!event.getSendOut()&&event.getMessageStatus()!= MessageEvent.MessageStatus.SUCCESS) {
                    menu.findItem(R.id.menu_chat_context_download).setVisible(true);
                }
            }
            else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)){
                menu.findItem(R.id.menu_chat_context_transfer).setVisible(true);
                if(!event.getSendOut()&&event.getMessageStatus()!= MessageEvent.MessageStatus.SUCCESS) {
                    menu.findItem(R.id.menu_chat_context_download).setVisible(true);
                }
            }else if(MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){
                menu.findItem(R.id.menu_chat_context_transfer).setVisible(true);
                if(!event.getSendOut()&&event.getMessageStatus()!= MessageEvent.MessageStatus.SUCCESS) {
                    menu.findItem(R.id.menu_chat_context_download).setVisible(true);
                }
            }
        }
        mAdapter.setItemCheck(v, info.id, true);
        enterSelectStatus();
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mAdapter.setSelectNone();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long messageRowId = mAdapter.getContextMenuId();

        MessageEvent old;
        Uri uri = null;
        switch (item.getItemId()) {
            case R.id.menu_chat_context_copy:
                old = DataBaseManager.findMessageByMessageRowId(this,messageRowId);

                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                JSONObject jContent= old.getJsonContent();
                String realContent ="";
                try{
                    realContent = jContent.getString(KEY_TEXT_CONTENT);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ClipData data = ClipData.newPlainText(old.getMime(),realContent);
                clipboard.setPrimaryClip(data);
                exitSelectStatus();
                break;
            case R.id.menu_chat_context_delete:
                DataBaseManager.deleteMessage(this,messageRowId);
                exitSelectStatus();
                break;
            case R.id.menu_chat_context_more:
                enterSelectStatus();
                break;
            case R.id.menu_chat_context_download://重新下载附件
                old = DataBaseManager.findMessageByMessageRowId(this,messageRowId);
                if(old!=null){
                    JSONObject content = old.getJsonContent();
                    String type;
                    String fileName,filePath,fileUrl;
                    try {
                        type = content.getString(MessageEvent.KEY_MESSAGE_TYPE);
                        fileName =content.getString(MessageEvent.KEY_FILE_NAME);
                        fileUrl=content.getString(MessageEvent.KEY_FILE_PATH);
                    } catch (JSONException e) {
                        fileName = "";
                        type="";
                        fileUrl = "";
                        e.printStackTrace();
                        break;
                    }
                    if(MessageEvent.MESSAGE_TYPE_FILE.equals(type)){
                        filePath = ConfigurationManager.getInstance().getStringValue(this,ConfigurationManager.PRESENCE_FILE_PATH,
                                getExternalFilesDir(null).getAbsolutePath()+ConfigurationManager.PRESENCE_FILE_DEFALUT_SUBPATH);
                    }else{
                        filePath = ConfigurationManager.getInstance().getStringValue(this,ConfigurationManager.PRESENCE_VIDEO_PATH,
                                getExternalFilesDir(null).getAbsolutePath());
                    }
                    binder.downloadFile(old.getSMSId(),filePath,fileName,fileUrl);
                }
                break;
            case R.id.menu_chat_context_send:
                old = DataBaseManager.findMessageByMessageRowId(this,messageRowId);
                if(old!=null){
                    if(old.getMime().startsWith(MIMEType.MIMETYPE_text)){
                        resendTextMessage(old);
                    }else {
                        sendStreamMessage(old);
                    }
                }
                break;
            case R.id.menu_chat_context_transfer:
                //old = DataBaseManager.findMessageByMessageRowId(this,messageRowId);
                transferMessageId.clear();
                transferMessageId.add(messageRowId);
                Intent intent = new Intent();
                intent.setClass(this, PortActivityPhoneNumberSelect.class);
                startActivityForResult(intent, SELECT_CONTACT_TRANSFER);
                break;
        }
        return super.onContextItemSelected(item);
    }

    void initView() {

        LinearLayout lReceiver = (LinearLayout) findViewById(R.id.activity_main_message_fragment_chat_layout_receiver);
        findViewById(R.id.activity_main_message_fragment_chat_select_contact).setOnClickListener(this);

        mAdapter = new ChatRecyclerCursoAdapter(this, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mRecycleView = findViewById(R.id.screen_chat_recycle);

        mRecycleView.setAdapter(mAdapter);
        mAdapter.setonCreateContextMenuListener(this);
        mAdapter.setOnMessageViewClickLister(this);
        layoutManager =  new LinearLayoutManager(this);
        layoutManager .setStackFromEnd(true);
        mRecycleView.setLayoutManager(layoutManager);
        //Layout Manager：Item的布局。Adapter：为Item提供数据。Item Decoration：Item之间的Divider。 Item Animator：添加
//        mRecycleView.setOnCreateContextMenuListener(this);

        if (chatSession == null) {//beginChat(Context context, String remote, int contactID, String displayName)
            lReceiver.setVisibility(View.VISIBLE);//会话尚未创建
            CursorEndEditTextView receiver = (CursorEndEditTextView) findViewById(R.id.activity_main_message_fragment_chat_receiver);
            receiver.addTextChangedListener(this);
            if (mRemoteSip == null) {
                mRemoteSip = "";
            }

            receiver.setTextCursorEnd(mRemoteSip);
            mAdapter.setUserAvartar(null,localDisname,null,mRemoteDisName);
        }else {
            lReceiver.setVisibility(View.GONE);//会话已经存在，隐藏目的地输入框
            mAdapter.setUserAvartar(null,localDisname,null,chatSession.getDisplayName());
            PortLoaderManager.restartLoader(this, loadMgr, LOADER_MESSAGE_ID, null, this);
            PortLoaderManager.restartLoader(this, loadMgr, LOADER_CONTACT_ID, null, this);//
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_button:
//				onSendMessageClick();
                break;
            case R.id.activity_main_message_fragment_chat_select_contact:
                Intent intent = new Intent();
                intent.setClass(this, PortActivityPhoneNumberSelect.class);
                startActivityForResult(intent, SELECT_CONTACT);
                break;
            case R.id.bottombar_left://transfer
                new AlertDialog.Builder(this).setItems(R.array.transfer, this).
                        setTitle(R.string.chat_transfer_style).show();
                break;
            case R.id.bottombar_right://delete
                List<Long> selectItems = mAdapter.getSelectItems();
                Long[]messageRowids = (Long[]) selectItems.toArray();
                DataBaseManager.deleteMessages(this,messageRowids);
                exitSelectStatus();
                break;
            default:
                break;

        }
    }

    final int SELECT_CONTACT = 234;//magic num
    final int SELECT_CONTACT_TRANSFER = SELECT_CONTACT + 1;//magic num
    final int SELECT_CONTACT_TRANSFER_ONEBYONE = SELECT_CONTACT + 2;//magic num
    final int SELECT_CONTACT_TRANSFER_TOGETHER = SELECT_CONTACT + 3;//magic num
    final int VIDEO_REOCRDER = SELECT_CONTACT + 4;//magic num

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    //    ArrayList<Spannable>
    @Override
    public void afterTextChanged(Editable editable) {
        RadiusBackgroundSpan[] spanbls = editable.getSpans(0, editable.length(), RadiusBackgroundSpan.class);
        mRemoteSip = editable.toString();
//		findViewById(R.id.activity_main_message_fragment_chat_send).setEnabled(isSendButtonEnable());

        int index = 0;
//        SpannableString span = new SpannableString(editable.toString());
        while (index >= 0) {
            int start = index + 1;
            index = editable.toString().indexOf(",", start);
            if (index > 0 && index > start) {
                start = (start == 1 ? 0 : start);//第一个不需要去除逗号，其他的需要去除逗号标识，需要加1
                editable.setSpan(new RadiusBackgroundSpan(getResources().
                        getColor(R.color.portgo_color_gray), 30), start, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
        unbindService(connection);
    }
    private void sendStreamMessage(MessageEvent event) {
        JSONObject content = event.getJsonContent();
        String fileName  = null;
        try {
            fileName = content.getString(MessageEvent.KEY_FILE_PATH);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        binder.uploadFile(event.getSMSId(),"application/x-export-json",fileName);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_normal, false);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_none, false);
    }

    public void loadSessionID() {
        if (chatSession ==null) {//会话已创建
            if (!NgnStringUtils.isNullOrEmpty(mRemoteSip)) {
                findViewById(R.id.activity_main_message_fragment_chat_layout_receiver)
                        .setVisibility(View.GONE);

                mRemoteSip = NgnUriUtils.getFormatUrif4Msg(mRemoteSip, localReaml);
                if (TextUtils.isEmpty(mRemoteDisName)) {
                    mRemoteDisName = NgnUriUtils.getUserName(mRemoteSip);
                }
                chatSession = DataBaseManager.getChatSession(this,localUri, mRemoteSip, mRemoteDisName, mRemoteContactid);
                if(mAdapter!=null){
                    mAdapter.setUserAvartar(null,localDisname,null,mRemoteDisName);
                }
            }else{
                Toast.makeText(PortActivityRecycleChat.this,R.string.input_number_tips,Toast.LENGTH_SHORT).show();
            }
            if(chatSession!=null) {
                PortLoaderManager.restartLoader(this, loadMgr, LOADER_MESSAGE_ID, null, this);//找到了sessionid，需要加载此会话以前的消息
                PortLoaderManager.restartLoader(this, loadMgr, LOADER_CONTACT_ID, null, this);//加载和系统联系人的关联
            }
        }
    }
    private void resendTextMessage(MessageEvent message) {
        String realContent;
        try {
            realContent= message.getJsonContent().getString(KEY_TEXT_CONTENT);
        } catch (JSONException e) {
            e.printStackTrace();
            realContent = "";
        }

       long messgeid = mSipMgr.portSipSendMessage(chatSession.getRemoteUri(), realContent, false,realContent.length());
        message.setSMSId(messgeid);
        DataBaseManager.upDataMessageIDStatus(this,messgeid,message.getId(), MessageEvent.MessageStatus.PROCESSING);
    }

    private void sendTextMessage(String messageString) {
        loadSessionID();
        if(chatSession==null)
            return;
        if (!NgnStringUtils.isNullOrEmpty(messageString)) {


            long messgeid;
            String disName = chatSession.getDisplayName();
            MessageEvent message = new MessageEvent(chatSession.getId(), chatSession.getDisplayName(),
                    MessageEvent.MessageStatus.PROCESSING, true);
            message.setContent("text/plain", messageString);
            String realContent;
            try {
                realContent= message.getJsonContent().getString(KEY_TEXT_CONTENT);
            } catch (JSONException e) {
                e.printStackTrace();
                realContent = "";
            }
            messgeid = mSipMgr.portSipSendMessage(chatSession.getRemoteUri(), realContent, false, messageString.length());
            message.setSMSId(messgeid);
            DataBaseManager.insertMessage(this,message);
        }

        toolbar.getMenu().setGroupVisible(R.id.chat_group_normal, false);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_none, false);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String where = null;
            String content = "";
            String selectContact, fullName, name;
            ChatSession session = null;

            switch (requestCode) {
                case SELECT_CONTACT:
                    mRemoteSip = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_NUMBER);
                    mRemoteContactid = data.getIntExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_ID, 0);
                    mRemoteDisName = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_DISNAME);
                    if (TextUtils.isEmpty(mRemoteSip))
                        return;

                    CursorEndEditTextView editText = (CursorEndEditTextView) findViewById(R.id.activity_main_message_fragment_chat_receiver);
                    editText.setTextCursorEnd(mRemoteSip);
                    break;
                case SELECT_CONTACT_TRANSFER:
                    session = getContactSelectSession(data);
                    if(session==null)
                    { return; }

                    for (long id : transferMessageId) {
                        MessageEvent old = MessageEvent.messageFromID(this,id);
                        if (old!=null) {
                            long messgeid;
                            JSONObject jsonObject = old.getJsonContent();
                            String messageType;
                            try {
                                messageType = jsonObject.getString(MessageEvent.KEY_MESSAGE_TYPE);
                            } catch (JSONException e) {
                                return;
                            }

                            MessageEvent message;
                            if(MessageEvent.MESSAGE_TYPE_TEXT.equals(messageType)) {
                                message = new MessageEvent(session.getId(), session.getDisplayName(), MessageEvent.MessageStatus.PROCESSING, true);
                                message.setContent(old.getMime(), old.getContent());
                                messgeid = mSipMgr.portSipSendMessage(session.getRemoteUri(), old.getMime(), false, old.getContent().getBytes());
                                message.setSMSId(messgeid);
                                if(message!=null) {
                                    DataBaseManager.insertMessage(this, message);
                                }
                            }else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)||MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){
                                String filePath;
                                try {
                                    filePath = jsonObject.getString(MessageEvent.KEY_FILE_PATH);
                                } catch (JSONException e) {
                                    filePath = "";
                                }
                                if (!fileExits(filePath)) {
                                    Toast.makeText(this,R.string.can_not_openfile,Toast.LENGTH_LONG).show();
                                    break;
                                }

                                message = constructStreamMessage(session.getId(),session.getRemoteUri(),jsonObject,old.getMessageDuration());
                                sendStreamMessage(message);
                            }

                        } else {
                            Toast.makeText(this, getString(R.string.chat_transfer_failed), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                case SELECT_CONTACT_TRANSFER_ONEBYONE:
                    session = getContactSelectSession(data);
                    if(session==null)
                    { return; }
                    where = getQuareyArg();
                    Cursor cursor = CursorHelper.resolverQuery(this.getContentResolver(), DBHelperBase.MessageColumns.CONTENT_URI, null, where, null, null);
                    ArrayList<MessageEvent> list = new ArrayList<>();

                    while (CursorHelper.moveCursorToNext(cursor)) {
                        MessageEvent old = MessageEvent.messageFromCursor(cursor);

                        MessageEvent message = new MessageEvent(session.getId(), session.getDisplayName(),  MessageEvent.MessageStatus.PROCESSING, true);
                        message.setContent(old.getMime(), old.getContent());
                        long messgeid = mSipMgr.portSipSendMessage(session.getRemoteUri(), old.getMime(), false, old.getContent().getBytes());
                        message.setSMSId(messgeid);
                        DataBaseManager.insertMessage(this, message);

                    }
                    CursorHelper.closeCursor(cursor);
                    Toast.makeText(this, getString(R.string.chat_transfer_success), Toast.LENGTH_LONG).show();

                    break;
                case SELECT_CONTACT_TRANSFER_TOGETHER: {
                    session = getContactSelectSession(data);
                    if(session==null)
                    { return; }
                    where = getQuareyArg();
                    cursor = CursorHelper.resolverQuery(this.getContentResolver(),
                            DBHelperBase.MessageColumns.CONTENT_URI, null, where, null, null);
                    MessageEvent old = null;
                    while (CursorHelper.moveCursorToNext(cursor)) {
                        old = MessageEvent.messageFromCursor(cursor);
                        String realContent = "";
                        try{
                            JSONObject jContent = old.getJsonContent();
                            realContent = jContent.getString(KEY_TEXT_CONTENT);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        content += realContent + "\r\n";
                    }
                    CursorHelper.closeCursor(cursor);
                    MessageEvent message = new MessageEvent(session.getId(), session.getDisplayName(),MessageEvent.MessageStatus.PROCESSING, true);
                    message.setContent(old.getMime(), content);
                    long messgeid = this.mSipMgr.portSipSendMessage(session.getRemoteUri(), old.getMime(), false, content.getBytes());
                    message.setSMSId(messgeid);
                    DataBaseManager.insertMessage(this, message);
                    Toast.makeText(this, getString(R.string.chat_transfer_success), Toast.LENGTH_LONG).show();
                }
                break;

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ChatSession getContactSelectSession(Intent data){
        ChatSession  session = null;
        String tempRemoteSip = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_NUMBER);
        int tempRemoteContactid = data.getIntExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_ID, 0);
        String tempRemoteDisName = data.getStringExtra(PortActivityPhoneNumberSelect.PHONE_CONTACT_DISNAME);
        if (TextUtils.isEmpty(tempRemoteSip))
            return session;

        tempRemoteSip = NgnUriUtils.getFormatUrif4Msg(tempRemoteSip, localReaml);
        if (TextUtils.isEmpty(tempRemoteDisName)) {
            tempRemoteDisName = NgnUriUtils.getUserName(tempRemoteSip);
        }

        return DataBaseManager.getChatSession(this, localUri, tempRemoteSip, tempRemoteDisName, tempRemoteContactid);
    }

    private boolean fileExits(String path){
        if(TextUtils.isEmpty(path)){
            return false;
        }
        return  new File(path).exists();

    }
    private void updateMenu() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String where = null;
        String[] args = null;
        switch (i) {
            case LOADER_MESSAGE_ID:

                UserAccount userAccount = AccountManager.getDefaultUser(this);
                where = "(("+DBHelperBase.MessageColumns.MESSAGE_SESSION_ID+"=?) AND ("+DBHelperBase.MessageColumns.MESSAGE_DELETE+"=0))";

                args = new String[]{"" + chatSession.getId()};
                if (userAccount != null) {
                    return new CursorLoader(this, DBHelperBase.MessageColumns.CONTENT_URI,
                            null, where, args, DBHelperBase.MessageColumns.DEFAULT_ORDER);
                }
                break;
            case LOADER_CONTACT_ID:

                where = DBHelperBase.RemoteColumns._ID + "=?";
                args = new String[]{"" + chatSession.getRemoteId()};
                return new CursorLoader(this, DBHelperBase.RemoteColumns.CONTENT_URI, null, where, args, null);

        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_MESSAGE_ID:

                int positon =  - 1;
                if (positionId > 0) {
                    while (CursorHelper.moveCursorToNext(cursor)) {
                        int id = cursor.getInt(0);
                        if (id == positionId) {
                            positon = cursor.getPosition();
                        }
                    }
                    positionId = -1;
                    cursor.moveToFirst();
                }

                int lastItemPosition = layoutManager.findLastVisibleItemPosition();
                int oldcount = mAdapter.getItemCount();
                mAdapter.swapCursor(cursor);
                int count = mAdapter.getItemCount();
                if(positon>0){
                    mRecycleView.scrollToPosition(positon);
                }else {
                    if ((oldcount != count) && (count - lastItemPosition <=2)) {
                        mRecycleView.smoothScrollToPosition(lastItemPosition+1);
                    }
                }

                DataBaseManager.updateSessionMessageHasReadExceptAudio(this,chatSession.getId());//将非音频的消息全部设置为已读，音频消息，点击播放后才设置
                DataBaseManager.updateSessionReadCount(this,0,chatSession.getId());

                break;
            case LOADER_CONTACT_ID:

                int contactId = 0;
                int remoteID = 0;
                String remoteUri = null;
                while (cursor.moveToFirst()) {
                    contactId = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_CONTACT_ID));
                    remoteID = cursor.getInt(cursor.getColumnIndex(DBHelperBase.RemoteColumns._ID));
                    remoteUri = cursor.getString(cursor.getColumnIndex(DBHelperBase.RemoteColumns.REMOTE_URI));
                    break;
                }

                Bitmap remote = null;
                Contact contact = null;
                String disName = "";
                if (contactId > 0) {
                    contact = ContactManager.getInstance().getObservableContacts().get(contactId);
                    if (contact != null) {
                        remote = contact.getAvatar();
                        disName = contact.getDisplayName();
                    }
                } else {

                    new ContactQueryTask().execute(this, remoteID, remoteUri);
                }

                if (NgnStringUtils.isNullOrEmpty(disName)) {
                    disName = chatSession.getDisplayName();
                    if(NgnStringUtils.isNullOrEmpty(disName)){
                        disName = NgnUriUtils.getDisplayName(remoteUri,this);
                    }
                    toolbar.setTitle(disName);
                } else {
                    toolbar.setTitle(disName);
                }
                if (mAdapter != null) {
                    mAdapter.setUserAvartar(null, localDisname, remote, disName);
                    mAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_MESSAGE_ID:
                mAdapter.swapCursor(null);
                break;
            default:
                break;
        }
    }

    private boolean createOutPutDir(String path) {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        File destDir = new File(path);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return true;
    }

    private void enterSelectStatus() {
        if(false) {
            mAdapter.setSelectMode(true);
            mAdapter.notifyDataSetChanged();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.getMenu().setGroupVisible(R.id.chat_group_none, false);
            toolbar.getMenu().setGroupVisible(R.id.chat_group_normal, false);
            toolbar.getMenu().setGroupVisible(R.id.chat_group_select, true);

            getFragmentManager().beginTransaction().replace(R.id.input_container, bottomBar).commit();
            bottomBar.setBottombarLeftText(getString(R.string.chat_transfer));
            bottomBar.setOnClicklinstner(this);
            updateSelectStatus();
        }
    }

    private void exitSelectStatus() {
        mRecycleView.setSelected(false);
        mAdapter.setSelectMode(false);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_none, false);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_normal, false);
        toolbar.getMenu().setGroupVisible(R.id.chat_group_select, false);

        mAdapter.setSelectNone();
        mAdapter.notifyDataSetChanged();

        getFragmentManager().beginTransaction().replace(R.id.input_container, inputChat).commit();
    }

    private void updateSelectStatus() {

        if (mAdapter != null && !mAdapter.getSelectMode())
            return;

        if (mAdapter != null && !mAdapter.isAllSelect()) {
            toolbar.getMenu().findItem(R.id.menu_chat_selectall).setVisible(true);
            toolbar.getMenu().findItem(R.id.menu_chat_clearall).setVisible(false);
        } else {
            toolbar.getMenu().findItem(R.id.menu_chat_clearall).setVisible(true);
            toolbar.getMenu().findItem(R.id.menu_chat_selectall).setVisible(false);
        }
        int selectSize = mAdapter.getSelectItems().size();

        if (selectSize > 0) {
            toolbar.setTitle(String.format(getString(R.string.select_sum), selectSize));
        } else {
            toolbar.setTitle(R.string.please_select);
        }

        View bottomView = LayoutInflater.from(this).inflate(R.layout.view_bottombar, null);
        bottomView.findViewById(R.id.bottombar_left).setEnabled(selectSize > 0);
        bottomView.findViewById(R.id.bottombar_right).setEnabled(selectSize > 0);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        enterSelectStatus();
        return false;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {

        List<Long> selectItems = mAdapter.getSelectItems();
        transferMessageId.clear();
        transferMessageId.addAll(selectItems);
        Intent intent = new Intent();
        intent.setClass(this, PortActivityPhoneNumberSelect.class);
        switch (i) {
            case 0:
                startActivityForResult(intent, SELECT_CONTACT_TRANSFER_TOGETHER);
                break;
            case 1:
                startActivityForResult(intent, SELECT_CONTACT_TRANSFER_ONEBYONE);
                break;
        }
    }

    private String getQuareyArg() {
        String where = null;
        if (transferMessageId.size() > 0) {
            where = DBHelperBase.MessageColumns._ID + " IN (";
            int size = transferMessageId.size();
            for (int count = 0; count < size; count++) {
                where += transferMessageId.get(count);
                if (count + 1 != size) {
                    where += ",";
                } else {
                    where += ")";
                }
            }
        }
        return where;
    }

    @Override
    protected void onPause() {
        player.reset();
        if(mAdapter!=null){
            mAdapter.sendAudioPlayMessage(-1L);
        }
        super.onPause();
    }


    long playEventId = -1;
    long playEventTime = -1;
    @Override
    public void onMessageViewClick(View view, long messageid, long messageRowId, long messagetime, String mimeType, boolean read, boolean local, @NonNull JSONObject content, MessageEvent.MessageStatus staus) {
        String filePath;
        String messageType;
        try {
            filePath = content.getString(MessageEvent.KEY_FILE_PATH);
            messageType = content.getString(MessageEvent.KEY_MESSAGE_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if(MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)) {
            if(player.playSound(filePath,messageid,this)){
                if(mAdapter!=null) {
                    mAdapter.sendAudioPlayMessage(messageid);
                }
                playEventId = messageid;
                playEventTime =messagetime;
            }else {
                playEventId = -1L;
                if(mAdapter!=null) {
                    mAdapter.sendAudioPlayMessage(-1L);
                }
            }
            if(!read&&!local){
                DataBaseManager.updateMessageReadStatus(this,true,messageid);
            }
        }else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)){
            player.reset();
            Intent playIntent = new Intent(this, PortActivityVideoPlayer.class);
            playIntent.putExtra(PortActivityVideoPlayer.PLAYER_NAME,filePath);
            startActivity(playIntent);
        }else if(MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    Intent viewIntent = new Intent(this, PortActivityImageView.class);
                    viewIntent.setDataAndType(ImagePathUtils.getFileProviderUri(this, file), "image/*");
                    this.startActivity(viewIntent);
                }
            }catch (NullPointerException e){

            }
        }else if(MessageEvent.MESSAGE_TYPE_FILE.equals(messageType)){
            File file = new File(filePath);
            if(file.exists()&&!file.isDirectory()) {
                openFile(file,file.getName());
            }
        }
    }

    void openFile(File file,String orignalName){
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.setDataAndType(ImagePathUtils.getFileProviderUri(this,file), MIMEType.getMIMEType(orignalName));
        try {
            startActivity(intent);
        }catch (ActivityNotFoundException e){
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        MessageEvent event = DataBaseManager.getNextReceivedAudioMessage(this,playEventId,playEventTime);

        if(mAdapter!=null){
            playEventId = -1L;
            playEventTime = 0xEFFFFFFF;
            mAdapter.sendAudioPlayMessage(-1L);
        }

        if(event!=null&&!event.isMessageRead()){
            onMessageViewClick(null,event.getSMSId(),event.getId(),event.getMessageTime(),event.getMime(),false,false,event.getJsonContent(),event.getMessageStatus());
        }

    }
}
