package com.portgo.util;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.UserAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.HttpUrl;

import static com.portgo.util.OkHttpUpDownLoader.TYPE_GET;

public class OkHttpHelper {
    //192.168.1.136 deng zhuo
    public static final String HTTPSHEAD ="https://";
    public static final String HTTPHEAD ="http://";
//    public static final String OFFLINE_MESSAGE_BASEURL =  "http://192.168.1.136:8899";
//    public static final String FILE_BASEURL =  "http://192.168.1.136:9333";
    public static final int OFFLINE_MESSAGE_BASEURL_HTTP_PORT = 8899;//https=8900 http=8899
    public static final int OFFLINE_MESSAGE_BASEURL_HTTPS_PORT = 8900;//https=8900 http=8899
    public static final int FILE_BASEURL_HTTP_PORT = 9333;//https=8900 http=9333
    public static final int FILE_BASEURL_HTTPS_PORT = 8887;//https=8900 http=9333

    public static final String KEY_ACCESSTOKEN ="access_token";

    public static final String SCHEAM_VERIFY =  "/api/account/extension/sip/verify";//
    public static final String VERIFY_KEY_DOMAIN = "domain";//
    public static final String VERIFY_KEY_NAME = "extension_number";//
    public static final String VERIFY_KEY_PWD = "sip_password";//

    public static final String VERIFY_RESULT_KEY_EXTENSIONID="id";//
    public static final String VERIFY_RESULT_KEY_EXPIRES = "expires";


    public static final String SCHEAM_UPDATE =  "/api/comm_message/update";//
    public static final String UPDATE_KEY_MSGID = "msg_ids";//

    public static final String SCHEAM_CONTACT_LIST =  "/api/comm_message/contact_list";//
    public static final String UPDATE_KEY_PAG="pagination";
    public static final String UPDATE_KEY_STATUS = "status";
    public static final String CONTACT_RESULT_KEY_COUNT = "count";
    public static final String CONTACT_RESULT_KEY_EXTENSIONS = "extensions";
    public static final String CONTACT_RESULT_KEY_EXTENSION_NUM = "extension_number";

    public static final String SCHEAM_UNREAD_COUNT =  "/api/comm_message/unread_count/show";//

    public static final String UNREAD_KEY_SEND = "sender_extension";
    public static final String UNREAD_KEY_RECEIVER = "receiver_extension";
    public static final String UNREAD_RESULT_KEY_COUNT = "count";
    //未读数目接口参数名
    public static final String SCHEAM_LIST =  "/api/comm_message/list";
    public static final String LIST_KEY_SENDER = "sender_extension";
    public static final String LIST_KEY_RECEIVER ="receiver_extension";
    public static final String LIST_KEY_PAG ="pagination";
    public static final String LIST_KEY_MESSAGEID ="specify_msg_id";
    public static final String LIST_KEY_LIMITED_COUNT ="limit_count";
    public static final String LIST_KEY_MOD ="list_mode";
    public static final String LIST_KEY_TIME_STAR ="time_start";
    public static final String LIST_KEY_TIME_STOP ="time_end";
    public static final int DEFAULT_LIST_KEY_LIMITED_COUNT =100;
    public static final String SCHEAM_LIST_MOD_MORMAL ="NORMAL";
    public static final String SCHEAM_LIST_MOD_AFTER ="SPECIFY_LIST_AFTER";
    public static final String SCHEAM_LIST_MOD_BEFORE ="SPECIFY_LIST_BEFOR";
    public static final String SCHEAM_LIST_MOD_TIME ="TIME_DISTANCE";

    public static final String SCHEAM_FILE_UPLOAD =  "/submit";
    public static final String UPLOAD_KEY_MEDIATYPE = "media_type";

    public static final String FILEUPDATE_RESULT_KEY_FID = "fid";
    public static final String FILEUPDATE_RESULT_KEY_fILENAME = "fileName";
    public static final String FILEUPDATE_RESULT_KEY_ETAG = "eTag";
    public static final String FILEUPDATE_RESULT_KEY_SIZE = "size";
    public static final String FILEUPDATE_RESULT_KEY_URL = "fileUrl";
    public static final String FILEUPDATE_RESULT_KEY_ID = "id";
    public static final String FILEUPDATE_RESULT_KEY_FILEID = "fileid";

    public static final String  LIST_RESULT_KEY_MESSAGE_SENDID = "sender_extension_id";

    public static final String SCHEAM_FILE_DOWNLOAD =  "/submit";
    public static final String SCHEAM_FILE_DELETE =  "/delete";
    public static final String DELETE_KEY_TENENTID =  "tenantid";
    public static final String DELETE_KEY_FILEID =  "fid";

    public static enum OKHTTP_ACTION{
        OFFLINE_VERIFY,
        OFFLINE_CONTACT,
        OFFLINE_UNREADCOUNT,
        OFFLINE_LIST_NORMAL,
        OFFLINE_LIST_AFTER,
        OFFLINE_LIST_BETWEEN,
        OFFLINE_UPDATE,
        FILE_UPLOAD,
        FILE_DOWNLOAD,
        FILE_DELETE,
    }

    static String getMessageUrl(@NonNull String scheam){
        String url = HTTPSHEAD + SIP_SERVER + ":" + OFFLINE_MESSAGE_BASEURL_HTTPS_PORT + scheam;

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            url =null;
        }
        return url;
    }

    static public String getFileUrl(@NonNull String scheam){
        String url = HTTPHEAD+SIP_SERVER+":"+FILE_BASEURL_HTTP_PORT+scheam;
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            url =null;
        }
        return url;
    }

    public static String upLoadFile(long messageid, String mediatype, String fileAbsPath, OkHttpUpDownLoader.ReqCallBack callBack){
        if(TextUtils.isEmpty(mediatype)){
            mediatype = "";
        }
        HashMap headParamsMap = new HashMap<String,String>();
        headParamsMap.put(UPLOAD_KEY_MEDIATYPE,mediatype);
        HashMap<String ,Object> fromparams = new HashMap();
        File file = new File(fileAbsPath);
        fromparams.put("file",file);

        OkHttpUpDownLoader httpUpDownLoader= OkHttpUpDownLoader.getInstance();
        String url = getFileUrl(SCHEAM_FILE_UPLOAD);
        return httpUpDownLoader.upLoadFile(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.FILE_UPLOAD,messageid,mediatype),url,headParamsMap,fromparams,callBack);
    }

    public static String downLoadFile(String fileUrl, long messageid, String fileName, String destFileDir, OkHttpUpDownLoader.ReqCallBack callBack){
        if(TextUtils.isEmpty(fileUrl)|| TextUtils.isEmpty(destFileDir))
            return null;
        HttpUrl parsed = HttpUrl.parse(fileUrl);
        if (parsed == null){
            fileUrl="http://"+fileUrl;
            parsed = HttpUrl.parse(fileUrl);
            if (parsed == null){
                return null;
            }
        }

        OkHttpUpDownLoader httpUpDownLoader= OkHttpUpDownLoader.getInstance();
        return  httpUpDownLoader.downLoadFile(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.FILE_DOWNLOAD,messageid,fileName),fileUrl, destFileDir, callBack);
    }

    static  String SIP_SERVER = "";
    static final String SIP_GETWAY_SERVER = "gateway.oncall.vn";
    static final String SIP_FILE_SERVER = "file.oncall.vn";

    public static void setAccount(@NonNull UserAccount account){
        OkHttpUpDownLoader httpUpDownLoader= OkHttpUpDownLoader.getInstance();
        httpUpDownLoader.setAccount(account);
        if(account==null){
            return;
        }
        SIP_SERVER = account.getRealm();//
        if(TextUtils.isEmpty(SIP_SERVER)){
            SIP_SERVER = account.getDomain();
        }

    }
    public static String deleteFile(String fileId, long messageid, OkHttpUpDownLoader.ReqCallBack callBack){
        String url = getFileUrl(SCHEAM_FILE_DELETE);

        HashMap headParamsMap = new HashMap<String,String>();
        headParamsMap.put(DELETE_KEY_TENENTID,"");

        HashMap params = new HashMap<String,String>();
        params.put(DELETE_KEY_FILEID,fileId);

        OkHttpUpDownLoader httpUpDownLoader= OkHttpUpDownLoader.getInstance();
        return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.FILE_DELETE),url,TYPE_GET,headParamsMap,params,callBack);
    }

    public static String offlineMsgUpdate(List<Long> msgIds, OkHttpUpDownLoader.ReqCallBack callBack){
        if(msgIds==null||msgIds.isEmpty())
            return null;

        HashMap headParamsMap = new HashMap<String,String>();

        JSONObject root = new JSONObject();
        JSONArray messages = new JSONArray();
        try {
            for (Long messageID : msgIds) {
                messages.put(messageID);
            }
            root.put("msg_ids", messages);
        }catch (JSONException e) {
                e.printStackTrace();
        }
        String url = getMessageUrl (SCHEAM_UPDATE);
        if(url!=null) {
            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();
            return httpUpDownLoader.requestPostByAsynWithJSON(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_UPDATE), url, headParamsMap, root, callBack);
        }else{
            return "";
        }
    }

    public static String offlineMsgListNormal(String pagination , String sender, String receiver, OkHttpUpDownLoader.ReqCallBack callBack){
        String url = getMessageUrl (SCHEAM_LIST);

        if(url!=null) {
            HashMap headParamsMap = new HashMap<String, String>();

            HashMap params = new HashMap<String, String>();
            params.put(LIST_KEY_SENDER, sender);
            params.put(LIST_KEY_RECEIVER, receiver);
            params.put(LIST_KEY_PAG, pagination);
            params.put(LIST_KEY_MOD, SCHEAM_LIST_MOD_MORMAL);

            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();
            return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_LIST_NORMAL), url, TYPE_GET, headParamsMap, params, callBack);
        }else{
            return "";
        }
    }

    public static String offlineMsgListAfter(long messageid , String sender, String receiver, OkHttpUpDownLoader.ReqCallBack callBack){
        String url = getMessageUrl (SCHEAM_LIST);
        if(url!=null) {
            HashMap headParamsMap = new HashMap<String, String>();

            HashMap params = new HashMap<String, String>();
            params.put(LIST_KEY_SENDER, sender);
            params.put(LIST_KEY_RECEIVER, receiver);
            params.put(LIST_KEY_MESSAGEID, "" + messageid);
            params.put(LIST_KEY_LIMITED_COUNT, "" + DEFAULT_LIST_KEY_LIMITED_COUNT);
            params.put(LIST_KEY_MOD, SCHEAM_LIST_MOD_AFTER);

            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();
            return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_LIST_AFTER), url, TYPE_GET, headParamsMap, params, callBack);
        }else{
            return "";
        }

    }

    public static String offlineMsgListByTime(String sender, String receiver, OkHttpUpDownLoader.ReqCallBack callBack){
        String url = getMessageUrl (SCHEAM_LIST);
        if(url!=null) {
            HashMap headParamsMap = new HashMap<String, String>();

            HashMap params = new HashMap<String, String>();
            params.put(LIST_KEY_SENDER, sender);
            params.put(LIST_KEY_RECEIVER, receiver);
            params.put(LIST_KEY_MOD, SCHEAM_LIST_MOD_TIME);

            long curTime = new Date().getTime() / 1000;
            long halfMoth = curTime - 7 * 24 * 3600;
            params.put(LIST_KEY_TIME_STAR, "" + halfMoth);
            params.put(LIST_KEY_TIME_STOP, "" + curTime);

            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();
            return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_LIST_BETWEEN), url, TYPE_GET, headParamsMap, params, callBack);
        }else{
            return "";
        }
    }

    public static String offlineMsgContactList(String pag, String status, OkHttpUpDownLoader.ReqCallBack callBack){
        String url = getMessageUrl (SCHEAM_CONTACT_LIST);
        if(url!=null) {
            HashMap headParamsMap = new HashMap<String, String>();

            HashMap params = new HashMap<String, String>();
            params.put(UPDATE_KEY_PAG, pag);
            params.put(UPDATE_KEY_STATUS, status);

            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();
            return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_CONTACT), url, TYPE_GET, headParamsMap, params, callBack);
        }else{
            return "";
        }
    }

    public static String offlineMsgUnreadCount(String send, String receiver, OkHttpUpDownLoader.ReqCallBack callBack){

        String url = getMessageUrl (SCHEAM_UNREAD_COUNT);
        if(url!=null) {
            OkHttpUpDownLoader httpUpDownLoader = OkHttpUpDownLoader.getInstance();

            HashMap headParamsMap = new HashMap<String, String>();

            HashMap params = new HashMap<String, String>();
            params.put(UNREAD_KEY_SEND, send);
            params.put(UNREAD_KEY_RECEIVER, receiver);

            return httpUpDownLoader.requestAsyn(new OkHttpUpDownLoader.DownLoadParams(OKHTTP_ACTION.OFFLINE_UNREADCOUNT), url, TYPE_GET, headParamsMap, params, callBack);
        }else{
            return "";
        }
    }
    public static String getFilePath(Context context){
        String dirName = context.getString(R.string.prefrence_record_filepath_default);
        String filePath = context.getExternalFilesDir(dirName).getAbsolutePath();
        return filePath;
    }
    public static void cancellAll(){
            OkHttpUpDownLoader httpUpDownLoader= OkHttpUpDownLoader.getInstance();
            httpUpDownLoader.cancellAll();
        }

}
