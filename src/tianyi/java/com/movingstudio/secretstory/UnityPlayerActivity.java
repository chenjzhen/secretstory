package com.movingstudio.secretstory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.commonsdk.UMConfigure;
import com.umeng.commonsdk.UMU3DCommonSDK;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.UMShareAPI;
import com.unity3d.player.UnityPlayer;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.MobclickAgent.EScenarioType;
import com.umeng.commonsdk.UMConfigure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zeus.sdk.AresCode;
import com.zeus.sdk.AresSDK;
import com.zeus.sdk.ad.AresAdSdk;
import com.zeus.sdk.ad.base.AdCallbackType;
import com.zeus.sdk.ad.base.AdType;
import com.zeus.sdk.ad.base.AresAdEvent;
import com.zeus.sdk.ad.base.IAdCallbackListener;
import com.zeus.sdk.ad.base.IAdListener;
import com.zeus.sdk.base.AresAwardCallback;
import com.zeus.sdk.base.AresInitListener;
import com.zeus.sdk.base.AresPayListener;
import com.zeus.sdk.base.AresPlatform;
import com.zeus.sdk.base.SwichType;
import com.zeus.sdk.param.AresToken;
import com.zeus.sdk.param.PayParams;
import com.zeus.sdk.param.UserExtraData;
import com.zeus.sdk.test.AnalyticsActivity;
import com.zeus.sdk.test.CDKEYDialog;
import com.zeus.sdk.tools.InnerTools;
import com.zeus.sdk.tools.SdkTools;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class UnityPlayerActivity extends Activity
        implements DialogInterface.OnDismissListener {
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code
    private String _gameObjectToNotify;
    ProgressDialog mWaitProgress = null;
    static final String TAG = "Secret In Story";
    static public String mLocalString = "";
    int sceneId = 0;
    boolean enableToast = false;
    boolean rewarded = false;
    // true if dialog already open
    //private boolean alreadyAskedForStoragePermission = false;

    {
        /*
        //微信 wx12342956d1cab4f9,a5ae111de7d9ea137e88a5e02c07c94d
        PlatformConfig.setWeixin(getString(R.string.wx_app_id),getString(R.string.wx_app_secret));
        //新浪微博
        PlatformConfig.setSinaWeibo(getString(R.string.sina_app_id), getString(R.string.sina_app_secret),getString(R.string.sina_app_redirecturl));
        PlatformConfig.setTwitter(getString(R.string.twitter_app_id),getString(R.string.twitter_app_secret));
        */

        PlatformConfig.setWeixin("wxeac9c7ba1045539a", "64020361b8ec4c99936c0e3999a9f249");
        //新浪微博
        PlatformConfig.setSinaWeibo("2489447285", "527f5286205e813b6cd410e36e598986", "http://www.weibo.com/rocheon\n");
        PlatformConfig.setTwitter("NwDjSVm3hB8P3aOd34XS0Gfpx", "fn9aOQa0DRLqGVmcn0k3jtoXRvPt7xqHtom1d6FdwRIGWq9iIV");
    }

    // Setup activity layout
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy

        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();

        MultiDex.install(this);

        UMConfigure.init(this, "5c04f20bb465f5e7d7000084", "tykj", UMConfigure.DEVICE_TYPE_PHONE, null);
        MobclickAgent.setScenarioType(this, EScenarioType.E_UM_NORMAL);
        // 将默认Session间隔时长改为40秒。
        MobclickAgent.setSessionContinueMillis(1000*40);
        //Helper.CheckValid("2021-6-1");

        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                initLeaderBoard();
            }
        }, 1000);

        this.initSDK();
        this.initAds();
        //在SDK初始化后，调用onCreate生命周期
        AresSDK.getInstance().onCreate();
        //进入游戏主页后，调用验证掉单接口z
        checkPay();

        adsRequireStart();//控制插页广告时间间隔
    }

    public void startPurchase(String gameObjectToNotify) {
        _gameObjectToNotify = gameObjectToNotify;
    }


    private void showToast(String message) {
        if(enableToast)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Quit Unity
    @Override
    protected void onDestroy() {
        mUnityPlayer.quit();
        super.onDestroy();
        AresSDK.getInstance().onDestroy();
        //在每个要展示广告activity的onDestroy中调用此接口
        AresAdSdk.getInstance().closeAd(AdType.NONE);
    }

    // Pause Unity
    @Override
    protected void onPause() {
        super.onPause();
        mUnityPlayer.pause();
        AresSDK.getInstance().onPause();
        MobclickAgent.onPause(this);
    }

    // Resume Unity
    @Override
    protected void onResume() {
        super.onResume();
        mUnityPlayer.resume();
        AresSDK.getInstance().onResume();
        AresAdSdk.getInstance().setCurrentActivity(this);

        MobclickAgent.onResume(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUnityPlayer.start();
        AresSDK.getInstance().onStart();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        AresSDK.getInstance().onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUnityPlayer.stop();
        AresSDK.getInstance().onStop();
    }

    // Low Memory Unity
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            mUnityPlayer.lowMemory();
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
        AresSDK.getInstance().onNewIntent(newIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AresSDK.getInstance().onActivityResult(requestCode, resultCode, data);

        Log.e(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        super.onActivityResult(requestCode, resultCode, data);

        //UMShareAPI.get(this).onActivityResult(requestCode,resultCode, intent);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);

        dismissWaitDialog();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
        AresSDK.getInstance().onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AresSDK.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }


    // Notify Unity of the focus change.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
        final View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mUnityPlayer.injectEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return mUnityPlayer.injectEvent(event);
        }
        return mUnityPlayer.injectEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mUnityPlayer.injectEvent(event);
    }

    /*API12*/
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mUnityPlayer.injectEvent(event);
    }

    public boolean hasInternetConnection() {
        try {
            String url = "https://www.google.com";
            if (isSimpleChinese()) url = "https://www.baidu.com";//For Simple Chinese
            //waitProgress.show();
            HttpURLConnection urlc = (HttpURLConnection) (new URL(url).openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
        } catch (IOException e) {
            showMessageWithLocalString("Network Failed");
        }
        return false;
    }

    public boolean isSimpleChinese() {
        return true;
    }

    public boolean isTraditionChinese() {
        return false;
    }

    void showMessage(final String msg) {
        this.runOnUiThread(new Runnable() {
            public void run() {
				/*
				AlertDialog.Builder bld = new AlertDialog.Builder(PuzzleNumbers.this);
				bld.setMessage(msg);
				bld.setNeutralButton("OK", null);
				bld.create().show();
				*/

                AlertDialog.Builder builder = new AlertDialog.Builder(UnityPlayerActivity.this);
                //builder.setTitle("My Title");
                builder.setMessage(msg);
                builder.setPositiveButton("OK", null);
                AlertDialog dialog = builder.show();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                });

                LinearLayout.LayoutParams buttonParams;
                Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonParams = (LinearLayout.LayoutParams) buttonPositive.getLayoutParams();
                buttonParams.weight = 1;
                buttonParams.width = buttonParams.MATCH_PARENT;
                buttonPositive.setLayoutParams(buttonParams);

                TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
                messageText.setGravity(Gravity.CENTER);

            }
        });
    }

    void showWaitDialog() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if (mWaitProgress == null) {
                    mWaitProgress = ProgressDialog.show(UnityPlayerActivity.this, "", "", true);
                    mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mWaitProgress.setCancelable(false);
                    mWaitProgress.setOnKeyListener(onKeyListener);
                    mWaitProgress.setContentView(R.layout.progress_dialog);
                    //waitProgress.addContentView(new Spinner(getContext()), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    mWaitProgress.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } else {
                    mWaitProgress.show();
                }
            }
        });

		/*
		this.runOnUiThread(new Runnable()
		{
			public void run()
			{
				if(mWaitProgress == null)
				{
					mWaitProgress = ProgressDialog.show(UnityPlayerActivity.this, "", "", true);
					mWaitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				}
				else
				{
					mWaitProgress.show();
				}
			}
		});
		*/
    }

    private DialogInterface.OnKeyListener onKeyListener = new DialogInterface.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                dismissWaitDialog();
            }
            return false;
        }
    };


    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    void dismissWaitDialog() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if (mWaitProgress != null && mWaitProgress.isShowing()) mWaitProgress.dismiss();
            }
        });
    }

    public void showMessageWithLocalString(String msg) {
        UnityPlayer.UnitySendMessage("Main Camera", "getLocalString", msg);
    }

    public void doShowLocalString(String value) {
        mLocalString = value; //Get new one
        showMessage(value);
    }

    public void initPay(int pi) { }

    public void exitPay() { }

    public void restore() {
//        if (hasInternetConnection()) {
//            if (mPayId == 0) {
//                googleRestore();
//            }
//        }
    }

    public void purchase() {
//        if (hasInternetConnection()) {
//            if (mPayId == 0) {
//
//            }
//        }
    }

    void googlePurchase() {
//        if (isNoAds == false) {
//            if (setupFinished) {
//                /* TODO: for security, generate your payload here for verification. See the comments on
//                 *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
//                 *        an empty string, but on a production app you should carefully generate this. */
//                String payload = "";
//                mHelper.launchPurchaseFlow(this, getString(R.string.sku_removeads), RC_REQUEST,
//                        mPurchaseFinishedListener, payload);
//                showWaitDialog();
//            } else {
//                showMessageWithLocalString("Purchase Cancel");
//            }
//        } else {
//            //MessageBox restore;
//            //Log.e(TAG, "You Have purchased, please Restore");
//            showMessageWithLocalString("Purchase Failed");
//        }
    }

    public void googleRestore() {
//        if (isNoAds == true) {
//            UnityPlayer.UnitySendMessage("Scene", "DidPurchaseFinish", "yes");
//            showMessageWithLocalString("Purchase Success");
//        } else {
//            showMessageWithLocalString("Restore Failed");
//        }
    }

    ////////////////////////////////////////////////Zeus////////////////////////////////////////////////
    /**
     * 云步SDK初始化（必接）
     */
    private void initSDK() {
        AresPlatform.getInstance().init(this, new AresInitListener() {
            /**
             * 切换账号的回调
             * @param data 新账号的信息
             */
            @Override
            public void onSwitchAccount(AresToken data) {
                // 游戏中通过SDK切换到新账号的回调，游戏收到该回调，需要引导用户重新登录，重新加载该新用户对应的角色数据
                //mResultView.setText("切换账号成功，请登录游戏服务:" + "UserID=" + data.getUserID() + ",UserName=" + data.getUserName() + ",Token=" + data.getToken());
                showToast("切换账号成功，请登录游戏服务:" + "UserID=" + data.getUserID() + ",UserName=" + data.getUserName() + ",Token=" + data.getToken() );
                //Log.d(TAG, "data:" + JSON.toJSONString(data));

            }

            /**
             * 支付的回调
             * @param code 结果code
             * @param msg 当code为AresCode.CODE_PAY_SUCCESS时,msg为支付时传入的参数,以json格式返回;code为其他时,msg为支付失败消息
             */
            @Override
            public void onPayResult(int code, String msg) {
                Log.d(TAG, "pay result. code:" + code + ";msg:" + msg);
                switch (code) {
                    case AresCode.CODE_PAY_SUCCESS:
                        //支付成功,当回调支付成功时,msg为支付时传入的商品ID和订单ID参数,以json格式返回;
                        //如:msg:{"orderID":"","productId":"1"}
                        showToast("支付成功 ---- Demo提示,请修改");
                        //通知云步SDK，游戏收到支付成功的回调
                        JSONObject jsonObject = JSON.parseObject(msg);
                        SdkTools.gameReceivePayResultEvent(jsonObject.getString("orderID"), jsonObject.getString("productId"));
                        UnityPlayer.UnitySendMessage("ShopCanvas", "OnPricePaid", jsonObject.getString("productId"));
                        break;

                    case AresCode.CODE_PAY_FAIL:
                        UnityPlayer.UnitySendMessage("ShopCanvas", "OnPricePaid", "failed");
                        showToast("付失败 ---- Demo提示,请修改");
                        break;

                    case AresCode.CODE_PAY_CANCEL:
                        showToast("支付取消 ---- Demo提示,请修改");
                        break;

                    case AresCode.CODE_PAY_UNKNOWN:
                        showToast("未知错误 ---- Demo提示,请修改");
                        break;
                }
            }

            /**
             * 登出的回调
             */
            @Override
            public void onLogout() {
                // 调用主动登出或用户切换渠道账号会回调此方法
                //mResultView.setText("未登录");
                showToast("未登录");
            }

            /**
             * 登录回调
             * @param code 结果code
             * @param data 当code为AresCode.CODE_LOGIN_SUCCESS时,返回登录信息
             */
            @Override
            public void onLoginResult(int code, AresToken data) {
                switch (code) {
                    case AresCode.CODE_LOGIN_SUCCESS:
                        // 登录成功
                        //mResultView.setText("登录账号成功，请登录游戏服务:" + "UserID=" + data.getUserID() + ",UserName=" + data.getUserName() + ",Token=" + data.getToken());
                        //Log.d(TAG, "data:" + JSON.toJSONString(data));
                        // 请登录游戏服务器
                        showToast("登录账号成功，请登录游戏服务:" + "UserID=" + data.getUserID() + ",UserName=" + data.getUserName() + ",Token=" + data.getToken());
                        break;
                    case AresCode.CODE_LOGIN_FAIL:
                        showToast("登录账号失败");
                        //mResultView.setText("登录账号失败");
                        break;
                }
            }

            /**
             * 初始化回调
             * @param code
             * @param msg
             */
            @Override
            public void onInitResult(int code, String msg) {
                Log.d(TAG, "init result.code:" + code + ";msg:" + msg);
                switch (code) {
                    case AresCode.CODE_INIT_SUCCESS:
                        showToast("初始化成功 ---- Demo提示,请修改");
                        break;
                    case AresCode.CODE_INIT_FAIL:
                        showToast("初始化失败 ---- Demo提示,请修改");
                        break;
                }
            }
        });
    }

    /**
     * 游戏退出确认（必接）
     */
    public void exit() {
        AresPlatform.getInstance().exitSDK();
    }

    /**
     * 登录渠道(单机游戏不需要接)
     */
    public void login(View v) {
        AresPlatform.getInstance().login(this);
    }

    /**
     * 登出(单机游戏不需要接)
     */
    public void logout(View v) {
        AresPlatform.getInstance().logout();
    }

    /**
     * 提交游戏角色参数(单机游戏不需要接)
     */
    public void submitUserExtra(View v) {
        submitExtraData(UserExtraData.TYPE_CREATE_ROLE);
    }

    private void submitExtraData(int dataType) {
        long roleCreateTime = System.currentTimeMillis() / 1000;
        UserExtraData data = new UserExtraData();
        //调用场景类型
        //1.选择服务器：UserExtraData.TYPE_SELECT_SERVER
        //2.创建角色：UserExtraData.TYPE_CREATE_ROLE
        //3.进入游戏：UserExtraData.TYPE_ENTER_GAME
        //4.等级提升：UserExtraData.TYPE_LEVEL_UP
        //5.退出游戏：UserExtraData.TYPE_EXIT_GAME
        data.setDataType(dataType);
        //当前角色身上拥有的游戏币数量
        data.setMoneyNum(100);
        //单位是秒，此处是参考，请传真实的角色创建时间，并且每次传递的值固定，建议从游戏服务器获取真实的角色创建时间，不能传随机值或当前系统时间值
        data.setRoleCreateTime(roleCreateTime);
        //角色id
        data.setRoleID("role_100");
        //角色名称
        data.setRoleName("test_112");
        //角色等级
        data.setRoleLevel("10");
        //服务器id
        data.setServerID("10");
        //服务器名称
        data.setServerName("server_10");
        //VIP等级
        data.setVip("vip1");
        AresPlatform.getInstance().submitExtraData(data);
    }

    /**
     * 游戏内购（必接）
     */
    public void pay(int productId) {
        if(productId == 1)
        {
            PayParams params = new PayParams();
            // 购买数量，固定1（必须）
            params.setBuyNum(1);

            // 充值金额(单位：元)（必须）
            params.setPrice(1);

            // 充值商品id，productId必须是数字，且必须是1、2、3、4、5......（必须）
            params.setProductId("1");

            // 商品名称，比如：100元宝，500钻石，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductName("6个币");

            // 商品描述，比如：充值100元宝，赠送20元宝，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductDesc("获得6个币");

            // 当前玩家身上拥有的游戏币数量（网游必须，单机不用传）
            //params.setCoinNum(100);

            // 透传参数，充值成功，回调通知游戏服务器的时候，会原封不动返回。（透传字段长度不超过64）（网游可选，单机不用传）
            //params.setExtraMessage("ExtraMessage");

            // 服务器发货模式的订单标识，订单ID要包含支付的信息，订单ID长度不能超过30个字符，不可重复，且只能由字母大小写和数字组成，字母区分大小写（网游必须，单机不用传）
            //params.setOrderID("T" + System.currentTimeMillis());

            AresPlatform.getInstance().pay(this, params);
        }
        else if(productId == 2)
        {
            PayParams params = new PayParams();
            // 购买数量，固定1（必须）
            params.setBuyNum(1);

            // 充值金额(单位：元)（必须）
            params.setPrice(6);

            // 充值商品id，productId必须是数字，且必须是1、2、3、4、5......（必须）
            params.setProductId("2");

            // 商品名称，比如：100元宝，500钻石，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductName("解锁所有提示");

            // 商品描述，比如：充值100元宝，赠送20元宝，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductDesc("解锁所有提示");

            // 当前玩家身上拥有的游戏币数量（网游必须，单机不用传）
            //params.setCoinNum(100);

            // 透传参数，充值成功，回调通知游戏服务器的时候，会原封不动返回。（透传字段长度不超过64）（网游可选，单机不用传）
            //params.setExtraMessage("ExtraMessage");

            // 服务器发货模式的订单标识，订单ID要包含支付的信息，订单ID长度不能超过30个字符，不可重复，且只能由字母大小写和数字组成，字母区分大小写（网游必须，单机不用传）
            //params.setOrderID("T" + System.currentTimeMillis());

            AresPlatform.getInstance().pay(this, params);
        }
        else if(productId == 3)
        {
            PayParams params = new PayParams();
            // 购买数量，固定1（必须）
            params.setBuyNum(1);

            // 充值金额(单位：元)（必须）
            params.setPrice(10);

            // 充值商品id，productId必须是数字，且必须是1、2、3、4、5......（必须）
            params.setProductId("3");

            // 商品名称，比如：100元宝，500钻石，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductName("解锁提示和60个币");

            // 商品描述，比如：充值100元宝，赠送20元宝，不可包含特殊字符，只能包含中文、英文字母大小写、数字、下划线。（必须）
            params.setProductDesc("解锁所有提示并获得60个币");

            // 当前玩家身上拥有的游戏币数量（网游必须，单机不用传）
            //params.setCoinNum(100);

            // 透传参数，充值成功，回调通知游戏服务器的时候，会原封不动返回。（透传字段长度不超过64）（网游可选，单机不用传）
            //params.setExtraMessage("ExtraMessage");

            // 服务器发货模式的订单标识，订单ID要包含支付的信息，订单ID长度不能超过30个字符，不可重复，且只能由字母大小写和数字组成，字母区分大小写（网游必须，单机不用传）
            //params.setOrderID("T" + System.currentTimeMillis());
            AresPlatform.getInstance().pay(this, params);
        }

    }

    /**
     * 验证是否存在支付掉单（单机游戏必接）
     */
    public void checkPay() {
        AresPlatform.getInstance().checkPay(new AresPayListener() {
            @Override
            public void onResult(int code, String productId) {
                Log.d(TAG, "code=" + code + ", msg=" + productId);
                if (code == AresCode.CODE_PAY_SUCCESS) {
                    //验证成功，根据回调的商品id发货
                    //mResultView.setText("掉单验证成功：" + productId);
                    showToast("掉单验证成功：" + productId);
                } else {
                   //mResultView.setText("未查询到掉单");
                    showToast("未查询到掉单：" + productId);
                }
            }
        });
    }
    ////////////////////////////////////////////////Zeus////////////////////////////////////////////////

    ///////////////////////////////////////////////Wechat Pay//////////////////////////////////////////
    //private static IWXAPI api = null;
    public void initWechatPay() {
        //api = WXAPIFactory.createWXAPI(this, null);
        //api.registerApp(getString(R.string.wx_app_id));
    }

    public void wechatPurchase() {
        //Log.e("PAY_GET", "Get Pay");
        //String url =  "http://www.rocheon1.byethost22.com/wxpayapi_php_v3/example/test.php"
        ;//"http://wxpay.weixin.qq.com/pub_v2/app/app_pay.php?plat=android";
    /*
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            showWaitDialog();
            //url =new URL("http://ec2-52-25-90-205.us-west-2.compute.amazonaws.com/wxpayapi_php_v3_ilbwy/example/test.php");
            url =new URL("http://ec2-52-25-90-205.us-west-2.compute.amazonaws.com/wxpayapi_php_v3_sis/example/appquery.php");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("connection", "close");
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            //urlConnection.setDoOutput(true);
            //urlConnection.setDoOutput(false);
            urlConnection.connect();
            dismissWaitDialog();
            InputStream in =  urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
            StringBuffer sb = new StringBuffer();

            String str = "";
            while ((str = reader.readLine()) != null)
            {
                sb.append(str).append("\n");
            }
            Log.e("PAY_GET: ", sb.toString());

            JSONObject json = new JSONObject(sb.toString());
            if (null != json && !json.has("retcode")) {

                PayReq req = new PayReq();
                req.appId = json.getString("appid");
                req.partnerId = json.getString("partnerid");
                req.prepayId = json.getString("prepayid");
                req.nonceStr = json.getString("noncestr");
                req.timeStamp = json.getString("timestamp");
                req.packageValue = json.getString("package");;//Only For Android
                req.sign = json.getString("sign");
                req.extData = json.getString("extdata"); // optional
                //Toast.makeText(PuzzleNumbers.this, "", Toast.LENGTH_SHORT).show();
                Log.e("PAY_GET", "req.appId = " + req.appId.toString());

                api.sendReq(req);

            } else {
                Log.e("PAY_GET", "json is null" + json.getString("retmsg"));
                //Toast.makeText(PuzzleNumbers.this, "json is null"+json.getString("retmsg"), Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            dismissWaitDialog();
            e.printStackTrace();
            showMessageWithLocalString("Server Failed");
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            dismissWaitDialog();
        }
        */
    }

    public void wechatRestore() {

    }
    /*
        @Override
        protected void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
            setIntent(intent);
            api.handleIntent(intent, this);
        }

        @Override
        public void onReq(BaseReq req) {
        }

        @Override
        public void onResp(BaseResp resp) {
            Log.e(TAG, "onPayFinish, errCode = " + resp.errCode);

            if (resp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
                //AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //builder.setTitle("COMMAND_PAY_BY_WX");
                //builder.setMessage("onPayFinish, errCode = " + resp.errCode);
                //builder.show();
                UnityPlayer.UnitySendMessage("Scene", "DidPurchaseFinish","yes");
                getLocalString("Purchase Success");
            }
        }
    */
    ////////////////////////////////////////////////LeaderBoard//////////////////////////////////////////
    public void initLeaderBoard() { }
    public void reportScore(int score) { }
    public void showLeaderboard() { }
    private boolean isSignedIn() {
        return false;
    }
    ////////////////////////////////////////////////LeaderBoard//////////////////////////////////////////
    public void aboutMe() {
        if (isSimpleChinese()) {
            //Simple code for ope web browser in android
            Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
            webPageIntent.setData(Uri.parse("http://weibo.com/rocheon"));
            startActivity(webPageIntent);
        } else {
            //Simple code for ope web browser in android
            Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
            webPageIntent.setData(Uri.parse("http://www.rocheon.com"));
            startActivity(webPageIntent);
        }
    }

    public void more() {
		 /*
		 Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
		 if(this.publishPlatform == PublishPlatform.GOOGLEPLAY )
		 {
			webPageIntent.setData(Uri.parse("http://play.google.com/store/search?q=pub:Luo+Zhi+En"));
		 }
		 else if(this.publishPlatform == PublishPlatform.QIHU360)
		 {
			webPageIntent.setData(Uri.parse("http://rocheon.com/?page_id=2"));
		 }
		 else
		 {
			webPageIntent.setData(Uri.parse("http://rocheon.com/?page_id=2"));
		 }
		 startActivity(webPageIntent);
		 */

        String languageDefault = Locale.getDefault().getLanguage();
        if (isSimpleChinese()) {
            //Simple code for ope web browser in android
            Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
            webPageIntent.setData(Uri.parse("http://blog.sina.com.cn/s/blog_b38c47370102vdbk.html"));
            startActivity(webPageIntent);
        } else {
			/*
			Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.android.vending");
			ComponentName comp = new ComponentName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity"); // package name and activity
			launchIntent.setComponent(comp);
			launchIntent.setData(Uri.parse("market://search?q=pub:Luo+Zhi+En"));
			startActivity(launchIntent);
			*/

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://search?q=pub:Luo+Zhi+En"));
            PackageManager pm = getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            boolean isWeb = true;
            for (int a = 0; a < list.size(); a++) {
                ResolveInfo info = list.get(a);
                ActivityInfo activity = info.activityInfo;
                if (activity.name.contains("com.google.android")) {
                    ComponentName name = new ComponentName(
                            activity.applicationInfo.packageName,
                            activity.name);
                    Intent i = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://search?q=pub:Luo+Zhi+En"));
                    //i.addCategory(Intent.CATEGORY_LAUNCHER);
                    //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    //| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    i.setComponent(name);
                    startActivity(i);
                    isWeb = false;
                    //finish();
                }
            }
            if (isWeb) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pub:Luo+Zhi+En")));
            }


		   /*
			final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:Luo+Zhi+En")));
			} catch (android.content.ActivityNotFoundException anfe) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pub:Luo+Zhi+En")));
			}
			*/
        }
/*
			//Simple code for ope web browser in android
			Intent webPageIntent = new Intent(Intent.ACTION_VIEW);
			webPageIntent.setData(Uri.parse("https://play.google.com/store/search?q=pub:Luo+Zhi+En"));
			startActivity(webPageIntent);
*/
    }

    /*************************************** Ads Require *********************************************************/
    private boolean adsRequireStarted = false;
    private android.os.Handler adsRequireHandler = new android.os.Handler(Looper.getMainLooper());
    private boolean enableAdsRequire = false;
    private Runnable adsRequireRunnable = new Runnable() {
        @Override
        public void run() {
            enableAdsRequire = true;
            if (adsRequireStarted) {
                adsRequireStart();
            }
        }
    };

    public void adsRequireStop() {
        adsRequireStarted = false;
        adsRequireHandler.removeCallbacks(adsRequireRunnable);
    }

    public void adsRequireStart() {
        adsRequireStarted = true;
        adsRequireHandler.postDelayed(adsRequireRunnable, 2000*60);//每一次屏间隔2分钟
    }
    /*************************************** Ads Require *********************************************************/
    public boolean AskForShowRewardBasedVideo() { return false; }
    public boolean AskForShowHintVideo() { return false; }
    public boolean IsHintVideoReady() { return false; }


    /***************************************zeus ads *********************************************************/
    public void initAds()
    {
        //初始化广告SDK
        AresAdSdk.getInstance().init(this, new IAdListener() {
            @Override
            public void onInitResult(int code, String msg) {
                showToast("onInitResult: code=" + code + ",msg=" + msg);
            }

            @Override
            public void onAward(String productId) {
                OnRewardAdsClosed(4,rewarded);
                rewarded = false;
            }
        });

        //添加广告SDK激励性广告的回调（非激励性广告无回调）
        AresAdSdk.getInstance().setAdCallbackListener(new IAdCallbackListener() {
            @Override
            public void onAdCallback(AdType adType, AdCallbackType adCallbackType, String eventType) {
                showToast("onAdCallback: adType=" + adType + ",adCallbackType=" + adCallbackType + ",eventType=" + eventType);
                if (adType == AdType.VIDEO && adCallbackType == AdCallbackType.PLAY_FINISH) {
                    //激励视频广告播放完成
                    rewarded = true;
                } else if (adType == AdType.INTERSTITIAL && adCallbackType == AdCallbackType.CLICK_AD) {
                    //激励插屏广告被点击
                    rewarded = true;
                }
            }
        });
    }

    public void ShowInterstialAd() {
        if(enableAdsRequire)//2分钟内不再展示新广告
        {
            AresAdSdk.getInstance().showInterstitial(this, AresAdEvent.PAGE_FAIL);
            enableAdsRequire = false;
        }
    }

    public boolean IsRewardBasedVideoReady() {
        boolean ready = (AresAdSdk.getInstance().hasAwardAd(AresAdEvent.PAGE_GIFT) != AdType.NONE);
        showToast("IsRewardBasedVideoReady: " + ready);
        return ready;
    }
    public boolean AskForShowKeyVideo() {
        if(IsRewardBasedVideoReady())
        {
            rewarded = false;
            AresAdSdk.getInstance().showVideo(this, AresAdEvent.PAGE_GIFT, true);
            return true;
        }
        return false;
    }

    public void OnRewardAdsClosed(int placementId, boolean rewarded) {
        if (placementId == 1) {
            if (rewarded) {
                UnityPlayer.UnitySendMessage("Scene", "OnRewardBasedVideoAdDidClose", "AddHP");
            } else {
                UnityPlayer.UnitySendMessage("Scene", "OnRewardBasedVideoAdDidClose", "GameOver");
            }
        } else if (placementId == 2) {
            if (rewarded) {
                UnityPlayer.UnitySendMessage("Scene", "OnRewardBasedVideoAdDidClose", "ShowHint");
            } else {
                UnityPlayer.UnitySendMessage("Scene", "OnRewardBasedVideoAdDidClose", "HideHint");
            }
        } else if (placementId == 4) {
            if (rewarded) {
                UnityPlayer.UnitySendMessage("ShopCanvas", "OnRewardBasedVideoAdDidClose", "AddKey");
            } else {
                UnityPlayer.UnitySendMessage("ShopCanvas", "OnRewardBasedVideoAdDidClose", "DontAddKey");
            }
        }
    }

    /***************************************zeus ads *********************************************************/

    void setScene(int id) {
        sceneId = id;
    }
    public void initSocial() { }

    public void TestScene(String sceneName) {
        UnityPlayer.UnitySendMessage("Main Camera", "TestScene", sceneName);
    }


    public int getChannelId()
    {
        return Helper.getChannelId(getBaseContext());
    }

    public boolean IsNoAdsVersion()
    {
        return false;
    }

}
