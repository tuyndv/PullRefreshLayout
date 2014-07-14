package me.xiaopan.pullrefreshlayout.sample;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.widget.RefreshHeader;
import me.xiaopan.widget.RefreshLayout2HeaderBridge;

public class MyRefreshHeader extends LinearLayout implements RefreshHeader {
    private ImageView arrowImageView;
    private ProgressBar progressBar;
    private Matrix matrix;  // 用来旋转箭头

    private int maxDegrees; // 最大旋转角度
    private int triggerDistance = -1;    // 触发距离
    private float px = -1, py = -1; // 旋转中心的坐标

    private Status status; // 状态

    public MyRefreshHeader(Context context) {
        this(context, null);
    }

    public MyRefreshHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(R.layout.refresh_header, this);
        arrowImageView = (ImageView) findViewWithTag("arrow");
        progressBar = (ProgressBar) findViewWithTag("progress");

        adjustArrowViewSize(arrowImageView);
        arrowImageView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix = new Matrix();
        maxDegrees = 180;
        status = Status.NORMAL;
    }

    @Override
    public void onTouchMove(int distance) {
        if(status == Status.TRIGGERING){
            return;
        }

        // 初始化触发距离
        if(triggerDistance == -1){
            triggerDistance = getHeight() * 2;
        }

        // 计算旋转角度
        float degrees = maxDegrees;
        if(distance >= triggerDistance){
            status = Status.WAIT_TRIGGER;
        }else{
            degrees = ((float) distance/triggerDistance) * maxDegrees;
            status = Status.NORMAL;
        }

        //当滚动的时候旋转箭头
        matrix.setRotate(degrees, px, py);
        arrowImageView.setImageMatrix(matrix);
    }

    @Override
    public void onTouchUp(RefreshLayout2HeaderBridge refreshLayout2HeaderBridge) {
        if(status == Status.WAIT_TRIGGER){
            refreshLayout2HeaderBridge.setBaseLine(getHeight());
            arrowImageView.setVisibility(INVISIBLE);
            progressBar.setVisibility(VISIBLE);
            status = Status.TRIGGERING;
        }
    }

    public enum Status {
        NORMAL,
        WAIT_TRIGGER,
        TRIGGERING,
    }

    private void adjustArrowViewSize(ImageView arrowImageView){
        if(arrowImageView.getDrawable() == null){
            return;
        }

        int width = arrowImageView.getDrawable().getIntrinsicWidth();
        int height = arrowImageView.getDrawable().getIntrinsicHeight();
        int paddingLeft = arrowImageView.getPaddingLeft();
        int paddingTop = arrowImageView.getPaddingTop();
        int paddingRight = arrowImageView.getPaddingRight();
        int paddingBottom = arrowImageView.getPaddingBottom();
        if(width > height){
            int offset = (width - height)/2;
            if(paddingTop<offset){
                paddingTop = offset;
            }
            if(paddingBottom<offset){
                paddingBottom = offset;
            }
        }else if(width < height){
            int offset = (height - width)/2;
            if(paddingLeft<offset){
                paddingLeft = offset;
            }
            if(paddingRight<offset){
                paddingRight = offset;
            }
        }
        arrowImageView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        px = width /2;
        py = height /2;
    }
}
