package com.portgo.view;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.portgo.R;

/**
 * Created by huacai on 2017/9/19.
 */

public class IconTipsMenu extends ActionProvider {
    private ImageView mIcon;
    private TextView mTips;


    private int clickWhat;
    private OnClickListener onClickListener;

    public IconTipsMenu(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        int size = getContext().getResources().getDimensionPixelSize(
                R.dimen.abc_action_bar_default_height_material);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(size, size);
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.view_menu_item, null, false);

        view.setLayoutParams(layoutParams);
        mIcon = (ImageView) view.findViewById(R.id.menu_icon);
        mTips = (TextView) view.findViewById(R.id.menu_tips);
        view.setOnClickListener(onViewClickListener);
        return view;
    }

    // 点击处理。
    private View.OnClickListener onViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (onClickListener != null)
                onClickListener.onClick(clickWhat);
        }
    };


    public void setOnClickListener(int what, OnClickListener onClickListener) {
        this.clickWhat = what;
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int what);
    }

    public void setIcon(@DrawableRes int icon) {
        mIcon.setImageResource(icon);
    }

    public void setTips(@StringRes int i) {
        mTips.setText(i);
    }

    public void setTips(CharSequence i) {
        mTips.setText(i);
    }

    public void setTipsVisable(int visibility){
        mTips.setVisibility(visibility);
    }
    public void setNumTips(int i) {
        float textSize = mTips.getTextSize();
        TextDrawable drawable = TextDrawable.builder().beginConfig()
                .textColor(Color.WHITE)
                .useFont(Typeface.SERIF)
                .fontSize((int)textSize)
                .bold()
                .toUpperCase()
                .height((int)textSize*2)
                .width((int)textSize*2)
                .endConfig()
                .buildRound("" + i, getContext().getResources().getColor(R.color.portgo_color_red));
        mTips.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
    }
}
