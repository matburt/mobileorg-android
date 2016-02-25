package com.matburt.mobileorg.Gui.Outline;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutlineItem extends RecyclerView.ViewHolder {
    public final View mView;
    public TextView titleView;
	private TextView tagsView;
	private Button todoButton;
	public TextView levelView;

	public OutlineItem(View view) {
		super(view);
        mView = view;
		tagsView = (TextView) view.findViewById(R.id.outline_item_tags);
        titleView = (TextView) view.findViewById(R.id.outline_item_title);
        todoButton = (Button) view.findViewById(R.id.outline_item_todo);
        levelView = (TextView) view.findViewById(R.id.outline_item_level);

//		todoButton.setOnClickListener(todoClick);

		int fontSize = PreferenceUtils.getFontSize();
		tagsView.setTextSize(fontSize);
		todoButton.setTextSize(fontSize);
	}

	@Override
	public String toString() {
		return super.toString() + " '" + levelView.getText() + "'";
	}

}
