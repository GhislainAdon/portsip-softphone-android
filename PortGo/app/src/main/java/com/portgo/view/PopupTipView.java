package com.portgo.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.portgo.R;

import java.util.List;

/**
 * This utility class can add a horizontal popup-menu easily
 * <p>
 * 该工具类可以很方便的为View、ListView/GridView绑定长按弹出横向气泡菜单
 */
public class PopupTipView extends DataSetObserver implements PopupWindow.OnDismissListener, View.OnClickListener, View.OnSystemUiVisibilityChangeListener {

    public static final int DEFAULT_NORMAL_TEXT_COLOR = Color.WHITE;
    public static final int DEFAULT_PRESSED_TEXT_COLOR = Color.WHITE;
    public static final float DEFAULT_TEXT_SIZE_DP = 14;
    public static final float DEFAULT_TEXT_PADDING_LEFT_DP = 10.0f;
    public static final float DEFAULT_TEXT_PADDING_TOP_DP = 5.0f;
    public static final float DEFAULT_TEXT_PADDING_RIGHT_DP = 10.0f;
    public static final float DEFAULT_TEXT_PADDING_BOTTOM_DP = 5.0f;
    public static final int DEFAULT_NORMAL_BACKGROUND_COLOR = 0xCC000000;
    public static final int DEFAULT_PRESSED_BACKGROUND_COLOR = 0xE7777777;
    public static final int DEFAULT_BACKGROUND_RADIUS_DP = 8;
    public static final int DEFAULT_DIVIDER_COLOR = 0x9AFFFFFF;
    public static final float DEFAULT_DIVIDER_WIDTH_DP = 0.5f;
    public static final float DEFAULT_DIVIDER_HEIGHT_DP = 16.0f;

    private Context mContext;
    private PopupWindow mPopupWindow;
    private View mAnchorView;
    private View mAdapterView;
    private View mContextView;
    private View mIndicatorView;
    private BaseAdapter mPopupItemAdapter;
    private PopupListListener mPopupListListener;
    private int mContextPosition;
    private float mRawX;
    private float mRawY;
    private StateListDrawable mLeftItemBackground;
    private StateListDrawable mRightItemBackground;
    private StateListDrawable mCornerItemBackground;
    private ColorStateList mTextColorStateList;
    private GradientDrawable mCornerBackground;
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    private int mPopupWindowWidth;
    private int mPopupWindowHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mNormalTextColor;
    private int mPressedTextColor;
    private float mTextSize;
    private int mTextPaddingLeft;
    private int mTextPaddingTop;
    private int mTextPaddingRight;
    private int mTextPaddingBottom;
    private int mNormalBackgroundColor;
    private int mPressedBackgroundColor;
    private int mBackgroundCornerRadius;
    private int mDividerColor;
    private int mDividerWidth;
    private int mDividerHeight;

    public PopupTipView(Context context) {
        this.mContext = context;
        this.mNormalTextColor = DEFAULT_NORMAL_TEXT_COLOR;
        this.mPressedTextColor = DEFAULT_PRESSED_TEXT_COLOR;
        this.mTextSize = dp2px(DEFAULT_TEXT_SIZE_DP);
        this.mTextPaddingLeft = dp2px(DEFAULT_TEXT_PADDING_LEFT_DP);
        this.mTextPaddingTop = dp2px(DEFAULT_TEXT_PADDING_TOP_DP);
        this.mTextPaddingRight = dp2px(DEFAULT_TEXT_PADDING_RIGHT_DP);
        this.mTextPaddingBottom = dp2px(DEFAULT_TEXT_PADDING_BOTTOM_DP);
        this.mNormalBackgroundColor = DEFAULT_NORMAL_BACKGROUND_COLOR;
        this.mPressedBackgroundColor = DEFAULT_PRESSED_BACKGROUND_COLOR;
        this.mBackgroundCornerRadius = dp2px(DEFAULT_BACKGROUND_RADIUS_DP);
        this.mDividerColor = DEFAULT_DIVIDER_COLOR;
        this.mDividerWidth = dp2px(DEFAULT_DIVIDER_WIDTH_DP);
        this.mDividerHeight = dp2px(DEFAULT_DIVIDER_HEIGHT_DP);
        this.mIndicatorView = getDefaultIndicatorView(mContext);
        if (mScreenWidth == 0) {
            mScreenWidth = getScreenWidth();
        }
        if (mScreenHeight == 0) {
            mScreenHeight = getScreenHeight();
        }
        refreshBackgroundOrRadiusStateList();
        refreshTextColorStateList(mPressedTextColor, mNormalTextColor);
    }

    /**
     * Popup a window when anchorView is clicked and held.
     * That method will call {@link View#setOnTouchListener(View.OnTouchListener)} and
     * {@link View#setOnLongClickListener(View.OnLongClickListener)}(or
     * {@link AbsListView#setOnItemLongClickListener(AdapterView.OnItemLongClickListener)}
     * if anchorView is a instance of AbsListView), so you can only use
     * {@link PopupTipView#showPopupListWindow(View, int, float, float, BaseAdapter, PopupListListener)}
     * if you called those method before.
     *
     * @param anchorView        the view on which to pin the popup window
     * @param popupItemList     the list of the popup menu
     * @param popupListListener the Listener
     */
    public void bindViewAndLongClick(View anchorView, BaseAdapter popupItemList, PopupListListener popupListListener) {
        this.mAnchorView = anchorView;
        this.mPopupItemAdapter = popupItemList;
        this.mPopupListListener = popupListListener;
        this.mPopupWindow = null;
        mAnchorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mRawX = event.getRawX();
                mRawY = event.getRawY();
                return false;
            }
        });
        if (mAnchorView instanceof AbsListView) {
            ((AbsListView) mAnchorView).setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mPopupListListener != null
                            && !mPopupListListener.showPopupList(parent, view, position)) {
                        return false;
                    }
                    mAdapterView = parent;
                    mContextView = view;
                    mContextPosition = position;
                    showPopupListWindow();
                    return true;
                }
            });
        } else {
            mAnchorView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mPopupListListener != null
                            && !mPopupListListener.showPopupList(v, v, 0)) {
                        return false;
                    }
                    mContextView = v;
                    mContextPosition = 0;
                    showPopupListWindow();
                    return true;
                }
            });
        }
    }


    /**
     * Popup a window when anchorView is clicked and held.
     * That method will call {@link View#setOnTouchListener(View.OnTouchListener)} and
     * {@link View#setOnLongClickListener(View.OnLongClickListener)}(or
     * {@link AbsListView#setOnItemLongClickListener(AdapterView.OnItemLongClickListener)}
     * if anchorView is a instance of AbsListView), so you can only use
     * {@link PopupTipView#showPopupListWindow(View, int, float, float, BaseAdapter, PopupListListener)}
     * if you called those method before.
     *
     * @param anchorView        the view on which to pin the popup window
     * @param popupItemList     the list of the popup menu
     * @param popupListListener the Listener
     */
    public void bindViewAndClick(View anchorView, BaseAdapter popupItemList, PopupListListener popupListListener) {
        this.mAnchorView = anchorView;
        this.mPopupItemAdapter = popupItemList;
        this.mPopupListListener = popupListListener;

        mAnchorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                mRawX = event.getRawX();
//                mRawY = event.getRawY();
                int[] location =new int[2];
                v.getLocationInWindow(location);
                mRawX = location[0]+mAnchorView.getWidth()/2;
                mRawY = location[1]-mAnchorView.getHeight()/2;
                return false;
            }
        });
        mAnchorView.setOnSystemUiVisibilityChangeListener(this);
        if (mAnchorView instanceof AbsListView) {
            ((AbsListView) mAnchorView).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mPopupListListener != null
                            && !mPopupListListener.showPopupList(parent, view, position)) {
                        return ;
                    }
                    mAdapterView = parent;
                    mContextView = view;
                    mContextPosition = position;
                    showPopupListWindow();
                    return ;
                }
            });
        } else {
            mAnchorView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPopupListListener != null
                            && !mPopupListListener.showPopupList(v, v, 0)) {
                        return ;
                    }
                    mContextView = v;
                    mContextPosition = 0;
                    showPopupListWindow();
                    return ;
                }
            });
        }
    }

    /**
     * show a popup window in a bubble style.
     *
     * @param anchorView        the view on which to pin the popup window
     * @param contextPosition   context position
     * @param rawX              the original raw X coordinate
     * @param rawY              the original raw Y coordinate
     * @param popupItemAdapter     the list of the popup menu
     * @param popupListListener the Listener
     */
    public void showPopupListWindow(View anchorView, int contextPosition, float rawX, float rawY, BaseAdapter popupItemAdapter, PopupListListener popupListListener) {
        this.mAnchorView = anchorView;
        this.mPopupItemAdapter = popupItemAdapter;
        this.mPopupListListener = popupListListener;
        this.mRawX = rawX;
        this.mRawY = rawY;
        mContextView = anchorView;
        mContextPosition = contextPosition;
        if (mPopupListListener != null
                && !mPopupListListener.showPopupList(mContextView, mContextView, contextPosition)) {
            return;
        }
        showPopupListWindow();
    }

    private void showPopupListWindow() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            return;
        }

//        if(mPopupListListener instanceof AdapterPopupListListener) {
            if (mPopupWindow != null&&mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
            if (mPopupWindow == null) {
                LinearLayout contentView = new LinearLayout(mContext);
                contentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                contentView.setOrientation(LinearLayout.VERTICAL);
                LinearLayout popupListContainer = new LinearLayout(mContext);
                popupListContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                popupListContainer.setOrientation(LinearLayout.HORIZONTAL);
                popupListContainer.setBackgroundDrawable(mCornerBackground);
                contentView.addView(popupListContainer);
                if (mIndicatorView != null) {
                    LinearLayout.LayoutParams layoutParams;
                    if (mIndicatorView.getLayoutParams() == null) {
                        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    } else {
                        layoutParams = (LinearLayout.LayoutParams) mIndicatorView.getLayoutParams();
                    }
                    layoutParams.gravity = Gravity.CENTER;
                    mIndicatorView.setLayoutParams(layoutParams);
                    ViewParent viewParent = mIndicatorView.getParent();
                    if (viewParent instanceof ViewGroup) {
                        ((ViewGroup) viewParent).removeView(mIndicatorView);
                    }
                    contentView.addView(mIndicatorView);
                }

                if (mPopupItemAdapter != null) {
                    int viewCount = mPopupItemAdapter.getCount();

                    for (int i = 0; i < viewCount; i++) {
                        View childview = mPopupItemAdapter.getView(i, null, popupListContainer);
                        //item view Beijing
                        if (viewCount > 1 && i == 0) {
                            childview.setBackgroundDrawable(mLeftItemBackground);
                        } else if (viewCount > 1 && i == viewCount - 1) {
                            childview.setBackgroundDrawable(mRightItemBackground);
                        } else if (viewCount == 1) {
                            childview.setBackgroundDrawable(mCornerItemBackground);
                        } else {
                            childview.setBackgroundDrawable(getCenterItemBackground());
                        }
                        childview.setTag(i);
                        childview.setOnClickListener(this);
                        popupListContainer.addView(childview);
                        if (viewCount > 1 && i != viewCount - 1) {
                            View divider = new View(mContext);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mDividerWidth, mDividerHeight);
                            layoutParams.gravity = Gravity.CENTER;
                            divider.setLayoutParams(layoutParams);
                            divider.setBackgroundColor(mDividerColor);
                            popupListContainer.addView(divider);
                        }
                    }
                }

                if (mPopupWindowWidth == 0) {
                    mPopupWindowWidth = getViewWidth(popupListContainer);
                }
                if (mIndicatorView != null && mIndicatorWidth == 0) {
                    if (mIndicatorView.getLayoutParams().width > 0) {
                        mIndicatorWidth = mIndicatorView.getLayoutParams().width;
                    } else {
                        mIndicatorWidth = getViewWidth(mIndicatorView);
                    }
                }
                if (mIndicatorView != null && mIndicatorHeight == 0) {
                    if (mIndicatorView.getLayoutParams().height > 0) {
                        mIndicatorHeight = mIndicatorView.getLayoutParams().height;
                    } else {
                        mIndicatorHeight = getViewHeight(mIndicatorView);
                    }
                }
                if (mPopupWindowHeight == 0) {
                    mPopupWindowHeight = getViewHeight(popupListContainer) + mIndicatorHeight;
                }
                mPopupWindow = new PopupWindow(contentView, mPopupWindowWidth, mPopupWindowHeight, true);
                mPopupWindow.setTouchable(true);
                mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
                mPopupWindow.setOnDismissListener(this);
            }
//            }else{
//                LinearLayout contentView = (LinearLayout) mPopupWindow.getContentView();
//                contentView.removeAllViews();
//                LinearLayout popupListContainer = new LinearLayout(mContext);
//                popupListContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
//                popupListContainer.setOrientation(LinearLayout.HORIZONTAL);
//                popupListContainer.setBackgroundDrawable(mCornerBackground);
//                contentView.addView(popupListContainer);
//                if (mIndicatorView != null) {
//                    LinearLayout.LayoutParams layoutParams;
//                    if (mIndicatorView.getLayoutParams() == null) {
//                        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                    } else {
//                        layoutParams = (LinearLayout.LayoutParams) mIndicatorView.getLayoutParams();
//                    }
//                    layoutParams.gravity = Gravity.CENTER;
//                    mIndicatorView.setLayoutParams(layoutParams);
//                    ViewParent viewParent = mIndicatorView.getParent();
//                    if (viewParent instanceof ViewGroup) {
//                        ((ViewGroup) viewParent).removeView(mIndicatorView);
//                    }
//                    contentView.addView(mIndicatorView);
//                }
//
//                if (mPopupItemAdapter != null) {
//                    int viewCount = mPopupItemAdapter.getCount();
//
//                    for (int i = 0; i < viewCount; i++) {
//                        View childview = mPopupItemAdapter.getView(i, null, popupListContainer);
//                        //item view Beijing
//                        if (viewCount > 1 && i == 0) {
//                            childview.setBackgroundDrawable(mLeftItemBackground);
//                        } else if (viewCount > 1 && i == viewCount - 1) {
//                            childview.setBackgroundDrawable(mRightItemBackground);
//                        } else if (viewCount == 1) {
//                            childview.setBackgroundDrawable(mCornerItemBackground);
//                        } else {
//                            childview.setBackgroundDrawable(getCenterItemBackground());
//                        }
//                        childview.setTag(i);
//                        childview.setOnClickListener(this);
//                        popupListContainer.addView(childview);
//                        if (viewCount > 1 && i != viewCount - 1) {
//                            View divider = new View(mContext);
//                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mDividerWidth, mDividerHeight);
//                            layoutParams.gravity = Gravity.CENTER;
//                            divider.setLayoutParams(layoutParams);
//                            divider.setBackgroundColor(mDividerColor);
//                            popupListContainer.addView(divider);
//                        }
//                    }
////                }
//
//                if (mPopupWindowWidth == 0) {
//                    mPopupWindowWidth = getViewWidth(popupListContainer);
//                }
//                if (mIndicatorView != null && mIndicatorWidth == 0) {
//                    if (mIndicatorView.getLayoutParams().width > 0) {
//                        mIndicatorWidth = mIndicatorView.getLayoutParams().width;
//                    } else {
//                        mIndicatorWidth = getViewWidth(mIndicatorView);
//                    }
//                }
//                if (mIndicatorView != null && mIndicatorHeight == 0) {
//                    if (mIndicatorView.getLayoutParams().height > 0) {
//                        mIndicatorHeight = mIndicatorView.getLayoutParams().height;
//                    } else {
//                        mIndicatorHeight = getViewHeight(mIndicatorView);
//                    }
//                }
//                if (mPopupWindowHeight == 0) {
//                    mPopupWindowHeight = getViewHeight(popupListContainer) + mIndicatorHeight;
//                }
//                mPopupWindow.setHeight(mPopupWindowHeight);
//                mPopupWindow.setWidth(mPopupWindowWidth);
//            }
//        }
        if (mIndicatorView != null) {
            float marginLeftScreenEdge = mRawX;
            float marginRightScreenEdge = mScreenWidth - mRawX;
            if (marginLeftScreenEdge < mPopupWindowWidth / 2f) {
                // in case of the draw of indicator out of corner's bounds
                if (marginLeftScreenEdge < mIndicatorWidth / 2f + mBackgroundCornerRadius) {
                    mIndicatorView.setTranslationX(mIndicatorWidth / 2f + mBackgroundCornerRadius - mPopupWindowWidth / 2f);
                } else {
                    mIndicatorView.setTranslationX(marginLeftScreenEdge - mPopupWindowWidth / 2f);
                }
            } else if (marginRightScreenEdge < mPopupWindowWidth / 2f) {
                if (marginRightScreenEdge < mIndicatorWidth / 2f + mBackgroundCornerRadius) {
                    mIndicatorView.setTranslationX(mPopupWindowWidth / 2f - mIndicatorWidth / 2f - mBackgroundCornerRadius);
                } else {
                    mIndicatorView.setTranslationX(mPopupWindowWidth / 2f - marginRightScreenEdge);
                }
            } else {
                mIndicatorView.setTranslationX(0);
            }
        }


        if (!mPopupWindow.isShowing()) {
//            mPopupWindow.showAtLocation(mAnchorView, Gravity.CENTER,
//                    (int)mRawX,
//                    (int)mRawY);
            mPopupWindow.showAtLocation(mAnchorView, Gravity.CENTER,
                    (int) mRawX - mScreenWidth / 2,
                    (int) mRawY - mScreenHeight / 2 - mPopupWindowHeight + mIndicatorHeight);
        }
//        else{
//            mPopupWindow.update(
//                    (int) mRawX - mScreenWidth / 2,
//                    (int) mRawY - mScreenHeight / 2 - mPopupWindowHeight + mIndicatorHeight);
//        }

        if(mPopupItemAdapter!=null){
            try {
                mPopupItemAdapter.unregisterDataSetObserver(this);
            }catch (IllegalStateException e){
            }

            mPopupItemAdapter.registerDataSetObserver(this);
        }
    }

    private void refreshBackgroundOrRadiusStateList() {
        int strokeColor = mContext.getResources().getColor(R.color.portgo_color_blue);
        int dashWidth =1;
        // left
        GradientDrawable leftItemPressedDrawable = new GradientDrawable();
        leftItemPressedDrawable.setColor(mPressedBackgroundColor);
        leftItemPressedDrawable.setCornerRadii(new float[]{
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                0, 0,
                0, 0,
                mBackgroundCornerRadius, mBackgroundCornerRadius});
        leftItemPressedDrawable.setStroke(3,strokeColor, dashWidth,0);

        GradientDrawable leftItemNormalDrawable = new GradientDrawable();
        leftItemNormalDrawable.setColor(Color.TRANSPARENT);
        leftItemNormalDrawable.setCornerRadii(new float[]{
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                0, 0,
                0, 0,
                mBackgroundCornerRadius, mBackgroundCornerRadius});
        mLeftItemBackground = new StateListDrawable();
        mLeftItemBackground.addState(new int[]{android.R.attr.state_pressed}, leftItemPressedDrawable);
        mLeftItemBackground.addState(new int[]{}, leftItemNormalDrawable);
        // right
        GradientDrawable rightItemPressedDrawable = new GradientDrawable();
        rightItemPressedDrawable.setColor(mPressedBackgroundColor);
        rightItemPressedDrawable.setCornerRadii(new float[]{
                0, 0,
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                0, 0});
        rightItemPressedDrawable.setStroke(3,strokeColor, dashWidth,0);
        GradientDrawable rightItemNormalDrawable = new GradientDrawable();
        rightItemNormalDrawable.setColor(Color.TRANSPARENT);
        rightItemNormalDrawable.setCornerRadii(new float[]{
                0, 0,
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                mBackgroundCornerRadius, mBackgroundCornerRadius,
                0, 0});
        mRightItemBackground = new StateListDrawable();
        mRightItemBackground.addState(new int[]{android.R.attr.state_pressed}, rightItemPressedDrawable);
        mRightItemBackground.addState(new int[]{}, rightItemNormalDrawable);
        // corner
        GradientDrawable cornerItemPressedDrawable = new GradientDrawable();
        cornerItemPressedDrawable.setColor(mPressedBackgroundColor);
        cornerItemPressedDrawable.setCornerRadius(mBackgroundCornerRadius);
        cornerItemPressedDrawable.setStroke(3,strokeColor, dashWidth,0);
        GradientDrawable cornerItemNormalDrawable = new GradientDrawable();
        cornerItemNormalDrawable.setColor(Color.TRANSPARENT);
        cornerItemNormalDrawable.setCornerRadius(mBackgroundCornerRadius);
        mCornerItemBackground = new StateListDrawable();
        mCornerItemBackground.addState(new int[]{android.R.attr.state_pressed}, cornerItemPressedDrawable);
        mCornerItemBackground.addState(new int[]{}, cornerItemNormalDrawable);
        mCornerBackground = new GradientDrawable();
        mCornerBackground.setColor(mNormalBackgroundColor);
        mCornerBackground.setCornerRadius(mBackgroundCornerRadius);
    }

    private StateListDrawable getCenterItemBackground() {
        StateListDrawable centerItemBackground = new StateListDrawable();
        GradientDrawable centerItemPressedDrawable = new GradientDrawable();
        centerItemPressedDrawable.setColor(mPressedBackgroundColor);
        GradientDrawable centerItemNormalDrawable = new GradientDrawable();
        centerItemNormalDrawable.setColor(Color.TRANSPARENT);
        centerItemBackground.addState(new int[]{android.R.attr.state_pressed}, centerItemPressedDrawable);
        centerItemBackground.addState(new int[]{}, centerItemNormalDrawable);
        return centerItemBackground;
    }

    private void refreshTextColorStateList(int pressedTextColor, int normalTextColor) {
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_pressed};
        states[1] = new int[]{};
        int[] colors = new int[]{pressedTextColor, normalTextColor};
        mTextColorStateList = new ColorStateList(states, colors);
    }

    public void hidePopupListWindow() {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            return;
        }
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    public View getIndicatorView() {
        return mIndicatorView;
    }

    public View getDefaultIndicatorView(Context context) {
        return getTriangleIndicatorView(context, dp2px(16), dp2px(8), DEFAULT_NORMAL_BACKGROUND_COLOR);
    }

    public View getTriangleIndicatorView(Context context, final float widthPixel, final float heightPixel,
                                         final int color) {
        ImageView indicator = new ImageView(context);
        Drawable drawable = new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                Path path = new Path();
                Paint paint = new Paint();
                paint.setColor(color);
                paint.setStyle(Paint.Style.FILL);
                path.moveTo(0f, 0f);
                path.lineTo(widthPixel, 0f);
                path.lineTo(widthPixel / 2, heightPixel);
                path.close();
                canvas.drawPath(path, paint);
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }

            @Override
            public int getIntrinsicWidth() {
                return (int) widthPixel;
            }

            @Override
            public int getIntrinsicHeight() {
                return (int) heightPixel;
            }
        };
        indicator.setImageDrawable(drawable);
        return indicator;
    }

    public void setIndicatorView(View indicatorView) {
        this.mIndicatorView = indicatorView;
    }

    public void setIndicatorSize(int widthPixel, int heightPixel) {
        this.mIndicatorWidth = widthPixel;
        this.mIndicatorHeight = heightPixel;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mIndicatorWidth, mIndicatorHeight);
        layoutParams.gravity = Gravity.CENTER;
        if (mIndicatorView != null) {
            mIndicatorView.setLayoutParams(layoutParams);
        }
    }

    public int getNormalTextColor() {
        return mNormalTextColor;
    }

    public void setNormalTextColor(int normalTextColor) {
        this.mNormalTextColor = normalTextColor;
        refreshTextColorStateList(mPressedTextColor, mNormalTextColor);
    }

    public int getPressedTextColor() {
        return mPressedTextColor;
    }

    public void setPressedTextColor(int pressedTextColor) {
        this.mPressedTextColor = pressedTextColor;
        refreshTextColorStateList(mPressedTextColor, mNormalTextColor);
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSizePixel) {
        this.mTextSize = textSizePixel;
    }

    public int getTextPaddingLeft() {
        return mTextPaddingLeft;
    }

    public void setTextPaddingLeft(int textPaddingLeft) {
        this.mTextPaddingLeft = textPaddingLeft;
    }

    public int getTextPaddingTop() {
        return mTextPaddingTop;
    }

    public void setTextPaddingTop(int textPaddingTop) {
        this.mTextPaddingTop = textPaddingTop;
    }

    public int getTextPaddingRight() {
        return mTextPaddingRight;
    }

    public void setTextPaddingRight(int textPaddingRight) {
        this.mTextPaddingRight = textPaddingRight;
    }

    public int getTextPaddingBottom() {
        return mTextPaddingBottom;
    }

    public void setTextPaddingBottom(int textPaddingBottom) {
        this.mTextPaddingBottom = textPaddingBottom;
    }

    /**
     * @param left   the left padding in pixels
     * @param top    the top padding in pixels
     * @param right  the right padding in pixels
     * @param bottom the bottom padding in pixels
     */
    public void setTextPadding(int left, int top, int right, int bottom) {
        this.mTextPaddingLeft = left;
        this.mTextPaddingTop = top;
        this.mTextPaddingRight = right;
        this.mTextPaddingBottom = bottom;
    }

    public int getNormalBackgroundColor() {
        return mNormalBackgroundColor;
    }

    public void setNormalBackgroundColor(int normalBackgroundColor) {
        this.mNormalBackgroundColor = normalBackgroundColor;
        refreshBackgroundOrRadiusStateList();
    }

    public int getPressedBackgroundColor() {
        return mPressedBackgroundColor;
    }

    public void setPressedBackgroundColor(int pressedBackgroundColor) {
        this.mPressedBackgroundColor = pressedBackgroundColor;
        refreshBackgroundOrRadiusStateList();
    }

    public int getBackgroundCornerRadius() {
        return mBackgroundCornerRadius;
    }

    public void setBackgroundCornerRadius(int backgroundCornerRadiusPixel) {
        this.mBackgroundCornerRadius = backgroundCornerRadiusPixel;
        refreshBackgroundOrRadiusStateList();
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
    }

    public int getDividerWidth() {
        return mDividerWidth;
    }

    public void setDividerWidth(int dividerWidthPixel) {
        this.mDividerWidth = dividerWidthPixel;
    }

    public int getDividerHeight() {
        return mDividerHeight;
    }

    public void setDividerHeight(int dividerHeightPixel) {
        this.mDividerHeight = dividerHeightPixel;
    }

    public Resources getResources() {
        if (mContext == null) {
            return Resources.getSystem();
        } else {
            return mContext.getResources();
        }
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    private int getScreenHeight() {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    private int getViewWidth(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredWidth();
    }

    private int getViewHeight(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredHeight();
    }

    public int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics());
    }

    public int sp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                value, getResources().getDisplayMetrics());
    }

    @Override
    public void onChanged() {
        super.onChanged();
        if(mPopupWindow!=null&&mPopupWindow.isShowing()) {
            showPopupListWindow();
        }
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        if(mPopupWindow!=null&&mPopupWindow.isShowing()) {
            showPopupListWindow();
        }
    }

    @Override
    public void onDismiss() {
        if(mPopupItemAdapter!=null){
            try {
                mPopupItemAdapter.unregisterDataSetObserver(this);
            }catch (IllegalStateException e){

            }
        }
        mPopupWindowWidth = 0;
        mPopupWindowHeight = 0;
        mPopupWindow = null;
    }

    @Override
    public void onClick(View view) {
        if(mPopupListListener!=null) {
            mPopupListListener.onPopupListClick(view,(int)view.getTag(),(int)view.getTag());
            this.hidePopupListWindow();
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int i) {
        if(mAnchorView!=null&&!mAnchorView.isShown()&&mPopupWindow!=null&&mPopupWindow.isShowing()){
            mPopupWindow.dismiss();
        }
    }

    public interface PopupListListener {

        /**
         * Whether the PopupList should be bound to the special view
         *
         * @param adapterView     The context view(The AbsListView where the click happened or normal view).
         * @param contextView     The view within the AbsListView that was clicked or normal view
         * @param contextPosition The position of the view in the list
         * @return true if the view should bind the PopupList, false otherwise
         */
        boolean showPopupList(View adapterView, View contextView, int contextPosition);

        /**
         * The callback to be invoked with an item in this PopupList has
         * been clicked
         *
         * @param contextView     The context view(The AbsListView where the click happened or normal view).
         * @param contextPosition The position of the view in the list
         * @param position        The position of the view in the PopupList
         */
        void onPopupListClick(View contextView, int contextPosition, int position);
    }

    public interface AdapterPopupListListener extends PopupListListener {
        String formatText(View adapterView, View contextView, int contextPosition, int position, String text);
    }

}