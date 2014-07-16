/*
 * Copyright (C) 2014 Peng fei Pan <sky@xiaopan.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.android.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * 下拉刷新布局
 * @author xiaopan
 * @version 1.0.0 Home https://github.com/xiaopansky/PullRefreshLayout
 */
public class PullRefreshLayout extends ViewGroup{
    private static final String NAME = PullRefreshLayout.class.getSimpleName();
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;

    private View targetView;
    private View headerView;
    private PullRefreshHeader headerInterface;
    private Class<? extends PullRefreshHeader> pullRefreshHeaderClass;

    private int touchSlop; // 触摸抖动范围，意思是说，移动距离超过此值才会被认为是一次移动操作，否则就是点击操作
    private int currentOffset; // 当前偏移量
    private int originalOffset;    // 原始偏移量
    private int activePointerId = INVALID_POINTER;
    private int targetViewHeightDecrement;  // TargetView高度减量，用于在回滚的动态的调整Target的高度
    private float downMotionY;  // 按下的时候记录Y位置
    private float lastMotionY; // 上一个Y位置，联合最新的Y位置计算移动量
    private float elasticForce = 0.5f;  //弹力强度，用来实现拉橡皮筋效果
    private boolean mReturningToStart;  // 标识是否正在返回到开始位置
    private boolean mIsBeingDragged;    // 标识是否开始拖拽

    private RollbackRunnable mRollbackRunnable; // 回滚器，用于回滚TargetView和HeaderView
    private OnRefreshListener mOnRefreshListener;   // 刷新监听器

    private boolean ready;  // PullRefreshLayout是否已经准备好可以执行刷新了
    private boolean waitRefresh;    // 是否有等待执行的刷新请求

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        originalOffset = getPaddingTop();
        mRollbackRunnable = new RollbackRunnable(getResources().getInteger(android.R.integer.config_mediumAnimTime), new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 限制子视图数量不能少于1个
        if (getChildCount() < 1){
            throw new IllegalStateException(NAME+" can be only one directly child");
        }

        // 限制子视图数量不能超过2个
        if (getChildCount() > 2){
            throw new IllegalStateException(NAME+" can host only two direct child");
        }

        // 测量第一个子视图，由于第一个子视图被认定为内容体，所以其一定要充满PullRefreshLayout
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        getChildAt(0).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        if (getChildCount() < 2){
            if(pullRefreshHeaderClass != null){
                try {
                    addView((View) pullRefreshHeaderClass.getConstructor(Context.class).newInstance(getContext()));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }else{
                return;
            }
        }

        // 确保HeaderView实现了RefreshHeader接口
        if(!(getChildAt(1) instanceof PullRefreshHeader)){
            throw new IllegalStateException(NAME+" the second view must implement "+PullRefreshHeader.class.getSimpleName()+" interface");
        }

        // 测量第二个子视图，由于第二个被认定为刷新头，所以其高度只能是UNSPECIFIED
        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = getPaddingLeft();
        int childTop;
        int childRight;
        int childBottom;
        View childView;

        // 布局第一个子视图
        if(getChildCount() < 1) return;
        childView = getChildAt(0);
        childTop = currentOffset;
        childRight = childLeft + childView.getMeasuredWidth();
        childBottom = childTop + childView.getMeasuredHeight() - targetViewHeightDecrement;
        childView.layout(childLeft, childTop, childRight, childBottom);
        targetView = childView;

        // 布局第二个子视图
        if(getChildCount() < 2) return;
        childView = getChildAt(1);
        childTop = currentOffset - childView.getMeasuredHeight();
        childRight = childLeft + childView.getMeasuredWidth();
        childBottom = childTop + childView.getMeasuredHeight();
        childView.layout(childLeft, childTop, childRight, childBottom);
        headerView = childView;
        headerInterface = (PullRefreshHeader) headerView;

        // 如果是第一次，并且有等待刷新的请求，就延迟启动刷新
        if(!ready){
            ready = true;
            if(waitRefresh){
                delayedStartRefresh();
            }
        }
    }

    /**
     * 延迟启动刷新
     */
    private void delayedStartRefresh(){
        postDelayed(new Runnable() {
            @Override
            public void run() {
                startRefresh();
            }
        }, getResources().getInteger(android.R.integer.config_mediumAnimTime));
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || targetView == null || headerView == null || headerInterface == null || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downMotionY = lastMotionY = ev.getY();
                activePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (activePointerId == INVALID_POINTER) {
                    Log.e(NAME, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                if (pointerIndex < 0) {
                    Log.e(NAME, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - downMotionY;
                if (yDiff > touchSlop) {
                    lastMotionY = y;
                    downMotionY = y;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                activePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || targetView == null || headerView == null || headerInterface == null || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:{
                downMotionY = ev.getY();
                lastMotionY = ev.getY();
                activePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                if (pointerIndex < 0) {
                    Log.e(NAME, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - downMotionY;

                if (!mIsBeingDragged && yDiff > touchSlop) {
                    lastMotionY = y;
                    downMotionY = y;
                    mIsBeingDragged = true;
                }
                updateOffset((int) ((y - lastMotionY) * elasticForce), true, true);
                lastMotionY = y;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                lastMotionY = MotionEventCompat.getY(ev, index);
                activePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:{
                onSecondaryPointerUp(ev);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:{
                mIsBeingDragged = false;
                activePointerId = INVALID_POINTER;

                if(isRefreshing()){
                    rollback(-1, true); // 如果正在刷新中就回滚
                }else if(headerInterface != null){
                    if(headerInterface.getStatus() == PullRefreshHeader.Status.WAIT_REFRESH){
                        startRefresh(); // 如果是等待刷新就立马开启刷新
                    }else{
                        rollback(-1, true); // 否则就回滚
                    }
                }else{
                    rollback(-1, true); // 否则就回滚
                }

                return false;
            }
        }

        return true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mRollbackRunnable);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mRollbackRunnable);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == activePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            lastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            activePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    private boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return targetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(targetView, -1);
        }
    }

    /**
     * 更新基准线位置偏移
     * @param offset 偏移量
     * @param rollback 是否是在往回滚
     * @param callbackHeader 是否需要回调HeaderView
     */
    private void updateOffset(int offset, boolean rollback, boolean callbackHeader) {
        // 检查偏移量防止滚动过头
        int newOffset = currentOffset + offset;
        if(rollback){
            if(newOffset < originalOffset){
                offset -= newOffset;
            }
        }else{
            if(newOffset > originalOffset){
                offset = newOffset - originalOffset;
            }
        }

        // 更新TargetView和HeaderView的位置
        currentOffset += offset;
        requestLayout();

        // 回调HeaderView
        if(!isRefreshing() && headerInterface != null && mOnRefreshListener != null && callbackHeader){
            int distance = Math.abs(currentOffset - getPaddingTop());
            if(distance >= headerInterface.getTriggerHeight()){
                headerInterface.setStatus(PullRefreshHeader.Status.WAIT_REFRESH);
                headerInterface.onToWaitRefresh();
            }else{
                headerInterface.setStatus(PullRefreshHeader.Status.NORMAL);
                headerInterface.onToNormal();
            }
            headerInterface.onScroll(distance);
        }
    }

    /**
     * 回滚
     * @param newOriginalOffset 新的原始偏移量
     * @param diminish 如果需要改变TargetView的大小的话，在回滚的过程中是否采用慢慢变小的方式来改变其大小，否则的话采用慢慢变大的方式
     */
    private void rollback(int newOriginalOffset, boolean diminish){
        if(newOriginalOffset != originalOffset && newOriginalOffset >= 0){
            if(diminish){
                mRollbackRunnable.from2 = 0;
                mRollbackRunnable.to2 = Math.abs(newOriginalOffset - originalOffset);
            }else{
                mRollbackRunnable.from2 = Math.abs(newOriginalOffset - originalOffset);
                mRollbackRunnable.to2 = 0;
            }
            originalOffset = newOriginalOffset;
        }else{
            mRollbackRunnable.from2 = 0;
            mRollbackRunnable.to2 = 0;
        }
        
        mRollbackRunnable.mFrom = currentOffset;
        mRollbackRunnable.mTo = originalOffset;
        mRollbackRunnable.run();
    }

    /**
     * 设置下拉刷新头的Class，稍后会用此Class实例化一个下拉刷新头
     */
    public void setPullRefreshHeaderClass(Class<? extends PullRefreshHeader> pullRefreshHeaderClass) {
        this.pullRefreshHeaderClass = pullRefreshHeaderClass;
    }

    /**
     * 设置动画持续时间
     */
    public void setAnimationDuration(int animationDuration) {
        mRollbackRunnable.setAnimationDuration(animationDuration);
    }

    /**
     * 设置动画插值器
     */
    public void setAnimationInterpolator(Interpolator interpolator) {
        mRollbackRunnable.setInterpolator(interpolator);
    }

    /**
     * 设置刷新监听器
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    /**
     * 设置拉力强度
     * @param elasticForce 拉力强度，取值范围是[0.0f-1.0f]用来实现橡皮筋效果，此值越小垃圾越大，用户越难拉，默认是0.5f
     */
    public void setElasticForce(float elasticForce) {
        this.elasticForce = elasticForce;
    }

    /**
     * 是否正在刷新
     */
    public boolean isRefreshing() {
        return headerInterface != null && headerInterface.getStatus() == PullRefreshHeader.Status.REFRESHING;
    }

    /**
     * 开始刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头或没有设置刷新监听器
     */
    public boolean startRefresh() {
        if(!ready){
            waitRefresh = true;
            return true;
        }

        if(isRefreshing() || headerInterface == null || mOnRefreshListener == null){
           return false;
        }

        headerInterface.setStatus(PullRefreshHeader.Status.REFRESHING);
        headerInterface.onToRefreshing();
        mOnRefreshListener.onRefresh();
        rollback(getPaddingTop() + headerInterface.getTriggerHeight(), true);

        return true;
    }

    /**
     * 停止刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头
     */
    public boolean stopRefresh() {
        if(!isRefreshing() || headerInterface == null){
            return false;
        }

        headerInterface.setStatus(PullRefreshHeader.Status.NORMAL);
        headerInterface.onToNormal();
        rollback(getPaddingTop(), false);

        return true;
    }

    private class RollbackRunnable implements Runnable{
        private int animationDuration;
        private Interpolator mInterpolator;
        private RollbackAnimation rollbackAnimation;

        private int mFrom;
        private int mTo;
        private int from2;
        private int to2;

        private RollbackRunnable(int animationDuration, Interpolator mInterpolator) {
            this.animationDuration = animationDuration;
            this.rollbackAnimation = new RollbackAnimation();
            this.mInterpolator = mInterpolator;
        }

        private int getFrom() {
            return mFrom;
        }

        private int getTo() {
            return mTo;
        }

        private int getFrom2() {
            return from2;
        }

        private int getTo2() {
            return to2;
        }

        public void setAnimationDuration(int animationDuration) {
            this.animationDuration = animationDuration;
        }

        public void setInterpolator(Interpolator interpolator) {
            this.mInterpolator = interpolator;
        }

        @Override
        public void run() {
            if(targetView == null ||getFrom() == getTo()){
                return;
            }
            
            mReturningToStart = true;
            rollbackAnimation.reset();
            rollbackAnimation.setDuration(animationDuration);
            rollbackAnimation.setInterpolator(mInterpolator);
            targetView.startAnimation(rollbackAnimation);
        }

        private class RollbackAnimation extends Animation {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                if(targetView == null){
                    return;
                }

                // 计算整体偏移量
                int targetTop = (getFrom() + (int)((getTo() - getFrom()) * interpolatedTime));
                int currentTop = targetView.getTop();
                int offset = targetTop - targetView.getTop();
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }

                // 计算TargetView高度减量
                if(getFrom2() != getTo2()){
                    targetViewHeightDecrement = (getFrom2() + (int)((getTo2() - getFrom2()) * interpolatedTime));
                }else{
                    targetViewHeightDecrement = 0;
                }

                updateOffset(offset, getFrom() > getTo(), false);
            }
        }
    }

    public interface OnRefreshListener {
        public void onRefresh();
    }

    public interface PullRefreshHeader {
        public void onScroll(int distance);
        public void onToRefreshing();
        public void onToNormal();
        public void onToWaitRefresh();
        public int getTriggerHeight();
        public Status getStatus();
        public void setStatus(Status status);

        public enum Status {
            /**
             * 正常
             */
            NORMAL,

            /**
             * 等待刷新
             */
            WAIT_REFRESH,

            /**
             * 刷新中
             */
            REFRESHING,
        }
    }
}
