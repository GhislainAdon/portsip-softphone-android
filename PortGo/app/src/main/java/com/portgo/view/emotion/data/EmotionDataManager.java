package com.portgo.view.emotion.data;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.portgo.R;
import com.portgo.util.PListHelper;
import com.portgo.util.UnitConversion;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by Administrator on 2015/11/14.
 */
public class EmotionDataManager {
    private static EmotionDataManager instance = new EmotionDataManager();
    private EmotionDataManager(){}

    public static EmotionDataManager getInstance() {
        return instance;
    }
    List<EmotionData> emotionList =null;

    //typedef "drawable" "mipmap" ...
    int getResourceIdByName(@NonNull Context context,@NonNull String typedef, @NonNull String name){
        return context.getResources().getIdentifier(name,typedef,context.getPackageName());
    }
    public void loadEmotion(Context context){
        if(emotionList == null) {
            emotionList = new ArrayList<>();
            InputStream inputStream = context.getResources().openRawResource(R.raw.innerstickersinfo);
            List<Object> emotions = PListHelper.readXmlBySAX(inputStream);
            Object ob=emotions.get(1);
//            for(Object ob:emotions){
                if(ob instanceof HashMap){
                    Object emojis = ((HashMap)ob).get("emoticons");
                    Object unicode = ((HashMap)ob).get("unicode_type");
                    Object coverPic = ((HashMap)ob).get("cover_pic");
                    if(coverPic instanceof String
                        &&unicode instanceof Boolean
                        && emojis instanceof ArrayList){
                        List<Emoticon> emojiList = new ArrayList<>();
                        List<Object> tempemoj = (List<Object>) emojis;
                        for (int i = 0; i < tempemoj.size(); ++i) {
                            Object emojiHash = tempemoj.get(i);
                            Object image="",desc="";

                            if(emojiHash instanceof HashMap){
                                image =  ((HashMap) emojiHash).get("image");
                                desc = ((HashMap) emojiHash).get("desc");
                            }
                            if(!TextUtils.isEmpty((String)image)){
                                image = ((String) image).toLowerCase();
                                image = ((String) image).replaceAll("[^a-z0-9_]","");

                                Emoticon emoji;
                                if((Boolean)unicode){
                                    emoji = new Emoji(context,getResourceIdByName(context,"raw",((String)image)),((String)desc));
                                }else{
                                    emoji = new Emoji(context,getResourceIdByName(context,"raw",((String)image)),((String)desc));
                                }

                                emojiList.add(emoji);
                            }

                        }

                        String regCover = ((String)coverPic).toLowerCase();
                        regCover = regCover.replaceAll("[^a-z0-9_]","");

                        EmotionData data = new EmotionData(emojiList,
                            UnitConversion.getResourceUriString(context,getResourceIdByName(context,"raw",((String)regCover))),
                            EmotionData.EmotionCategory.emoji,
                            new UniqueEmoji(R.drawable.bx_emotion_delete), 3, 7);
                        emotionList.add(data);
                    }
//                }
//                break;
            }
        }
    }
    public SpannableString getSpanelText(String string, TextView tv){
        Resources res = tv.getContext().getResources();
        int textsize = (int)tv.getTextSize();
        SpannableString spannableString= new SpannableString(string);
        if(emotionList==null)
            return spannableString;
        for(EmotionData emotionData:emotionList){
            spannableString =emotionData.getSpanelText(spannableString,res,textsize);
        }
        return spannableString;
    }
    public SpannableString getSpanelText(String string,Resources res, int textsize){
        SpannableString spannableString= new SpannableString(string);
        if(emotionList==null)
            return spannableString;
        for(EmotionData emotionData:emotionList){
            spannableString =emotionData.getSpanelText(spannableString,res,textsize);
        }
        return spannableString;
    }
    public void unloadEmotion(){
        if(emotionList!=null) {
            emotionList.clear();
            emotionList = null;
        }
    }

    public List<EmotionData> getEmotionList(){
        return emotionList;
    }
}
