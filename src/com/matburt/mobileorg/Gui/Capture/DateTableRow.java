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

	private int year;
	private int monthOfYear;
	private int dayOfMonth;
	private int timeOfDay;
	private int minute;
	private EditDetailsFragment activity;
	private Button endTimeButton;

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener, String title) {
		super(context);
		this.activity = parentFragment;
		final Calendar c = Calendar.getInstance();
		init(context, parentFragment, parentTable, removeListener, title, c.get(Calendar.YEAR),
				c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), 0, 0);
	}

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener, String title, int year, int monthOfYear,
			int dayOfMonth) {
		super(context);
		init(context, parentFragment, parentTable, removeListener, title, year, monthOfYear,
				dayOfMonth, 0, 0);
	}

	public DateTableRow(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener, String title, int year, int monthOfYear,
			int dayOfMonth, int timeOfDay, int minute) {
		super(context);
		init(context, parentFragment, parentTable, removeListener, title, year, monthOfYear,
				dayOfMonth, timeOfDay, minute);
	}

	private void init(Context context, EditDetailsFragment parentFragment,
			TableLayout parentTable, View.OnClickListener removeListener, String title, int year, int monthOfYear,
			int dayOfMonth, int timeOfDay, int minute) {
		this.activity = parentFragment;
		this.year = year;
		this.monthOfYear = monthOfYear;
		this.dayOfMonth = dayOfMonth;
		this.timeOfDay = timeOfDay;
		this.minute = minute;

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
				FragmentTransaction ft = activity.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new DatePickerDialogFragment(
						dateChangeListener);
				newFragment.show(ft, "dialog");
			}
		});

		startTimeButton = (Button) findViewById(R.id.dateTimeStartButton);
		startTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = activity.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new TimePickerDialogFragment(
						startTimeChangeListener);
				newFragment.show(ft, "dialog");
			}
		});
		
		endTimeButton = (Button) findViewById(R.id.dateTimeEndButton);
		endTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = activity.getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new TimePickerDialogFragment(
						endTimeChangeListener);
				newFragment.show(ft, "dialog");
			}
		});
		
		setDate();
		setTime();
	}

	private String getTime() {
		String time = startTimeButton.getText().toString();

		if (time.equals("00:00"))
			return "";
		else
			return " " + time;
	}

	private void setTime(int timeOfDay, int minute) {
		this.timeOfDay = timeOfDay;
		this.minute = minute;
		startTimeButton.setText(String.format("%02d:%02d", timeOfDay, minute));
	}

	private void setTime() {
		startTimeButton.setText(String.format("%02d:%02d", timeOfDay, minute));
	}

	private void setDate() {
		dateButton.setText(String.format("%d-%02d-%02d", year, monthOfYear,
				dayOfMonth));
	}

	private void setDate(int year, int monthOfYear, int dayOfMonth) {
		this.year = year;
		this.monthOfYear = monthOfYear;
		this.dayOfMonth = dayOfMonth;

		setDate();
	}

	private DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			setDate(year, monthOfYear + 1, dayOfMonth);
		}
	};

	private TimePickerDialog.OnTimeSetListener startTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			setTime(hourOfDay, minute);
		}
	};
	
	private TimePickerDialog.OnTimeSetListener endTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			endTimeButton.setText(String.format("%02d:%02d", timeOfDay, minute));		
		}
	};

	private class TimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public TimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new TimePickerDialog(getActivity(), callback, timeOfDay,
					minute, true);
		}
	}

	private class DatePickerDialogFragment extends DialogFragment {
		private OnDateSetListener callback;

		public DatePickerDialogFragment(OnDateSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new DatePickerDialog(getActivity(), callback, year,
					monthOfYear - 1, dayOfMonth);
		}
	}

	public String getDate() {
		return dateButton.getText().toString() + getTime();
	}
}
