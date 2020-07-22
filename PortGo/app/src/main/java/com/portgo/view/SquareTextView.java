package com.portgo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;

import com.portgo.R;

/**
 * Created by huacai on 2017/4/26.
 */

public class SquareTextView extends androidx.appcompat.widget.AppCompatTextView{
    String number="",letters="";
    private int keyNumberColor,keyLetterColor;
    float numberTextSize =20,lettersTextSize=12;


    public SquareTextView(Context context) {
        super(context);
    }

    public SquareTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet paramAttributeSet, int paramInt)
    {
        TypedArray localTypedArray = getContext().obtainStyledAttributes(paramAttributeSet, R.styleable.DialPadViewStyle, paramInt, 0);
        number = localTypedArray.getString(R.styleable.DialPadViewStyle_keyNumber);
        letters = localTypedArray.getString(R.styleable.DialPadViewStyle_keyLetters);
        numberTextSize = localTypedArray.getDimension(R.styleable.DialPadViewStyle_keyTextSize,numberTextSize);
        lettersTextSize = localTypedArray.getDimension(R.styleable.DialPadViewStyle_letterTextSize,lettersTextSize);
        keyNumberColor = localTypedArray.getColor(R.styleable.DialPadViewStyle_keyNumberColor,Color.BLACK);
        keyLetterColor = localTypedArray.getColor(R.styleable.DialPadViewStyle_keyLetterColor,Color.LTGRAY);


        localTypedArray.recycle();
        Spannable span = new SpannableString("");
        if(letters==null)
            letters ="";
        if(number==null)
            number="";

        setTextSize(numberTextSize);
        if(number.length()>0&&letters.length()>0) {
            String text = number+letters;
            span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(keyNumberColor), 0, number.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(keyNumberColor), 0, number.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new RelativeSizeSpan( lettersTextSize/numberTextSize), number.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(keyLetterColor), number.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }else{
            if(number.length()>0) {
                String text = number;
                span = new SpannableString(text);
//                span.setSpan(new RelativeSizeSpan(1.2F), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(keyNumberColor), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if(letters.length()>0){
                String text = letters;
                span = new SpannableString(text);
                span.setSpan(new RelativeSizeSpan(lettersTextSize/numberTextSize),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(keyLetterColor), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        setText(span);
    }
}
