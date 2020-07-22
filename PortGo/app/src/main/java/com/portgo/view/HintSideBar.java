package com.portgo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.portgo.R;

public class HintSideBar extends RelativeLayout implements ContactSideBar.OnChooseLetterChangedListener {

    private TextView tv_hint;

    private ContactSideBar.OnChooseLetterChangedListener onChooseLetterChangedListener;

    public HintSideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_contact_sidebar, this);
        initView();
    }

    private void initView() {
        ContactSideBar ContactSideBar = (ContactSideBar) findViewById(R.id.contact_side_bar);
        tv_hint = (TextView) findViewById(R.id.contact_first_letter);
        ContactSideBar.setOnTouchingLetterChangedListener(this);
    }

    @Override
    public void onChooseLetter(String s) {
        tv_hint.setText(s);
        tv_hint.setVisibility(VISIBLE);
        if (onChooseLetterChangedListener != null) {
            onChooseLetterChangedListener.onChooseLetter(s);
        }
    }

    @Override
    public void onNoChooseLetter() {
        tv_hint.setVisibility(GONE);
        if (onChooseLetterChangedListener != null) {
            onChooseLetterChangedListener.onNoChooseLetter();
        }
    }

    public void setOnChooseLetterChangedListener(ContactSideBar.OnChooseLetterChangedListener onChooseLetterChangedListener) {
        this.onChooseLetterChangedListener = onChooseLetterChangedListener;
    }

}
