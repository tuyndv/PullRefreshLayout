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

package me.xiaopan.android.pullrefreshlayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import me.xiaopan.android.inject.InjectView;
import me.xiaopan.android.inject.app.InjectFragment;
import me.xiaopan.android.widget.PullRefreshLayout;

public abstract class PullRefreshFragment extends InjectFragment{
    @InjectView(R.id.pullRefreshLayout) protected PullRefreshLayout pullRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                invalidateOptionsMenu();
                onRefreshContent();
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                pullRefreshLayout.startRefresh();
            }
        });
    }

    protected abstract void onRefreshContent();

    protected void invalidateOptionsMenu(){
        setHasOptionsMenu(false);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
        refreshMenuItem.setIcon(pullRefreshLayout != null && pullRefreshLayout.isRefreshing() ? R.drawable.ic_refresh_disable : R.drawable.ic_refresh);
        refreshMenuItem.setTitle(pullRefreshLayout != null && pullRefreshLayout.isRefreshing()?"停止刷新":"刷新");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if(pullRefreshLayout.isRefreshing()){
                        pullRefreshLayout.stopRefresh();
                    }else{
                        pullRefreshLayout.startRefresh();
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                        invalidateOptionsMenu();
                    }
                }
            });
            return true;
        }else if(item.getItemId() == R.id.action_github){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xiaopansky/PullRefreshLayout"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
