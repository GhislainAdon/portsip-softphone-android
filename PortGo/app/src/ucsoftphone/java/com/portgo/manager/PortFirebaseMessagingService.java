package com.portgo.manager;

import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.portgo.R;
import com.portgo.BuildConfig;

import com.portgo.util.NgnUriUtils;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class PortFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /*
     * @param messageBody FCM message body received.
     */
    InComingMessageProcessor messageProcessor;
    @Override
    public void onCreate() {
        super.onCreate();
        messageProcessor = new InComingMessageProcessor(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messageProcessor = null;
    }

    final int NOTIVICATIONID = 492371;
    final int MESSAGE_NOTIFICATION = NOTIVICATIONID+3;
    private void sendNotification(String title,String from ,String messageBody,int messageid) {

        NotificationUtils.getInstance(this).showMessageNotification(this,R.drawable.app_icon,title,messageBody);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String>  data = remoteMessage.getData();
        if(data!=null){
            if ("audio".equals(data.get("msg_type"))||"call".equals(data.get("msg_type"))||"video".equals(data.get("msg_type"))) {
                Intent sipService = new Intent(this, PortSipService.class);
                sipService.setAction(BuildConfig.PORT_ACTION_REGIEST);

                PortSipService.startServiceCompatibility(this,sipService);
            }else if ("im".equals(data.get("msg_type"))) {
                String content = data.get("msg_content");
                String from = data.get("send_from");
                String to = data.get("send_to");
                String pushid = data.get("portsip-push-id");//old
                String xpushid = data.get("x-push-id");//new version
                String mimeType = data.get("mime_type");

                if (content == null || TextUtils.isEmpty(from) || TextUtils.isEmpty(to) ||(TextUtils.isEmpty(pushid)&&TextUtils.isEmpty(xpushid))){
                    return;
                }

                long messageid;

                if(TextUtils.isEmpty(pushid)){
                    pushid = xpushid;
                }
                if(TextUtils.isEmpty(pushid)){
                    messageid = new Random().nextLong();
                }else{
                    try {
                        messageid = Long.parseLong(pushid);
                    }catch (Exception e) {
                        messageid = new Random().nextLong();
                    }
                }

                if(TextUtils.isEmpty(mimeType)||mimeType.indexOf("/")==-1){//老版本，支持text，没有mime字段
                    mimeType = "text/plain";
                }
                String[] mimes = mimeType.split("/");
                String fromDisname = NgnUriUtils.getUserName(from);

                byte[]contentData = content.getBytes(Charset.forName("UTF-8"));
                messageProcessor.onRecvOutOfDialogMessage(fromDisname, from,to,  to,  mimes[0],
                        mimes[1],contentData, contentData.length,null,messageid, new Date().getTime());
            }
        }
    }

    @Override
    public void onNewToken(String s) {
        sendRegistrationToServer(s);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    private void sendRegistrationToServer(String token) {
        Intent intent = new Intent(this,PortSipService.class);
        intent.setAction(BuildConfig.PORT_ACTION_TOKEN);
        intent.putExtra(PortSipService.TOKEN_REFRESH,token);

        PortSipService.startServiceCompatibility(this,intent);
    }

}
