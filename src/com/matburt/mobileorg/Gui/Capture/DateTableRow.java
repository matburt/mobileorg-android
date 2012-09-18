package com.matburt.mobileorg.Gui.Capture;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TimePicker;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;

public class DateTableRow extends TableRow {
	private Context context;

	private DatesFragment parentFragment;
	private TableLayout parentLayout;
	private DateTableRowListener listener = null;

	private Button dateButton;
	private Button startTimeButton;
	private Button endTimeButton;
	private Button removeButton;

	private OrgNodeTimeDate timeDate;
	
	public interface DateTableRowListener {
		public abstract void onDateTableRowModified(OrgNodeTimeDate.TYPE type);
	}
	
	public void setDateTableRowListener(DateTableRowListener listener) {
		this.listener = listener;
	}
	
	public DateTableRow(Context context) {
		super(context);
		this.context = context;
	}

	
	public void init(DatesFragment parentFragment, TableLayout parentLayout,
			OrgNodeTimeDate timeDate) {
		this.timeDate = timeDate;
		this.parentFragment = parentFragment;
		this.parentLayout = parentLayout;
		
		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) layoutInflater.inflate(R.layout.edit_daterow,
				this);
		parentLayout.addView(row);
		
		prepareDateImage();
		prepareButtons();
		refreshDates();
	}
	
	public void setModifiable(boolean enabled) {
		this.dateButton.setEnabled(enabled);
		this.startTimeButton.setEnabled(enabled);
		this.endTimeButton.setEnabled(enabled);
		if(enabled)
			this.removeButton.setVisibility(View.VISIBLE);
		else
			this.removeButton.setVisibility(View.GONE);
	}

	public void refreshDates() {
		if(timeDate.year == -1)
			this.timeDate.setToCurrentDate();
			
		this.dateButton.setText(this.timeDate.getDate());
		
		if (timeDate.startTimeOfDay != -1
				|| timeDate.startMinute != -1)
			startTimeButton.setText(this.timeDate.getStartTime());
		
		if (timeDate.endTimeOfDay != -1
				|| timeDate.endMinute != -1)
			endTimeButton.setText(this.timeDate.getEndTime());
	}

	private void prepareDateImage() {
		ImageView imageView = (ImageView) findViewById(R.id.dateImage);
		
		if(this.timeDate.type.equals(OrgNodeTimeDate.TYPE.Deadline))
			imageView.setImageResource(R.drawable.ic_menu_today);
		else if(this.timeDate.type.equals(OrgNodeTimeDate.TYPE.Scheduled))
			imageView.setImageResource(R.drawable.ic_menu_month);
		else
			imageView.setImageResource(R.drawable.ic_menu_recent_history);
	}
	
	private void remove() {
		parentLayout.removeView(this);
		
		switch (this.timeDate.type) {
		case Scheduled:
			parentFragment.scheduledEntry = null;
			break;
		case Deadline:
			parentFragment.deadlineEntry = null;
			break;
		case Timestamp:
			parentFragment.timestampEntry = null;
			break;
		default:
			break;
		}
		
		notifyListenerOfChange();
	}


	private void notifyListenerOfChange() {
		refreshDates();
		if(this.listener != null)
			this.listener.onDateTableRowModified(this.timeDate.type);
	}

	public String toString() {
		return this.timeDate.toString();
	}
	
	
	private void prepareButtons() {
		removeButton = (Button) findViewById(R.id.dateRemove);
		removeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				remove();
			}});
		
		dateButton = (Button) findViewById(R.id.dateButton);
		dateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FragmentTransaction ft = parentFragment.getSherlockActivity().getSupportFragmentManager()
						.beginTransaction();
				DialogFragment newFragment = new DatePickerDialogFragment(
						dateChangeListener);
				newFragment.show(ft, "dateDialog");
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
	
	
	private TimePickerDialog.OnTimeSetListener startTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			setStartTimeCallback(hourOfDay, minute);
		}
	};

	private DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			setDateCallback(year, monthOfYear + 1, dayOfMonth);
		}
	};

	private TimePickerDialog.OnTimeSetListener endTimeChangeListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			setEndTimeCallback(hourOfDay, minute);
		}
	};
	
	
	private void setStartTimeCallback(int timeOfDay, int minute) {
		this.timeDate.startTimeOfDay = timeOfDay;
		this.timeDate.startMinute = minute;
		notifyListenerOfChange();
	}
	
	private void setEndTimeCallback(int timeOfDay, int minute) {
		this.timeDate.endTimeOfDay = timeOfDay;
		this.timeDate.endMinute = minute;
		notifyListenerOfChange();
	}

	private void setDateCallback(int year, int monthOfYear, int dayOfMonth) {
		this.timeDate.year = year;
		this.timeDate.monthOfYear = monthOfYear;
		this.timeDate.dayOfMonth = dayOfMonth;
		notifyListenerOfChange();
	}
	
	public class StartTimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public StartTimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int timeOfDay= timeDate.startTimeOfDay;
			int minute = timeDate.startMinute;
			
			if(timeOfDay == -1 || minute == -1) {
				timeOfDay = 12;
				minute = 0;
			}
				
			return new TimePickerDialog(getActivity(), callback, timeOfDay,
					minute, true);
		}
	}
	
	public class EndTimePickerDialogFragment extends DialogFragment {
		private OnTimeSetListener callback;

		public EndTimePickerDialogFragment(OnTimeSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int timeOfDay= timeDate.endTimeOfDay;
			int minute = timeDate.endMinute;
			int startTimeOfDay = timeDate.startTimeOfDay;
			int startMinute = timeDate.startMinute;
			
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

	public class DatePickerDialogFragment extends DialogFragment {
		private OnDateSetListener callback;

		public DatePickerDialogFragment(OnDateSetListener callback) {
			this.callback = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new DatePickerDialog(getActivity(), callback, timeDate.year,
					timeDate.monthOfYear - 1, timeDate.dayOfMonth);
		}
	}
}
