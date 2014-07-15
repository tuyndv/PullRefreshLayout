package me.xiaopan.pullrefreshlayout.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import me.xiaopan.widget.PullRefreshLayout;

public class MyPullRefreshLayout extends PullRefreshLayout{
    public MyPullRefreshLayout(Context context) {
        this(context, null);
    }

    public MyPullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(getChildCount() < 2){
            addView(new MyPullRefreshHeader(getContext()));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
