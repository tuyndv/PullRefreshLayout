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

package me.xiaopan.android.pullrefreshlayout.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.xiaopan.android.pullrefreshlayout.R;
import me.xiaopan.android.widget.PullRefreshLayout;

public class MyPullRefreshLayout extends PullRefreshLayout {
    public MyPullRefreshLayout(Context context) {
        this(context, null);
    }

    public MyPullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPullRefreshHeaderClass(MyPullRefreshHeader.class);
    }

    private static class MyPullRefreshHeader extends LinearLayout implements PullRefreshLayout.PullRefreshHeader {
        private ImageView arrowImageView;
        private ProgressBar progressBar;
        private TextView hintTextView;

        private Matrix matrix;  // 用来旋转箭头
        private int maxDegrees; // 最大旋转角度
        private int triggerHeight = -1;    // 触发高度
        private float px = -1, py = -1; // 旋转中心的坐标
        private Status status; // 状态

        public MyPullRefreshHeader(Context context) {
            this(context, null);
        }

        public MyPullRefreshHeader(Context context, AttributeSet attrs) {
            super(context, attrs);
            LayoutInflater.from(getContext()).inflate(R.layout.refresh_header, this);
            arrowImageView = (ImageView) findViewWithTag("arrowImage");
            progressBar = (ProgressBar) findViewWithTag("progressBar");
            hintTextView = (TextView) findViewWithTag("hintText");

            adjustArrowImageViewPadding(arrowImageView);
            px = arrowImageView.getDrawable().getIntrinsicWidth() /2;
            py = arrowImageView.getDrawable().getIntrinsicHeight() /2;
            arrowImageView.setScaleType(ImageView.ScaleType.MATRIX);
            matrix = new Matrix();
            maxDegrees = 180;
            status = Status.NORMAL;

            onToNormal();
        }

        @Override
        public void onScroll(int distance) {
            // 如果当前正在刷新，就什么也不做
            if(status == Status.REFRESHING){
                return;
            }

            // 计算旋转角度并旋转箭头
            float degrees;
            if(distance >= getTriggerHeight()){
                degrees = maxDegrees;
            }else{
                degrees = ((float) distance/ getTriggerHeight()) * maxDegrees;
            }
            matrix.setRotate(degrees, px, py);
            arrowImageView.setImageMatrix(matrix);
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public void onToWaitRefresh() {
            arrowImageView.setVisibility(VISIBLE);
            progressBar.setVisibility(INVISIBLE);
            hintTextView.setText("松手立即刷新");
        }

        @Override
        public void onToRefreshing() {
            arrowImageView.setVisibility(INVISIBLE);
            progressBar.setVisibility(VISIBLE);
            hintTextView.setText("正在刷新...");
        }

        @Override
        public void onToNormal() {
            arrowImageView.setVisibility(VISIBLE);
            progressBar.setVisibility(INVISIBLE);
            hintTextView.setText("下拉刷新");
        }

        @Override
        public int getTriggerHeight() {
            // 初始化触发高度
            if(triggerHeight == -1){
                triggerHeight = getMeasuredHeight();
            }
            return triggerHeight;
        }

        /**
         * 调整箭头图片的内边距，保证在旋转箭头的时候不会被遮盖
         */
        private void adjustArrowImageViewPadding(ImageView arrowImageView){
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
        }
    }
}
