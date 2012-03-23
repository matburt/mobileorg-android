package com.matburt.mobileorg.Services;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class TimeclockDialog extends FragmentActivity {

	private NodeWrapper node;
	private int hour = 0;
	private int minute = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.timeclock_dialog);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, 
                android.R.drawable.ic_dialog_alert);
        
        Intent intent = getIntent();
        long node_id = intent.getLongExtra(TimeclockService.NODE_ID, -1);

        MobileOrgApplication appInst = (MobileOrgApplication) getApplication();
        OrgDatabase db = appInst.getDB();
		this.node = new NodeWrapper(db.getNode(node_id));

		String elapsedTime = TimeclockService.getInstance().getElapsedTimeString();
		parseElapsedTime(elapsedTime);
        
		setTitle("MobileOrg Timeclock");
		TextView textView = (TextView) findViewById(R.id.timeclock_text);
		textView.setText(node.getName() + "@" + elapsedTime);
		node.close();
        
        Button button = (Button) findViewById(R.id.timeclock_cancel);
        button.setOnClickListener(cancelListener);
        button = (Button) findViewById(R.id.timeclock_edit);
        button.setOnClickListener(editListener);
        button = (Button) findViewById(R.id.timeclock_save);
        button.setOnClickListener(saveListener);
	}
	
	private void parseElapsedTime(String elapsedTime) {
		String[] split = elapsedTime.trim().split(":");
		try {
			this.hour = Integer.parseInt(split[0]);
			this.minute = Integer.parseInt(split[1]);
		} catch(NumberFormatException e) {
		}
	}

	private void saveClock(int hour, int minute) {
		long startTime = TimeclockService.getInstance().getStartTime();
		node.addLogbook(startTime, hour, minute);
	}
	
	private void endTimeclock() {
		TimeclockService.getInstance().cancelNotification();
		finish();
	}

	
	private View.OnClickListener cancelListener = new View.OnClickListener() {
		public void onClick(View v) {
			endTimeclock();
		}
	};
	
	private View.OnClickListener saveListener = new View.OnClickListener() {
		public void onClick(View v) {
			saveClock(hour, minute);
			endTimeclock();
		}
	};

	
	private View.OnClickListener editListener = new View.OnClickListener() {
		public void onClick(View v) {
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			DialogFragment newFragment = new EditTimePickerFragment();
			if(newFragment != null)
				newFragment.show(ft, "TimeDialog");
		}
	};
	
	private class EditTimePickerFragment extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			if(hour <= 23 && minute <= 59)
				return new TimePickerDialog(getActivity(), timeEditListener, hour,
						minute, true);
			else
				return null;
		}

		private TimePickerDialog.OnTimeSetListener timeEditListener = new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hour, int minute) {
				saveClock(hour, minute);
				endTimeclock();
			}
		};
	}
}
