package com.portgo.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.portgo.PortApplication;
import com.portgo.manager.UserAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import static com.portgo.util.MIMEType.getExtensionNameByMIME;
import static com.portgo.util.OkHttpHelper.KEY_ACCESSTOKEN;
import static com.portgo.util.OkHttpHelper.SCHEAM_VERIFY;
import static com.portgo.util.OkHttpHelper.VERIFY_KEY_DOMAIN;
import static com.portgo.util.OkHttpHelper.VERIFY_KEY_NAME;
import static com.portgo.util.OkHttpHelper.VERIFY_KEY_PWD;

public class OkHttpUpDownLoader {
    //private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");//mdiatype 这个需要和服务端保持一致
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");//mdiatype
//    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded;");//mdiatype
    private static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");//mdiatype
    private static final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream");
    private static final String TAG = OkHttpUpDownLoader.class.getSimpleName();
    public static final int TYPE_GET = 0;//
    public static final int TYPE_POST_JSON = 1;//
    public static final int TYPE_POST_FORM = 2;//
    private Handler okHttpHandler;//
    private final int FILE_BUFF_SIZE = 2048;

    private String accessToken;//token
    private long accessTime=0;//
    private long mExtensionId=0;//

    public static class DownLoadParams extends Object {
        OkHttpHelper.OKHTTP_ACTION mActionId;
        long mMessageid;
        String mFileName;
        public DownLoadParams( OkHttpHelper.OKHTTP_ACTION action){
            this(action,0);
        }

        public DownLoadParams(OkHttpHelper.OKHTTP_ACTION action, long messageid){
            this(action,messageid,"");
        }

        public DownLoadParams(OkHttpHelper.OKHTTP_ACTION action,  long messageid,String mime){
            this.mActionId =action;
            this.mMessageid = messageid;
            this.mFileName = mime;
        }

        public long getMessageid() {
            return mMessageid;
        }

        public OkHttpHelper.OKHTTP_ACTION getAction() {
            return mActionId;
        }
        public String getFileName() {
            return mFileName;
        }
    }

    ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<Call>();
    private OkHttpClient mOkHttpClient;
    private static class LazyHolder {
        private static final OkHttpUpDownLoader INSTANCE = new OkHttpUpDownLoader();
    }
    static public OkHttpUpDownLoader getInstance(){
        return LazyHolder.INSTANCE;
    }
    private OkHttpUpDownLoader(){
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .sslSocketFactory(getSSLSocketFactory(),getX509TTrustManager())
                .hostnameVerifier(getHostnameVerifier())
                .addInterceptor(new TokenInterceptor())//
                .build();

        okHttpHandler = new Handler(Looper.getMainLooper());
    };


    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");//"SSL" "TLSv1.1" "TLSv1.2"
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                getX509TTrustManager()
        };
        return trustAllCerts;
    }

    private static X509TrustManager getX509TTrustManager() {

        return new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                };
    }
    //
    public static HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        return hostnameVerifier;
    }

    public long getMyExtensionId(){
        return  mExtensionId;
    }

    public void setAccount(@NonNull UserAccount mAccount) {
        this.mAccount = mAccount;
        accessTime = 0;
    }

    UserAccount mAccount;
    void refreshToken(){
        synchronized (TokenInterceptor.class) {
            if (accessTime==0||accessTime < System.currentTimeMillis()) {

                String verifyUrl = OkHttpHelper.getMessageUrl(SCHEAM_VERIFY) ;
                if(verifyUrl!=null) {
                    OkHttpClient AccessHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .sslSocketFactory(getSSLSocketFactory(),getX509TTrustManager())
                            .hostnameVerifier(getHostnameVerifier())
                            .readTimeout(20, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true).build();
                    try {
                        JSONObject requestBody = new JSONObject();
                        if (mAccount != null) {
                            String domain = mAccount.getDomain();
                            String number = mAccount.getAccountNumber();
                            String password = mAccount.getPassword();

                            requestBody.put(VERIFY_KEY_DOMAIN, domain);
                            requestBody.put(VERIFY_KEY_NAME, number);
                            requestBody.put(VERIFY_KEY_PWD, password);
                        }
                        HashMap<String, String> headParamsMap = new HashMap<>();
                        headParamsMap.put("Content-Type", "application/json");

                        StringBuilder tempParams = new StringBuilder();
                        int pos = 0;
                        for (String key : headParamsMap.keySet()) {
                            if (pos > 0) {
                                tempParams.append("&");
                            }
                            tempParams.append(String.format("%s=%s", key, URLEncoder.encode(headParamsMap.get(key), "utf-8")));
                            pos++;
                        }
                        //
                        String params = tempParams.toString();
                        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, requestBody.toString());
                        final Request request = addHeaders(headParamsMap).url(verifyUrl).post(body).addHeader("Connection", "close").build();
                        final Call call = AccessHttpClient.newCall(request);
                        Response response = call.execute();

                        if (response.isSuccessful()) {
                            String result = response.body().string();
                            try {
                                JSONObject resultJson = new JSONObject(result);
                                accessToken = resultJson.getString(OkHttpHelper.KEY_ACCESSTOKEN);
                                int duration = resultJson.getInt(OkHttpHelper.VERIFY_RESULT_KEY_EXPIRES);
                                mExtensionId = resultJson.getLong(OkHttpHelper.VERIFY_RESULT_KEY_EXTENSIONID);
                                accessTime = System.currentTimeMillis() + duration * 1000 - 500;
                            } catch (JSONException e) {

                            }
                        }
                    } catch (Exception e) {
                    }
                }else{

                }
            }else{
            }
        }
    }

    public class TokenInterceptor implements Interceptor {
        public final Charset UTF_8 = Charset.forName("UTF-8");

        @Override
        public Response intercept(Chain chain) throws IOException {
            refreshToken();
            if(TextUtils.isEmpty(accessToken)) {
                accessToken = UUID.randomUUID().toString();
            }
            Request request = chain.request().newBuilder()
                    .addHeader(KEY_ACCESSTOKEN, accessToken)
                    .build();
            Response originalResponse = chain.proceed(request);
            return originalResponse;
        }
    }

        /**
     *  okHttp同步请求统一入口
     * @param actionUrl  接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     */
    public void requestSyn(String actionUrl, int requestType, HashMap<String, String> head, HashMap<String, String> paramsMap) {
        switch (requestType) {
            case TYPE_GET:
                requestGetBySyn(actionUrl,head, paramsMap);
                break;
            case TYPE_POST_JSON:
                requestPostBySyn(actionUrl,head, paramsMap);
                break;
            case TYPE_POST_FORM:
                requestPostBySynWithForm(actionUrl,head, paramsMap);
                break;
        }
    }

    /**
     * okHttp get同步请求
     * @param actionUrl  接口地址
     * @param paramsMap   请求参数
     */
    private void requestGetBySyn(String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap) {
        StringBuilder tempParams = new StringBuilder();
        try {
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String requestUrl = String.format("%s?%s", actionUrl, tempParams.toString());
            Request request = addHeaders(headParamsMap).url(requestUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            calls.add(call);
            final Response response = call.execute();
            calls.remove(call);
            response.body().string();
        } catch (Exception e) {
        }
    }

    private void requestPostBySyn(String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap) {
        try {
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String params = tempParams.toString();
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            final Request request = addHeaders(headParamsMap).url(actionUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            Response response = call.execute();
            if (response.isSuccessful()) {

            }
        } catch (Exception e) {

        }
    }

    /**
     * okHttp post同步请求表单提交
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     */
    private void requestPostBySynWithForm(String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap) {
        try {
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                builder.add(key, paramsMap.get(key));
            }
            RequestBody formBody = builder.build();
            final Request request = addHeaders(headParamsMap).url(actionUrl).post(formBody).build();
            final Call call = mOkHttpClient.newCall(request);
            call.execute();
        } catch (Exception e) {
        }
    }

    /**
     * okHttp异步请求统一入口
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     * @return  本次请求的sessionID
     **/
    public <T> String requestAsyn(DownLoadParams action, String actionUrl, int requestType, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap, ReqCallBack callBack) {
        String call = null;
        switch (requestType) {
            case TYPE_GET:
                call = requestGetByAsyn(action,actionUrl,headParamsMap, paramsMap, callBack);
                break;
            case TYPE_POST_JSON:
                //call = requestPostByAsyn(action,actionUrl,headParamsMap, paramsMap, callBack);
                //call = requestPostByAsynWithJSON(action,actionUrl,headParamsMap, paramsMap, callBack);

                break;
            case TYPE_POST_FORM:
                call = requestPostByAsynWithForm(action,actionUrl,headParamsMap, paramsMap, callBack);
                break;
        }
        return call;
    }

    /**
     * okHttp get异步请求
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack 请求返回数据回调
     * @return
     */
    private <T> String requestGetByAsyn(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap, final ReqCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        StringBuilder tempParams = new StringBuilder();
        try {
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String requestUrl = String.format("%s?%s", actionUrl, tempParams.toString());
            final Request request = addHeaders(headParamsMap).url(requestUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),"access error", callBack);

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    calls.remove(call);
                    if (response.isSuccessful()) {
                        String string = response.body().string();

                        successCallBack(action,call.request(),string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"server error", callBack);
                    }
                }
            });
            return sessionId;
        } catch (Exception e) {
        }
        return sessionId;
    }

    /**
     * okHttp post异步请求
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack 请求返回数据回调
     * @param <T> 数据泛型
     * @return
     */
    private <T> String requestPostByAsyn(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap, final ReqCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        try {
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String params = tempParams.toString();
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            final Request request = addHeaders(headParamsMap).url(actionUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),e.toString(), callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    calls.remove(call);
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        successCallBack(action,call.request(),string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"server error", callBack);
                    }
                }
            });
            return sessionId;
        } catch (Exception e) {
        }
        return sessionId;
    }
    /**
     * okHttp post异步请求
     * @param actionUrl 接口地址
     * @param params 请求参数
     * @param callBack 请求返回数据回调
     * @param <T> 数据泛型
     * @return
     */
    public  <T> String requestPostByAsynWithJSON(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, JSONObject params, final ReqCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        try {
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params.toString());
            final Request request = addHeaders(headParamsMap).url(actionUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),e.toString(), callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    calls.remove(call);
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        successCallBack(action,call.request(),string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"server error", callBack);
                    }
                }
            });
            return sessionId;
        } catch (Exception e) {
        }
        return sessionId;
    }
    /**
     * okHttp post异步请求表单提交
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack 请求返回数据回调
     * @param <T> 数据泛型
     * @return
     */
    private <T> String requestPostByAsynWithForm(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, String> paramsMap, final ReqCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        try {
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                builder.add(key, paramsMap.get(key));
            }
            RequestBody formBody = builder.build();
            final Request request = addHeaders(headParamsMap).url(actionUrl).post(formBody).build();
            final Call call = mOkHttpClient.newCall(request);
            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),"access error", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    calls.remove(call);
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        successCallBack(action,call.request(), string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"server error", callBack);
                    }
                }
            });
            return sessionId;
        } catch (Exception e) {
        }
        return sessionId;
    }

    public interface ReqCallBack {

        void onReqSuccess(final DownLoadParams action, final Request requst, String result);

        void onProgress(final DownLoadParams action, final String filePath, long total, long current);
        void onReqFailed(final DownLoadParams action, final Request requst, String errorMsg);
    }

    /**
     * 统一为请求添加头信息
     * @return
     */
    private Request.Builder addHeaders(HashMap<String, String> headParamsMap) {
        Request.Builder builder = new Request.Builder()
                .addHeader("Connection", "keep-alive")
                .addHeader("platform", "2")
                .addHeader("phoneModel", Build.MODEL)
                .addHeader("systemVersion", Build.VERSION.RELEASE)
                .addHeader("appVersion", "3.2.0");
        if(headParamsMap!=null&&!headParamsMap.isEmpty()){
            for (String key : headParamsMap.keySet()) {
                String object = headParamsMap.get(key);
                builder.addHeader(key,object);
            }
        }
        return builder;
    }

    /**
     * 统一同意处理成功信息
     * @param result
     * @param callBack
     * @param <T>
     */
    private <T> void successCallBack(final DownLoadParams action, final Request requst, final String result, final ReqCallBack callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqSuccess(action,requst,result);
                }
            }
        });
    }

    /**
     * 统一处理失败信息
     * @param errorMsg
     * @param callBack
     */
    private void failedCallBack(final DownLoadParams action, final Request requst, final String errorMsg, final ReqCallBack callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqFailed(action,requst,errorMsg);
                }
            }
        });
    }
    /**
     * 上传文件
     * @param actionUrl 接口地址
     * @param filePath  本地文件地址
     */
    public <T> String upLoadFile(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, String filePath, final ReqCallBack callBack) {
        File file = new File(filePath);
        RequestBody body = RequestBody.create(MEDIA_TYPE_STREAM, file);

        Request.Builder requestBuilder =new Request.Builder().url(actionUrl).post(body);
        if(headParamsMap!=null&&headParamsMap.size()>0) {
            for (String key : headParamsMap.keySet()) {
                String object = headParamsMap.get(key);
                requestBuilder.addHeader(key,object);
            }
        }
        final Request request = requestBuilder.build();

        final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
        calls.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                calls.remove(call);
                failedCallBack(action,call.request(),"upload failed", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                calls.remove(call);
                if (response.isSuccessful()) {
                    String string = response.body().string();
                    successCallBack(action,call.request(),string, callBack);
                } else {
                    failedCallBack(action,call.request(),"upload failed", callBack);
                }
            }
        });
        return null;
    }

    //2.）带参数上传文件

    /**
     *上传文件
     * @param actionUrl 接口地址
     * @param paramsMap 参数
     * @param callBack 回调
     * @param
     */
    public String upLoadFile(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, Object> paramsMap, final ReqCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        try {
            String requestUrl = actionUrl;//String.format("%s/%s", upload_head, actionUrl);
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);
            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), createProgressRequestBody(action,MEDIA_TYPE_STREAM, file, callBack));
                }
            }
            RequestBody body = builder.build();
            Request.Builder requestBuilder =new Request.Builder().url(requestUrl).post(body);
            if(headParamsMap!=null&&headParamsMap.size()>0) {
                for (String key : headParamsMap.keySet()) {
                    String object = headParamsMap.get(key);
                    requestBuilder.addHeader(key,object);
                }
            }
            final Request request = requestBuilder.build();
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),"upload failed", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    calls.remove(call);
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        successCallBack(action,call.request(),string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"upload failed", callBack);
                    }
                }
            });
        } catch (Exception e) {
        }
        return sessionId;
    }

    //3.)带参数带进度上传文件

    /**
     *上传文件
     * @param actionUrl 接口地址
     * @param headParamsMap 请求头参数
     * @param paramsMap 表格键值对内容
     * @param callBack 回调
     * @param
     */
    public String upLoadFile(final DownLoadParams action, String actionUrl, HashMap<String, String> headParamsMap, HashMap<String, Object> paramsMap, final ReqProgressCallBack callBack) {
        String sessionId = UUID.randomUUID().toString();
        try {
            String requestUrl = actionUrl;//String.format("%s/%s", upload_head, actionUrl);
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);

            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), createProgressRequestBody(action,MEDIA_TYPE_STREAM, file, callBack));
                }
            }
            RequestBody body = builder.build();
            Request.Builder requestBuilder =new Request.Builder().url(requestUrl).post(body);
            if(headParamsMap!=null&&headParamsMap.size()>0) {
                for (String key : headParamsMap.keySet()) {
                    String object = headParamsMap.get(key);
                    requestBuilder.addHeader(key,object);
                }
            }
            final Request request = requestBuilder.build();
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);

            calls.add(call);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    calls.remove(call);
                    failedCallBack(action,call.request(),"upload failed", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        calls.remove(call);
                        String string = response.body().string();
                        successCallBack(action,call.request(),string, callBack);
                    } else {
                        failedCallBack(action,call.request(),"upload failed", callBack);
                    }
                }
            });
        } catch (Exception e) {
        }
        return sessionId;
    }

    /**
     * 创建带进度的RequestBody
     * @param contentType MediaType
     * @param file  准备上传的文件
     * @param callBack 回调
     * @param
     * @return
     */
    public RequestBody createProgressRequestBody(final DownLoadParams action, final MediaType contentType, final File file, final ReqCallBack callBack) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source;
                try {
                    source = Okio.source(file);
                    Buffer buf = new Buffer();
                    long remaining = contentLength();
                    long current = 0;
                    for (long readCount; (readCount = source.read(buf, FILE_BUFF_SIZE)) != -1; ) {
                        sink.write(buf, readCount);
                        current += readCount;
                        progressCallBack(action,file.getAbsolutePath(),remaining, current, callBack);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
    //5.)不带进度文件下载

    /**
     * 下载文件
     * @param fileUrl 文件url
     * @param destFileDir 存储目标目录
     * @return sessionId
     */
    public String downLoadFile(final DownLoadParams action, String fileUrl, final String destFileDir, final ReqCallBack callBack) {

        String fileName = action.getFileName();
        final File file;
        try {
            file = NgnStringUtils.getFileFromOriginal(new File(destFileDir, fileName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        calls.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                calls.remove(call);
                failedCallBack(action,call.request(),"download failed", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                calls.remove(call);
                InputStream is = null;
                byte[] buf = new byte[FILE_BUFF_SIZE];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    long total = response.body().contentLength();
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        progressCallBack(action,file.getAbsolutePath(),total,current, callBack);
                    }
                    fos.flush();
                    successCallBack(action,call.request(),file.getAbsolutePath(), callBack);
                } catch (IOException e) {
                    failedCallBack(action,call.request(),"download failed", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        });
        return fileName;
    }

    /**
     * 下载文件
     * @param fileUrl 文件url
     * @param destFileDir 存储目标目录
     */

    public String downLoadFile(final DownLoadParams action, String fileUrl, final String destFileDir, final ReqProgressCallBack callBack) {
        String fileName = action.getFileName();

        final File file = new File(destFileDir, fileName);
        if (file.exists()) {
            return fileName;
        }
        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        calls.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                calls.remove(call);
                failedCallBack(action,call.request(),"download failed", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                InputStream is = null;
                byte[] buf = new byte[FILE_BUFF_SIZE];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    long total = response.body().contentLength();

                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        progressCallBack(action,file.getName(),total, current, callBack);
                    }
                    fos.flush();
                    calls.remove(call);
                    successCallBack(action,call.request(),file.getAbsolutePath(), callBack);
                } catch (IOException e) {
                    failedCallBack(action,call.request(),"download failed", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                        calls.remove(call);
                    } catch (IOException e) {
                    }
                }
            }
        });
        return fileName;
    }

    public interface ReqProgressCallBack  extends ReqCallBack{
        /**
         * 响应进度更新
         */
        void onProgress(long total, long current);
    }

    /**
     * 统一处理进度信息
     * @param total    总计大小
     * @param current  当前进度
     * @param callBack
     */
    private  void progressCallBack(final DownLoadParams action, final String filePath, final long total, final long current, final ReqCallBack callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onProgress(action,filePath,total, current);
                }
            }
        });
    }

    public void cancellAll(){
        for(Call call:calls){
            call.cancel();
        }
        calls.clear();
    }
}
