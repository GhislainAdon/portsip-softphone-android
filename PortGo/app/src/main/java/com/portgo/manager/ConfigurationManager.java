package com.portgo.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.portgo.R;
import com.portgo.util.Codec;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipSdk;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigurationManager{
    final int SESSIONTIME = 190;

    static public final String PRESENCE_AGENT = "tewqt";
    static public final String PRESENCE_SUB_REFRESH= "wqet";
    static public final String PRESENCE_PUB_REFRESH= "yerye";
    static public final String PRESENCE_TLS_CERT= "qewgf";
    static public final String PRESENCE_USEWIFI="herb";
    static public final String PRESENCE_VOIP = "ofjsidf";
    static public final String PRESENCE_RTP_PORT_START = "foidsafoja";
    static public final String PRESENCE_AEC = "nvweqiw";
    static public final String PRESENCE_VAD = "fewfaf";
    static public final String PRESENCE_NR = "43vv43ew";
    static public final String PRESENCE_CNG = "afssafdf";
    static public final String PRESENCE_AGC = "sdgdsgs";
    static public final String PRESENCE_AUOT_START = "sgfdsgfd";

    static public final String PRESENCE_VIDEOREASOUTION ="eurowquroqw";
    static public final String PRESENCE_FPS="fsdafdsaf";
    static public final String PRESENCE_BITRAT="fdsfaf";

    static public final String PRESENCE_DTMF="h5h4h";
    static public final String PRESENCE_DTMF_BACK="li8kkiu";
    static public final String PRESENCE_VIDEO_NACK="kuikfy";
    static public final String PRESENCE_SRTP="tyuru";
    static public final String PRESENCE_AUDIO_CODE = "jfhgjghj";
    static public final String PRESENCE_VIDEO_CODE= "gfhdfh";
    static public final String PRESENCE_DEBUG = "ncnnc";
    static public final String INSTANCE_ID = "fdhgfh";
    static public final String PRESENCE_PRACK="treytry";
    static public final String PRESENCE_EARLYMEDIA="dsfhfa";
    static public final String PRESENCE_PST="fdhfdgh";
    static public final String PRESENCE_IMS="hfdghfd";
    static public final String PRESENCE_CALLING_RECORD="ytryrey";
    static public final String PRESENCE_RECORD_DIR="dfghdfh";

    static public final String PRESENCE_CALLRING="retewt";
    static public final String PRESENCE_IMRING="gfdsg";
    static public final String PRESENCE_ENABLE_RING="sdgdsg";
    static public final String PRESENCE_ENABLE_VIBRATE="sgfdty";
    static public final String PRESENCE_NATIVE_DIALER="ruoewr";
    static public final String PRESENCE_NATIVE_DIALER_ACTIVITY="fsdoif";

    static public final String PRESENCE_VIDEO_PATH="oewinvrqc";
    static public final String PRESENCE_FILE_PATH="dsfasdf";
    static public final String PRESENCE_FILE_DEFALUT_SUBPATH="/recvfiles";
    static public final String PRESENCE_VIDEOTHUMBNAIL_PATH="vncwueqorn";
    static public final String PRESENCE_ALTER_SET="0jgso943jr";

    private ConfigurationManager(){
    }
    private  static ConfigurationManager instatce = new ConfigurationManager();
    public  static ConfigurationManager getInstance(){
        return instatce;
    }
    
    public boolean getPremium() {
//        return getBooleanValue("load_finish",false);
        return  premium;
    }
    
    public void setPremium(boolean premiumed) {
//        setBooleanValue("load_finish",premiumed);
        premium =premiumed;
    }
    boolean premium = false;
    
    public synchronized boolean getBooleanValue(Context context,String key,boolean defaultValue){
        boolean result;
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        try {
            result = preferences.getBoolean(key, defaultValue);
        }catch (ClassCastException e){
            preferences.edit().putBoolean(key,defaultValue).commit();
            result = defaultValue;
        }

        return result;

    }
    
    public synchronized boolean setBooleanValue(Context context,String key,boolean value){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        value = preferences.edit().putBoolean(key,value).commit();

        return value;
    }
    
    public synchronized boolean setStringValue(Context context,String key,String value){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.edit().putString(key,value).commit();
    }



    public synchronized boolean setIntegerValue(Context context,String key,Integer value){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return preferences.edit().putInt(key,value).commit();
    }


    //keyId is a resource id reference to a string

    public synchronized boolean getBooleanValue(Context context,int keyId,boolean defaultValue){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);

        String key = context.getResources().getString(keyId);
        if(key !=null) {
            defaultValue = preferences.getBoolean(key, defaultValue);
        }
//        PortApplication.getLogUtils().d("getBooleanValue","value = "+defaultValue);
        return  defaultValue;
    }

    public synchronized int getIntergerValue(Context context,String key,int defaultValue){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        int result;
        try {
            result = preferences.getInt(key,defaultValue); // <== this generate exception -- why?
        }
        catch (ClassCastException e) {
            preferences.edit().putInt(key,defaultValue).commit();
            result = defaultValue;
        }
        return result;
    }
    
    public synchronized Set<String> getStringSet(Context context,String key){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        return  preferences.getStringSet(key,null);
    }
    
    /***
     *
     */
    public synchronized void putStringSet(Context context,String key, Set<String> values) {
        String value = "";
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(key,values);
        editor.commit();
    }


    public synchronized String getStringValue(Context context,String key,String defaultValue){
        String resutlt;
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        try {
            resutlt = preferences.getString(key,defaultValue);
        }catch (ClassCastException e){
            preferences.edit().putString(key,defaultValue).commit();
            resutlt  = defaultValue;
        }
        return resutlt;
    }

    public String getStringValue(Context context,int keyId,String defaultValue){
        String resutlt;
        String key = context.getResources().getString(keyId);
        if(key ==null)
            return defaultValue;
        resutlt = getStringValue(context,key,defaultValue);
        return  resutlt;
    }
    
    public int[] getIntArray(String key, int[] defaultValue) {
        return new int[0];
    }
    final long ALL_SESSION = -1;
    //if change the default value,we need modify media_set or prefrence_set asyn
    public void setMediaConfig(Context context,PortSipSdk sdk){
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
            Resources res = context.getResources();
            sdk.clearAudioCodec();

            int audioport = getIntergerValue(context,PRESENCE_RTP_PORT_START,
                    res.getInteger(R.integer.prefrence_rtpport_default));
            sdk.setRtpPortRange(audioport , audioport + 5000, audioport + 5002, audioport + 10002);

            addAVCodec(context,sdk);

            sdk.enableAEC(getBooleanValue(context,PRESENCE_AEC, res.getBoolean(R.bool.prefrence_aec_default)));
            sdk.enableVAD(getBooleanValue(context,PRESENCE_VAD, res.getBoolean(R.bool.prefrence_vad_default)));
            sdk.enableANS(getBooleanValue(context,PRESENCE_NR, res.getBoolean(R.bool.prefrence_nr_default)));
            sdk.enableCNG(getBooleanValue(context,PRESENCE_CNG, res.getBoolean(R.bool.prefrence_cng_default)));
            sdk.enableAGC(getBooleanValue(context,PRESENCE_AGC, res.getBoolean(R.bool.prefrence_agc_default)));

            sdk.setVideoNackStatus(true);

            boolean prack = getBooleanValue(context,PRESENCE_PRACK,res.getBoolean(R.bool.prefrence_prack_default));
            boolean ims =getBooleanValue(context,PRESENCE_IMS,res.getBoolean(R.bool.prefrence_ims_default));
            boolean pst = getBooleanValue(context,PRESENCE_PST,res.getBoolean(R.bool.prefrence_pst_default));
            boolean record = getBooleanValue(context,PRESENCE_CALLING_RECORD,res.getBoolean(R.bool.prefrence_record_default));
            boolean earlymedia = getBooleanValue(context,PRESENCE_EARLYMEDIA,res.getBoolean(R.bool.prefrence_earlymedia_default));

            if(pst) {
                sdk.enableSessionTimer(SESSIONTIME);
            }else{
                sdk.disableSessionTimer();
            }

            sdk.enableReliableProvisional(prack);
            sdk.enable3GppTags(ims);
            sdk.enableEarlyMedia(earlymedia);
            int select;
            int[] selector;

            select = res.getInteger(R.integer.prefrence_video_bitrate_default);
            select = getIntergerValue(context,PRESENCE_BITRAT,select);
            selector = res.getIntArray(R.array.videobits_value);
            sdk.setVideoBitrate(ALL_SESSION,selector[select]);

            select = res.getInteger(R.integer.prefrence_video_fps_default);//
            select = getIntergerValue(context,PRESENCE_FPS,select);//
            selector = res.getIntArray(R.array.videofps_value);//
            sdk.setVideoFrameRate(ALL_SESSION,selector[select]);

            select = res.getInteger(R.integer.prefrence_video_resolution_default);
            select = getIntergerValue(context,PRESENCE_VIDEOREASOUTION,select);
            selector = res.getIntArray(R.array.videoresolution_value);
            int width,height,value = selector[select];
            height = value&0x0000FFFF;
            width = (value&0xFFFF0000)>>16;
            sdk.setVideoResolution(width,height);

            int netSrtpPolicy = getIntergerValue(context,PRESENCE_SRTP,
                    context.getResources().getInteger(R.integer.prefrence_srtp_default));
            sdk.setSrtpPolicy(netSrtpPolicy);

        }

        private void addAVCodec(Context context,PortSipSdk sdk){
            SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
            Resources res = context.getResources();
            List<Codec> audioCodecs = null;
            List<Codec> videoCodecs = null;
            Set<String> audiokeys = getStringSet(context,PRESENCE_AUDIO_CODE);
            Set<String> videokeys = getStringSet(context,PRESENCE_VIDEO_CODE);
            if(audiokeys!=null&&audiokeys.size()>0){
                audioCodecs = Codec.getCodecList(audiokeys);
            }else {
                audioCodecs = Codec.getCodecList(res.getStringArray(R.array.audio_codecs));
            }
            if(videokeys!=null&&videokeys.size()>0){
                videoCodecs = Codec.getCodecList(videokeys);
            }else
            {
                videoCodecs = Codec.getCodecList(res.getStringArray(R.array.video_codecs));
            }

            Collections.sort(audioCodecs, null); //
            Collections.sort(videoCodecs, null); //

            sdk.clearAudioCodec();
            boolean premium = getPremium();
            for(Codec audio:audioCodecs){
                audio.setPreminumed(premium);
                if(audio!=null&&audio.isEnable()){
//                    int codeid = audio.getCodecId();
//                    if(premium||codeid!=PortSipEnumDefine.ENUM_AUDIOCODEC_AMRWB) {
                    sdk.addAudioCodec(audio.getCodecId());
//                    }

                }
            }

            sdk.clearVideoCodec();
            for(Codec video:videoCodecs){
                video.setPreminumed(premium);
                if(video!=null&&video.isEnable()){
//                    int codeid = video.getCodecId();
//                    if(premium||codeid!=PortSipEnumDefine.ENUM_VIDEOCODEC_H264) {
                        sdk.addVideoCodec(video.getCodecId());
//                    }
                }
            }
        }

    public void updateConfig(Context context,PortSipSdk sdk,String key ) {
        SharedPreferences preferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        Resources res =context.getResources();
        int select;
        int[] selector;
        switch (key){
            case PRESENCE_VOIP:
                break;
//                sdk.disableCallForward()
            case PRESENCE_RTP_PORT_START:
                int audioport = getIntergerValue(context,PRESENCE_RTP_PORT_START,10000);
                sdk.setRtpPortRange(audioport , audioport + 5000, audioport + 5002, audioport + 10002);
                break;
            case PRESENCE_AEC:
                sdk.enableAEC(getBooleanValue(context,PRESENCE_AEC, res.getBoolean(R.bool.prefrence_aec_default)));
                break;
            case  PRESENCE_VAD://PRESENCE_VAD
                sdk.enableVAD(getBooleanValue(context,PRESENCE_VAD, res.getBoolean(R.bool.prefrence_vad_default)));
                break;
            case PRESENCE_NR://PRESENCE_NR
                sdk.enableANS(getBooleanValue(context,PRESENCE_NR, res.getBoolean(R.bool.prefrence_nr_default)));
                break;
            case PRESENCE_CNG:
                sdk.enableCNG(getBooleanValue(context,PRESENCE_CNG, res.getBoolean(R.bool.prefrence_cng_default)));
                break;
            case PRESENCE_AGC:
                sdk.enableAGC(getBooleanValue(context,PRESENCE_AGC, res.getBoolean(R.bool.prefrence_agc_default)));
                break;
            case PRESENCE_VIDEOREASOUTION:
                select = res.getInteger(R.integer.prefrence_video_resolution_default);
                select = getIntergerValue(context,PRESENCE_VIDEOREASOUTION,select);
                selector = res.getIntArray(R.array.videoresolution_value);
                int width,height,resolution = selector[select];
                height = resolution&0x0000FFFF;
                width = (resolution&0xFFFF0000)>>16;
                sdk.setVideoResolution(width,height);

                break;
            case PRESENCE_FPS:
                select = res.getInteger(R.integer.prefrence_video_fps_default);//
                select = getIntergerValue(context,PRESENCE_FPS,select);//
                selector = res.getIntArray(R.array.videofps_value);//
                sdk.setVideoFrameRate(ALL_SESSION,selector[select]);
                break;
            case PRESENCE_BITRAT:
                select = res.getInteger(R.integer.prefrence_video_bitrate_default);
                select = getIntergerValue(context,PRESENCE_BITRAT,select);
                selector = res.getIntArray(R.array.videobits_value);
                sdk.setVideoBitrate(ALL_SESSION,selector[select]);
                break;
            case PRESENCE_SRTP:
                sdk.setSrtpPolicy(getIntergerValue(context,PRESENCE_SRTP,context.getResources().getInteger(R.integer.prefrence_srtp_default)));
                break;
            case PRESENCE_DTMF:
                setMediaConfig(context,sdk);
                break;
            case PRESENCE_DTMF_BACK:
                break;
        }
    }

}
