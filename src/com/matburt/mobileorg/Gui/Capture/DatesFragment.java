package com.matburt.mobileorg.Gui.Capture;

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
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;

public class DatesFragment extends SherlockFragment {
	private final String DATES_SCHEDULED = "scheduled";
	private final String DATES_DEADLINE = "deadline";
	private final String DATES_TIMESTAMP = "timestamp";
	
	private TableLayout datesView;
	private boolean isModifiable = true;
	
	private DateTableRow scheduledEntry = null;
	private DateTableRow deadlineEntry = null;
	private DateTableRow timestampEntry = null;

	private OrgNodePayload payload;
	
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
			setupDates(node.getOrgNodePayload());
		
		setModifable(editActivity.isNodeModifiable());
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

	public void setupDates(OrgNodePayload payload) {
		this.payload = payload;
		this.payload.getCleanedPayload(); // Hack
		this.scheduledEntry = setupDate(payload.getScheduled(), "SCHEDULED", scheduledRemoveListener);
		this.deadlineEntry = setupDate(payload.getDeadline(), "DEADLINE", deadlineRemoveListener);
		this.timestampEntry = setupDate(payload.getTimestamp(), "", timestampRemoveListener);
	}
	
	public void setModifable(boolean enabled) {
		if(this.scheduledEntry != null)
			this.scheduledEntry.setModifiable(enabled);
		if(this.timestampEntry != null)
			this.timestampEntry.setModifiable(enabled);
		if(this.deadlineEntry != null)
			this.deadlineEntry.setModifiable(enabled);
		
		this.isModifiable = enabled;
	}
	
	private DateTableRow setupDate(String date, String title, View.OnClickListener removeListener) {
		try {
			OrgNodeTimeDate timeDate = new OrgNodeTimeDate();
			timeDate.parseDate(date);

			DateTableRow dateEntry = new DateTableRow(getActivity(), this,
					datesView, removeListener, title, timeDate);
			return dateEntry;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_dates, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if(this.deadlineEntry == null && isModifiable)
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(true);
		else
			menu.findItem(R.id.menu_nodeedit_deadline).setVisible(false);
		
		if(this.scheduledEntry == null && isModifiable)
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(true);
		else
			menu.findItem(R.id.menu_nodeedit_scheduled).setVisible(false);
		
		if(this.timestampEntry == null && isModifiable)
			menu.findItem(R.id.menu_nodeedit_timestamp).setVisible(true);
		else
			menu.findItem(R.id.menu_nodeedit_timestamp).setVisible(false);
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
