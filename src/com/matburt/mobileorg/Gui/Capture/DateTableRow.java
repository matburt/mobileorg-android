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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TimePicker;

import com.matburt.mobileorg.R;

public class DateTableRow extends TableRow {
	private Button dateButton;
	private Button startTimeButton;
	private Button endTimeButton;

	private OrgTimeDate timeDateContainer;
	
	public static class OrgTimeDate {
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
			String title, OrgTimeDate timeDateContainer) {
		super(context);
		this.timeDateContainer = timeDateContainer;
		init(context, parentFragment, parentTable, removeListener, title);

		updateDate();
		
		if (timeDateContainer.startTimeOfDay != -1
				|| timeDateContainer.startMinute != -1)
			updateStartTime();
		
		if (timeDateContainer.endTimeOfDay != -1
				|| timeDateContainer.endMinute != -1)
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

		ImageView imageView = (ImageView) findViewById(R.id.dateImage);
		
		if(title.equals("DEADLINE"))
			imageView.setImageResource(R.drawable.ic_menu_today);
		else
			imageView.setImageResource(R.drawable.ic_menu_month);


		dateButton = (Button) findViewById(R.id.dateButton);
		dateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSherlockActivity().getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new DatePickerDialogFragment(
						dateChangeListener);
				newFragment.show(ft, "dateialog");
			}
		});

		startTimeButton = (Button) findViewById(R.id.dateTimeStartButton);
		startTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSherlockActivity().getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new StartTimePickerDialogFragment(
						startTimeChangeListener);
				newFragment.show(ft, "startTimeDialog");
			}
		});
		
		endTimeButton = (Button) findViewById(R.id.dateTimeEndButton);
		endTimeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSherlockActivity().getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new EndTimePickerDialogFragment(
						endTimeChangeListener);
				newFragment.show(ft, "endTimeDialog");
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
		endTimeButton.setText(String.format("%02d:%02d", this.timeDateContainer.endTimeOfDay, this.timeDateContainer.endMinute));
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
			Log.d("MobileOrg", "startTime Listener");
			setStartTime(hourOfDay, minute);
		}
	};
	
	private TimePickerDialog.OnTimeSetListener endTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			Log.d("MobileOrg", "endTime Listener");
			setEndTime(hourOfDay, minute);		
		}
	};

	private class StartTimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public StartTimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int timeOfDay= timeDateContainer.startTimeOfDay;
			int minute = timeDateContainer.startMinute;
			
			if(timeOfDay == -1 || minute == -1) {
				timeOfDay = 12;
				minute = 0;
			}
				
			return new TimePickerDialog(getActivity(), callback, timeOfDay,
					minute, true);
		}
	}
	
	private class EndTimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public EndTimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int timeOfDay= timeDateContainer.endTimeOfDay;
			int minute = timeDateContainer.endMinute;
			int startTimeOfDay = timeDateContainer.startTimeOfDay;
			int startMinute = timeDateContainer.startMinute;
			
			if ((timeOfDay == -1 || minute == -1)) {
				if (startTimeOfDay != -1 && startMinute != -1) {
				timeOfDay = startTimeOfDay + 1;
				minute = startMinute;
				
				if(timeOfDay > 23)
					timeOfDay = 0;
				}
				else {
					timeOfDay = 12;
					minute = 0;
				}
			}
				
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
			return new DatePickerDialog(getActivity(), callback, timeDateContainer.year,
					timeDateContainer.monthOfYear - 1, timeDateContainer.dayOfMonth);
		}
	}

	
	public String getDate() {
		return dateButton.getText().toString() + getStartTime() + getEndTime();
	}
	
	private String getStartTime() {
		String time = startTimeButton.getText().toString();

		if (timeDateContainer.startTimeOfDay == -1
				|| timeDateContainer.startMinute == -1 || TextUtils.isEmpty(time))
			return "";
		else
			return " " + time;
	}
	
	private String getEndTime() {
		String time = endTimeButton.getText().toString();

		if (timeDateContainer.endTimeOfDay == -1
				|| timeDateContainer.endMinute == -1 || TextUtils.isEmpty(time))
			return "";
		else
			return "-" + time;
	}
}
