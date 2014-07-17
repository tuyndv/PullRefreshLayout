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
import android.view.animation.Interpolator;
import android.widget.AbsListView;

/**
 * 下拉刷新布局
 * @author xiaopan
 * @version 1.0.0 Home https://github.com/xiaopansky/PullRefreshLayout
 */
public class PullRefreshLayout extends ViewGroup implements NewScrollManager.Bridge, NewScrollManager.ScrollListener{
    private static final String NAME = PullRefreshLayout.class.getSimpleName();
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
    private boolean mIsBeingDragged;    // 标识是否开始拖拽

    private NewScrollManager mScrollManager; // 回滚器，用于回滚TargetView和HeaderView
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
        mScrollManager = new NewScrollManager(getContext(), this, this);
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

    @Override
    public void computeScroll() {
        super.computeScroll();
        if(mScrollManager != null){
            mScrollManager.computeScroll();
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
        if (!isEnabled() || targetView == null || headerView == null || headerInterface == null || (mScrollManager != null && mScrollManager.isRunning()) || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (MotionEventCompat.getActionMasked(ev)) {
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
        if (!isEnabled() || targetView == null || headerView == null || headerInterface == null || (mScrollManager != null && mScrollManager.isRunning()) || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (MotionEventCompat.getActionMasked(ev)) {
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
                int addOffset = (int) ((y - lastMotionY) * elasticForce);
                updateCurrentOffset(currentOffset + addOffset, true, true);
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
                    mScrollManager.startScroll(-1, true); // 如果正在刷新中就回滚
                }else if(headerInterface != null){
                    if(headerInterface.getStatus() == PullRefreshHeader.Status.WAIT_REFRESH){
                        startRefresh(); // 如果是等待刷新就立马开启刷新
                    }else{
                        mScrollManager.startScroll(-1, true); // 否则就回滚
                    }
                }else{
                    mScrollManager.startScroll(-1, true); // 否则就回滚
                }

                return false;
            }
        }

        return true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
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

    @Override
    public int getCurrentOffset() {
        return currentOffset;
    }

    @Override
    public int getOriginalOffset() {
        return originalOffset;
    }

    @Override
    public void setOriginalOffset(int newOriginalOffset) {
        this.originalOffset = newOriginalOffset;
    }

    @Override
    public void setTargetViewHeightDecrement(int newTargetViewHeightDecrement) {
        this.targetViewHeightDecrement = newTargetViewHeightDecrement;
    }

    /**
     * 更新基准线位置
     * @param newCurrentOffset 新的位置
     * @param rollback 是否是在往回滚
     * @param callbackHeader 是否需要回调HeaderView
     */
    @Override
    public void updateCurrentOffset(int newCurrentOffset, boolean rollback, boolean callbackHeader) {
        if(rollback){
            if(newCurrentOffset < originalOffset){
                newCurrentOffset = originalOffset;
            }
        }else{
            if(newCurrentOffset > originalOffset){
                newCurrentOffset = originalOffset;
            }
        }
        currentOffset = newCurrentOffset;
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

    @Override
    public View getView() {
        return this;
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
        mScrollManager.setAnimationDuration(animationDuration);
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
        mScrollManager.startScroll(getPaddingTop() + headerInterface.getTriggerHeight(), true);

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
        mScrollManager.startScroll(getPaddingTop(), false);

        return true;
    }

    @Override
    public void onScrollEnd() {

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
