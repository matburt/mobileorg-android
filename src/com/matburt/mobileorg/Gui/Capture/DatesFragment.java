package com.matburt.mobileorg.Gui.Capture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.DateTableRow.OrgTimeDate;
import com.matburt.mobileorg.OrgData.OrgNode;

public class DatesFragment extends SherlockFragment {
	private final String DATES_SCHEDULED = "scheduled";
	private final String DATES_DEADLINE = "deadline";
	private final String DATES_TIMESTAMP = "timestamp";
	
	private TableLayout datesView;
	
	private DateTableRow scheduledEntry = null;
	private DateTableRow deadlineEntry = null;
	private DateTableRow timestampEntry = null;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);
		this.datesView = new TableLayout(getActivity());
		return datesView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		EditActivity editActivity = (EditActivity) getActivity();
		OrgNode node = editActivity.getOrgNode();
		
		if(savedInstanceState != null)
			restoreInstanceState(savedInstanceState);
		else
			setupDates(node);
		
		editActivity.invalidateOptionsMenu();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString(DATES_SCHEDULED, getScheduled());
        outState.putString(DATES_DEADLINE, getDeadline());
        outState.putString(DATES_TIMESTAMP, getTimestamp());
	}
	
	public void restoreInstanceState(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			String timestamp = savedInstanceState.getString(DATES_TIMESTAMP);
			if (timestamp != null)
				this.timestampEntry = setupDate(timestamp, "",
						timestampRemoveListener);
			String scheduled = savedInstanceState.getString(DATES_SCHEDULED);
			if (scheduled != null)
				this.scheduledEntry = setupDate(scheduled, "SCHEDULED",
						scheduledRemoveListener);
			String deadline = savedInstanceState.getString(DATES_DEADLINE);
			if (deadline != null)
				this.deadlineEntry = setupDate(deadline, "DEADLINE",
						deadlineRemoveListener);
		}
	}

	private void setupDates(OrgNode node) {
		this.scheduledEntry = setupDate(node.getOrgNodePayload().getScheduled(), "SCHEDULED", scheduledRemoveListener);
		this.deadlineEntry = setupDate(node.getOrgNodePayload().getDeadline(), "DEADLINE", deadlineRemoveListener);
		this.timestampEntry = setupDate(node.getOrgNodePayload().getTimestamp(), "", timestampRemoveListener);
	}
	
	private DateTableRow setupDate(String date, String title, View.OnClickListener removeListener) {
		final Pattern schedulePattern = Pattern
				.compile("((\\d{4})-(\\d{1,2})-(\\d{1,2}))(?:[^\\d]*)" 
						+ "((\\d{1,2})\\:(\\d{2}))?(-((\\d{1,2})\\:(\\d{2})))?");
		Matcher propm = schedulePattern.matcher(date);
		DateTableRow dateEntry = null;

		if (propm.find()) {
			OrgTimeDate timeDate = new OrgTimeDate();

			try {
				timeDate.year = Integer.parseInt(propm.group(2));
				timeDate.monthOfYear = Integer.parseInt(propm.group(3));
				timeDate.dayOfMonth = Integer.parseInt(propm.group(4));

				timeDate.startTimeOfDay = Integer.parseInt(propm.group(6));
				timeDate.startMinute = Integer.parseInt(propm.group(7));

				timeDate.endTimeOfDay = Integer.parseInt(propm.group(10));
				timeDate.endMinute = Integer.parseInt(propm.group(11));
			} catch (NumberFormatException e) {
			}
			dateEntry = new DateTableRow(getActivity(), this, datesView,
					removeListener, title, timeDate);
		}
		
		return dateEntry;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_dates, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if(this.deadlineEntry != null)
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(true);
		
		if(this.scheduledEntry != null)
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(true);
		
		if(this.timestampEntry != null)
			menu.findItem(R.id.menu_nodeedit_timestamp).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_timestamp).setVisible(true);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_nodeedit_scheduled:
			addDateScheduled();
			return true;

		case R.id.menu_nodeedit_deadline:
			addDateDeadline();
			return true;
			
		case R.id.menu_nodeedit_timestamp:
			addDateTimestamp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void addDateTimestamp() {
		DateTableRow dateEntry = new DateTableRow(getActivity(), this, datesView, timestampRemoveListener,
				"");
		this.timestampEntry = dateEntry;
	}
	
	private void addDateScheduled() {
		DateTableRow dateEntry = new DateTableRow(getActivity(), this, datesView, scheduledRemoveListener,
				"SCHEDULED");
		this.scheduledEntry = dateEntry;
	}
	
	private void addDateDeadline() {
		DateTableRow dateEntry = new DateTableRow(getActivity(), this, datesView, deadlineRemoveListener,
				"DEADLINE");
		this.deadlineEntry = dateEntry;
	}
	
	private View.OnClickListener timestampRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(timestampEntry);
			timestampEntry = null;
		}
	};
	
	private View.OnClickListener scheduledRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(scheduledEntry);
			scheduledEntry = null;
		}
	};
	
	private View.OnClickListener deadlineRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(deadlineEntry);
			deadlineEntry = null;
		}
	};

	
	public String getTimestamp() {
		if(this.timestampEntry == null || TextUtils.isEmpty(this.timestampEntry.getDate()))
			return "";
		else
			return "<" + this.timestampEntry.getDate() + ">";
	}
	
	public String getScheduled() {
		if(this.scheduledEntry == null || TextUtils.isEmpty(this.scheduledEntry.getDate()))
			return "";
		else
			return "SCHEDULED: <" + this.scheduledEntry.getDate() + ">";
	}

	public String getDeadline() {
		if(this.deadlineEntry == null || TextUtils.isEmpty(this.deadlineEntry.getDate()))
			return "";
		else
			return "DEADLINE: <" + this.deadlineEntry.getDate() + ">";
	}
}
