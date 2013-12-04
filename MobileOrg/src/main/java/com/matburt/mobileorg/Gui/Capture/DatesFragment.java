package com.matburt.mobileorg.Gui.Capture;

import android.app.Activity;
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
import com.matburt.mobileorg.Gui.Capture.DateTableRow.DateTableRowListener;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;

public class DatesFragment extends SherlockFragment {
	private final String DATES_SCHEDULED = "scheduled";
	private final String DATES_DEADLINE = "deadline";
	private final String DATES_TIMESTAMP = "timestamp";
	
	private TableLayout datesView;
	private boolean isModifiable = true;
	
	DateTableRow scheduledEntry = null;
	DateTableRow deadlineEntry = null;
	DateTableRow timestampEntry = null;

	private OrgNodePayload payload;
	
	private OnDatesModifiedListener mListener;
	
	public interface OnDatesModifiedListener {
		public void onDatesModified();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
            mListener = (OnDatesModifiedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDatesModifiedListener");
        }
	}
	
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
		EditHost host = (EditHost) getActivity();
		this.payload = host.getController().getOrgNodePayload();
		
		if(savedInstanceState != null)
			restoreInstanceState(savedInstanceState);
		else
			setupDates();
		
		setModifable(host.getController().isPayloadEditable());
		
		try {
			EditActivity activity = (EditActivity) host;
			activity.invalidateOptionsMenu();
		}
		catch (ClassCastException e) {}	}

	public void setupDates() {
		removeDateEntries();
		payload.getCleanedPayload(); // Hack
		
		addDateScheduled(payload.getScheduled());
		addDateDeadline(payload.getDeadline());
		addDateTimestamp(payload.getTimestamp());
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
			String scheduled = savedInstanceState.getString(DATES_SCHEDULED);
			if (scheduled != null)
				addDateScheduled(scheduled);
			String deadline = savedInstanceState.getString(DATES_DEADLINE);
			if (deadline != null)
				addDateDeadline(deadline);
			String timestamp = savedInstanceState.getString(DATES_TIMESTAMP);
			if (timestamp != null)
				addDateTimestamp(timestamp);
		}
	}


	private void addDateTimestamp(String date) {
		if(date == null || TextUtils.isEmpty(date) == false)
			this.timestampEntry = getDateTableRow(date, OrgNodeTimeDate.TYPE.Timestamp);
	}
	
	private void addDateScheduled(String date) {
		if(date == null || TextUtils.isEmpty(date) == false)
			this.scheduledEntry = getDateTableRow(date, OrgNodeTimeDate.TYPE.Scheduled);
	}
	
	private void addDateDeadline(String date) {
		if(date == null || TextUtils.isEmpty(date) == false)
			this.deadlineEntry = getDateTableRow(date, OrgNodeTimeDate.TYPE.Deadline);
	}
	
	private DateTableRow getDateTableRow(String date, OrgNodeTimeDate.TYPE type) {
		DateTableRow dateEntry = new DateTableRow(getActivity());
		OrgNodeTimeDate timeDate = new OrgNodeTimeDate(type);
		timeDate.parseDate(date);

		dateEntry.init(this, datesView, timeDate);
		dateEntry.setDateTableRowListener(new DateTableRowListener() {
			@Override
			public void onDateTableRowModified(OrgNodeTimeDate.TYPE type) {
				announceDateModified(type);
			}
		});

		return dateEntry;
	}

	
	private void removeDateEntries() {
		if(this.scheduledEntry != null) {
			datesView.removeView(scheduledEntry);
			scheduledEntry = null;
		}
		if(this.deadlineEntry != null) {
			datesView.removeView(deadlineEntry);
			deadlineEntry = null;
		}
		if(this.timestampEntry != null) {
			datesView.removeView(timestampEntry);
			timestampEntry = null;
		}
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

	private void announceDateModified(OrgNodeTimeDate.TYPE type) {
		switch (type) {
		case Scheduled:
			payload.insertOrReplaceDate(type, getScheduled());
			break;
		case Deadline:
			payload.insertOrReplaceDate(type, getDeadline());
			break;
		case Timestamp:
			payload.insertOrReplaceDate(type, getTimestamp());
			break;
		default:
			return;
		}
		
		if(mListener != null)
			mListener.onDatesModified();
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
			addDateScheduled(null);
			announceDateModified(OrgNodeTimeDate.TYPE.Scheduled);
			return true;

		case R.id.menu_nodeedit_deadline:
			addDateDeadline(null);
			announceDateModified(OrgNodeTimeDate.TYPE.Deadline);
			return true;
			
		case R.id.menu_nodeedit_timestamp:
			addDateTimestamp(null);
			announceDateModified(OrgNodeTimeDate.TYPE.Timestamp);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public String getScheduled() {
		if(this.scheduledEntry != null)
			return this.scheduledEntry.toString();
		else
			return "";
	}
	
	public String getDeadline() {
		if(this.deadlineEntry != null)
			return this.deadlineEntry.toString();
		else
			return "";
	}
	
	public String getTimestamp() {
		if(this.timestampEntry != null)
			return this.timestampEntry.toString();
		return "";
	}
}
