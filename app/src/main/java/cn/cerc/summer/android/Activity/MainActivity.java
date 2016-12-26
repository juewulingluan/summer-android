package cn.cerc.summer.android.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import cn.cerc.summer.android.Entity.Config;
import cn.cerc.summer.android.Entity.Menu;
import cn.cerc.summer.android.Interface.ConfigFileLoafCallback;
import cn.cerc.summer.android.Interface.JSInterfaceLintener;
import cn.cerc.summer.android.Receiver.MyBroadcastReceiver;
import cn.cerc.summer.android.Utils.AppUtil;
import cn.cerc.summer.android.Utils.Constans;
import cn.cerc.summer.android.Interface.JSInterface;
import cn.cerc.summer.android.Utils.PermissionUtils;

import cn.cerc.summer.android.Utils.XHttpRequest;
import cn.cerc.summer.android.View.DragPointView;
import cn.cerc.summer.android.View.MyWebView;
import cn.cerc.summer.android.View.ShowDialog;
import cn.cerc.summer.android.View.ShowPopupWindow;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.huagu.ehealth.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;

/**
 * 主界面
 */
public class MainActivity extends BaseActivity implements View.OnLongClickListener, View.OnClickListener, JSInterfaceLintener {

    public MyWebView webview;
    private ProgressBar progress;
    private DragPointView dragpointview;
    private ImageView image_tips;

    private boolean isGoHome = false;//是否返回home
    private boolean is_ERROR = false;//是否错误了

    private ImageView back, more;
    private TextView title;

    public String homeurl;//默认打开页

    private GoogleApiClient client;

    private String[] menus;//菜单
    private int[] menu_img = new int[]{R.mipmap.message, R.mipmap.msg_manager, R.mipmap.home, R.mipmap.setting, R.mipmap.wipe, R.mipmap.logout, R.mipmap.reload};
    private List<Menu> menulist;
    private ListPopupWindow lpw;//列表弹框

    public static final String NETWORK_CHANGE = "android.net.conn.NETWORK_CHANGE";
    public static final String APP_UPDATA = "com.fmk.huagu.efitness.APP_UPDATA";
    public static final String JSON_ERROR = "com.fmk.huagu.efitness.JSON_ERROR";

    private final int REQUEST_SETTING = 101;

    /**
     * 初始化广播
     */
    private void initbro() {
        //通过代码的方式动态注册MyBroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_CHANGE);
        filter.addAction(APP_UPDATA);
        filter.addAction(JSON_ERROR);
        //注册receiver
        registerReceiver(receiver, filter);
    }

    private static MainActivity mainactivity;

    public static MainActivity getInstance() {
        return mainactivity;
    }

    public void setHomeurl(String homeurl) {
        this.homeurl = homeurl;
        webview.loadUrl(homeurl);
    }

    /**
     * 推送消息的消息id， 点击通知栏打开
     */
    private String msgId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainactivity = this;

        initbro();
        InitView();

        startActivity(new Intent(this, StartActivity.class));

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * 查看消息的url
     */
    private String getMsgUrl(String read) {
        String url = settingShared.getString(Constans.SHARED_MSG_URL, "") + read;
        return AppUtil.buildDeviceUrl(url);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("msgId")) {
            msgId = intent.getStringExtra("msgId");
            String msgurl = getMsgUrl(".show") + "&msgId=" + msgId;
            Log.e("mainactivity", msgurl);
            webview.loadUrl(msgurl);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTING) {
            if (resultCode == RESULT_OK) {
                homeurl = data.getStringExtra("home");
                webview.loadUrl(homeurl);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        JPushInterface.onResume(this);

        Set<String> set = new HashSet<String>();
        set.add("android");
        JPushInterface.setAlias(getApplicationContext(), PermissionUtils.IMEI, tac);//极光推送设置别名
    }

    private TagAliasCallback tac = new TagAliasCallback() {
        @Override
        public void gotResult(int i, String s, Set<String> set) {
            switch (i) {
                case 0://成功
                    Log.e("regsert:", "设置成功");
                    // 建议这里往 SharePreference 里写一个成功设置的状态。成功设置一次后，以后不必再次设置了。
                    break;
                case 6002://失败
                    // 延迟 30 秒来调用 Handler 设置别名
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            JPushInterface.setAlias(MainActivity.this, PermissionUtils.IMEI, tac);
                        }
                    }, 30000);
                    break;
                default:
            }
        }
    };

    @SuppressLint("JavascriptInterface")
    private void InitView() {
        back = (ImageView) this.findViewById(R.id.back);
        more = (ImageView) this.findViewById(R.id.more);
        title = (TextView) this.findViewById(R.id.title);
        back.setOnClickListener(this);
        more.setOnClickListener(this);

        dragpointview = (DragPointView) this.findViewById(R.id.dragpointview);
        dragpointview.setEnable(false);
        dragpointview.setClickable(false);
        dragpointview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        dragpointview.setVisibility(View.INVISIBLE);

        progress = (ProgressBar) this.findViewById(R.id.progress);
        image_tips = (ImageView) this.findViewById(R.id.image_tips);

        webview = (MyWebView) this.findViewById(R.id.webview);

        webview.addJavascriptInterface(new JSInterface(this), "JSobj");//JSobj 供web端js调用标识，修改请通知web开发者

        webview.setWebViewClient(new WebViewClient() {

            @Override
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse webreq = webview.WebResponseO(request.getUrl().toString());
                return webreq != null ? webreq : super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                WebResourceResponse webreq = webview.WebResponseO(url);
                return webreq != null ? webreq : super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request.getUrl().toString().startsWith("http:") || request.getUrl().toString().startsWith("https:")) {
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                }
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.toString().startsWith("http:") || url.startsWith("https:")) {
                    return super.shouldOverrideUrlLoading(view, url);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!AppUtil.getNetWorkStata(view.getContext())) return;
                Log.e("cururl", url);
//                if (url.contains(".html")){
//                    String local = XHttpRequest.getInstance().GetHtml(url, MainActivity.this);
//                    if (!TextUtils.isEmpty(local))
//                        url = local;
//                }
                is_ERROR = false;
                if (Config.getConfig() == null) return;
                is_exit = false;
                isGoHome = false;
                for (int i = 0; i < Config.getConfig().getHomePagers().size(); i++) {
                    if (url.contains(Config.getConfig().getHomePagers().get(i).getHomeurl())) {
                        isGoHome = true;
                        is_exit = Config.getConfig().getHomePagers().get(i).is_home();
                    }
                }
                progress.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                is_ERROR = true;
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                is_ERROR = true;
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (is_ERROR) {
                    title.setText("出错了");
                    image_tips.setVisibility(View.VISIBLE);
                } else {
                    title.setText(webview.getTitle());
                    image_tips.setVisibility(View.GONE);
                }
                if (isGoHome) {
                    webview.clearHistory();
                    webview.clearCache(true);
                    back.setVisibility(View.INVISIBLE);
                } else {
                    back.setVisibility(View.VISIBLE);
                }
                progress.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                super.onPermissionRequest(request);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    progress.setProgress(newProgress, true);
                else progress.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
                super.onReceivedTouchIconUrl(view, url, precomposed);
            }
        });
        webview.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_BACK) && event.getAction() == KeyEvent.ACTION_UP) {
                    if (is_exit) {
                        Intent home = new Intent(Intent.ACTION_MAIN);
                        home.addCategory(Intent.CATEGORY_HOME);
                        startActivity(home);
                    } else {
                        if (webview.canGoBack()) webview.goBack();// 返回键退回
                        else finish();
                    }
                    return true;
                } else
                    return false;
            }
        });
        webview.setOnLongClickListener(this);
    }

    /**
     * 退出点击的第一次的时间戳
     */
    private long timet = 0;
    /**
     * 是否直接退出
     */
    private boolean is_exit = false;

    @Override
    public boolean onLongClick(View v) {
        //webview的长按事件，设置true后webview将不会触发长按复制动作
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                webview.goBack();
                break;
            case R.id.more:
                showPopu(more);
                break;
            default:
                break;
        }
    }

    public void Update() {
        try {//检查是否需要更新
            if (!AppUtil.getVersionName(this).equals(Config.getConfig().getAppVersion())) {
                ShowDialog.getDialog(this).UpDateDialogShow();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean islogin = false;

    /**
     * 显示菜单栏的窗口
     *
     * @param view
     */
    public void showPopu(View view) {
        menulist = new ArrayList<Menu>();
        menus = getResources().getStringArray(R.array.menu);
        ss: for (int i = 0; i < menus.length; i++) {
            if ("退出登录".equals(menus[i]) && !islogin) continue ss;
            Menu menu = new Menu(i == 0 ? 12 : 0, menus[i], menu_img[i]);
            menulist.add(menu);
        }


        lpw = ShowPopupWindow.getPopupwindow().show(this, menulist);
        lpw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        webview.loadUrl(getMsgUrl("") + "&unread=1");
                        break;
                    case 1:
                        webview.loadUrl(getMsgUrl(""));
                        break;
                    case 2:
                        webview.loadUrl(homeurl);
                        break;
                    case 3:
                        startActivityForResult(new Intent(MainActivity.this, SettingActivity.class), REQUEST_SETTING);
                        break;
                    case 4:
                        clearCacheFolder(MainActivity.this.getCacheDir(), System.currentTimeMillis());
                        break;
                    case 5:
                        if (islogin){
                            webview.loadUrl("http://ehealth.lucland.com/forms/Login.exit");
                            webview.clearCache(true);
                            webview.clearHistory();
                        } else
                            webview.reload();
                        break;
                    case 6:
                        webview.reload();
                        break;
                }
                lpw.dismiss();
            }
        });
        lpw.setAnchorView(view);
        lpw.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 111:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PowerManager pManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    pManager.reboot("重启");
                } else {
                    ActivityCompat.requestPermissions(this, permissions, requestCode);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);//注销广播
    }

    /**
     * 广播接收者
     */
    private MyBroadcastReceiver receiver = new MyBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("xxxx", "mainactivity " + action);
            switch (action) {
                case NETWORK_CHANGE:
                    if (AppUtil.getNetWorkStata(context)) webview.reload();
                    else ShowDialog.getDialog(context).showTips();
                    break;
                case APP_UPDATA://有更新
                    break;
                default:
                    Log.e("mainact", "mainactivity:接收到广播");
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        JPushInterface.onPause(this);
    }

    /**
     * 清除缓存
     */
    private int clearCacheFolder(File dir, long numDays) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    if (child.isDirectory())
                        deletedFiles += clearCacheFolder(child, numDays);
                    if (child.lastModified() < numDays)
                        if (child.delete()) deletedFiles++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deletedFiles;
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW).setObject(object).setActionStatus(Action.STATUS_TYPE_COMPLETED).build();
    }

    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void LoginOrLogout(boolean islogin) {
        this.islogin = islogin;
    }

    public void reload(int scales) {
        webview.getSettings().setTextZoom(Integer.valueOf(settingShared.getInt(Constans.SCALE_SHAREDKEY, 90)));
        webview.reload();
    }
}
