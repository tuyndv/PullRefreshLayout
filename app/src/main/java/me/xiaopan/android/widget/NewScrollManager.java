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

        int from2;
        int to2;

        if(newOriginalOffset != bridge.getOriginalOffset() && newOriginalOffset >= 0){
            if(diminish){
                from2 = 0;
                to2 = Math.abs(newOriginalOffset - bridge.getOriginalOffset());
            }else{
                from2 = Math.abs(newOriginalOffset - bridge.getOriginalOffset());
                to2 = 0;
            }
            bridge.setOriginalOffset(newOriginalOffset);
        }else{
            from2 = 0;
            to2 = 0;
        }

        if(bridge.getCurrentOffset() == bridge.getOriginalOffset()){
            return false;
        }
        running = true;
        int startX = bridge.getCurrentOffset();
        int endX = bridge.getOriginalOffset();
        int dX = endX - startX;
        int startY = from2;
        int endY = to2;
        int dY = to2 - from2;
        rollback = startX > endX;

        Log.w("开始执行动画", "整体："+startX+(dX>0?"+":"-")+Math.abs(dX)+"="+endX+"; TargetViewHeight："+startY+(dY>0?"+":"-")+Math.abs(dY)+"="+endY);

        scroller.startScroll(startX, startY, dX, dY, animationDuration);
        bridge.getView().invalidate();

        return true;
    }

    public void computeScroll(){
        if(!running){
            return;
        }

        // 正常结束了
        if(!scroller.computeScrollOffset()){
            if(scrollListener != null){
                scrollListener.onScrollEnd();
            }
            Log.w("动画", "结束了");
            return;
        }

        // 计算新的基准线位置和TargetView高度的减量
        int newCurrentOffset = scroller.getCurrX();
        int newTargetViewHeightDecrement = scroller.getCurrY();
        Log.d("动画执行中", "newCurrentOffset=" + newCurrentOffset + "; newTargetViewHeightDecrement" + newTargetViewHeightDecrement);

        // 更新
        bridge.setTargetViewHeightDecrement(newTargetViewHeightDecrement);
        bridge.updateCurrentOffset(newCurrentOffset, rollback, false);
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
