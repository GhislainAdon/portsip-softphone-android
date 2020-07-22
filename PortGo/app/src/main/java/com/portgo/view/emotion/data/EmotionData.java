package com.portgo.view.emotion.data;

import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2015/11/14.
 */
public class EmotionData{

    public String getStickerIcon() {
        return stickerIcon;
    }

    public enum EmotionCategory {
        emoji, image
    }

    private List<Emoticon> emotionList;
    private String stickerIcon;
    private EmotionCategory category;
    private int row;
    private int column;
    private Emoticon uniqueItem;

    /**
     * EmotionView所展示的数据结构
     * @param emotionList emotionView中显示的图片资源或路径
     * @param stickerIcon 在emotionView下发显示的该表情组的icon
     * @param category emotionView的类别，现在暂时有 emoji 和 image 两种
     * @param row 需要显示的行
     * @param column 需要显示的列
     */
    public EmotionData(List<Emoticon> emotionList, String stickerIcon, EmotionCategory category, int row, int column) {
        this.emotionList = emotionList;
        this.stickerIcon = stickerIcon;
        this.category = category;
        this.row = row;
        this.column = column;
    }

    /**
     * EmotionView所展示的数据结构
     * @param emotionList emotionView中显示的图片资源或路径
     * @param stickerIcon 在emotionView下发显示的该表情组的icon
     * @param category emotionView的类别，现在暂时有 emoji 和 image 两种
     * @param uniqueItem 在这组表情中特有的表情 EmotionAdapter对应为 删除，CustomAdapter对应为添加
     * @param row 需要显示的行
     * @param column 需要显示的列
     */
    public EmotionData(List<Emoticon> emotionList, String stickerIcon, EmotionCategory category, Emoticon uniqueItem, int row, int column) {
        this(emotionList, stickerIcon, category, row, column);
        this.uniqueItem = uniqueItem;
    }

    public Emoticon getUniqueItem() {
        return uniqueItem;
    }

    public List<Emoticon> getEmotionList() {
        return emotionList;
    }

    public void setEmotionList(List<Emoticon> emotionList) {
        this.emotionList = emotionList;
    }

    public EmotionCategory getCategory() {
        return category;
    }
    SpannableString getSpanelText(SpannableString string,Resources res, int textsize){
        SpannableStringBuilder builder = new SpannableStringBuilder(string);
        if(emotionList!=null) {
            for (Emoticon emoticon : emotionList) {
                int index = 0;
                while(index!=-1) {
                    index = string.toString().indexOf(emoticon.getDesc(), index);
                    if (index != -1) {
                        SpannableString temp = emoticon.getSpanelText(res, textsize);
                        int spanEnd = index+emoticon.getDesc().length();
                        builder.replace(index, spanEnd, temp);
                        index = spanEnd;
                    }
                }
            }
        }
        return new SpannableString(builder);
//        return builder.;
    }

    SpannableString getSpanelText(String string, TextView tv){
        SpannableStringBuilder builder = new SpannableStringBuilder(string);
        if(emotionList!=null) {
            for (Emoticon emoticon : emotionList) {
                int index = 0;
                while(index!=-1) {
                    index = string.indexOf(emoticon.getDesc(), index);
                    if (index != -1) {
                        SpannableString temp = emoticon.getSpanelText(tv);
                        int spanEnd = index+emoticon.getDesc().length();
                        builder.replace(index, spanEnd, temp);
                        index = spanEnd;

                    }
                }
            }
        }
        return new SpannableString(builder);
//        return builder.;
    }
    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
