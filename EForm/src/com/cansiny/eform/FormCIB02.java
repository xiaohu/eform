/* EForm - Electronic Form System
 *
 * Copyright (C) 2013 Wu Xiaohu. All rights reserved.
 * Copyright (C) 2013 Cansiny Trade Co.,Ltd. All rights reserved.
 */
package com.cansiny.eform;

import com.cansiny.eform.IDCard.IDCardInfo;

import android.app.Activity;
import android.widget.Button;


public class FormCIB02 extends Form
{
    public FormCIB02(Activity activity, String label) {
	super(activity, label);

	pages.add(new FormPage(activity, R.string.form_title_cib01_2, R.layout.form_cib02_1));
    }

    @Override
    void onIDCardResponse(Button button, IDCardInfo info) {
    }

}
