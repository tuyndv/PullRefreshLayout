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

package me.xiaopan.android.pullrefreshlayout.fragment;

import android.os.AsyncTask;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectParentMember;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.android.pullrefreshlayout.PullRefreshFragment;

@InjectParentMember
@InjectContentView(me.xiaopan.android.pullrefreshlayout.R.layout.fragment_list_view)
public class ListViewFragment extends PullRefreshFragment {
    @InjectView(android.R.id.list) private ListView listView;

    @Override
    protected void onRefreshContent() {
        new AsyncTask<Integer, Integer, String[]>(){

            @Override
            protected String[] doInBackground(Integer... params) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                String dateString = format.format(new Date());
                String[] items = new String[]{"AbsListView"+" "+dateString
                        , "ImageView"
                        , "AbsoluteLayout"
                        , "AdapterViewFlipper"
                        , "AnalogClock"
                        , "AutoCompleteTextView"
                        , "Button"
                        , "CalendarView"
                        , "CheckBox"
                        , "CheckedTextView"
                        , "Chronometer"
                        , "DatePicker"
                        , "DialerFilter"
                        , "DigitalClock"
                        , "DrawerLayout"
                        , "EditText"
                        , "ExpandableListView"
                        , "FrameLayout"
                        , "Gallery"
                        , "GestureOverlayView"
                        , "GridLayout"
                        , "GridView"
                        , "HorizontalScrollView"
                        , "ImageButton"
                        , "ImageSwitcher"
                        , "ImageView"
                        , "LinearLayout"
                        , "ListView"
                        , "MediaController"
                        , "MultiAutoCompleteTextView"
                        , "NumberPicker"
                        , "ProgressBar"
                        , "QuickContactBadge"
                        , "RadioButton"
                        , "RadioGroup"
                        , "RatingBar"
                        , "RelativeLayout"
                        , "ScrollView"
                        , "SearchView"
                        , "SeekBar"
                        , "SlidingDrawer"
                        , "SlidingPaneLayout"
                        , "Spinner"
                        , "StackView"
                        , "Switch"
                        , "TabHost"
                        , "TableLayout"
                        , "TableRow"
                        , "TabWidget"
                        , "TextSwitcher"
                        , "TextView"
                        , "TimePicker"
                        , "ToggleButton"
                        , "TwoLineListItem"
                        , "VideoView"
                        , "ViewAnimator"
                        , "ViewFlipper"
                        , "ViewStub"
                        , "ViewSwitcher"
                        , "WebView"
                        , "ZoomButton"
                        , "ZoomControls"
                };

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return items;
            }

            @Override
            protected void onPostExecute(String[] strings) {
                if(getActivity() == null){
                    return;
                }

                listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));
                pullRefreshLayout.stopRefresh();
                invalidateOptionsMenu();
            }
        }.execute(0);
    }
}
