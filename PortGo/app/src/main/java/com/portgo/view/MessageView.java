package com.portgo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.portgo.PortApplication;
import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.MIMEType;
import com.portgo.util.MatrixUtils;
import com.portgo.util.NgnStringUtils;
import com.portgo.util.UnitConversion;
import com.portgo.util.VideoThumbnailUtils;

import java.io.File;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;


/**
 * Created by huacai on 2017/4/26.
 */

public class MessageView extends LinearLayout{

    public MessageView(Context context) {
        super(context);
    }

    public MessageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    static public class Builder{
        public static final int SHOW_FIRST =1;
        public static final int SHOW_MIDDLE =2;
        public static final int SHOW_END =3;
        final Context mContext;
        private boolean mLocal;
        private boolean success;
        private boolean mAddgap;
        private boolean mShowAvartar;
        private String mMessage;
        private RoundedImageView imageView;
        private Drawable drawable;
        private int mPosition = SHOW_FIRST;
        private String msgTime ="";
        private String disName ="";
        private boolean read=true;
        private String type =MIMEType.MIMETYPE_textplain;
        public Builder(Context context) {
            mContext =context;
        }
        private float messageDuration=1;
        public void setMessageType(String mime){
            type = mime;
        }
        boolean mPlaying=false;
        public void setAudioPlayStatus(boolean playing){
            mPlaying = playing;
        }
        public void setMessageReadstatus(boolean readstatus){
            read = readstatus;
        }
        public Builder setAvatar(Bitmap bmp){
            if(bmp!=null) {
                drawable = new BitmapDrawable(bmp);
            }
            else {
                drawable = null;
            }
            return this;
        }

        public Builder setAvatar(@DrawableRes int avatarResId){
            drawable = mContext.getResources().getDrawable(avatarResId);
            return this;
        }

        public Builder setAvatar(Drawable drawable){
            this.drawable = drawable;
            return this;
        }

        public Builder setMessageDuration(float duration){
            this.messageDuration = duration;
            return this;
        }

        public Builder setSendSuccess(boolean sendSuccess){
            this.success = sendSuccess;
            return this;
        }

        public void setAddGap(boolean gap){
            mAddgap =gap;
        }
        public Builder setShowAvatar(boolean show){
            mShowAvartar = show;
            return this;
        }

        public Builder setLocal(boolean local){
            mLocal =local;
            return this;
        }

        public Builder setMessage(String message) {
            this.mMessage = message;
            return this;
        }
        public Builder setMessageTime(String time) {
            this.msgTime = time;
            return this;
        }
        public Builder setDisName(String disName) {
            this.disName = disName;
            if(disName!=null){
                if(disName.length()>2){
                    disName=disName.substring(0,2);
                }
            }
            return this;
        }

        public Builder setMessagePositon(int positon) {
            this.mPosition = positon;
            return this;
        }

        public MessageView build(){
            MessageView msg = new MessageView(mContext);
            LinearLayout layout;
            LinearLayout contentView;
            msg.setOrientation(VERTICAL);
            msg.setGravity(Gravity.CENTER_HORIZONTAL);

            layout = (LinearLayout) LayoutInflater.from(mContext).inflate(mLocal?R.layout.view_message_sent:R.layout.view_message_received, msg);

            contentView = buildContentView();
            LinearLayout messege = (LinearLayout) layout.findViewById(R.id.message_content);
            FrameLayout avartar = (FrameLayout) layout.findViewById(R.id.message_avatar);
            TextView time = (TextView) layout.findViewById(R.id.message_time);

            if(!NgnStringUtils.isNullOrEmpty(msgTime)){
                time.setText(msgTime);
                time.setVisibility(VISIBLE);
            }
            messege.addView(contentView);
            if(mShowAvartar) {
                RoundedImageView imgAvatar = (RoundedImageView) avartar.findViewById(R.id.user_avatar_image);
                TextView textAvatar = (TextView) avartar.findViewById(R.id.user_avatar_text);
                if(drawable==null) {
                    imgAvatar.setVisibility(GONE);
                    textAvatar.setVisibility(VISIBLE);
                    textAvatar.setText(NgnStringUtils.getAvatarText(disName));
                }else{
                    imgAvatar.setImageDrawable(drawable);
                    imgAvatar.setVisibility(VISIBLE);
                    textAvatar.setVisibility(GONE);
                }
                avartar.setVisibility(VISIBLE);
            }else {
                if(avartar!=null)
                    avartar.setVisibility(INVISIBLE);
            }
            if(mLocal) {

                if (success) {
                    layout.findViewById(R.id.message_failed).setVisibility(INVISIBLE);
                } else {
                    layout.findViewById(R.id.message_failed).setVisibility(VISIBLE);
                }
            }else {
                if(type.startsWith("audio")) {
                    layout.findViewById(R.id.audio_message_read).setVisibility(read ? INVISIBLE : VISIBLE);
                }
            }

            if(mAddgap&&time.getVisibility()!=VISIBLE){
                msg.findViewById(R.id.message_gap).setVisibility(INVISIBLE);
            }
            return msg;
        }

        int minwidth = 200;
        int audiominwidth = 60;
        int minheight = 200;
        private LinearLayout buildContentView(){
            LinearLayout content = new LinearLayout(mContext);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(mLocal?Gravity.RIGHT:Gravity.LEFT);//本地?靠右:/远端，靠左
//            content.setId(R.id.message_content);

            LinearLayout.LayoutParams lParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                ,ViewGroup.LayoutParams.WRAP_CONTENT);

            int resBeging,resMid,resEnd;

            TextView tv;

            View contentWithfailed = null;
            if (mLocal) {
                resBeging = R.drawable.message_recive_begin_bk;
                resMid = R.drawable.message_recive_middle_bk;
                resEnd = R.drawable.message_recive_end_bk;
            } else {
                resBeging = R.drawable.message_send_begin_bk;
                resMid = R.drawable.message_send_middle_bk;
                resEnd = R.drawable.message_send_end_bk;

            }

            switch (mPosition) {
                case SHOW_FIRST:
                    content.setBackgroundResource(resBeging);//begin
                    break;
                case SHOW_END:
                    content.setBackgroundResource(resEnd);//end
                    break;
                case SHOW_MIDDLE:
                    content.setBackgroundResource(resMid);//mid
                    break;

            }
            if(MIMEType.MIMETYPE_textplain.equals(type)){//
                contentWithfailed = LayoutInflater.from(mContext).inflate(mLocal?R.layout.view_message_textview_sent:R.layout.view_message_textview_received, null);
                tv = (TextView) contentWithfailed;

                mMessage = replaceAllControlCharRow(mMessage);
                tv.setText(mMessage);

            }else {
                String videoPath = ConfigurationManager.getInstance().getStringValue(mContext, ConfigurationManager.PRESENCE_VIDEO_PATH, mContext.getExternalFilesDir(null).getAbsolutePath());
                String thumbnailPath =videoPath+ File.separator+"thumbnails";
                String filePath ="",fileName = "";

                int index = 0;
                if(!TextUtils.isEmpty(mMessage)){
                    index = mMessage.lastIndexOf("/");
                }
                if(index>0&&index<mMessage.length()){
                    filePath = mMessage.substring(0,index);
                    fileName = mMessage.substring(index+1);
                }
                //
                if (type.startsWith(MIMEType.MIMETYPE_audio)) {
                    float length = audiominwidth+(messageDuration/30)*300;
                    final float scale = mContext.getResources().getDisplayMetrics().density;

                    contentWithfailed = LayoutInflater.from(mContext).inflate(mLocal ? R.layout.view_message_audioview_sent : R.layout.view_message_audioview_received, null);
                    ImageView audioPlay =  contentWithfailed.findViewById(mLocal ? R.id.message_audio_send_content: R.id.message_audio_rev_content);
                    AnimationDrawable anim = (AnimationDrawable)audioPlay.getDrawable();
                    if(mPlaying){
                        anim.start();
                    }else{
                        anim.stop();
                    }
                    length = (length * scale + 0.5f);
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) audioPlay.getLayoutParams();
                    layoutParams.width = (int) length;

                    audioPlay.setLayoutParams(layoutParams);

                } else if (type.startsWith(MIMEType.MIMETYPE_video)){
                    ImageView imageView;
                    contentWithfailed = LayoutInflater.from(mContext).inflate(mLocal ? R.layout.view_message_videoview_sent : R.layout.view_message_videoview_received, null);
                    imageView = (ImageView) contentWithfailed.findViewById(mLocal ? R.id.message_video_sent : R.id.message_video_received);
                    View player = contentWithfailed.findViewById(mLocal ? R.id.message_video_sent_play : R.id.message_video_receivedplay);
                    Bitmap bitmap =null;

                    bitmap = VideoThumbnailUtils.getVideoThumbnail(filePath, thumbnailPath, VideoThumbnailUtils.width, VideoThumbnailUtils.height);//

                    if (bitmap == null) {
                        imageView.setImageResource(R.drawable.logo);
                    } else {
                        int height = bitmap.getHeight();
                        int width = bitmap.getWidth();

                        if (bitmap.getHeight() < minheight && bitmap.getWidth() < minwidth) {
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
                            float hRate = height / (float) minheight;
                            float wRate = width / (float) minwidth;
                            if (wRate < hRate) {
                                layoutParams.width = (int) (width / hRate);
                                layoutParams.height = minheight;
                            } else {
                                layoutParams.width = minwidth;
                                layoutParams.height = (int) (height / wRate);
                            }
                            imageView.setLayoutParams(layoutParams);
                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        }

                        imageView.setImageBitmap(bitmap);
                    }

                    player.setVisibility(VISIBLE);

                } else if (type.startsWith(MIMEType.MIMETYPE_image)) {
                    contentWithfailed = LayoutInflater.from(mContext).inflate(mLocal ? R.layout.view_message_videoview_sent : R.layout.view_message_videoview_received,
                            null);
                    ImageView imageView = (ImageView) contentWithfailed.findViewById(mLocal ? R.id.message_video_sent : R.id.message_video_received);
                    View player = contentWithfailed.findViewById(mLocal ? R.id.message_video_sent_play : R.id.message_video_receivedplay);


                    Bitmap bitmap = VideoThumbnailUtils.getBitmapThumbnail(filePath, thumbnailPath, fileName, VideoThumbnailUtils.width, VideoThumbnailUtils.height);
                    if (bitmap == null) {
                        imageView.setImageResource(R.drawable.logo);
                    } else {
                        int height = bitmap.getHeight();
                        int width = bitmap.getWidth();
                        if (bitmap.getHeight() < minheight && bitmap.getWidth() < minwidth) {
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
                            float hRate = height / (float) minheight;
                            float wRate = width / (float) minwidth;
                            if (wRate < hRate) {
                                layoutParams.width = (int) (width / hRate);
                                layoutParams.height = minheight;
                            } else {
                                layoutParams.width = minwidth;
                                layoutParams.height = (int) (height / wRate);
                            }
                            imageView.setLayoutParams(layoutParams);
                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        }
                        imageView.setImageBitmap(bitmap);
                    }
                    player.setVisibility(INVISIBLE);

                } else {
                    contentWithfailed = LayoutInflater.from(mContext).inflate(mLocal ? R.layout.view_message_textview_sent : R.layout.view_message_textview_received, null);
                    tv = (TextView) contentWithfailed;

                    tv.setText(mContext.getResources().getText(R.string.unknow_message_format));

                }

            }

            content.addView(contentWithfailed,lParams);
            return content;
        }
    }

    static  private  String replaceAllControlCharRow(String str){
        if(NgnStringUtils.isNullOrEmpty(str))
            return str;
        return str.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "");
    }
}
