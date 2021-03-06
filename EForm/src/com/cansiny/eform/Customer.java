/* EForm - Electronic Form System
 *
 * Copyright (C) 2013 Wu Xiaohu. All rights reserved.
 * Copyright (C) 2013 Cansiny Trade Co.,Ltd. All rights reserved.
 */
package com.cansiny.eform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.res.AssetManager;

public abstract class Customer
{
    static final public int CUSTOMER_CIB = 1;
    static final public int CUSTOMER_CCB = 2;
    static final public int CUSTOMER_CURRENT = CUSTOMER_CIB;

    static private Customer single_instance = null;

    static Customer getCurrentCustomer() {
	if (single_instance == null) {
	    switch(CUSTOMER_CURRENT) {
	    case CUSTOMER_CIB:
		single_instance = new CustomerCIB();
		break;
	    case CUSTOMER_CCB:
		single_instance = new CustomerCCB();
		break;
	    default:
		LogActivity.writeLog("不支持的客户编号 %d", CUSTOMER_CURRENT);
		single_instance = null;
		break;
	    }
	}
	return single_instance;
    }

    protected String name;
    protected int background;
    protected String banner_url;
    protected int item_column_count;
    protected int item_image_size;
    protected ArrayList<HomeItem> items;
    protected ArrayList<String> slogans;

    protected Customer() {
	items = new ArrayList<HomeItem>();
	slogans = new ArrayList<String>();

	name = "Unknown";
	background = R.drawable.dark;
	item_column_count = 6;
	item_image_size = 88;

	slogans.add("欢迎使用电子填单系统");
	String date = new SimpleDateFormat("yyyy年MM月dd日 EEE",
		   Locale.CHINA).format(Calendar.getInstance().getTime());
	slogans.add("今天是 " + date.replace("周", "星期"));

	Context context = EFormApplication.getContext();
	File file = new File(context.getFilesDir(), "banner.html");
	if (file.exists()) {
	    banner_url = "file://" + file.getAbsolutePath();
	} else {
	    banner_url = null;
	}
    }

    public String getName() {
	return name;
    }

    public class HomeItem
    {
	public String klass;
	public int image;
	public int label;
	public int label_size;

	private void initial(String klass, int image, int label, int label_size) {
	    this.klass = EFormApplication.getContext().getPackageName() + "." + klass;
	    this.image = image;
	    this.label = label;
	    this.label_size = label_size;
	}

	public HomeItem(String klass, int image, int label, int label_size) {
	    initial(klass, image, label, label_size);
	}

	public HomeItem(String klass, int image, int label) {
	    initial(klass, image, label, 21);
	}
    }
}


class CustomerCIB extends Customer
{
    public CustomerCIB() {
	super();

	name = "CIB";

	slogans.add("客服热线：95561");
	slogans.add("贵宾专线：400 889 5561");
	slogans.add("境外客服热线：86-21-3876 9999");
	slogans.add("境外信用卡白金专线：86-21-3842 9696");
	slogans.add("网上银行：http://www.cib.com.cn");
	slogans.add("手机银行：http://wap.cib.com.cn");

	if (banner_url == null) {
	    try {
		AssetManager assets = EFormApplication.getContext().getAssets();
		InputStream stream = assets.open("cib/banner.html");
		stream.close();
		banner_url = "file:///android_asset/cib/banner.html";
	    } catch (IOException e) {
		LogActivity.writeLog(e);
		banner_url = null;
	    }
	}

	items.add(new HomeItem("FormCIB01", R.drawable.cib01, R.string.form_label_cib01, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib02));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib04, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib03));
	items.add(new HomeItem("FormCIB01", R.drawable.cib01, R.string.form_label_cib01, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib02));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib04, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib03));
	items.add(new HomeItem("FormCIB01", R.drawable.cib01, R.string.form_label_cib01, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib02));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib04, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib03));
	items.add(new HomeItem("FormCIB01", R.drawable.cib01, R.string.form_label_cib01, 21));
	items.add(new HomeItem("FormCIB01", R.drawable.cib01, R.string.form_label_cib01, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib02));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib04, 21));
	items.add(new HomeItem("FormCIB02", R.drawable.cib02, R.string.form_label_cib03));
    }
}


class CustomerCCB extends Customer
{
    public CustomerCCB() {
	super();

	name = "CCB";

	slogans.add("客服热线：95533");
	slogans.add("网上银行：http://www.ccb.com.cn");
	slogans.add("手机银行：http://wap.ccb.com.cn");
    }
}
