package com.matburt.mobileorg.Gui.Capture;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import com.matburt.mobileorg.R;

class DateTableRow extends TableRow {
	private Button dateButton;
	private Button startTimeButton;
	private Button endTimeButton;

	private OrgTimeDate timeDateContainer;
	
	public class OrgTimeDate {
		public int year;
		public int monthOfYear;
		public int dayOfMonth;
		public int startTimeOfDay = -1;
		public int startMinute = -1;
		public int endTimeOfDay = -1;
		public int endMinute = -1;
		
		OrgTimeDate() {
			final Calendar c = Calendar.getInstance();
			this.year = c.get(Calendar.YEAR);
			this.monthOfYear = c.get(Calendar.MONTH) + 1;
			this.dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
		}

		public OrgTimeDate(int year, int monthOfYear, int dayOfMonth) {
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
		}

		public OrgTimeDate(int year, int monthOfYear, int dayOfMonth,
				int timeOfDay, int minute) {
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
			this.startTimeOfDay = timeOfDay;
			this.startMinute = minute;
		}

		public OrgTimeDate(int year, int monthOfYear, int dayOfMonth,
				int startTimeOfDay, int startMinute, int endTimeOfDay, int endMinute) {
			this.year = year;
			this.monthOfYear = monthOfYear;
			this.dayOfMonth = dayOfMonth;
			this.startTimeOfDay = startTimeOfDay;
			this.startMinute = startMinute;
			this.endTimeOfDay = endTimeOfDay;
			this.endMinute = endMinute;
		}
	};
	
	
	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener,
			String title) {
		super(context);
		this.timeDateContainer = new OrgTimeDate();
		init(context, parentFragment, parentTable, removeListener, title);
	}

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener,
			String title, int year, int monthOfYear, int dayOfMonth) {
		super(context);
		this.timeDateContainer = new OrgTimeDate(year, monthOfYear, dayOfMonth);
		init(context, parentFragment, parentTable, removeListener, title);
		updateDate();
	}

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener,
			String title, int year, int monthOfYear, int dayOfMonth,
			int timeOfDay, int minute) {
		super(context);
		this.timeDateContainer = new OrgTimeDate(year,
				monthOfYear, dayOfMonth, timeOfDay, minute);
		init(context, parentFragment, parentTable, removeListener, title);
		updateDate();
		updateStartTime();
	}

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener,
			String title, int year, int monthOfYear, int dayOfMonth,
			int timeOfDay, int minute, int endTimeOfDay, int endMinute) {
		super(context);
		this.timeDateContainer = new OrgTimeDate( year,
				monthOfYear, dayOfMonth, timeOfDay, minute, endTimeOfDay, endMinute);
		init(context, parentFragment, parentTable, removeListener, title);

		updateDate();
		updateStartTime();
		updateEndTime();
	}

	private void init(Context context,
			final EditDetailsFragment parentFragment, TableLayout parentTable,
			View.OnClickListener removeListener, String title) {

		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) layoutInflater.inflate(
				R.layout.edit_daterow, this);
		parentTable.addView(row);

		Button removeButton = (Button) findViewById(R.id.dateRemove);
		removeButton.setOnClickListener(removeListener);

		TextView textView = (TextView) findViewById(R.id.dateText);
		textView.setText(title);

		dateButton = (Button) findViewById(R.id.dateButton);
		dateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new DatePickerDialogFragment(
						dateChangeListener);
				newFragment.show(ft, "dialog");
			}
		});

		startTimeButton = (Button) findViewById(R.id.dateTimeStartButton);
		startTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new TimePickerDialogFragment(
						startTimeChangeListener);
				newFragment.show(ft, "dialog");
			}
		});
		
		endTimeButton = (Button) findViewById(R.id.dateTimeEndButton);
		endTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new TimePickerDialogFragment(
						endTimeChangeListener);
				newFragment.show(ft, "dialog");
			}
		});
	}

	private void setStartTime(int timeOfDay, int minute) {
		this.timeDateContainer.startTimeOfDay = timeOfDay;
		this.timeDateContainer.startMinute = minute;
		updateStartTime();
	}

	private void updateStartTime() {
		startTimeButton.setText(String.format("%02d:%02d", this.timeDateContainer.startTimeOfDay, this.timeDateContainer.startMinute));
	}
	
	private void setEndTime(int timeOfDay, int minute) {
		this.timeDateContainer.endTimeOfDay = timeOfDay;
		this.timeDateContainer.endMinute = minute;
		updateEndTime();
	}

	private void updateEndTime() {
		startTimeButton.setText(String.format("%02d:%02d", this.timeDateContainer.endTimeOfDay, this.timeDateContainer.endMinute));
	}

	private void setDate(int year, int monthOfYear, int dayOfMonth) {
		this.timeDateContainer.year = year;
		this.timeDateContainer.monthOfYear = monthOfYear;
		this.timeDateContainer.dayOfMonth = dayOfMonth;

		updateDate();
	}

	private void updateDate() {
		dateButton.setText(String.format("%d-%02d-%02d", this.timeDateContainer.year, this.timeDateContainer.monthOfYear,
				this.timeDateContainer.dayOfMonth));
	}

	private DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			setDate(year, monthOfYear + 1, dayOfMonth);
		}
	};

	private TimePickerDialog.OnTimeSetListener startTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			setStartTime(hourOfDay, minute);
		}
	};
	
	private TimePickerDialog.OnTimeSetListener endTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			setEndTime(hourOfDay, minute);		
		}
	};

	private class TimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public TimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new TimePickerDialog(getActivity(), callback, timeDateContainer.startTimeOfDay,
					timeDateContainer.startMinute, true);
		}
	}

	private class DatePickerDialogFragment extends DialogFragment {
		private OnDateSetListener callback;

		public DatePickerDialogFragment(OnDateSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new DatePickerDialog(getActivity(), callback, timeDateContainer.year,
					timeDateContainer.monthOfYear - 1, timeDateContainer.dayOfMonth);
		}
	}

	
	public String getDate() {
		return dateButton.getText().toString() + getStartTime() + getEndTime();
	}
	
	private String getStartTime() {
		String time = startTimeButton.getText().toString();

		if (time.equals("00:00") || TextUtils.isEmpty(time))
			return "";
		else
			return " " + time;
	}
	
	private String getEndTime() {
		String time = endTimeButton.getText().toString();

		if (time.equals("00:00") || TextUtils.isEmpty(time))
			return "";
		else
			return "-" + time;
	}
}
