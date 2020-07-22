package com.portgo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.portgo.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;

import android.os.Message;
import androidx.annotation.Nullable;
import android.view.MotionEvent;
import java.lang.ref.WeakReference;

public class VideoPress extends View {
    private static final int VIDEO_RECORD_DEFAULT_MAX_TIME = 10;
    private final int VIDEO_RECORD_DEFAULT_MIN_TIME = 1;
    private final float VIDEO_RECORD_DEFAULT_INNER_CIRCLE_RADIUS = 5f;
    private final float VIDEO_RECORD_DEFAULT_EXCIRCLE_RADIUS = 12f;
    private final float VIDEO_RECORD_DEFAULT_CIRCLE_STROKE = 5f;

    private final int VIDEO_RECORD_DEFAULT_ANNULUS_COLOR = getResources().getColor(android.R.color.darker_gray);
    private final int VIDEO_RECORD_DEFAULT_INNER_CIRCLE_COLOR = getResources().getColor(android.R.color.white);
    private final int VIDEO_RECORD_DEFAULT_PROGRESS_COLOR = getResources().getColor(android.R.color.holo_blue_bright);

    private final float EXCICLE_MAGNIFICATION = 1.5f;
    private float excicleMagnification;
    private final float INNER_CIRCLE_SHRINKS = 0.5f;
    private float innerCircleShrinks;
    private int mMaxTime;
    private int mMinTime;
    private float mExCircleRadius, mInitExCircleRadius;
    private float mInnerCircleRadius, mInitInnerRadius;
    private int mAnnulusColor;
    private int mInnerCircleColor;
    private int mProgressColor;
    private Paint mExCirclePaint;
    private Paint mInnerCirclePaint;
    private Paint mProgressPaint;
    private boolean isRecording = false;

    private float circleStrokeWith = 5;
    private ValueAnimator mProgressAni;
    private long mStartTime = 0;
    private long mEndTime = 0;
    public long LONG_CLICK_MIN_TIME = 500;
    private Context context;
    private int mWidth;
    private int mHeight;
    private float mCurrentProgress;
    private MHandler handler = new MHandler(this);

    public VideoPress(Context context) {
        this(context, null);
    }


    public VideoPress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs);
    }

    private void initData(Context context, AttributeSet attrs) {
        this.context = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VideoPress);
        mMaxTime = a.getInt(R.styleable.VideoPress_maxTime, VIDEO_RECORD_DEFAULT_MAX_TIME);
        mMinTime = a.getInt(R.styleable.VideoPress_minTime, VIDEO_RECORD_DEFAULT_MIN_TIME);

        excicleMagnification = a.getFloat(R.styleable.VideoPress_excicleMagnification
                , EXCICLE_MAGNIFICATION);
        innerCircleShrinks = a.getFloat(R.styleable.VideoPress_innerCircleShrinks
                , INNER_CIRCLE_SHRINKS);
        if (excicleMagnification < 1) {
            throw new RuntimeException("must >1");
        }
        if (innerCircleShrinks > 1) {
            throw new RuntimeException("must < 1");
        }

        circleStrokeWith= a.getDimension(R.styleable.VideoPress_circleWidth,
                VIDEO_RECORD_DEFAULT_CIRCLE_STROKE);
        mAnnulusColor = a.getColor(R.styleable.VideoPress_annulusColor
                , VIDEO_RECORD_DEFAULT_ANNULUS_COLOR);
        mInnerCircleColor = a.getColor(R.styleable.VideoPress_innerCircleColor
                , VIDEO_RECORD_DEFAULT_INNER_CIRCLE_COLOR);
        mProgressColor = a.getColor(R.styleable.VideoPress_progressColor
                , VIDEO_RECORD_DEFAULT_PROGRESS_COLOR);
        a.recycle();
        //
        mExCirclePaint = new Paint();
        mExCirclePaint.setColor(mAnnulusColor);
        mExCirclePaint.setAntiAlias(true);
        //
        mInnerCirclePaint = new Paint();
        mInnerCirclePaint.setColor(mInnerCircleColor);
        mInnerCirclePaint.setAntiAlias(true);
        //
        mProgressPaint = new Paint();
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStrokeWidth(circleStrokeWith);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setAntiAlias(true);
        //
        mProgressAni = ValueAnimator.ofFloat(0, 360f);
        mProgressAni.setDuration(mMaxTime * 1000);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(changed){
            mWidth = right-left;
            mHeight = bottom- top;
            int radius = Math.min(mWidth, mHeight);
            mInitExCircleRadius= mExCircleRadius = radius/(excicleMagnification*2)-1;
            mInitInnerRadius = mInnerCircleRadius =mExCircleRadius- circleStrokeWith;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mWidth / 2, mHeight / 2, mExCircleRadius, mExCirclePaint);
        canvas.drawCircle(mWidth / 2, mHeight / 2, mInnerCircleRadius, mInnerCirclePaint);
        if (isRecording) {
            drawProgress(canvas);
        }
    }


    private void drawProgress(Canvas canvas) {
        final RectF rectF = new RectF(
                circleStrokeWith,
                circleStrokeWith,
                mWidth-circleStrokeWith,
                mHeight-circleStrokeWith);
        canvas.drawArc(rectF, -90, mCurrentProgress, false, mProgressPaint);
    }

    @Override
    public boolean performClick() {
        return false;
    }
    MotionEvent lastEvent;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastEvent = event;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                isRecording = true;
                mStartTime = System.currentTimeMillis();
                handler.sendEmptyMessageDelayed(MSG_START_LONG_RECORD, LONG_CLICK_MIN_TIME);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                if(isRecording) {
                    isRecording = false;
                    mEndTime = System.currentTimeMillis();

                    if (handler.hasMessages(MSG_START_LONG_RECORD)) {
                        handler.removeMessages(MSG_START_LONG_RECORD);
                    }


                    if (mEndTime - mStartTime < LONG_CLICK_MIN_TIME) {
                        if (onRecordListener != null) {
                            onRecordListener.onShortClick();
                        }

                    } else {

                        if (mProgressAni != null && mProgressAni.getCurrentPlayTime() / 1000 < mMinTime) {
                            //The recording time is less than the minimum recording time.
                            if (onRecordListener != null) {
                                onRecordListener.OnFinish(0);
                            }
                        } else {
                            //The end of the normal
                            if (onRecordListener != null) {
                                onRecordListener.OnFinish(1);
                            }
                        }
                    }
                    mExCircleRadius = mInitExCircleRadius;
                    mInnerCircleRadius = mInitInnerRadius;
                    mProgressAni.cancel();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        return true;
    }

    private void startAnimation(float bigStart, float bigEnd, float smallStart, float smallEnd) {
        ValueAnimator bigObjAni = ValueAnimator.ofFloat(bigStart, bigEnd);
        bigObjAni.setDuration(100);
        bigObjAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mExCircleRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        ValueAnimator smallObjAni = ValueAnimator.ofFloat(smallStart, smallEnd);
        smallObjAni.setDuration(100);
        smallObjAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInnerCircleRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        bigObjAni.start();
        smallObjAni.start();

        smallObjAni.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isRecording) {
                    startAniProgress();
                }
            }


            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

    }

    private void startAniProgress() {
        if (mProgressAni == null) {
            return;
        }
        mProgressAni.start();
        mProgressAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentProgress = (float) animation.getAnimatedValue();
                invalidate();

            }
        });

        mProgressAni.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {

                mCurrentProgress = 0;
                invalidate();
                MotionEvent event = MotionEvent.obtain(lastEvent);
                event.setAction(MotionEvent.ACTION_UP);
                onTouchEvent(event);
                isRecording = false;
            }
        });
    }
    private OnRecordListener onRecordListener;
    public void setOnRecordListener(OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
    }

    public static interface  OnRecordListener {
        public void onShortClick();
        public void OnRecordStartClick();
        public void OnFinish(int resultCode);
    }

    private static final int MSG_START_LONG_RECORD = 0x37419;


    static class MHandler extends android.os.Handler {

        private WeakReference<VideoPress> weakReference = null;

        public MHandler(VideoPress controlView) {
            weakReference = new WeakReference<VideoPress>(controlView);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (weakReference == null || weakReference.get() == null) return;
            final VideoPress VideoPress = weakReference.get();
            switch (msg.what) {
                case MSG_START_LONG_RECORD:
                    if (VideoPress.onRecordListener != null) {
                        VideoPress.onRecordListener.OnRecordStartClick();
                    }
                    VideoPress.startAnimation(VideoPress.mExCircleRadius,
                            VideoPress.mExCircleRadius * VideoPress.excicleMagnification,
                            VideoPress.mInnerCircleRadius,
                            VideoPress.mInnerCircleRadius * VideoPress.innerCircleShrinks);
                    break;
            }
        }
    }
}