package com.portgo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import com.portgo.R;

/**
 * Created by huacai on 2017/4/26.
 */

public class DropDownListTextView extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {

        public DropDownListTextView(Context context) {
            super(context);
        }

        public DropDownListTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public DropDownListTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean enoughToFilter() {
            return true;
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
        }

    }
