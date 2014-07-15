package me.xiaopan.pullrefreshlayout.sample.fragment;

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
import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.pullrefreshlayout.sample.PullRefreshFragment;

@InjectParentMember
@InjectContentView(R.layout.fragment_list_view)
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
                listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }
        }.execute(0);
    }
}
