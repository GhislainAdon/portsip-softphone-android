package com.portgo.manager;

import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.portgo.BuildConfig;
import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.database.DBHelperBase;
import com.portgo.database.DataBaseManager;


import com.portgo.util.ContactQueryTask;
import com.portgo.util.NgnUriUtils;
import com.portgo.util.OkHttpHelper;
import com.portgo.util.OkHttpUpDownLoader;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.Request;

import static com.portgo.BuildConfig.ENABLEIM;
import static com.portgo.BuildConfig.HASIM;
import static com.portgo.manager.ContactManager.PORTSIP_IM_PROTOCAL;
import static com.portgo.util.MIMEType.MIMETYPE_appJson;
import static com.portgo.util.MIMEType.MIMETYPE_custom_file;
import static com.portgo.util.MIMEType.MIMETYPE_textplain;
import static com.portgo.util.OkHttpHelper.DEFAULT_LIST_KEY_LIMITED_COUNT;
import static com.portgo.util.OkHttpHelper.FILEUPDATE_RESULT_KEY_FILEID;
import static com.portgo.util.OkHttpHelper.FILEUPDATE_RESULT_KEY_URL;
import static com.portgo.util.OkHttpHelper.LIST_KEY_SENDER;
import static com.portgo.util.OkHttpHelper.LIST_RESULT_KEY_MESSAGE_SENDID;
import static com.portgo.util.OkHttpHelper.UNREAD_RESULT_KEY_COUNT;
import static com.portgo.util.media.MediaCodecInfo.getFilePlayTime;

/**
 * Contact class defining an entity from the native address book or XCAP server.
 */
public class InComingMessageProcessor implements OkHttpUpDownLoader.ReqCallBack{
    Context mContext;
	public InComingMessageProcessor(@NonNull Context context){
		super();
        mContext = context;
	}


    @Override
    public void onReqSuccess(OkHttpUpDownLoader.DownLoadParams action, Request requst, String result) {

        switch (action.getAction())
        {
            case OFFLINE_VERIFY:
                break;
            case OFFLINE_CONTACT:
                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        int count = resultJson.getInt(OkHttpHelper.CONTACT_RESULT_KEY_COUNT);
                        if(count>0){
                            JSONArray extensions = resultJson.getJSONArray(OkHttpHelper.CONTACT_RESULT_KEY_EXTENSIONS);
                            UserAccount account = AccountManager.getDefaultUser(mContext);
                            String domain = null;
                            if(account!=null) {
                                domain = account.getDomain();
                            }
                            String receiverextension = account.getAccountNumber()+"@"+domain;
                            for(int i=0;i<extensions.length();i++){
								JSONObject extension = extensions.getJSONObject(i);
								String extensionNum = extension.getString(OkHttpHelper.CONTACT_RESULT_KEY_EXTENSION_NUM);
                                OkHttpHelper.offlineMsgUnreadCount(extensionNum + "@" + domain,receiverextension,this);
                                //JSONObject extension = extensions.getJSONObject(i);
                                //int unreadCount = extension.getInt(OkHttpHelper.UNREAD_RESULT_KEY_COUNT);
                                //String extensionNum = extension.getString(OkHttpHelper.CONTACT_RESULT_KEY_EXTENSION_NUM);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case OFFLINE_UPDATE:

                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case OFFLINE_LIST_NORMAL:

                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        int count = resultJson.getInt("count");
                        if(count>0){
                            ArrayList<Long> msgIds = new ArrayList<>();
                            JSONArray messages = resultJson.getJSONArray("messages");
                            String sender =requst.url().queryParameter(LIST_KEY_SENDER);
                            String receiver =requst.url().queryParameter(LIST_KEY_SENDER);
                            if(TextUtils.isEmpty(sender)||TextUtils.isEmpty(sender))
                                return;
                            for(int i=0;i<messages.length();i++){
                                JSONObject msg = messages.getJSONObject(i);
                                long messageId = msg.getLong("id");
                                String message = msg.getString("msg_body");
                                String mimeType = msg.getString("msg_type");
                                String readStatus = msg.getString("status");
                                long messagetime = msg.getLong("post_time")*1000;
                                long sendId = msg.getLong(LIST_RESULT_KEY_MESSAGE_SENDID);
                                if(mimeType.equals("amr")){
                                    mimeType="audio/amr";
                                }
                                if(mimeType.indexOf("/")==-1){
                                    mimeType = "text/plain";
                                }
                                String[] mime = mimeType.split("/");
                                byte[] content = message.getBytes();
                                if(!"read".equals(readStatus)) {
                                    msgIds.add(messageId);
                                }
                                if(sendId==OkHttpUpDownLoader.getInstance().getMyExtensionId()){
                                }else{
                                    onRecvOutOfDialogMessage(null, sender, receiver, receiver, mime[0],
                                            mime[1], content, content.length, null, messageId, messagetime);
                                }
                            }

                            OkHttpHelper.offlineMsgUpdate(msgIds,this);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break;
            case OFFLINE_LIST_BETWEEN:
//
                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        int count = resultJson.getInt("count");
                        if(count>0){
                            ArrayList<Long> msgIds = new ArrayList<>();
                            JSONArray messages = resultJson.getJSONArray("messages");
                            String sender =requst.url().queryParameter(LIST_KEY_SENDER);
                            String receiver =requst.url().queryParameter(LIST_KEY_SENDER);
                            if(TextUtils.isEmpty(sender)||TextUtils.isEmpty(sender))
                                return;
                            for(int i=0;i<messages.length();i++){
                                JSONObject msg = messages.getJSONObject(i);
                                long messageId = msg.getLong("id");

                                String message = msg.getString("msg_body");
                                String mimeType = msg.getString("msg_type");
                                String readStatus = msg.getString("status");
                                long messagetime = msg.getLong("post_time")*1000;
                                long sendId = msg.getLong(LIST_RESULT_KEY_MESSAGE_SENDID);
                                if(mimeType.equals("amr")){
                                    mimeType="audio/amr";
                                }
                                if(mimeType.indexOf("/")==-1){
                                    mimeType = "text/plain";
                                }
                                String[] mime = mimeType.split("/");
                                byte[] content = message.getBytes();
                                if(!"read".equals(readStatus)) {
                                    msgIds.add(messageId);
                                }
                                if(sendId==OkHttpUpDownLoader.getInstance().getMyExtensionId()){

                                }else{
                                    onRecvOutOfDialogMessage(null, sender, receiver, receiver, mime[0],
                                            mime[1], content, content.length, null, messageId, messagetime);
                                }
                            }

                            OkHttpHelper.offlineMsgUpdate(msgIds,this);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case OFFLINE_LIST_AFTER:

                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        int count = resultJson.getInt("count");
                        if(count>0){
                            ArrayList<Long> msgIds = new ArrayList<>();
                            JSONArray messages = resultJson.getJSONArray("messages");
                            String sender =requst.url().queryParameter(LIST_KEY_SENDER);
                            String receiver =requst.url().queryParameter(LIST_KEY_SENDER);
                            long messageId =1;
                            if(TextUtils.isEmpty(sender)||TextUtils.isEmpty(sender))
                                return;
                            for(int i=0;i<messages.length();i++){
                                JSONObject msg = messages.getJSONObject(i);
                                //boolean send = msg.getBoolean("is_send");
                                long sendId = msg.getLong(LIST_RESULT_KEY_MESSAGE_SENDID);
                                messageId = msg.getLong("id");
                                String message = msg.getString("msg_body");
                                String mimeType = msg.getString("msg_type");
                                String readStatus = msg.getString("status");

                                long messagetime = msg.getLong("post_time")*1000;
                                if(mimeType.equals("amr")){
                                    mimeType="audio/amr";
                                }
                                if(mimeType.indexOf("/")==-1){
                                    mimeType = "text/plain";
                                }
                                String[] mime = mimeType.split("/");
                                byte[] content = message.getBytes();
                                if(!"read".equals(readStatus)) {
                                    msgIds.add(messageId);
                                }
                                if(sendId==OkHttpUpDownLoader.getInstance().getMyExtensionId()){
//                                    JSONObject jsonObject =MessageEvent.parserMessage(message);
//                                    String disName = NgnUriUtils.getDisplayName(sender,mContext);
//                                    ChatSession session = DataBaseManager.getChatSession(mContext,receiver,sender,disName,-1);
//                                    MessageEvent event = new MessageEvent((int)session.getId(),disName, MessageEvent.MessageStatus.SUCCESS,true,messagetime);
//                                    event.setContent(mimeType,jsonObject);
//                                    event.setSMSId(messageId);
//                                    DataBaseManager.insertMessage(mContext,event);
                                }else{
                                    onRecvOutOfDialogMessage(null, sender, receiver, receiver, mime[0],
                                            mime[1], content, content.length, null, messageId, messagetime);
                                }
                            }

                            OkHttpHelper.offlineMsgUpdate(msgIds,this);

                            if(count==DEFAULT_LIST_KEY_LIMITED_COUNT){
                                OkHttpHelper.offlineMsgListAfter(messageId,sender, receiver, this);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case OFFLINE_UNREADCOUNT:

                if(!TextUtils.isEmpty(result)) {
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        int count = resultJson.getInt(OkHttpHelper.UNREAD_RESULT_KEY_COUNT);

                        HttpUrl url= requst.url();
                        String senderextension= url.queryParameter("sender_extension");
                        String receiverextension= url.queryParameter("receiver_extension");
                        long messid = DataBaseManager.lastReceivedMessageId(mContext,senderextension,receiverextension);
                        if(messid==-1) {
                            if(count>0) {
                                int page = count / 100;
                                while (page >= 0) {
                                    OkHttpHelper.offlineMsgListNormal("" + page, senderextension, receiverextension, this);
                                    page--;
                                }
                            }
                        }else{
                            OkHttpHelper.offlineMsgListAfter(messid,senderextension, receiverextension, this);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case FILE_UPLOAD:
                long messageid = action.getMessageid();
                if(!TextUtils.isEmpty(result)) {
                    try {
                        String url;
                        JSONObject resultJson = new JSONObject(result);
                        url = resultJson.getString(FILEUPDATE_RESULT_KEY_URL);

                        MessageEvent event =  DataBaseManager.findMessageByMessageId(mContext,messageid);

                        String mime = null, remoteSip = null;
                        JSONObject content = null;
                        ChatSession session =null;
                        long rowID = 0;
                        if (event != null) {
                            session = ChatSession.findChatSession(mContext,event.getSessionid());
                            rowID = event.getId();
                            mime = event.getMime();
                            content = event.getJsonContent();
                        }
                        if(session!=null){
                            remoteSip = session.getRemoteUri();
                        }

                        if (remoteSip != null && url != null) {
                            content.put(MessageEvent.KEY_FILE_URL,url);
                            String newContent = content.toString();
                            long realmessgeid = SipManager.getSipManager().portSipSendMessage(remoteSip, mime, false, newContent.getBytes());

                            ContentValues values = new ContentValues();
                            values.put(DBHelperBase.MessageColumns.MESSAGE_ID, realmessgeid);
                            values.put(DBHelperBase.MessageColumns.MESSAGE_STATUS, MessageEvent.MessageStatus.ATTACH_SUCESS.ordinal());
                            values.put(DBHelperBase.MessageColumns.MESSAGE_CONTENT, newContent.getBytes());
                            DataBaseManager.upDataMessageByRowID(mContext,rowID,values);
                        }
                    } catch (JSONException e) {
                        DataBaseManager.upDataMessageStatus(mContext,messageid,MessageEvent.MessageStatus.ATTACH_FAILED);
                    }
                }
                break;

            case FILE_DOWNLOAD:
                long messageId = action.getMessageid();
                if(!TextUtils.isEmpty(result)) {
                    int duration = 0;
                    MessageEvent event = DataBaseManager.findMessageByMessageId(mContext,messageId);
                    JSONObject oldContent = event.getJsonContent();
                    String messageType;
                    String newContent;
                    try {
                        messageType = oldContent.getString(MessageEvent.KEY_MESSAGE_TYPE);
                        if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)||MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)){
                            duration= oldContent.getInt(MessageEvent.KEY_AV_DURATION);
                            if(duration<=0) {
                                duration = getFilePlayTime(new File(result)) / 1000;//转成秒
                            }
                        }

                        oldContent.put(MessageEvent.KEY_FILE_PATH,result);
                        newContent = oldContent.toString();
                    }catch (JSONException e){
                        return;
                    }

                    DataBaseManager.upDataMessageDurationStatus(mContext,messageId,duration,MessageEvent.MessageStatus.SUCCESS,newContent.getBytes());
                }
                break;
            case FILE_DELETE:

                break;

        }
    }

    @Override
    public void onProgress(OkHttpUpDownLoader.DownLoadParams action, String filename, long total, long current) {
        switch (action.getAction()) {
            case FILE_UPLOAD:
                break;
            case FILE_DOWNLOAD:
                break;
        }
    }

    @Override
    public void onReqFailed(OkHttpUpDownLoader.DownLoadParams action, Request requst, String errorMsg) {
        long messageid = 0;
        switch (action.getAction()) {
            case OFFLINE_VERIFY:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_VERIFY"+errorMsg);
                break;
            case OFFLINE_UNREADCOUNT:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_UNREADCOUNT"+errorMsg);
                break;
            case OFFLINE_LIST_NORMAL:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_LIST_NORMAL"+errorMsg);
                break;
            case OFFLINE_LIST_BETWEEN:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_LIST_BETWEEN"+errorMsg);
                break;
            case OFFLINE_LIST_AFTER:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_LIST_AFTER"+errorMsg);
                break;
            case OFFLINE_UPDATE:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_UPDATE"+errorMsg);
                break;
            case OFFLINE_CONTACT:
//                PortApplication.getLogUtils().d("offlineMsg Failed","OFFLINE_CONTACT"+errorMsg);
                break;
            case FILE_UPLOAD://
                messageid = action.getMessageid();
                DataBaseManager.upDataMessageStatus(mContext,messageid,MessageEvent.MessageStatus.ATTACH_FAILED);
                break;
            case FILE_DOWNLOAD:
                messageid = action.getMessageid();
//                PortApplication.getLogUtils().d("FILE_UPLOAD","oooooo messageid = "+messageid);
                DataBaseManager.upDataMessageStatus(mContext,messageid,MessageEvent.MessageStatus.ATTACH_FAILED);
                break;
            case FILE_DELETE:
//                PortApplication.getLogUtils().d("onReqFailed","FILE_DELETE"+errorMsg);
                break;
        }
    }

    public void onRecvOutOfDialogMessage(String fromDisplayName, String from,
                                          String toDisplayName, String to, String mimeType,
                                          String subMimeType, byte[] messageData, int messageDataLength,String sipMessage,long messageid,long posttime){
        if(!HASIM||!ENABLEIM){
            return;
        }
        UserAccount userAccount =AccountManager.getDefaultUser(mContext);
        if(userAccount==null){
            return;
        }

        if(DataBaseManager.messageExitsByMessageId(mContext,messageid)){
            return;
        }
        String totalMime = mimeType+"/"+subMimeType;
        String local = userAccount.getFullAccountReamName();
        String disName = fromDisplayName ;
        from = from.replaceFirst(NgnUriUtils.SIP_HEADER,"");
        String fromUserName = NgnUriUtils.getUserName(from);
        from = NgnUriUtils.getFormatUrif4Msg(fromUserName,userAccount.getRealmDefaultDomain());
        if(TextUtils.isEmpty(fromDisplayName)||fromDisplayName.trim().length()==0){
            disName = NgnUriUtils.getDisplayName(from,mContext);
        }

        String strMessageData = "";
        MessageEvent.MessageStatus status = MessageEvent.MessageStatus.PROCESSING;
        if (mimeType.equals("application")){
            if( subMimeType.equals("vnd.3gpp.sms")) {

            } else if (subMimeType.equals("vnd.3gpp2.sms")) {

            }else if (subMimeType.equals("octet-stream")) {//.* .bin .class .dms exe .lha .lzh

            }
        }

        if(!MIMETYPE_textplain.equals(totalMime)&&!MIMETYPE_appJson.equals(totalMime)){
            return;
        }
        strMessageData = new String(messageData);
        JSONObject jsonObject = MessageEvent.parserMessage(strMessageData);

        try {
            String messageType = jsonObject.getString(MessageEvent.KEY_MESSAGE_TYPE);
            if(MessageEvent.MESSAGE_TYPE_TEXT.equals(messageType)){
                status = MessageEvent.MessageStatus.SUCCESS;
            }else if(SipManager.getSipManager().pbxSupportFileTransfer()){
                status = MessageEvent.MessageStatus.PROCESSING;
                String url = jsonObject.getString(MessageEvent.KEY_FILE_URL);
                String fileName = jsonObject.getString(MessageEvent.KEY_FILE_NAME);
                jsonObject.put(MessageEvent.KEY_FILE_PATH,"");

                String path;
                if(MessageEvent.MESSAGE_TYPE_FILE.equals(messageType)){
                    path = ConfigurationManager.getInstance().getStringValue(mContext, ConfigurationManager.PRESENCE_FILE_PATH,
                            mContext.getExternalFilesDir(null).getAbsolutePath()+ConfigurationManager.PRESENCE_FILE_DEFALUT_SUBPATH);
                    OkHttpHelper.downLoadFile(url, messageid, fileName, path, this);
                }else if(MessageEvent.MESSAGE_TYPE_VIDEO.equals(messageType)||MessageEvent.MESSAGE_TYPE_AUDIO.equals(messageType)
                            ||MessageEvent.MESSAGE_TYPE_IMAGE.equals(messageType)){

                    path = ConfigurationManager.getInstance().getStringValue(mContext, ConfigurationManager.PRESENCE_VIDEO_PATH,
                            mContext.getExternalFilesDir(null).getAbsolutePath());
                    OkHttpHelper.downLoadFile(url, messageid, fileName, path, this);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ChatSession session = DataBaseManager.getChatSession(mContext,local,from,fromDisplayName,-1);
        if(session == null){
            return;
        }else{
            new ContactQueryTask().execute(this,session.getRemoteId(),from);
        }

        MessageEvent event = new MessageEvent((int)session.getId(),disName,status ,false,posttime);
        event.setContent(mimeType+"/"+subMimeType,jsonObject);

        event.setSMSId(messageid);
        DataBaseManager.insertMessage(mContext,event);
        PortApplication mApp =(PortApplication)mContext.getApplicationContext();

        if(!mApp.isForeground()) {
            String description = String.format(mContext.getResources().getString(R.string.message_tips), disName);
            NotificationUtils.getInstance(mContext).showMessageNotification(mContext,R.drawable.app_icon, disName,description);
        }
    }

}
