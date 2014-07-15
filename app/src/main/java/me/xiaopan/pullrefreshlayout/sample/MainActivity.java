package me.xiaopan.pullrefreshlayout.sample;

import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.widget.PullRefreshLayout;

public class MainActivity extends ActionBarActivity {
    private PullRefreshLayout pullRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pullRefreshLayout = (PullRefreshLayout) findViewById(R.id.pullRefreshLayout);
        pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                     public void run() {
                        pullRefreshLayout.stopRefresh();
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            invalidateOptionsMenu();
                        }
                    }
                }, 4000);
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pullRefreshLayout.startRefresh();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                    invalidateOptionsMenu();
                }
            }
        }, 400);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
        refreshMenuItem.setIcon(pullRefreshLayout.isRefreshing()?R.drawable.ic_refresh_disable:R.drawable.ic_refresh);
        refreshMenuItem.setTitle(pullRefreshLayout.isRefreshing()?"停止刷新":"刷新");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            if(pullRefreshLayout.isRefreshing()){
                pullRefreshLayout.stopRefresh();
            }else{
                pullRefreshLayout.startRefresh();
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                invalidateOptionsMenu();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
