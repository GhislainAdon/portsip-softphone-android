package com.portgo.view;

/**
 * Created by huacai on 2014/11/11.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

import com.portgo.BuildConfig;

/**
     * @blog http://blog.csdn.net/xiaanming
     *
     * @author xiaanming
     *
     */
    public class SlideDeleteListView extends ListView {
        /**
         * 当前滑动的ListView　position
         */
        private int slidePosition;
        /**
         * 手指按下X的坐标
         */
        private int downY;
        /**
         * 手指按下Y的坐标
         */
        private int downX;
        /**
         * 屏幕宽度
         */
        private int screenWidth;
        /**
         * ListView的item
         */
        private View itemView;
        /**
         * 滑动类
         */
        private Scroller scroller;
        private static final int SNAP_VELOCITY = 600;
        /**
         * 速度追踪对象
         */
        private VelocityTracker velocityTracker;
        /**
         * 是否响应滑动，默认为不响应
         */
        private boolean isSlide = false;
        /**
         * 认为是用户滑动的最小距离
         */
        private int mTouchSlop;
        /**
         *  移除item后的回调接口
         */
        private OnRemoveListener mRemoveListener;
        /**
         *  item滚回初始位置的回调接口
         */
        private OnResumeListener onItemResumeListener;
        /**
         * 用来指示item滑出屏幕的方向,向左或者向右,用一个枚举值来标记
         */
        private RemoveDirection removeDirection;
        boolean slideMode = false;
        private boolean expandeMode = false;

        // 滑动删除方向的枚举值
        public enum RemoveDirection {
            RIGHT, LEFT
        }


        public SlideDeleteListView(Context context) {
            this(context, null);
        }

        public SlideDeleteListView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public SlideDeleteListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            screenWidth = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
            scroller = new Scroller(context);
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        /**
         * 设置滑动删除的回调接口
         * @param removeListener
         */
        public void setRemoveListener(OnRemoveListener removeListener) {
            this.mRemoveListener = removeListener;
        }

        public void setOnItemResumeListener(OnResumeListener onItemResumeListener) {
            this.onItemResumeListener = onItemResumeListener;
        }

        /**
         * 分发事件，主要做的是判断点击的是那个item, 以及通过postDelayed来设置响应左右滑动事件
         */
        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if(!slideMode)
                return super.dispatchTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    addVelocityTracker(event);

                    // 假如scroller滚动还没有结束，我们直接返回
                    if (!scroller.isFinished()) {
                        return super.dispatchTouchEvent(event);
                    }
                    downX = (int) event.getX();
                    downY = (int) event.getY();

                    slidePosition = pointToPosition(downX, downY);

                    // 无效的position, 不做任何处理
                    if (slidePosition == AdapterView.INVALID_POSITION) {
                        return super.dispatchTouchEvent(event);
                    }

                    // 获取我们点击的item view
                    itemView = getChildAt(slidePosition - getFirstVisiblePosition());
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (Math.abs(getScrollVelocity()) > SNAP_VELOCITY
                            || (Math.abs(event.getX() - downX) > mTouchSlop && Math
                            .abs(event.getY() - downY) < mTouchSlop)){
//                      if (getScrollVelocity() > SNAP_VELOCITY           //只支持向右滑
//                            || (event.getX() - downX > mTouchSlop && Math
//                            .abs(event.getY() - downY) < mTouchSlop)){
//                        isSlide = true;//开启滑动
                        isSlide = false;//不开启滑动
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                    recycleVelocityTracker();
                    break;
            }

            return super.dispatchTouchEvent(event);
        }

        public void setSlideMode(boolean slide) {
            this.slideMode = slide;
        }

        /**
         * 往右滑动，getScrollX()返回的是左边缘的距离，就是以View左边缘为原点到开始滑动的距离，所以向右边滑动为负值
         */
        private void scrollRight() {
            removeDirection = RemoveDirection.RIGHT;

            final int delta = (screenWidth + itemView.getScrollX());
            // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
            scroller.startScroll(itemView.getScrollX(), 0, -delta, 0,
                    Math.abs(delta));
            postInvalidate(); // 刷新itemView
        }

        /**
         * 向左滑动，根据上面我们知道向左滑动为正值
         */
        private void scrollLeft() {
            removeDirection = RemoveDirection.LEFT;
            final int delta = (screenWidth - itemView.getScrollX());
            // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
            scroller.startScroll(itemView.getScrollX(), 0, delta, 0,
                    Math.abs(delta));
            postInvalidate(); // 刷新itemView
        }

        /**
         * 根据手指滚动itemView的距离来判断是滚动到开始位置还是向左或者向右滚动
         */
        private void scrollByDistanceX() {
            // 如果向左滚动的距离大于屏幕的二分之一，就让其删除
            if (itemView.getScrollX() >= screenWidth / 2) {
                scrollLeft();
            } else if (itemView.getScrollX() <= -screenWidth / 2) {
                scrollRight();
            } else {
                // 滚回到原始位置,为了偷下懒这里是直接调用scrollTo滚动
                itemView.scrollTo(0, 0);
                if(onItemResumeListener!=null)
                    onItemResumeListener.resumeItem(itemView);
            }

        }

        /**
         * 处理我们拖动ListView item的逻辑
         */
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (isSlide && slidePosition != AdapterView.INVALID_POSITION) {

                addVelocityTracker(ev);
                final int action = ev.getAction();
                int x = (int) ev.getX();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:

                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (ev.getActionIndex()<< MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        onTouchEvent(cancelEvent);

                        int deltaX = downX - x;
                        downX = x;

                        // 手指拖动itemView滚动, deltaX大于0向左滚动，小于0向右滚
                        itemView.scrollBy(deltaX, 0);

                        return true;  //拖动的时候ListView不滚动
                    case MotionEvent.ACTION_UP://手抬起的时候，根据速度和距离来判断操作。
                        // 如果速度大于一定速度，或者距离大于一定距离。启动滑动动画。动画结束（computeScroll）,执行操作
                        int velocityX = getScrollVelocity();
                        if (velocityX > SNAP_VELOCITY) {//根据滑动速度来判断
                            scrollRight();
                        } else if (velocityX < -SNAP_VELOCITY) {//根据滑动
                            scrollLeft();
                        } else {
                            scrollByDistanceX();//根据滑动距离来判断
                        }
                        recycleVelocityTracker();
                        // 手指离开的时候就不响应左右滚动
                        isSlide = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }

            //否则直接交给ListView来处理onTouchEvent事件
            return super.onTouchEvent(ev);
        }

        @Override
        public void computeScroll() {
            // 调用startScroll的时候scroller.computeScrollOffset()返回true，
            if (scroller.computeScrollOffset()) {
                // 让ListView item根据当前的滚动偏移量进行滚动
                itemView.scrollTo(scroller.getCurrX(), scroller.getCurrY());
                itemView.getWidth();

                postInvalidate();

                // 滚动动画结束的时候调用回调接口
                if (scroller.isFinished()) {
                    if (mRemoveListener != null) {
                        mRemoveListener.removeItem(removeDirection, slidePosition);
                    }

                    itemView.scrollTo(0, 0);
                    if(onItemResumeListener!=null)
                        onItemResumeListener.resumeItem(itemView);

                }
            }
        }

        /**
         * 添加用户的速度跟踪器
         *
         * @param event
         */
        private void addVelocityTracker(MotionEvent event) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }

            velocityTracker.addMovement(event);
        }

        /**
         * 移除用户速度跟踪器
         */
        private void recycleVelocityTracker() {
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }

        /**
         * 获取X方向的滑动速度,大于0向右滑动，反之向左
         *
         * @return
         */
        private int getScrollVelocity() {
            velocityTracker.computeCurrentVelocity(1000);
            int velocity = (int) velocityTracker.getXVelocity();
            return velocity;
        }



        /**
         *
         * 当ListView item滑出屏幕，回调这个接口
         * 我们需要在回调方法removeItem()中移除该Item,然后刷新ListView
         *
         * @author xiaanming
         *
         */
        public interface OnRemoveListener {
            void removeItem(RemoveDirection direction, int position);
        }

        public interface OnResumeListener {
            void resumeItem(View view);
        }

        public void setListViewHeightBasedOnChildren(ListAdapter listAdapter) {
            int totalHeight = 0;
            // 获取ListView对应的Adapter
            if (listAdapter == null) {return;}

//            for (int i = 0; i < listAdapter.getCount(); i++) { // listAdapter.getCount()返回数据项的数目
//                View listItem = listAdapter.getView(i, null, this);
//
////            listItem.measure(0, 0); // 计算子项View 的宽高
//                int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
//                listItem.measure(desiredWidth, 0);
//
//                totalHeight += listItem.getMeasuredHeight(); // 统计所有子项的总高度
//            }
            if(0< listAdapter.getCount()){//假定所有项等高
                View listItem = listAdapter.getView(0, null, this);
//
//            listItem.measure(0, 0); // 计算子项View 的宽高
                int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                listItem.measure(desiredWidth, 0);

                totalHeight += listItem.getMeasuredHeight(); // 统计所有子项的总高度
            }
            ViewGroup.LayoutParams params = getLayoutParams();

            params.height = (totalHeight
                    + (getDividerHeight() )* (listAdapter.getCount() - 1));

            setLayoutParams(params);
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
        }

        public void setAdapter(ListAdapter adapter,boolean expand) {
            expandeMode = expand;
            super.setAdapter(adapter);
//            if(expandeMode) {
                setListViewHeightBasedOnChildren(adapter);
//            }
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if(expandeMode) {
                int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, expandSpec);
            }else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
}
