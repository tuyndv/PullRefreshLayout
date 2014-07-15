package me.xiaopan.pullrefreshlayout.sample.fragment;

import android.os.Build;
import android.os.Handler;

import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectParentMember;
import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.pullrefreshlayout.sample.PullRefreshFragment;

@InjectParentMember
@InjectContentView(R.layout.fragment_image_view)
public class ImageViewFragment extends PullRefreshFragment {

    @Override
    protected void onRefreshContent() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }
        }, 2000);
    }
}