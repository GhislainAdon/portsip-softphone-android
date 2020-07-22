package com.portgo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.portgo.R;

/**
 * Created by huacai on 2017/4/26.
 */

public class DialPadView extends LinearLayout {
    String number="",letters="";
    private Drawable keyNumberBackground = null,innerBackground =null;
    float numberTextSize,lettersTextSize;
    LinearLayout layout;
    public DialPadView(Context context) {
        super(context);
    }

    public DialPadView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialPadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
        LayoutInflater mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.activity_main_numpad_fragment_dial, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        TextView view = (TextView) findViewById(R.id.key_number);
        layout = (LinearLayout) findViewById(R.id.key_background);
        if(number!=null) {
            view.setText(number);
        }
        if(numberTextSize>0) {
            view.setTextSize(numberTextSize);
        }
        if(keyNumberBackground!=null){
            view.setBackground(keyNumberBackground);
        }
        view = (TextView) findViewById(R.id.key_letters);
        if(view!=null)
            view.setText(letters);
        if(numberTextSize>0)
            view.setTextSize(numberTextSize);

        if(innerBackground!=null)
            layout.setBackground(innerBackground);
    }

    private void init(AttributeSet paramAttributeSet, int paramInt)
    {
        TypedArray localTypedArray = getContext().obtainStyledAttributes(paramAttributeSet, R.styleable.DialPadViewStyle, paramInt, 0);
        number = localTypedArray.getString(R.styleable.DialPadViewStyle_keyNumber);
        numberTextSize = localTypedArray.getDimension(R.styleable.DialPadViewStyle_keyNumberTextSize,0);
        letters = localTypedArray.getString(R.styleable.DialPadViewStyle_keyLetters);
        lettersTextSize = localTypedArray.getDimension(R.styleable.DialPadViewStyle_keyNumberTextSize,0);
        keyNumberBackground = localTypedArray.getDrawable(R.styleable.DialPadViewStyle_keyNumberBackground);
        innerBackground = localTypedArray.getDrawable(R.styleable.DialPadViewStyle_innerBackground);
        localTypedArray.recycle();
    }
}
