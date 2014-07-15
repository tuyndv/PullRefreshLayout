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
    private float elasticForce = 0.5f;  //弹力强度，用来实现拉橡皮筋效果

    private View mTargetView;
    private View mRefreshHeaderView;
    private Class<? extends PullRefreshHeader> pullRefreshHeaderClass;
    private PullRefreshHeader mPullRefreshHeader;
    private int mBaselineOriginalOffset = -1;    // 基准线原始位置
    private int mCurrentBaseline;
    private int mTouchSlop; // 触摸抖动范围，意思是说，移动距离超过此值才会被认为是一次移动操作，否则就是点击操作

    private float mDownMotionY;  // 按下的时候记录Y位置
    private float mLastMotionY; // 上一个Y位置，联合最新的Y位置计算移动量
    private int mActivePointerId = INVALID_POINTER;
    private boolean mReturningToStart;  // 标识是否正在返回到开始位置
    private boolean mIsBeingDragged;    // 标识是否开始拖拽
    private RollbackRunnable mRollbackRunnable; // 回滚器，用于回滚TargetView和HeaderView
    private OnRefreshListener mOnRefreshListener;

    private boolean ready;
    private boolean waitRefresh;

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mRollbackRunnable = new RollbackRunnable(getResources().getInteger(android.R.integer.config_mediumAnimTime), new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR));
        mBaselineOriginalOffset = getPaddingTop();
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
        childTop = getPaddingTop();
        childRight = childLeft + childView.getMeasuredWidth();
        int offset = 0;
        if(mBaselineOriginalOffset != childTop){
            offset = mBaselineOriginalOffset - childTop;
        }
        childBottom = (childTop + childView.getMeasuredHeight()) - offset;
        childView.layout(childLeft, childTop, childRight, childBottom);
        mTargetView = childView;

        // 布局第二个子视图
        if(getChildCount() < 2) return;
        childView = getChildAt(1);
        childTop = getPaddingTop() - childView.getMeasuredHeight();
        childRight = childLeft + childView.getMeasuredWidth();
        childBottom = childTop + childView.getMeasuredHeight();
        childView.layout(childLeft, childTop, childRight, childBottom);
        mRefreshHeaderView = childView;
        mPullRefreshHeader = (PullRefreshHeader) mRefreshHeaderView;

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

        if (!isEnabled() || mTargetView == null || mRefreshHeaderView == null || mPullRefreshHeader == null || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownMotionY = mLastMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(NAME, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(NAME, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mDownMotionY;
                if (yDiff > mTouchSlop) {
                    mLastMotionY = y;
                    mDownMotionY = y;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
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

        if (!isEnabled() || mTargetView == null || mRefreshHeaderView == null || mPullRefreshHeader == null || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:{
                mDownMotionY = ev.getY();
                mLastMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(NAME, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mDownMotionY;

                if (!mIsBeingDragged && yDiff > mTouchSlop) {
                    mLastMotionY = y;
                    mDownMotionY = y;
                    mIsBeingDragged = true;
                }
                updateBaselineOffset((int) ((y - mLastMotionY) * elasticForce), true, true);
                mLastMotionY = y;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:{
                onSecondaryPointerUp(ev);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:{
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;

                if(isRefreshing()){
                    rollback(false); // 如果正在刷新中就回滚
                }else if(mPullRefreshHeader != null && mOnRefreshListener != null){
                    if(mPullRefreshHeader.getStatus() == PullRefreshHeader.Status.WAIT_REFRESH){
                        startRefresh(); // 如果是等待刷新就立马开启刷新
                    }else{
                        rollback(false); // 否则就回滚
                    }
                }else{
                    rollback(false); // 否则就回滚
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
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    private boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTargetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTargetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mTargetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTargetView, -1);
        }
    }

    /**
     * 更新基准线位置偏移
     * @param offset 偏移量
     * @param rollback 是否是在往回滚
     * @param callbackHeader 是否需要回调HeaderView
     */
    private void updateBaselineOffset(int offset, boolean rollback, boolean callbackHeader) {
        // 检查偏移量防止滚动过头
        int result = mTargetView.getTop() + offset;
        if(rollback){
            if(result < mBaselineOriginalOffset){
                offset -= result;
            }
        }else{
            if(result > mBaselineOriginalOffset){
                offset = result - mBaselineOriginalOffset;
            }
        }

        // 更新TargetView和HeaderView的位置
        mTargetView.offsetTopAndBottom(offset);
        mRefreshHeaderView.offsetTopAndBottom(offset);

        // 记录当前基准线的位置
        mCurrentBaseline = mTargetView.getTop();

        // 回调HeaderView
        if(callbackHeader){
            int distance = Math.abs(mCurrentBaseline - getPaddingTop());
            if(distance >= mPullRefreshHeader.getTriggerHeight()){
                mPullRefreshHeader.setStatus(PullRefreshHeader.Status.WAIT_REFRESH);
                mPullRefreshHeader.onToWaitRefresh();
            }else{
                mPullRefreshHeader.setStatus(PullRefreshHeader.Status.NORMAL);
                mPullRefreshHeader.onToNormal();
            }
            mPullRefreshHeader.onScroll(distance);
        }

        invalidate();
    }

    /**
     * 回滚
     * @param newBaseLine 新的基准线
     * @param rollbackBeforeRequestLayout 在回滚之前请求布局
     */
    private void rollback(int newBaseLine, boolean rollbackBeforeRequestLayout){
        if(newBaseLine != mBaselineOriginalOffset && newBaseLine >= 0){
            mBaselineOriginalOffset = newBaseLine;
            if(rollbackBeforeRequestLayout){
                requestLayout();
            }
        }
        mRollbackRunnable.setFrom(mCurrentBaseline);
        mRollbackRunnable.setTo(mBaselineOriginalOffset);
        mRollbackRunnable.run();
    }

    /**
     * 回滚
     * @param rollbackBeforeRequestLayout 在回滚之前请求布局
     */
    private void rollback(boolean rollbackBeforeRequestLayout){
        rollback(-1, rollbackBeforeRequestLayout);
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
        return mPullRefreshHeader != null && mPullRefreshHeader.getStatus() == PullRefreshHeader.Status.REFRESHING;
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

        if(isRefreshing() || mPullRefreshHeader == null || mOnRefreshListener == null){
           return false;
        }

        mOnRefreshListener.onRefresh();
        mPullRefreshHeader.setStatus(PullRefreshHeader.Status.REFRESHING);
        mPullRefreshHeader.onToRefreshing();
        rollback(getPaddingTop() + mPullRefreshHeader.getTriggerHeight(), false);

        return true;
    }

    /**
     * 停止刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头
     */
    public boolean stopRefresh() {
        if(!isRefreshing() || mPullRefreshHeader == null){
            return false;
        }

        mPullRefreshHeader.setStatus(PullRefreshHeader.Status.NORMAL);
        mPullRefreshHeader.onToNormal();
        rollback(getPaddingTop(), true);

        return true;
    }

    private class RollbackRunnable implements Runnable, Animation.AnimationListener {
        private int animationDuration;
        private Interpolator mInterpolator;
        private RollbackAnimation rollbackAnimation;

        private int mFrom;
        private int mTo;

        private RollbackRunnable(int animationDuration, Interpolator mInterpolator) {
            this.animationDuration = animationDuration;
            this.rollbackAnimation = new RollbackAnimation();
            this.mInterpolator = mInterpolator;
        }

        public int getFrom() {
            return mFrom;
        }

        public void setFrom(int from) {
            this.mFrom = from;
        }

        public int getTo() {
            return mTo;
        }

        public void setTo(int to) {
            this.mTo = to;
        }

        public void setAnimationDuration(int animationDuration) {
            this.animationDuration = animationDuration;
        }

        public void setInterpolator(Interpolator interpolator) {
            this.mInterpolator = interpolator;
        }

        @Override
        public void run() {
            if(getFrom() == getTo()){
                return;
            }

            mReturningToStart = true;
            rollbackAnimation.reset();
            rollbackAnimation.setDuration(animationDuration);
            rollbackAnimation.setAnimationListener(this);
            rollbackAnimation.setInterpolator(mInterpolator);
            mTargetView.startAnimation(rollbackAnimation);
        }

        private class RollbackAnimation extends Animation {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                int targetTop = (getFrom() + (int)((getTo() - getFrom()) * interpolatedTime));
                int currentTop = mTargetView.getTop();
                int offset = targetTop - mTargetView.getTop();
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }
                updateBaselineOffset(offset, getFrom() > getTo(), false);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            requestLayout();
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
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
