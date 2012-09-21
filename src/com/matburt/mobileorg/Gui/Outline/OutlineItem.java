package com.matburt.mobileorg.Gui.Outline;

import com.matburt.mobileorg.R;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Checkable;
import android.widget.RelativeLayout;

public class OutlineItem extends RelativeLayout implements Checkable {
	
	public OutlineItem(Context context) {
		super(context);
		View.inflate(getContext(), R.layout.outline_item, this);
	}

	@Override
	public boolean isChecked() {
		return false;
	}

	@Override
	public void setChecked(boolean checked) {
		if(checked)
			setBackgroundColor(Color.RED);
		else
			setBackgroundColor(Color.BLACK);
	}

	@Override
	public void toggle() {
		
	}

}
