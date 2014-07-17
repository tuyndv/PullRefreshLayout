package me.xiaopan.android.widget;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.Scroller;

public class NewScrollManager{
    private int animationDuration;
    private Bridge bridge;
    private Scroller scroller;
    private ScrollListener scrollListener;
    private boolean rollback;
    private boolean running;

    public NewScrollManager(Context context, Bridge bridge, ScrollListener scrollListener) {
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
        bridge.getView().invalidate();

        return true;
    }

    public void computeScroll(){
        if(!running){
            return;
        }

        // 正常结束了
        if(!scroller.computeScrollOffset()){
            running = false;
            if(scrollListener != null){
                scrollListener.onScrollEnd();
            }
            Log.w("动画", "结束了");
            return;
        }

        // 更新
        bridge.setTargetViewHeightDecrement(scroller.getCurrY());
        bridge.updateCurrentOffset(scroller.getCurrX(), rollback, false);
        bridge.getView().invalidate();
    }

    public void abort(){
        scroller.abortAnimation();
        running = false;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public boolean isRunning() {
        return !scroller.isFinished();
    }

    public interface Bridge{
        public int getCurrentOffset();
        public int getOriginalOffset();
        public void setOriginalOffset(int newOriginalOffset);
        public void setTargetViewHeightDecrement(int newTargetViewHeightDecrement);
        public void updateCurrentOffset(int newCurrentOffset, boolean rollback, boolean callbackHeader);
        public View getView();
    }

    public interface ScrollListener{
        public void onScrollEnd();
    }
}
