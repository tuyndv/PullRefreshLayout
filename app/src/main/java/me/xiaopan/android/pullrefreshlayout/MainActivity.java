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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.android.inject.app.InjectActionBarActivity;
import me.xiaopan.android.pullrefreshlayout.fragment.ExpandableListViewFragment;
import me.xiaopan.android.pullrefreshlayout.fragment.GridViewFragment;
import me.xiaopan.android.pullrefreshlayout.fragment.ListViewFragment;
import me.xiaopan.android.pullrefreshlayout.fragment.ScrollViewFragment;
import me.xiaopan.android.pullrefreshlayout.fragment.WebViewFragment;

@InjectContentView(R.layout.activity_main)
public class MainActivity extends InjectActionBarActivity {
    @InjectView(R.id.drawer_layout) private DrawerLayout mDrawerLayout;
    @InjectView(R.id.left_drawer) private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitle = mDrawerTitle = getTitle();
        mPlanetTitles = new String[]{"ScrollView", "ListView", "WebView", "GridView", "ExpandableListView"};

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if(fragment != null && fragment instanceof WebViewFragment){
            WebViewFragment webViewFragment = (WebViewFragment) fragment;
            if(!webViewFragment.onBackPressed()){
                super.onBackPressed();
            }
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItem(int position) {
        Fragment fragment = null;
        switch(position){
            case 0 :{
                fragment = new ScrollViewFragment();
                break;
            }
            case 1 :{
                fragment = new ListViewFragment();
                break;
            }
            case 2 :{
                fragment = new WebViewFragment();
                break;
            }
            case 3 :{
                fragment = new GridViewFragment();
                break;
            }
            case 4 :{
                fragment = new ExpandableListViewFragment();
                break;
            }
        }

        if(fragment != null){
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.window_slide_to_left_in, R.anim.window_slide_to_left_out).replace(R.id.content_frame, fragment).commit();
            mDrawerList.setItemChecked(position, true);
            getSupportActionBar().setSubtitle(mPlanetTitles[position]);
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
