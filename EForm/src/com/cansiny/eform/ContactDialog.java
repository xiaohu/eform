/* EForm - Electronic Form System
 *
 * Copyright (C) 2013 Wu Xiaohu. All rights reserved.
 * Copyright (C) 2013 Cansiny Trade Co.,Ltd. All rights reserved.
 */
package com.cansiny.eform;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;


public class ContactDialog extends Utils.DialogFragment implements OnTabChangeListener
{
    static final private String TAB_TAG_AFTERMARKET = "AfterMarket";
    static final private String TAB_TAG_COMPANYINFO = "CompanyInfo";
    static final private String TAB_TAG_DEVICEINFO  = "DeviceInfo";
    static final private String TAB_TAG_COPYRIGHT   = "Copyright";

    private boolean aftermarket_tab_is_active = false;
    private boolean companyinfo_tab_is_active = false;
    private boolean deviceinfo_tab_is_active = false;
    private boolean copyright_tab_is_active = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
	super.onCreateDialog(savedInstanceState);

	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	LayoutInflater inflater = getActivity().getLayoutInflater();
	builder.setView(inflater.inflate(R.layout.dialog_contact, null));

	return builder.create();
    }

    @Override
    public void onStart() {
	super.onStart();

	final TabHost tabhost = (TabHost)getDialog().findViewById(android.R.id.tabhost);
	TabWidget tabwidget = tabhost.getTabWidget();

	if (tabwidget == null || tabwidget.getTabCount() == 0) {
	    tabhost.setup();

	    TabHost.TabSpec tabspec = tabhost.newTabSpec(TAB_TAG_AFTERMARKET);
	    tabspec.setContent(R.id.aftermarket_tab);
	    tabspec.setIndicator("售后服务", null);
	    tabhost.addTab(tabspec);

	    tabspec = tabhost.newTabSpec(TAB_TAG_COMPANYINFO);
	    tabspec.setContent(R.id.companyinfo_tab);
	    tabspec.setIndicator("公司信息",null);
	    tabhost.addTab(tabspec);

	    tabspec = tabhost.newTabSpec(TAB_TAG_DEVICEINFO);
	    tabspec.setContent(R.id.deviceinfo_tab);
	    tabspec.setIndicator("产品信息",null);
	    tabhost.addTab(tabspec);

	    tabspec = tabhost.newTabSpec(TAB_TAG_COPYRIGHT);
	    tabspec.setContent(R.id.copyright_tab);
	    tabspec.setIndicator("版权信息",null);
	    tabhost.addTab(tabspec);

	    tabwidget = tabhost.getTabWidget();
	    for (int i = 0; i < tabwidget.getTabCount(); i++) {
		View layout = tabwidget.getChildTabViewAt(i);
		TextView textview = (TextView) layout.findViewById(android.R.id.title);
		textview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
	    }

	    tabhost.setOnTabChangedListener(this);

	    onTabChanged(TAB_TAG_AFTERMARKET);
	}
    }

    @Override
    public void onTabChanged(String tag) {
	if (tag.equals(TAB_TAG_AFTERMARKET)) {
	    if (!aftermarket_tab_is_active) {
		onAftermarketTabActive();
		aftermarket_tab_is_active = true;
	    }
	} else if (tag.equals(TAB_TAG_COMPANYINFO)) {
	    if (!companyinfo_tab_is_active) {
		companyinfo_tab_is_active = true;
	    }
	} else if (tag.equals(TAB_TAG_DEVICEINFO)) {
	    if (!deviceinfo_tab_is_active) {
		onDeviceinfoTabActive();
		deviceinfo_tab_is_active = true;
	    }
	} else if (tag.equals("")) {
	    if (!copyright_tab_is_active) {
		copyright_tab_is_active = true;
	    }
	}
    }

    private void onAftermarketTabActive() {
	Preferences prefs = Preferences.getPreferences();
	TextView textview;

	String name = prefs.getAftermarketName();
	String phone = prefs.getAftermarketPhone();
	if (name == null || name.length() == 0 || phone == null || phone.length() == 0) {
	    textview = (TextView) getDialog().findViewById(R.id.aftermarket_name_textview);
	    textview.setText("售后服务联系信息尚未录入！");
	    textview.setTextColor(getResources().getColor(R.color.fuchsia));
	    textview = (TextView) getDialog().findViewById(R.id.aftermarket_phone_textview);
	    textview.setText("请在“系统设置”中录入售后服务联系信息");
	    textview.setTextColor(getResources().getColor(R.color.fuchsia));
	} else {
	    textview = (TextView) getDialog().findViewById(R.id.aftermarket_name_textview);
	    textview.setText("姓 名：" + name);
	    textview.setTextColor(getResources().getColor(R.color.darkgray));
	    textview = (TextView) getDialog().findViewById(R.id.aftermarket_phone_textview);
	    textview.setText("电 话：" + phone);
	    textview.setTextColor(getResources().getColor(R.color.darkgray));
	}
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void onDeviceinfoTabActive() {
	Configuration config = getResources().getConfiguration();
	PackageInfo pInfo = null;

	try {
	    pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
	} catch (NameNotFoundException e) {
	    LogActivity.writeLog(e);
	}

	TextView textview = (TextView) getDialog().findViewById(R.id.product_textview);
	textview.setText(Build.PRODUCT);

	textview = (TextView) getDialog().findViewById(R.id.model_textview);
	textview.setText(Build.MODEL);

	if (pInfo != null) {
	    textview = (TextView) getDialog().findViewById(R.id.version_textview);
	    textview.setText(pInfo.versionName + "-" + pInfo.versionCode);
	}

	textview = (TextView) getDialog().findViewById(R.id.identifier_textview);
	textview.setText(Utils.getDeviceId().toString().toUpperCase(Locale.US));

	textview = (TextView) getDialog().findViewById(R.id.environment_textview);
	textview.setText(Build.DISPLAY);

//	Log.d("", "product: " + Build.PRODUCT);
//	Log.d("", "Manufacturer: " + Build.MANUFACTURER);
//	Log.d("", "Radio: " + Build.getRadioVersion());
//	Log.d("", "SERIAL: " + Build.SERIAL);
//	Log.d("", "User: " + Build.USER);
//	Log.d("", "Device: " + Build.DEVICE);
//	Log.d("", "Display: " + Build.DISPLAY);
//	Log.d("", "Board: " + Build.BOARD);
//	Log.d("", "Bootloader: " + Build.BOOTLOADER);
//	Log.d("", "barnd: " + Build.BRAND);
//	Log.d("", "Id: " + Build.ID);
//	Log.d("", "Hardware: " + Build.HARDWARE);
//	Log.d("", "FingerPrint: " + Build.FINGERPRINT);
//	Log.d("", "Tags: " + Build.TAGS);
//	Log.d("", "Host: " + Build.HOST);
//	Log.d("", "CPU ABI: " + Build.CPU_ABI);
//	Log.d("", "CPU ABI2: " + Build.CPU_ABI2);

	Date date = new Date(Build.TIME);
	String text = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(date);
	if (pInfo != null) {
	    date = new Date(pInfo.lastUpdateTime);
	    text += "  /  ";
	    text += new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(date);
	}
	textview = (TextView) getDialog().findViewById(R.id.manufacture_date_textview);
	textview.setText(text);

	textview = (TextView) getDialog().findViewById(R.id.screendp_textview);
	textview.setText("" + config.screenWidthDp + " X " + config.screenHeightDp);

	textview = (TextView) getDialog().findViewById(R.id.keyboard_textview);
	switch(config.keyboard) {
	case Configuration.KEYBOARD_NOKEYS:
	    textview.setText("无");
	    break;
	case Configuration.KEYBOARD_QWERTY:
	    textview.setText("QWERTY键盘");
	    break;
	case Configuration.KEYBOARD_12KEY:
	    textview.setText("12键键盘");
	    break;
	}
    }

}
