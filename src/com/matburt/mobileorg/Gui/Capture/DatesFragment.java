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

	private OrgNode node;
	
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
		EditActivity editActivity = (EditActivity) getActivity();
		this.node = editActivity.getOrgNode();
		
		if(savedInstanceState != null)
			restoreInstanceState(savedInstanceState);
		else
			setupDates();
		
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
				this.timestampEntry = getDateTableRow(timestamp, "",
						timestampRemoveListener);
			String scheduled = savedInstanceState.getString(DATES_SCHEDULED);
			if (scheduled != null)
				this.scheduledEntry = getDateTableRow(scheduled, "SCHEDULED",
						scheduledRemoveListener);
			String deadline = savedInstanceState.getString(DATES_DEADLINE);
			if (deadline != null)
				this.deadlineEntry = getDateTableRow(deadline, "DEADLINE",
						deadlineRemoveListener);
		}
	}

	public void setupDates() {
		removeDateEntries();
		OrgNodePayload payload = this.node.getOrgNodePayload();
		payload.getCleanedPayload(); // Hack
		
		addDateScheduled(payload.getScheduled());
		addDateDeadline(payload.getDeadline());
		addDateTimestamp(payload.getTimestamp());
	}

	private void addDateTimestamp(String date) {
		this.timestampEntry = getDateTableRow(date, "", timestampRemoveListener);
	}
	
	private void addDateScheduled(String date) {
		this.scheduledEntry = getDateTableRow(date, "SCHEDULED",
				scheduledRemoveListener);
	}
	
	private void addDateDeadline(String date) {
		this.deadlineEntry = getDateTableRow(date, "DEADLINE",
				deadlineRemoveListener);
	}
	
	private DateTableRow getDateTableRow(String date, String title,
			View.OnClickListener removeListener) {
		try {
			DateTableRow dateEntry = new DateTableRow(getActivity());
			if (date != null) {
				OrgNodeTimeDate timeDate = new OrgNodeTimeDate();
				timeDate.parseDate(date);
				dateEntry.set(this, datesView, removeListener, title, timeDate);
			}
			else
				dateEntry.set(this, datesView, removeListener, title);

			dateEntry.setDateTableRowListener(new DateTableRowListener() {
				@Override
				public void onDateTableRowModified() {
					announceDatesModified();
				}
			});

			return dateEntry;
		} catch (IllegalArgumentException e) {
			return null;
		}
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
	
	private View.OnClickListener timestampRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(timestampEntry);
			timestampEntry = null;
			announceDatesModified();
		}
	};
	
	private View.OnClickListener scheduledRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(scheduledEntry);
			scheduledEntry = null;
			announceDatesModified();
		}
	};
	
	private View.OnClickListener deadlineRemoveListener = new View.OnClickListener() {
		public void onClick(View v) {
			datesView.removeView(deadlineEntry);
			deadlineEntry = null;
			announceDatesModified();
		}
	};

	private void announceDatesModified() {
		this.node.getOrgNodePayload().modifyDates(getScheduled(), getDeadline(), getTimestamp());
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
			return true;

		case R.id.menu_nodeedit_deadline:
			addDateDeadline(null);
			return true;
			
		case R.id.menu_nodeedit_timestamp:
			addDateTimestamp(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
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
