package me.xiaopan.widget;

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
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * 下拉刷新布局
 */
public class PullRefreshLayout extends ViewGroup{
    private static final String NAME = PullRefreshLayout.class.getSimpleName();
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;

    private View mTargetView;
    private View mRefreshHeaderView;
    private RefreshHeader mRefreshHeader;
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
            return;
        }

        // 确保HeaderView实现了RefreshHeader接口
        if(!(getChildAt(1) instanceof RefreshHeader)){
            throw new IllegalStateException(NAME+" the second view must implement "+RefreshHeader.class.getSimpleName()+" interface");
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
        mRefreshHeader = (RefreshHeader) mRefreshHeaderView;
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

        if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
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

        if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
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
                updateBaselineOffset((int) (y - mLastMotionY), true);
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
                }else if(mRefreshHeader != null && mOnRefreshListener != null){
                    if(mRefreshHeader.getStatus() == RefreshHeader.Status.WAIT_REFRESH){
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
    public boolean canChildScrollUp() {
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
     * @param callbackHeader 是否需要回调HeaderView
     */
    private void updateBaselineOffset(int offset, boolean callbackHeader) {
        // 检查偏移量防止滚动过头
        int result = mTargetView.getTop() + offset;
        if(result < mBaselineOriginalOffset){
            offset -= result;
        }

        // 更新TargetView和HeaderView的位置
        mTargetView.offsetTopAndBottom(offset);
        mRefreshHeaderView.offsetTopAndBottom(offset);

        // 记录当前基准线的位置
        mCurrentBaseline = mTargetView.getTop();

        // 回调HeaderView
        if(callbackHeader){
            mRefreshHeader.onScroll(Math.abs(mCurrentBaseline - getPaddingTop()));
        }

        invalidate();
    }

    /**
     * 是否正在刷新
     */
    public boolean isRefreshing() {
        return mRefreshHeader != null && mRefreshHeader.getStatus() == RefreshHeader.Status.REFRESHING;
    }

    /**
     * 设置刷新监听器
     * @param listener 刷新监听器
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
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
     * 开始刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头或没有设置刷新监听器
     */
    public boolean startRefresh() {
        if(isRefreshing() || mRefreshHeader == null || mOnRefreshListener == null){
           return false;
        }

        mOnRefreshListener.onRefresh();
        mRefreshHeader.setStatus(RefreshHeader.Status.REFRESHING);
        mRefreshHeader.onToRefreshing();
        rollback(getPaddingTop() + mRefreshHeader.getTriggerHeight(), false);

        return true;
    }

    /**
     * 停止刷新
     * @return true：成功；false：失败，因为当前正在刷新中或者没有刷新头
     */
    public boolean stopRefresh() {
        if(!isRefreshing() || mRefreshHeader == null){
            return false;
        }

        mRefreshHeader.setStatus(RefreshHeader.Status.NORMAL);
        mRefreshHeader.onToNormal();
        rollback(getPaddingTop(), true);

        return true;
    }

    private class RollbackRunnable implements Runnable, Animation.AnimationListener {
        private int mFrom;

        private int animationDuration;
        private DecelerateInterpolator mDecelerateInterpolator;
        private RollbackAnimation rollbackAnimation;

        private RollbackRunnable(int animationDuration, DecelerateInterpolator mDecelerateInterpolator) {
            this.animationDuration = animationDuration;
            this.rollbackAnimation = new RollbackAnimation();
            this.mDecelerateInterpolator = mDecelerateInterpolator;
        }

        @Override
        public void run() {
            mReturningToStart = true;
            mFrom = mCurrentBaseline;
            rollbackAnimation.reset();
            rollbackAnimation.setDuration(animationDuration);
            rollbackAnimation.setAnimationListener(this);
            rollbackAnimation.setInterpolator(mDecelerateInterpolator);
            mTargetView.startAnimation(rollbackAnimation);
        }

        private class RollbackAnimation extends Animation {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                int targetTop = 0;
                if (mFrom != mBaselineOriginalOffset) {
                    targetTop = (mFrom + (int)((mBaselineOriginalOffset - mFrom) * interpolatedTime));
                }
                int offset = targetTop - mTargetView.getTop();
                final int currentTop = mTargetView.getTop();
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }
                updateBaselineOffset(offset, false);
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

    public interface RefreshHeader{
        public void onScroll(int distance);
        public void onToRefreshing();
        public void onToNormal();
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
