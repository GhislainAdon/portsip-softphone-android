package com.portgo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.util.AttributeSet;

import com.portgo.view.emotion.data.EmotionDataManager;

public class EmotionEditText extends androidx.appcompat.widget.AppCompatEditText {

    public EmotionEditText(Context context) {
        super(context);
    }

    public EmotionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmotionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            try {
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    String value = clipboard.getText().toString();
                    Editable edit = getEditableText();
                    SpannableString spannableString = EmotionDataManager.getInstance().getSpanelText(value,this);
                    int index = this.getSelectionStart();
                    if (index < 0 || index >= edit.length()) {

                        edit.append(spannableString);
                    } else {
                        edit.insert(index,spannableString);
                    }

                } else {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    String value = clipboard.getText().toString();
                    Editable edit = getEditableText();
                    SpannableString spannableString = EmotionDataManager.getInstance().getSpanelText(value,this);
                    int index = this.getSelectionStart();
                    if (index < 0 || index >= edit.length()) {
                        edit.append(spannableString);
                    } else {
                        edit.insert(index, spannableString);
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.onTextContextMenuItem(id);
    }
}
