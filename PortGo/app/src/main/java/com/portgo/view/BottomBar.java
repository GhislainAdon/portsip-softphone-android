package com.portgo.view;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.ui.RecordedActivity;
import com.portgo.util.CropTakePictrueUtils;
import com.portgo.util.UnitConversion;
import com.portgo.view.emotion.EmotionView;
import com.portgo.view.emotion.data.CustomEmoji;
import com.portgo.view.emotion.data.Emoji;
import com.portgo.view.emotion.data.Emoticon;
import com.portgo.view.emotion.data.EmotionData;
import com.portgo.view.emotion.data.UniqueEmoji;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/11/11.
 */
public class BottomBar extends Fragment {

    //this will be called at first, so we must make initial data there to avoid initial data being reset. !!!
    public BottomBar() {

    }
    RelativeLayout rootView;
    TextView bottombar_left,bottombar_right;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootView = (RelativeLayout) inflater.inflate(R.layout.view_bottombar, container, false);
        bottombar_left = rootView.findViewById(R.id.bottombar_left);
        bottombar_right = rootView.findViewById(R.id.bottombar_right);
        bottombar_left.setOnClickListener(mOnClicklinstner);
        bottombar_right.setOnClickListener(mOnClicklinstner);
        if(!TextUtils.isEmpty(mLeftTitle)) {
            bottombar_left.setText(mLeftTitle);
        }
        return rootView;
    }

    private  View.OnClickListener mOnClicklinstner;
    private  String mLeftTitle,mRightTitle;
    public  void setOnClicklinstner(View.OnClickListener onClicklinstner){
        mOnClicklinstner = onClicklinstner;
        if(bottombar_left!=null&&bottombar_right!=null) {
            bottombar_left.setOnClickListener(onClicklinstner);
            bottombar_right.setOnClickListener(onClicklinstner);
        }
    }
    public void setBottombarLeftText(String text){
        mLeftTitle = text;
        if(bottombar_left!=null) {
            bottombar_left.setText(text);
        }
    }
}
