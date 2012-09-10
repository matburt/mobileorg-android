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

public class DateFragment extends SherlockFragment {
	
	private TableLayout datesView;
	
	private DateTableRow scheduledEntry = null;
	private DateTableRow deadlineEntry = null;
	private DateTableRow timestampEntry = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		this.datesView = new TableLayout(getActivity());

		if(savedInstanceState != null) {
			setupScheduled(savedInstanceState.getString("scheduled"));
			setupDeadline(savedInstanceState.getString("deadline"));
		}
		return datesView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EditActivity editActivity = (EditActivity) getActivity();
		OrgNode node = editActivity.getOrgNode();
		setupDates(node);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString("scheduled", getScheduled());
        outState.putString("deadline", getDeadline());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if(this.deadlineEntry != null)
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(true);
		
		if(this.scheduledEntry != null)
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(false);
		else
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(true);
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupDates(OrgNode node) {
		this.scheduledEntry = setupDate(node.getPayload().getScheduled(), "SCHEDULED", scheduledRemoveListener);
		this.deadlineEntry = setupDate(node.getPayload().getDeadline(), "DEADLINE", deadlineRemoveListener);
		this.timestampEntry = setupDate(node.getPayload().getTimestamp(), "", timestampRemoveListener);
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
	
	private void setupScheduled(String scheduled) {
		if (scheduled != null)
			this.scheduledEntry = setupDate(scheduled, "SCHEDULED",
					scheduledRemoveListener);
	}

	private void setupDeadline(String deadline) {
		if (deadline != null)
			this.deadlineEntry = setupDate(deadline, "DEADLINE",
					deadlineRemoveListener);
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
