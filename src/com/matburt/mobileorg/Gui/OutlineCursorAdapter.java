package com.matburt.mobileorg.Gui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.matburt.mobileorg.R;

public class OutlineCursorAdapter extends SimpleCursorAdapter {

	private Cursor cursor;
	
	public OutlineCursorAdapter(Context context, Cursor cursor) {
		super(context, R.layout.outline_new, cursor, new String[] {}, new int[] {});
		this.cursor = cursor;
	}
	
	@Override
	public long getItemId(int position) {
		cursor.moveToPosition(position);
		int columnIndex = cursor.getColumnIndex("_id");
		return cursor.getInt(columnIndex);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		Cursor c = getCursor();

		final LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.outline_new, parent, false);

		bindView(v, context, c);

		return v;
	}

	@Override
	public void bindView(View v, Context context, Cursor c) {
		TextView orgItem = (TextView) v.findViewById(R.id.orgItem);
		TextView todoState = (TextView) v.findViewById(R.id.todoState);
		TextView priorityState = (TextView) v.findViewById(R.id.priorityState);
		LinearLayout tagsLayout = (LinearLayout) v
				.findViewById(R.id.tagsLayout);
		TextView dateInfo = (TextView) v.findViewById(R.id.dateInfo);

		int orgItemNum = c.getColumnIndex("name");
		orgItem.setText(c.getString(orgItemNum));
		
		todoState.setVisibility(View.GONE);

		// // Setup todo state view
		// if (TextUtils.isEmpty(todo)) {
		// holder.todoState.setVisibility(View.GONE);
		// } else {
		// holder.todoState.setText(todo);
		// Integer todoState = this.findTodoState(todo);
		// if (todoState > 0)
		// holder.todoState.setBackgroundColor(Color.GREEN);
		// else
		// holder.todoState.setBackgroundColor(Color.RED);
		// holder.todoState.setTextColor(Color.WHITE);
		// holder.todoState.setVisibility(View.VISIBLE);
		// }
		//
		// // Setup priority view
		// if (TextUtils.isEmpty(priority)) {
		// holder.priorityState.setVisibility(View.GONE);
		// } else {
		// holder.priorityState.setText(priority);
		// holder.priorityState.setVisibility(View.VISIBLE);
		// }
		//
		// // Setup date view
		// //if (TextUtils.isEmpty(dateInfo)) {
		// holder.dateInfo.setVisibility(View.GONE);
		// // } else {
		// // holder.dateInfo.setText(dateInfo);
		// // holder.dateInfo.setVisibility(View.VISIBLE);
		// // }
		//
		// // Add tag view(s)
		// holder.tagsLayout.removeAllViews();
		// for (String tag : node.getTags()) {
		// TextView tagView = new TextView(this.context);
		// tagView.setText(tag);
		// tagView.setTextColor(Color.LTGRAY);
		// tagView.setPadding(0, 0, 5, 0);
		// holder.tagsLayout.addView(tagView);
		// }

	}

	/**
	 * Used as part of the holding pattern.
	 * 
	 * The idea is to save the findViewById()'s into this container object to
	 * speed up the list adapter. setTag() and getTag() are used to bind and
	 * retrieve the container.
	 * 
	 */
	static class ViewHolder {
		TextView orgItem;
		TextView todoState;
		TextView priorityState;
		LinearLayout tagsLayout;
		TextView dateInfo;
	}
}
