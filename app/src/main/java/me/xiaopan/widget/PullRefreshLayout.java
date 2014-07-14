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
public class PullRefreshLayout extends ViewGroup implements RefreshLayout2HeaderBridge {
    private static final String NAME = PullRefreshLayout.class.getSimpleName();
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;

    private View mTargetView;
    private View mRefreshHeaderView;
    private RefreshHeader mRefreshHeader;
    private int mBaselineOriginalOffset = -1;    // 基准线原始位置
    private int mCurrentTargetOffsetTop;
    private int mTouchSlop; // 触摸抖动范围，意思是说，移动距离超过此值才会被认为是一次移动操作，否则就是点击操作

    private float mDownMotionY;  // 按下的时候记录Y位置
    private float mLastMotionY; // 上一个Y位置，联合最新的Y位置计算移动量
    private int mActivePointerId = INVALID_POINTER;
    private boolean mReturningToStart;  // 标识是否正在返回到开始位置
    private boolean mIsBeingDragged;    // 标识是否开始拖拽
    private RollbackRunnable mRollbackRunnable; // 回滚器，用于回滚TargetView和HeaderView

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
            case MotionEvent.ACTION_DOWN:
                mDownMotionY = mLastMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(NAME, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mDownMotionY;

                if (!mIsBeingDragged && yDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                }
                updateBaselineOffset((int) (y - mLastMotionY));
                mRefreshHeader.onTouchMove((int) (y - mDownMotionY));
                mLastMotionY = y;
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                if(mRefreshHeader != null){
                    mRefreshHeader.onTouchUp(this);
                }
                mRollbackRunnable.run();
                return false;
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
     */
    private void updateBaselineOffset(int offset) {
        int result = mTargetView.getTop() + offset;
        if(result < mBaselineOriginalOffset){
            offset -= result;
        }
        mTargetView.offsetTopAndBottom(offset);
        mRefreshHeaderView.offsetTopAndBottom(offset);

        mCurrentTargetOffsetTop = mTargetView.getTop();
        invalidate();
    }

    @Override
    public void setBaseLine(int newBaseLine) {
        mBaselineOriginalOffset = newBaseLine + getPaddingTop();
        requestLayout();
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
            mFrom = mCurrentTargetOffsetTop;
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
                updateBaselineOffset(offset);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mCurrentTargetOffsetTop = 0;
            requestLayout();
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
