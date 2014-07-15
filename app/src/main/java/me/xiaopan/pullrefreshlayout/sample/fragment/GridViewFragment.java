/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
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

package me.xiaopan.pullrefreshlayout.sample.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import me.xiaopan.android.imageloader.ImageLoader;
import me.xiaopan.android.imageloader.display.OriginalFadeInBitmapDisplayer;
import me.xiaopan.android.imageloader.process.TailorBitmapProcessor;
import me.xiaopan.android.imageloader.task.display.DisplayOptions;
import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectParentMember;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.pullrefreshlayout.sample.PullRefreshFragment;

@InjectParentMember
@InjectContentView(R.layout.fragment_grid_view)
public class GridViewFragment extends PullRefreshFragment {
    @InjectView(R.id.gridView) private GridView gridView;

    private int index;

    @Override
    protected void onRefreshContent() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(getActivity() == null){
                    return;
                }

                gridView.setAdapter(new GridImageAdapter(getActivity(), urls[index++%urls.length], 3));
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }
        }, 2000);
    }

    String[] urls1 = new String[]{
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1387347695254.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1600x900/1386814415425.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1385432333209.jpg",
        "http://d.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=83be833f0ef3d7ca0cf63b75c429856a/f9198618367adab45171df4289d4b31c8701e40a.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1387347718813.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1385101943856.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1385101992197.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/138448321642.jpg",
        "http://a.hiphotos.baidu.com/image/h%3D768%3Bcrop%3D0%2C0%2C1366%2C768/sign=9a57341cca95d143c576e6254bcbe170/bba1cd11728b471061e225d3c1cec3fdfd0323e4.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529070_1384156026015_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529105_1384156067650_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529106_1384156069005_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529107_1384156070394_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529108_1384156072471_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529111_1384156074019_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529113_1384156076013_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529115_1384156077604_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529117_1384156078856_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c0/28529118_1384156079925_800x600.jpg",};

    String[] urls2 = new String[]{
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045747_1382930731959_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045749_1382930733296_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045751_1382930735650_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045752_1382930737013_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045754_1382930738338_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045756_1382930739576_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045757_1382930740915_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28045760_1382930743231_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160032_1388457521885.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160033_1388457523133_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160036_1388457525457_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160040_1388457527619_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160042_1388457529515_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160046_1388457533853_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1312/31/c0/30160048_1388457535723_800x600.jpg",};

    String[] urls3 = new String[]{
    "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=171f69a8087b02080cc93be254efc9b0/ac4bd11373f0820223b6d03249fbfbedab641b92.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=ccefae0f5e6034a829e2bc82fd257237/b17eca8065380cd7c4e985e3a344ad34588281cc.jpg",
    "http://c.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=870140609058d109c4e3adb1e76ef7dc/b3b7d0a20cf431ad91ba60274936acaf2edd9847.jpg",
    "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=50b1b729347adab43dd01f40bde2887f/50da81cb39dbb6fdfacee1270b24ab18972b371d.jpg",
    "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=9e7a18386963f6241c5d3d00b172d09b/8ad4b31c8701a18bebb3814e9c2f07082838fe53.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=89936f5136a85edffa8cfa207f623240/cb8065380cd79123a27f9275af345982b2b78017.jpg",
    "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=570d1f5d0e3387449cc52b7f6739e29a/a08b87d6277f9e2fa6215c051d30e924b899f3a3.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=a6edf0ccfe039245a1b5e50cb1a29fa1/2cf5e0fe9925bc31394307995cdf8db1cb137090.jpg",
    "http://g.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=a956e8450a46f21fc9345a50c0125003/dbb44aed2e738bd4cd80148fa18b87d6267ff9ef.jpg",
    "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=71d34b98cf11728b302d8821fecaf8ad/ae51f3deb48f8c54343d012638292df5e1fe7fc9.jpg",
    "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=ef25dc259f16fdfad86cc2ed82b9b737/37d3d539b6003af3724dfb6e372ac65c1138b6cc.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=e0ec15e4e51190ef01fb96dcf82da675/359b033b5bb5c9ea7017c6ead739b6003bf3b3d7.jpg",
    "http://b.hiphotos.baidu.com/image/h%3D768%3Bcrop%3D0%2C0%2C1366%2C768/sign=7ba92cc51c178a82d13c7da6ce3810ff/b7fd5266d016092452977412d60735fae6cd340d.jpg",
    "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=d4128c248418367aad897bde1845b0b7/dcc451da81cb39dbd2be265bd2160924ab183044.jpg",
    "http://h.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=96dea0afd343ad4ba62e42c3b43461cc/314e251f95cad1c8e86ba12d7d3e6709c83d51e3.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=e982b575e9f81a4c2632e8cae11c5b3a/267f9e2f07082838fa81db15ba99a9014c08f127.jpg",
    "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=68a8f4371bd8bc3ec60802c9b4bd9d7d/2fdda3cc7cd98d1005c04d3c233fb80e7bec90a2.jpg",
    "http://d.hiphotos.baidu.com/image/h%3D768%3Bcrop%3D0%2C0%2C1366%2C768/sign=7848f17841a98226a7c12921b2b9da73/50da81cb39dbb6fdc1ea962b0b24ab18972b370d.jpg",
    "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=3bb4f63f58afa40f3cc6cade9d52382c/c8177f3e6709c93d388b4ffa9d3df8dcd1005445.jpg",
    "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=930ea42b708b4710ce2ff9cff5f8f89e/f2deb48f8c5494eed9b053132ff5e0fe99257e2d.jpg",
    "http://c.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=d0964323b27eca8012053de4a715acbe/503d269759ee3d6d377c62cb41166d224f4ade7c.jpg",
    "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=78f761f334d3d539c13d0bc00cb1d233/0bd162d9f2d3572ca0c2ddd18813632762d0c39f.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=c2343fd81b4c510faec4e619566f1e4e/9f2f070828381f30ac546cb9a8014c086e06f055.jpg",
    "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=e80740c09b25bc312b5d059b68e9b6d2/b3119313b07eca80a0caace9932397dda04483f5.jpg",
    "http://d.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=f4b44230a8ec8a13141a53e3c135aaec/aa64034f78f0f7360c5b2ade0b55b319eac41343.jpg",
    "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=0f29517c41a98226b8c12f24bcb48262/c9fcc3cec3fdfc0376a830b1d63f8794a5c226e3.jpg",
    "http://d.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=8ee60514b3fb43161a1f7e7916927d40/1f178a82b9014a9045882362a8773912b21beebf.jpg",
    "http://g.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=d9ace4f87f1ed21b79c92ae69b58e6a7/5fdf8db1cb134954571b9d6e544e9258d1094ab6.jpg",
    "http://g.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=b6d844f7a5efce1bea2bccc99967c8bd/c8ea15ce36d3d539903708c23887e950352ab0ac.jpg",};

    String[] urls4 = new String[]{
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347722456149.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347721740895.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347721857611.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347721991346.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347722593324.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347722660211.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1680x1050/1347780863974.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347781646697.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/1347972847713.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1920x1080/134779196194.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1440x900/1350487688362.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/1/1440x900/1350487693287.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1383719342222.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1383719345985.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/138371938334.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1383719386504.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1383719393382.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/3/1680x1050/138183054666.jpg",
        "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=4c2802ae902397ddd6799c076fb489d4/54fbb2fb43166d22665a2edd472309f79052d24d.jpg",
        "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=d637a71ad60735fa91f04abaa86734d0/18d8bc3eb13533faffa4962faad3fd1f41345ba4.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/12/c0/28564810_1384238874717_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450344_1383907997812_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450360_1383908020393_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450354_1383908015296_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450351_1383908012501_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450347_1383908006792_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/08/c2/28450345_1383908001011_800x600.jpg",};

    String[] urls5 = new String[]{
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1386814422279.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/1384483225833.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1366x768/138734772699.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=00289b304e086e066aa83b48343e4097/d833c895d143ad4b69a241c180025aafa40f0626.jpg",
        "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=0e93f5304890f60304b098440f248878/d53f8794a4c27d1e560a42c519d5ad6eddc43886.jpg",
        "http://e.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=41416b2d4b540923aa69677da46eea6a/96dda144ad3459827e47709c0ef431adcaef84eb.jpg",
        "http://d.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=856eb8e5b11c8701d6b6b6e51149a54c/d1160924ab18972b7f1a06cbe4cd7b899e510a8a.jpg",
        "http://b.zol-img.com.cn/desk/bizhi/image/4/1440x900/1387347738143.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=18abfe390ef3d7ca0cf63b75c429856a/f9198618367adab4ca64a24489d4b31c8701e405.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c1/28531545_1384159000688_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1311/11/c1/28531569_1384159036990_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28064240_1382954846746_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062724_1382952247340_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062729_1382952249917_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062734_1382952254138_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062737_1382952255497_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062748_1382952261238_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c1/28062754_1382952263741_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28046197_1382931344153_800x600.jpg",
        "http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1310/28/c0/28046200_1382931346407_800x600.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=27175538cb3d70cf4cfaae0eceeaea63/e61190ef76c6a7ef9632af27fffaaf51f3de669e.jpg",
        "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=d311f92c0b24ab18e016e53403ccddae/cdbf6c81800a19d83d49980f31fa828ba61e4675.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=4019652e4334970a4773142ca3fceaab/7aec54e736d12f2e51f203614dc2d56285356838.jpg",
        "http://f.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=3eb0f2bea41ea8d38a227007a13c0b2d/3801213fb80e7bec59ad46e42d2eb9389b506bbb.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=95a631d83b01213fcf334adf62d10db2/f7246b600c338744f34e2a7d530fd9f9d72aa023.jpg",
        "http://d.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=5a4d60ef257f9e2f7035190b2906d247/d50735fae6cd7b896597e88a0d2442a7d8330ec4.jpg",
        "http://b.hiphotos.baidu.com/image/w%3D1366%3Bcrop%3D0%2C0%2C1366%2C768/sign=677b4bb89c510fb378197394ef05f3f6/838ba61ea8d3fd1fc082a73b324e251f95ca5f23.jpg",
        "http://a.hiphotos.baidu.com/image/w%3D2048/sign=21b75619df54564ee565e33987e69d82/738b4710b912c8fcab2d9cc7fe039245d78821d9.jpg",
        "http://e.hiphotos.baidu.com/image/w%3D2048/sign=df72f04fc88065387beaa313a3e5a044/77c6a7efce1b9d16b19bef60f1deb48f8c546456.jpg",
        "http://c.hiphotos.baidu.com/image/w%3D2048/sign=5b3202a1f403738dde4a0b228723b151/a8ec8a13632762d01530d9bea2ec08fa513dc6a1.jpg",};

    private String[][] urls = new String[][]{urls1, urls2, urls3, urls4, urls5};

    public class GridImageAdapter extends BaseAdapter {
        private Context context;
        private String[] imageUris;
        private int cloumns;
        private int screenWidth;
        private DisplayOptions displayOptions;

        @SuppressWarnings("deprecation")
        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
        public GridImageAdapter(Context context, String[] imageUris, int cloumns){
            this.context = context;
            this.imageUris = imageUris;
            this.cloumns = cloumns;
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2){
                screenWidth = display.getWidth();
            }else{
                Point point = new Point();
                display.getSize(point);
                screenWidth = point.x;
            }

            displayOptions = new DisplayOptions(context)
                    .setLoadingDrawable(R.drawable.image_loading)
                    .setLoadFailDrawable(R.drawable.image_load_fail)
                    .setEmptyUriDrawable(R.drawable.image_loading)
                    .setDisplayer(new OriginalFadeInBitmapDisplayer())
                    .setProcessor(new TailorBitmapProcessor());
        }

        @Override
        public Object getItem(int position) {
            return imageUris[position];
        }

        @Override
        public int getCount() {
            return imageUris.length;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.grid_item_image, null);
                viewHolder.image = (ImageView) convertView.findViewById(R.id.image_gridItem);
                if(cloumns > 1){
                    viewHolder.image.setLayoutParams(new FrameLayout.LayoutParams(screenWidth/ cloumns, screenWidth/ cloumns));
                }
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }

            ImageLoader.getInstance(context).display(imageUris[position], viewHolder.image, displayOptions);
            return convertView;
        }

        class ViewHolder{
            ImageView image;
        }
    }
}
