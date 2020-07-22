package com.portgo.view;


import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.AttributeSet;


/**
 * Created by huacai on 2017/4/26.
 */

public class CursorEndEditTextView extends AppCompatEditText {

    public CursorEndEditTextView(Context context) {
        super(context);
    }

    public CursorEndEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CursorEndEditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final void setTextCursorEnd(CharSequence text) {
        setText(text);
        if(!TextUtils.isEmpty(text)){
            setSelection(getText().toString().length());
        }

    }

    public final void setTextCursorEnd(int textRes) {
        String text = getContext().getResources().getString(textRes);
        setText(text);
        if(!TextUtils.isEmpty(text)){
            setSelection(getText().toString().length());
        }
    }
}
