package com.taxi.baidunavisdkdemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NavigationMainActivity extends AppCompatActivity implements View.OnClickListener {

    public static List<Activity> activityList = new LinkedList<Activity>();
    private static final String TAG = "NavigationMainActivity";

    private String mSDCardPath = null;
    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";
    private String authinfo = null;

    private final static String authBaseArr[] =
            { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION };
    private final static String authComArr[] = { Manifest.permission.READ_PHONE_STATE };
    private final static int authBaseRequestCode = 1;
    private final static int authComRequestCode = 2;

    private boolean hasInitSuccess = false;
    private boolean hasRequestComAuth = false;
    private boolean naviType = false;
    public static final String ROUTE_PLAN_NODE = "routePlanNode";

    private MapView mMapView = null;
    protected BaiduMap baiduMap = null;
    private LocationClient mLocationClient;
    private LatLng mLatlng;
    private LatLng endLatlng;
    private MyLocationListener myListener = new MyLocationListener();
    private MarkerOptions options;
    private BNRoutePlanNode.CoordinateType mCoordinateType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityList.add(this);

        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_navigation_main);

        mMapView = (MapView) findViewById(R.id.mapView);
        baiduMap = mMapView.getMap();
        initView();
        init();

        if (initDirs()) {
            initNavi();
        }
    }

    private void initView() {
         findViewById(R.id.bt_location).setOnClickListener(this);
         findViewById(R.id.bt_mn_navigation).setOnClickListener(this);
         findViewById(R.id.bt_zs_navigation).setOnClickListener(this);

        baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                endLatlng = latLng;
                if (options == null) {
                    options = new MarkerOptions();
                    options.alpha(1.0f).anchor(0.5f, 1.0f)
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding));
                }else {
                    baiduMap.clear();
                    options.position(latLng);
                }
                baiduMap.addOverlay(options);
            }
        });
    }

    public void init() {
        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);
        //注册监听函数
        initLocation();
        baiduMap.setMyLocationEnabled(true);
        setMyLocationConfigeration();
        mLocationClient.start();
    }

    private void setMyLocationConfigeration() {
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
        MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.COMPASS,
                true,descriptor);
        baiduMap.setMyLocationConfiguration(configuration);
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        //option.setScanSpan(5000);
        option.setOpenGps(true);
        option.setLocationNotify(false);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGps(false);
        option.setIsNeedAddress(true);
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(false);
        mLocationClient.setLocOption(option);
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
                Log.e(TAG, "initDirs: 建立目录" );
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private void initNavi() {

        BNOuterTTSPlayerCallback ttsCallback = null;

        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (!hasBasePhoneAuth()) {

                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;

            }
        }

        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new BaiduNaviManager.NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                NavigationMainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(NavigationMainActivity.this, authinfo, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            public void initSuccess() {
                Toast.makeText(NavigationMainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                hasInitSuccess = true;
                initSetting();
                Log.e(TAG, "initSuccess: 导航初始化成功" );
            }

            public void initStart() {
                Toast.makeText(NavigationMainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "initSuccess: 导航初始化开始" );
            }

            public void initFailed() {
                Toast.makeText(NavigationMainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }

        }, null, ttsHandler, ttsPlayStateListener);

    }

    private boolean hasBasePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "hasBasePhoneAuth: 申请导航权限" );
                return false;
            }
        }
        return true;
    }

    private boolean hasCompletePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authComArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "hasBasePhoneAuth: 完善申请导航权限" );
                return false;
            }
        }
        return true;
    }

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    // showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    // showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            // showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            // showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };

    private void initSetting() {
        // BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        BNaviSettingManager
                .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        // BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
        BNaviSettingManager.setIsAutoQuitWhenArrived(true);
        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, "10532464");
        BNaviSettingManager.setNaviSdkParam(bundle);
    }

    private void routeplanToNavi(boolean navigation) {
        if (!hasInitSuccess) {
            Toast.makeText(NavigationMainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // 保证导航功能完备
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    this.requestPermissions(authComArr, authComRequestCode);
                    return;
                } else {
                    Toast.makeText(NavigationMainActivity.this, "没有完备的权限!", Toast.LENGTH_SHORT).show();
                }
            }

        }
        BNRoutePlanNode sNode = new BNRoutePlanNode(mLatlng.longitude,mLatlng.latitude,"起点",null, BNRoutePlanNode.CoordinateType.BD09LL);
        BNRoutePlanNode eNode = new BNRoutePlanNode(endLatlng.longitude,endLatlng.latitude,"终点",null, BNRoutePlanNode.CoordinateType.BD09LL);
        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);

            // 开发者可以使用旧的算路接口，也可以使用新的算路接口,可以接收诱导信息等
            // BaiduNaviManager.getInstance().launchNavigator(this, list, 1, true, new DemoRoutePlanListener(sNode));
            BaiduNaviManager.getInstance().launchNavigator(this, list, 1, navigation, new DemoRoutePlanListener(sNode),
                    eventListerner);
            Log.e(TAG, "routeplanToNavi: 发起导航算路" );
        }
    }

    BaiduNaviManager.NavEventListener eventListerner = new BaiduNaviManager.NavEventListener() {

        @Override
        public void onCommonEventCall(int what, int arg1, int arg2, Bundle bundle) {
            BNEventHandler.getInstance().handleNaviEvent(what, arg1, arg2, bundle);
        }
    };

    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onRoutePlanFailed() {

        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */

            for (Activity ac : activityList) {

                if (ac.getClass().getName().endsWith("BNDemoGuideActivity")) {
                    return;
                }
            }
            Intent intent = new Intent(NavigationMainActivity.this, BNDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);
            Log.e(TAG, "onJumpToNavigator: 调用诱导页面" );
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_location :
                setMyLocation();
                break;
            case R.id.bt_mn_navigation :
                naviType = false;
                routeplanToNavi(false);
                break;
            case R.id.bt_zs_navigation :
                naviType = true;
                routeplanToNavi(true);
                break;
            default:
        }
    }

    private void setMyLocation() {
        MapStatusUpdate statusUpdate = MapStatusUpdateFactory
                .newLatLngZoom(mLatlng,18f);
        baiduMap.setMapStatus(statusUpdate);
    }

    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location != null){
                MyLocationData.Builder builder = new MyLocationData.Builder();
                MyLocationData myLocationData = builder.accuracy(location.getRadius()).
                        direction(location.getDirection()).
                        latitude(location.getLatitude()).
                        longitude(location.getLongitude()).
                        build();
                baiduMap.setMyLocationData(myLocationData);
                mLatlng = new LatLng(location.getLatitude(),location.getLongitude());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                } else {
                    Toast.makeText(NavigationMainActivity.this, "缺少导航基本的权限!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            initNavi();
        } else if (requestCode == authComRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                }
            }
            routeplanToNavi(naviType);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }
}
