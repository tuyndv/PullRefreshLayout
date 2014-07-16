# ![Logo](https://github.com/xiaopansky/PullRefreshLayout/raw/master/app/src/main/res/drawable-mdpi/ic_launcher.png) PullRefreshLayout

PullRefreshLayout是Android上的一个下拉刷新控件，主要用于实现下拉刷新功能，使用方式非常简单，同SwipeRefreshLayout一样，只需包括一个View即可

![sample](https://github.com/xiaopansky/PullRefreshLayout/raw/master/docs/sample.jpg)

##Features
>* 可包括任意View并实现下拉刷新功能
>* 使用简单，只需包括View即可为这个View添加下拉刷新功能
>* 可自定义下拉刷新头

## Sample App
>* [Get it on Google Play](https://play.google.com/store/apps/details?id=me.xiaopan.android.pullrefreshlayout)
>* [Download APK](https://github.com/xiaopansky/PullRefreshLayout/raw/master/releases/PullRefreshLayout-1.0.0.apk)

##Usage guide
#### 1. 自定义下拉刷新头
自定义下拉刷新头其实很简单，你只需自定义一个View并实现PullRefreshLayout.PullRefreshHeader接口即可。在自定义的时候有以下几点需要注意：
> 1. getTriggerHeight()方法返回的数值将用来判断是否达到触发条件以及触发的时候界面的整体偏移值，所以一定要返回一个正确的值，一般情况下建议此值为刷新头的高度
> 2. getStatus()、setStatus(Status)方法用来保存和获取刷新头的状态，一定要实现并好好写
> 3. onToWaitRefresh()、onToRefreshing()、onToNormal()三个方法是刷新头不同状态的回调方法，你需要在这三个方法中改变刷新头的显示样式来提示用户
> 4. 在下拉的过程中会持续回调onScroll(int)方法并且会传进去一个下拉距离参数，你可以根据下拉距离参数做一些动画（例如旋转箭头）来增强用户体验

***注意事项***

你需要保证下拉刷新头里面的所有子View在下拉的过程中宽高不会发生改变，因为一旦发生改变就会触发PullRefreshLayout的onLayout()，结果是会重置滑动位置，导致无法往下拉，特别值得注意的是提示TextView的宽高，绝对不能用wrap_content，一定要用绝对宽高

另外，我会尽快解决此问题，但现在你要用的话还请这样做

***参考示例***

>* [refresh_header.xml](https://github.com/xiaopansky/PullRefreshLayout/raw/master/app/src/main/res/layout/refresh_header.xml)
>* [MyPullRefreshHeader](https://github.com/xiaopansky/PullRefreshLayout/raw/master/app/src/main/java/me/xiaopan/android/pullrefreshlayout/widget/MyPullRefreshHeader.java)

#### 2.使用自定义的下拉刷新头（MyPullRefreshHeader）

***方法1：布局中添加***

只用将MyPullRefreshHeader添加到PullRefreshLayout里并且保证是其第二个子View即可，如下：
```xml
<?xml version="1.0" encoding="utf-8"?>
<me.xiaopan.android.widget.PullRefreshLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/pullRefreshLayout">
    <ListView
        android:id="@android:id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/white"/>
    <me.xiaopan.android.pullrefreshlayout.widget.MyPullRefreshHeader
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
</me.xiaopan.android.widget.PullRefreshLayout>
```

***方法2：代码中添加***

在Activity的onCreate()方法中通过PullRefreshLayout的setPullRefreshHeaderClass(Class)方法设置下拉刷新头，如下：
```java
@Override
protected void onCreate(Bundle savedInstanceState){
    PullRefreshLayout pullRefreshLayout = (PullRefreshLayout) findViewById(R.id.pullrefreshlayout);
    pullRefreshLayout.setPullRefreshHeaderClass(MyPullRefreshHeader.class);
    pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            // ...
        }
    });
    pullRefreshLayout.startRefresh();
}
```

***方法3：自定义PullRefreshLayout***

创建一个MyPullRefreshLayout继承自PullRefreshLayout在构造函数中通过setPullRefreshHeaderClass(Class)方法设置下拉刷新头，如下：
```java
public class MyPullRefreshLayout extends PullRefreshLayout {
    public MyPullRefreshLayout(Context context) {
        this(context, null);
    }

    public MyPullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPullRefreshHeaderClass(MyPullRefreshHeader.class);
    }
}
```
然后在布局中直接使用MyPullRefreshLayout即可，如下：
```xml
<me.xiaopan.android.pullrefreshlayout.widget.MyPullRefreshLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/pullRefreshLayout">
    <ListView
        android:id="@android:id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/white"/>
</me.xiaopan.android.pullrefreshlayout.widget.MyPullRefreshLayout>
```

#### 3. 处理刷新事件
***启动或停止刷新***

你可以通过PullRefreshLayout.startRefresh()方法启动刷新，然后通过PullRefreshLayout.stopRefresh()方法停止刷新

***监听刷新事件***

你可以通过PullRefreshLayout.setOnRefreshListener(PullRefreshLayout.OnRefreshListener)方法设置一个监听器来监听刷新事件，如下：
```java
pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
    @Override
    public void onRefresh() {
        // 2秒钟后停止刷新
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pullRefreshLayout.stopRefresh();
            }
        }, 2000);
    }
});
```

#### 4. 扩展功能
>* isRefreshing() 判断是否正在刷新
>* setAnimationDuration(int) 设置回滚动画持续时间
>* setAnimationInterpolator(Interpolator) 设置回滚动画插值器
>* setElasticForce() 设置拉力强度，取值范围是[0.0f-1.0f]，值越小拉力越强，用户越难拉

#### 5. 注意事项：
>* PullRefreshLayout必须包含1个或2个子View才能正常工作，少了或者多了都会抛出异常
>* 你必须设置OnRefreshListener和添加下拉刷新头才会开启实现下拉刷新功能，否则就跟普通的Layout没有区别

##Downloads
>* [android-pull-refresh-layout-1.0.0.jar](https://github.com/xiaopansky/PullRefreshLayout/raw/master/releases/android-pull-refresh-layout-1.0.0.jar)
>* [android-pull-refresh-layout-1.0.0-with-src.jar](https://github.com/xiaopansky/PullRefreshLayout/raw/master/releases/android-pull-refresh-layout-1.0.0-with-src.jar)

dependencies
>* [android-support-v4.jar](https://github.com/xiaopansky/HappyImageLoader/raw/master/libs/android-support-v4.jar)

##License
```java
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
```
