package com.matburt.mobileorg.Gui.Wizard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class WideLinearLayout extends LinearLayout {

	private int screenWidth;

	public WideLinearLayout(Context context) {
		super(context);
	}

	public WideLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setWidth(int w) {
		screenWidth = w;
	}

	@Override
	protected void onMeasure(int w, int h) {
		// int width = MeasureSpec.getSize(screenWidth);
		int height = MeasureSpec.getSize(h);
		// and its children
		for (int i = 0; i < getChildCount(); i++) {
			View page = (View) getChildAt(i);
			// page.measure(screenWidth,hs);
			ViewGroup.LayoutParams lp = page.getLayoutParams();
			lp.width = screenWidth;
			lp.height = height;
		}
		// setMeasuredDimension(width,height);
		super.onMeasure(w, h);
	}
}
