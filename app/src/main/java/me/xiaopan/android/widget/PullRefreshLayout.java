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
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.Scroller;

/**
 * 下拉刷新布局
 * @author xiaopan
 * @version 1.0.0 Home https://github.com/xiaopansky/PullRefreshLayout
 */
public class PullRefreshLayout extends ViewGroup implements ScrollManager.Bridge, ScrollManager.ScrollListener {
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

    private ScrollManager scrollManager; // 回滚器，用于回滚TargetView和HeaderView
    private OnRefreshListener onRefreshListener;   // 刷新监听器

    private boolean ready;  // PullRefreshLayout是否已经准备好可以执行刷新了
    private boolean waitRefresh;    // 是否有等待执行的刷新请求

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        originalOffset = getPaddingTop();
        scrollManager = new ScrollManager(getContext(), this, this);
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
        if (!isEnabled()
                || targetView == null
                || headerView == null
                || headerInterface == null
                || scrollManager == null
                || canChildScrollUp()) {
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
                    scrollManager.abort(); // 立即中断滚动
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = INVALID_POINTER;
                mIsBeingDragged = false;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()
                || targetView == null
                || headerView == null
                || headerInterface == null
                || scrollManager == null
                || canChildScrollUp()) {
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
                    scrollManager.abort(); // 立即中断滚动
                }
                int addOffset = (int) ((y - lastMotionY) * elasticForce);
                updateOffset(currentOffset + addOffset, true, true);
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

                // 如果是等待刷新状态就开启刷新，否则就回滚
                if(headerInterface != null && headerInterface.getStatus() == PullRefreshHeader.Status.WAIT_REFRESH){
                    startRefresh();
                }else{
                    scrollManager.startScroll(-1, true);
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
     * 更新偏移位置
     * @param newOffset 新的位置
     * @param preventOverOriginalOffset true：防止滚动小于原始位置，false：防止滚动超出原始位置
     * @param manualSliding 是否是手动滑动，因为只有在手动滑动的时候才需要根据滑动距离来改变状态
     */
    @Override
    public void updateOffset(int newOffset, boolean preventOverOriginalOffset, boolean manualSliding) {
        // 防止小于或超出原始位置
        if(preventOverOriginalOffset){
            if(newOffset < originalOffset){
                newOffset = originalOffset;
            }
        }else{
            if(newOffset > originalOffset){
                newOffset = originalOffset;
            }
        }

        // 更新偏移位置
        currentOffset = newOffset;
        requestLayout();

        // 当没有监听器的时候就不处理通知HeaderView滚动事件和处理状态，
        // 这是因为如果没有监听器，那么开启了刷新状态后就没有关闭的时候，所以是没有意义的，因此就在这里加了一个限制
        if(headerInterface != null && onRefreshListener != null){
            // 通知HeaderView目前正在滚动
            int distance = Math.abs(currentOffset - getPaddingTop());
            headerInterface.onScroll(distance);

            // 没有在刷新中状态并且允许是手动滑动的就根据距离来更新状态
            if(headerInterface.getStatus() != PullRefreshHeader.Status.REFRESHING && manualSliding){
                if(distance >= headerInterface.getTriggerHeight()){
                    headerInterface.setStatus(PullRefreshHeader.Status.WAIT_REFRESH);
                    headerInterface.onToWaitRefresh();
                }else{
                    headerInterface.setStatus(PullRefreshHeader.Status.NORMAL);
                    headerInterface.onToNormal();
                }
            }
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
        scrollManager.setAnimationDuration(animationDuration);
    }

    /**
     * 设置刷新监听器
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        onRefreshListener = listener;
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
     * @return true：成功；false：失败，因为当前没有刷新头或没有设置刷新监听器亦或者正在等待刷新或刷新中状态
     */
    public boolean startRefresh() {
        if(!ready){
            waitRefresh = true;
            return true;
        }

        if(headerInterface == null || onRefreshListener == null || headerInterface.getStatus() == PullRefreshHeader.Status.REFRESHING){
           return false;
        }

        headerInterface.setStatus(PullRefreshHeader.Status.WAIT_REFRESH);
        scrollManager.startScroll(getPaddingTop() + headerInterface.getTriggerHeight(), true);

        return true;
    }

    /**
     * 停止刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头
     */
    public boolean stopRefresh() {
        if(headerInterface == null || headerInterface.getStatus() != PullRefreshHeader.Status.REFRESHING){
            return false;
        }

        headerInterface.setStatus(PullRefreshHeader.Status.NORMAL);
        headerInterface.onToNormal();
        scrollManager.startScroll(getPaddingTop(), false);

        return true;
    }

    @Override
    public void onScrollEnd() {
        if(headerInterface == null){
            return;
        }

        if(headerInterface.getStatus() == PullRefreshHeader.Status.WAIT_REFRESH){
            headerInterface.setStatus(PullRefreshHeader.Status.REFRESHING);
            headerInterface.onToRefreshing();
            onRefreshListener.onRefresh();
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


class ScrollManager implements Runnable{
    private int animationDuration;
    private Bridge bridge;
    private Scroller scroller;
    private ScrollListener scrollListener;
    private boolean rollback;
    private boolean running;

    public ScrollManager(Context context, Bridge bridge, ScrollListener scrollListener) {
        this.animationDuration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        this.bridge = bridge;
        this.scrollListener = scrollListener;
        this.scroller = new Scroller(context, new DecelerateInterpolator());
    }

    /**
     * 滚动
     * @param newOriginalOffset 新的原始偏移量
     * @param diminish 如果需要改变TargetView的大小的话，在回滚的过程中是否采用慢慢变小的方式来改变其大小，否则的话采用慢慢变大的方式
     */
    public boolean startScroll(int newOriginalOffset, boolean diminish){
        abort();

        int startX = bridge.getCurrentOffset();
        int endX = bridge.getOriginalOffset();
        int startY = 0;
        int endY = 0;
        if(newOriginalOffset != bridge.getOriginalOffset() && newOriginalOffset >= 0){
            if(diminish){
                startY = 0;
                endY = Math.abs(newOriginalOffset - bridge.getOriginalOffset());
            }else{
                startY = Math.abs(newOriginalOffset - bridge.getOriginalOffset());
                endY = 0;
            }
            bridge.setOriginalOffset(newOriginalOffset);
            endX = newOriginalOffset;
        }

        running = true;
        rollback = startX > endX;
        scroller.startScroll(startX, startY, endX-startX, endY-startY, animationDuration);
        bridge.getView().post(this);

        return true;
    }

    @Override
    public void run() {
        if(!running){
            return;
        }

        // 正常结束了
        if(!scroller.computeScrollOffset()){
            running = false;
            if(scrollListener != null){
                scrollListener.onScrollEnd();
            }
            return;
        }

        // 更新
        bridge.setTargetViewHeightDecrement(scroller.getCurrY());
        bridge.updateOffset(scroller.getCurrX(), rollback, false);

        bridge.getView().post(this);
    }

    public void abort(){
        if(!scroller.isFinished()){
            scroller.abortAnimation();
        }
        bridge.getView().removeCallbacks(this);
        running = false;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public interface Bridge{
        public int getCurrentOffset();
        public int getOriginalOffset();
        public void setOriginalOffset(int newOriginalOffset);
        public void setTargetViewHeightDecrement(int newTargetViewHeightDecrement);
        public void updateOffset(int newCurrentOffset, boolean rollback, boolean callbackHeader);
        public View getView();
    }

    public interface ScrollListener{
        public void onScrollEnd();
    }
}
