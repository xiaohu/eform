/* EForm - Electronic Form System
 *
 * Copyright (C) 2013 Wu Xiaohu. All rights reserved.
 * Copyright (C) 2013 Cansiny Trade Co.,Ltd. All rights reserved.
 */
package com.cansiny.eform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.cansiny.eform.Administrator.AdministratorListener;
import com.cansiny.eform.Member.MemberListener;
import com.cansiny.eform.Utils.Device;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextSwitcher;
import android.widget.TextView;


public class HomeActivity extends Activity
    implements OnClickListener, OnLongClickListener, MemberListener, AdministratorListener
{
    static public final int HOME_VIEW_ID_BASE = 0x3F000001;
    static public final int ITEM_VIEW_ID_BASE = 0x3F100001;

    static public final int ITEM_ACTIVITY_REQUEST_CODE = 1;
	
    private AtomicInteger atomic_int;
    private Customer home_info;
    private int curr_slogan = 0;
    private long slogan_lasttime;
    private long admin_logintime;
    private Handler  handler = new Handler();
    private Runnable runable = new Runnable() {
	@Override
	public void run() {
	    long currtime = System.currentTimeMillis();

	    if (currtime - slogan_lasttime >= 5000) {
		slogan_lasttime = currtime;
		TextSwitcher switcher = (TextSwitcher) findViewById(R.id.slogan_switcher);
		switcher.setText(home_info.slogans.get(curr_slogan++));
		if (curr_slogan >= home_info.slogans.size())
		    curr_slogan = 0;
	    }

	    Preferences prefs = Preferences.getPreferences();
	    if (prefs.getAdministratorAutoLogout()) {
		Administrator admin = Administrator.getAdministrator();
		if (admin.isLogin()) {
		    if (admin_logintime == 0) {
			admin_logintime = System.currentTimeMillis();
		    } else {
			int logouttime = prefs.getAdministratorAutoLogoutTime();
			if (currtime - admin_logintime >= logouttime * 60 * 1000) {
			    admin.logout();
			}
		    }
		}
	    }
	    handler.postDelayed(this, 1000);
	}
    };
    private BroadcastReceiver usb_receiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction(); 

	    if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
		Object device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (device != null) {
		    checkForDevices();
		}
		return;
	    }
	}
    };
    private int device_check_flags = 0;

    static private final int FLAG_PRINTER_DISCONNECTED = 0x0001;
    static private final int FLAG_PRINTER_DRIVER_ERROR = 0x0002;
    static private final int FLAG_MAGCARD_DISCONNECTED = 0x0004;
    static private final int FLAG_MAGCARD_DRIVER_ERROR = 0x0008;
    static private final int FLAG_IDCARD_DISCONNECTED  = 0x0010;
    static private final int FLAG_IDCARD_DRIVER_ERROR  = 0x0020;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	Log.d("HomeActivity", "onCreate");

	atomic_int = new AtomicInteger(HOME_VIEW_ID_BASE);
	home_info = Customer.getCurrentCustomer();

	LogActivity.clearLog();

	setContentView(R.layout.activity_home);
	setupLayout();

	/* catch long click event to popup menu */
	int[] ids = {
		R.id.root_layout,
		R.id.contents_frame,
	};
	for (int id : ids) {
	    View view = findViewById(id);
	    view.setLongClickable(true);
	    view.setOnLongClickListener(this);
	}
    }

    @Override
    protected void onPause() {
	super.onPause();

	Log.d("HomeActivity", "onPause");

	unregisterReceiver(usb_receiver);

	WebView webview = (WebView) findViewById(R.id.logo_webview);
	webview.onPause();

	handler.removeCallbacks(runable);
    }
	
    @Override
    protected void onResume() {
	super.onResume();

	Log.d("HomeActivity", "onResume");

	checkForDevices();

    	IntentFilter usb_filter = new IntentFilter();
    	usb_filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    	registerReceiver(usb_receiver, usb_filter);

    	WebView webview = (WebView) findViewById(R.id.logo_webview);
	webview.onResume();

	TextSwitcher switcher = (TextSwitcher) findViewById(R.id.slogan_switcher);
	switcher.setCurrentText(home_info.slogans.get(curr_slogan));

	slogan_lasttime = System.currentTimeMillis();
	handler.postDelayed(runable, 0);

	Administrator.getAdministrator().login(this, "222222");
	Member.getMember().login(this, "429005198005300614", "333333");
    }

    @Override
    protected void onStart() {
	super.onStart();

	Log.d("HomeActivity", "onStart");

	Administrator admin = Administrator.getAdministrator();
	admin.addListener(this);

	Member member = Member.getMember();
	member.setListener(this);
	setMemberLayoutVisible(member.isLogin(), member.isLogin());
    }

    @Override
    protected void onStop() {
	super.onStop();

	Log.d("HomeActivity", "onStop");

	Administrator admin = Administrator.getAdministrator();
	admin.removeListener(this);

	Member member = Member.getMember();
	member.setListener(null);
    }
	
    @Override
    protected void onRestart() {
	super.onRestart();
	Log.d("HomeActivity", "onRestart");
    }
	
    @Override
    public void onDestroy() {
	super.onDestroy();

	Log.d("HomeActivity", "onDestory");

	handler.removeCallbacks(runable);

	Administrator admin = Administrator.getAdministrator();
	admin.logout();

	Member member = Member.getMember();
	member.logout();
    }

    @Override
    public void onBackPressed() {
	if (BuildConfig.DEBUG) {
	    super.onBackPressed();
	}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
	    LogActivity.writeLog("Power button 按下");
	    event.startTracking();
	    return true;
	}
	return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
	    LogActivity.writeLog("Power button 长按");
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupLayout() {
	View layout = findViewById(R.id.root_layout);
	layout.setBackgroundResource(home_info.background);

	WebView webview = (WebView) findViewById(R.id.logo_webview);
	WebSettings settings = webview.getSettings();
	settings.setJavaScriptEnabled(true);
	settings.setPluginState(WebSettings.PluginState.ON);
	if (home_info.banner_url != null) {
	    webview.loadUrl(home_info.banner_url);
	} else {
	    String data = "<span style='color: red; font-size: large'>" +
		    "Assets not found !!!</span><br/><br/>" +
		    "Please contact technology supporter to solve this problem!";
	    webview.loadData(data, "text/html", "");
	}
	webview.setOnTouchListener(new View.OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		((WebView) findViewById(R.id.logo_webview)).reload();
	        return true;
	    }
	});

	buildContentsLayout();

	TextSwitcher switcher = (TextSwitcher) findViewById(R.id.slogan_switcher);
	switcher.setInAnimation(AnimationUtils.loadAnimation(this,
		android.R.anim.fade_in));
	switcher.setOutAnimation(AnimationUtils.loadAnimation(this,
		android.R.anim.fade_out));
	switcher.setCurrentText(home_info.slogans.get(0));
    }

	
    private void buildContentsLayout() {
	TableLayout table = new TableLayout(getApplicationContext());
	table.setId(atomic_int.incrementAndGet());

	TableRow table_row = new TableRow(getApplicationContext());
	table_row.setId(atomic_int.incrementAndGet());
	table_row.setGravity(Gravity.CENTER_HORIZONTAL);
	table_row.setLayoutParams(new TableLayout.LayoutParams(
		ViewGroup.LayoutParams.WRAP_CONTENT,
		ViewGroup.LayoutParams.WRAP_CONTENT));
	table.addView(table_row);

	int curr_column = 1;
	for (Customer.HomeItem item : home_info.items) {
	    LinearLayout button = buildImageButton(item);
	    TableRow.LayoutParams row_params = new TableRow.LayoutParams(
		    ViewGroup.LayoutParams.MATCH_PARENT,
		    ViewGroup.LayoutParams.WRAP_CONTENT);
	    if (curr_column < home_info.item_column_count) {
		row_params.rightMargin = (int) Utils.convertDpToPixel(80);
	    }
	    table_row.addView(button, row_params);

	    if (++curr_column > home_info.item_column_count) {
		table_row = new TableRow(getApplicationContext());
		table_row.setGravity(Gravity.CENTER_HORIZONTAL);
		TableLayout.LayoutParams params = new TableLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);
		params.topMargin = (int) Utils.convertDpToPixel(40);
		table.addView(table_row, params);
		curr_column = 1;
	    }
	}
	if (home_info.items.size() % home_info.item_column_count != 0) {
	    int paddings = home_info.item_column_count - 
		    (home_info.items.size() % home_info.item_column_count);
	    for (int i = 0; i < paddings; i++) {
		View view = new View(this);
		view.setVisibility(View.INVISIBLE);
		table_row.addView(view);
	    }
	}

	FrameLayout contents = (FrameLayout) findViewById(R.id.contents_frame);
	contents.removeAllViews();
	contents.addView(table, new FrameLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.MATCH_PARENT));
    }

	
    private LinearLayout buildImageButton(Customer.HomeItem item) {
	LinearLayout button_layout = new LinearLayout(getApplicationContext());
	button_layout.setId(atomic_int.incrementAndGet());
	button_layout.setOrientation(LinearLayout.VERTICAL);

	ImageButton button = new ImageButton(getApplicationContext());
	button.setId(atomic_int.incrementAndGet());
	button.setTag(item);
	button.setImageResource(item.image);
	button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	button.setBackgroundResource(R.drawable.home_button);
	button.setPadding(5, 5, 5, 5);
	button.setClickable(true);
	button.setOnClickListener(this);

	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
		(int) Utils.convertDpToPixel(home_info.item_image_size),
		(int) Utils.convertDpToPixel(home_info.item_image_size));
	params.gravity = Gravity.CENTER_HORIZONTAL;
	params.bottomMargin = 4;
	button_layout.addView(button, params);

	TextView text_view = new TextView(getApplicationContext());
	text_view.setId(atomic_int.incrementAndGet());
	text_view.setText(item.label);
	text_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.label_size);
	text_view.setTextColor(getResources().getColor(R.color.white));
	text_view.setGravity(Gravity.CENTER);

	button_layout.addView(text_view, new LinearLayout.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

	return button_layout;
    }

	
    public void refreshLayout() {
	ViewGroup group = (ViewGroup) findViewById(R.id.contents_frame);
	group.removeAllViews();
	setupLayout();
    }

    private void updateUsbDeviceTips() {
	TextView tips_text = (TextView) findViewById(R.id.tips_text);
	TextView tips_second_text = (TextView) findViewById(R.id.tips_second_text);
	View tips_layout = findViewById(R.id.tips_layout);

	if ((device_check_flags & FLAG_PRINTER_DRIVER_ERROR) != 0) {
	    tips_text.setText("设备软件故障：打印机驱动错误");
	    tips_second_text.setText("请进入系统设置检查打印机设备驱动配置是否正确");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	if ((device_check_flags & FLAG_PRINTER_DISCONNECTED) != 0) {
	    tips_text.setText("设备故障：打印机已断开连接");
	    tips_second_text.setText("请检查设备电源是否打开，电缆是否松动，或设备驱动是否正确配置");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	if ((device_check_flags & FLAG_MAGCARD_DRIVER_ERROR) != 0) {
	    tips_text.setText("设备故障：刷卡器驱动错误");
	    tips_second_text.setText("请进入系统设置检查刷卡器设备驱动配置是否正确");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	if ((device_check_flags & FLAG_MAGCARD_DISCONNECTED) != 0) {
	    tips_text.setText("设备故障：刷卡器已断开连接");
	    tips_second_text.setText("请检查设备电缆是否松动，或设备驱动是否正确配置");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	if ((device_check_flags & FLAG_IDCARD_DRIVER_ERROR) != 0) {
	    tips_text.setText("设备故障：身份证阅读器驱动错误");
	    tips_second_text.setText("请进入系统设置检查身份证阅读器设备驱动配置是否正确");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	if ((device_check_flags & FLAG_IDCARD_DISCONNECTED) != 0) {
	    tips_text.setText("设备故障：身份证阅读器已断开连接");
	    tips_second_text.setText("请检查设备电缆是否松动，或设备驱动是否正确配置");
	    tips_layout.setVisibility(View.VISIBLE);
	    return;
	}
	tips_layout.setVisibility(View.GONE);
    }

    private void checkForDevices() {
	boolean printer_disconnect = ((device_check_flags & FLAG_PRINTER_DISCONNECTED) != 0);

	checkForSingleDevice(Device.DEVICE_PRINTER,
		FLAG_PRINTER_DRIVER_ERROR, FLAG_PRINTER_DISCONNECTED);
	checkForSingleDevice(Device.DEVICE_MAGCARD,
		FLAG_MAGCARD_DRIVER_ERROR, FLAG_MAGCARD_DISCONNECTED);
	checkForSingleDevice(Device.DEVICE_IDCARD,
		FLAG_IDCARD_DRIVER_ERROR, FLAG_IDCARD_DISCONNECTED);

	if (printer_disconnect) {
	    if ((device_check_flags & FLAG_PRINTER_DISCONNECTED) == 0) {
		EFormApplication.printer_first_print = true;
	    }
	}
	checkForUDisk();

	updateUsbDeviceTips();
    }

    private void checkForSingleDevice(String klass, int driver_flag, int disconn_flag) {
	Device device = Device.getDevice(klass);
	if (device == null) {
	    device_check_flags |= driver_flag;
	    return;
	}
	device_check_flags &= ~driver_flag;

	if (device.probeDevice()) {
	    device_check_flags &= ~disconn_flag;
	} else {
	    device_check_flags |= disconn_flag;
	}
    }

    private void checkForUDisk() {
	int count = 0;

	UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
	HashMap<String, UsbDevice> devices = manager.getDeviceList();
	Iterator<UsbDevice> iterator = devices.values().iterator();
	while (iterator.hasNext()) {
	    UsbDevice device = iterator.next();
	    if (device.getVendorId() == 0x0204 && device.getProductId() == 0x6025) {
		count++;
	    }
	    if (device.getVendorId() == 0x0951 && device.getProductId() == 0x1642) {
		count++;
	    }
	}
	int udisk_count = EFormApplication.getInstance().getUDiskCount();
	if (count > udisk_count) {
	    Utils.showToast("U盘已插入");
	} else if (count < udisk_count) {
	    Utils.showToast("U盘已拔出");
	}
	EFormApplication.getInstance().setUDiskCount(count);
    }

    public void onDeviceCheckButtonClick(View view) {
	checkForDevices();
    }

    @Override
    public void onClick(View view) {
	if (view.getClass() == ImageButton.class) {
	    Object object = view.getTag();
	    if (object == null || !(object instanceof Customer.HomeItem)) {
		LogActivity.writeLog("程序错误: 主页按钮缺少 class Tag");
		return;
	    }
	    Customer.HomeItem item = (Customer.HomeItem) object;

	    Voucher voucher = new Voucher();

	    voucher.setFormClass(item.klass);
	    voucher.setFormLabel(getResources().getString(item.label));
	    voucher.setFormImage(item.image);

	    startFormActivity(voucher);
	}
    }

    public void startFormActivity(Voucher voucher) {
	Intent intent = new Intent(this, FormActivity.class);
	intent.putExtra(FormActivity.INTENT_MESSAGE_VOUCHER, voucher);
	startActivityForResult(intent, ITEM_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	switch(requestCode) {
	case ITEM_ACTIVITY_REQUEST_CODE:
	    switch(resultCode) {
	    case RESULT_CANCELED:
		if (intent != null) {
		    int reason = intent.getIntExtra(FormActivity.INTENT_RESULT_ERRREASON, 0);
		    if (reason != 0) {
			showErrorFlagment(reason);
		    }
		}
		break;
	    case RESULT_OK:
		break;
	    }
	    break;
	}
    }

    @Override
    public boolean onLongClick(View view) {
	switch (view.getId()) {
	case R.id.root_layout:
	case R.id.contents_frame:
	    HomeMenu menu = new HomeMenu();
	    menu.show(getFragmentManager(), "HomeMenu");
	    return true;
	default:
	    return false;
	}
    }


    public void onSloganSwitcherClick(View view) {
	TextSwitcher switcher = (TextSwitcher) findViewById(R.id.slogan_switcher);
	if (++curr_slogan >= home_info.slogans.size())
	    curr_slogan = 0;
	switcher.setText(home_info.slogans.get(curr_slogan));
    }

    private void showErrorFlagment(int reason) {
	FragmentManager fragmentManager = getFragmentManager();
	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	ErrorFragment fragment = new ErrorFragment();
	fragment.setErrorReason(reason);
	ViewGroup layout = (ViewGroup)findViewById(R.id.contents_frame);
	layout.removeAllViews();
	fragmentTransaction.add(R.id.contents_frame, fragment);
	fragmentTransaction.commit();
    }


    public void onMemberVoucherButtonClick(View view) {
	Member member = Member.getMember();
	member.listVouchers(getFragmentManager());
    }


    public void onMemberImportButtonClick(View view) {
	Member member = Member.getMember();
	member.importAndExport(getFragmentManager());
    }


    public void onMemberProfileButtonClick(View view) {
	Member member = Member.getMember();
	member.update(getFragmentManager());
    }


    public void onMemberLogoutButtonClick(View view) {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle("退出登录");
	builder.setMessage("\n您真的要退出登录吗？\n");
	builder.setNegativeButton("不退出", null);
	builder.setPositiveButton("退出登录", new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		Member member = Member.getMember();
		member.logout();
	    }
	});
	Dialog dialog = builder.create();
	dialog.setOnShowListener(new DialogInterface.OnShowListener() {
	    @Override
	    public void onShow(DialogInterface dialog) {
		Button button = ((AlertDialog) dialog).getButton(Dialog.BUTTON_NEGATIVE);
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		button = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
	    }
	});
	dialog.show();
    }


    private void setMemberLayoutVisible(boolean visible, boolean use_anim) {
	final View view = findViewById(R.id.member_layout);

	if (!use_anim) {
	    if (visible)
		view.setVisibility(View.VISIBLE);
	    else
		view.setVisibility(View.GONE);
	    return;
	}

	TranslateAnimation anim = null;

	if (visible) {
	    view.setVisibility(View.VISIBLE);
	    int width = (view.getWidth() == 0) ? 100 : view.getWidth();
	    anim = new TranslateAnimation(width, 0.0f, 0.0f, 0.0f);
	} else {
	    anim = new TranslateAnimation(0.0f, view.getWidth(), 0.0f, 0.0f);
	    anim.setAnimationListener(new Animation.AnimationListener() {
		public void onAnimationStart(Animation animation) {
		    if (view.getVisibility() != View.VISIBLE)
			view.setVisibility(View.GONE);
		}
		public void onAnimationRepeat(Animation animation) {
		}
		public void onAnimationEnd(Animation animation) {
		    view.setVisibility(View.GONE);
		    view.clearAnimation();
		}
	    });
	}
	AnimationSet anim_set = new AnimationSet(true);
	anim_set.addAnimation(anim);
	anim_set.setDuration(400);
	anim_set.setInterpolator(new AccelerateInterpolator(1.0f));
	view.startAnimation(anim_set);
    }


    @Override
    public void onMemberLogin() {
	setMemberLayoutVisible(true, true);
    }

    @Override
    public void onMemberLogout() {
	setMemberLayoutVisible(false, true);
    }

    @Override
    public void onAdministratorLogin(Administrator admin) {
	Preferences prefs = Preferences.getPreferences();
	if (prefs.getAdministratorAutoLogout())
	    admin_logintime = System.currentTimeMillis();
    }

    @Override
    public void onAdministratorLogout(Administrator admin) {
	admin_logintime = 0;
    }

}
