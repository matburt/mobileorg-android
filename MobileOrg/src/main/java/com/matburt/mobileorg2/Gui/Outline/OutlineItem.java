package com.matburt.mobileorg2.Gui.Outline;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.PreferenceUtils;

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
