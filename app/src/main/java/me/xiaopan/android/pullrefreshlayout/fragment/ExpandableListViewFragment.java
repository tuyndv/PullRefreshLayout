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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectParentMember;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.android.pullrefreshlayout.PullRefreshFragment;

@InjectParentMember
@InjectContentView(me.xiaopan.android.pullrefreshlayout.R.layout.fragment_expandable_list_view)
public class ExpandableListViewFragment extends PullRefreshFragment {
    @InjectView(android.R.id.list) private ExpandableListView listView;

    @Override
    protected void onRefreshContent() {
        new AsyncTask<Integer, Integer, List<Group>>(){
            @Override
            protected List<Group> doInBackground(Integer... params) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                String dateString = format.format(new Date());
                List<Group> groups = new ArrayList<Group>();
                for(int w = 0; w < 7; w++){
                    String[] texts = new String[10];
                    for(int w2 = 0; w2 < texts.length; w2++){
                        texts[w2] = "第"+(w+1)+"组的第"+(w2+1)+"个条目"+" "+dateString;
                    }
                    groups.add(new Group("第"+(w+1)+"组", texts));
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return groups;
            }

            @Override
            protected void onPostExecute(List<Group> groups) {
                if(getActivity() == null){
                    return;
                }

                listView.setAdapter(new GroupAdapter(getActivity(), groups));
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }
        }.execute(0);
    }

    public class GroupAdapter extends BaseExpandableListAdapter {
        private Context context;
        private List<Group> groupList;

        public GroupAdapter(Context context, List<Group> groupList) {
            this.context = context;
            this.groupList = groupList;
        }

        @Override
        public int getGroupCount() {
            return groupList != null?groupList.size():0;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return groupList!=null?groupList.get(groupPosition).getTexts().length:0;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return groupList.get(groupPosition).getTexts()[childPosition];
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new GroupViewHolder();
                convertView = LayoutInflater.from(context).inflate(me.xiaopan.android.pullrefreshlayout.R.layout.list_item_text, parent, false);
                viewHolder.text = (TextView) convertView.findViewById(me.xiaopan.android.pullrefreshlayout.R.id.text_textItem_text);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (GroupViewHolder) convertView.getTag();
            }

            viewHolder.text.setText(groupList.get(groupPosition).getName());
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ChildViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new ChildViewHolder();
                convertView = LayoutInflater.from(context).inflate(me.xiaopan.android.pullrefreshlayout.R.layout.list_item_text, parent, false);
                viewHolder.text = (TextView) convertView.findViewById(me.xiaopan.android.pullrefreshlayout.R.id.text_textItem_text);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ChildViewHolder) convertView.getTag();
            }

            viewHolder.text.setText(groupList.get(groupPosition).getTexts()[childPosition]);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        class GroupViewHolder{
            private TextView text;
        }

        class ChildViewHolder{
            private TextView text;
        }
    }

    public class Group{
        private String name;
        private String[] texts;

        public Group(String name, String[] texts) {
            this.name = name;
            this.texts = texts;
        }

        public String getName() {
            return name;
        }

        public String[] getTexts() {
            return texts;
        }
    }
}
