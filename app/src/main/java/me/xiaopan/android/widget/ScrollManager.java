package me.xiaopan.android.widget;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class ScrollManager {
    private int animationDuration;
    private Interpolator mInterpolator;
    private Bridge bridge;
    private Context context;
    private boolean running;

    public ScrollManager(Context context, Bridge bridge) {
        this.animationDuration = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        this.mInterpolator = new DecelerateInterpolator(2f);
        this.context = context;
        this.bridge = bridge;
    }

    /**
     * 回滚
     * @param newOriginalOffset 新的原始偏移量
     * @param diminish 如果需要改变TargetView的大小的话，在回滚的过程中是否采用慢慢变小的方式来改变其大小，否则的话采用慢慢变大的方式
     */
    public boolean startScroll(int newOriginalOffset, boolean diminish){
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
        RollbackAnimation rollbackAnimation = new RollbackAnimation(bridge.getCurrentOffset(), bridge.getOriginalOffset(), from2, to2, bridge);
        rollbackAnimation.setDuration(animationDuration);
        rollbackAnimation.setInterpolator(mInterpolator);
        rollbackAnimation.setAnimationListener(new RollbackListener(rollbackAnimation.hashCode()));
        bridge.getView().startAnimation(rollbackAnimation);

        return true;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public boolean isRunning() {
        return running;
    }

    private class RollbackAnimation extends Animation {
        private int from;
        private int to;
        private int from2;
        private int to2;
        private Bridge bridge;

        private RollbackAnimation(int from, int to, int from2, int to2, Bridge bridge) {
            this.from = from;
            this.to = to;
            this.from2 = from2;
            this.to2 = to2;
            this.bridge = bridge;
            Log.e("回滚", from + " - " + to);
        }

        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            // 计算新的基准线位置和TargetView高度的减量
            int newCurrentOffset = (from + (int)((to - from) * interpolatedTime));
            int newTargetViewHeightDecrement = (from2 != to2)?(from2 + (int) ((to2 - from2) * interpolatedTime)):0;

            Log.d("动画执行中："+hashCode(), "newCurrentOffset="+newCurrentOffset+"; newTargetViewHeightDecrement"+newTargetViewHeightDecrement);

            // 更新
            bridge.setTargetViewHeightDecrement(newTargetViewHeightDecrement);
            bridge.updateCurrentOffset(newCurrentOffset, from > to, false);
        }
    }

    private class RollbackListener implements Animation.AnimationListener{
        private int code;

        private RollbackListener(int code) {
            this.code = code;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            Log.i("动画开始", ""+ code);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Log.w("动画结束", "" + code);
            running = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public interface Bridge{
        public int getCurrentOffset();
        public int getOriginalOffset();
        public void setOriginalOffset(int newOriginalOffset);
        public void setTargetViewHeightDecrement(int newTargetViewHeightDecrement);
        public void updateCurrentOffset(int newCurrentOffset, boolean rollback, boolean callbackHeader);
        public View getView();
    }
}
