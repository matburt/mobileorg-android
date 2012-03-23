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
	private int hour;
	private int minute;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.timeclock_dialog);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, 
                android.R.drawable.ic_dialog_alert);
        
        Intent intent = getIntent();
        long node_id = intent.getLongExtra(TimeclockService.NODE_ID, -1);
        this.hour = intent.getIntExtra(TimeclockService.CLOCK_HOUR, 0);
        this.minute = intent.getIntExtra(TimeclockService.CLOCK_MINUTE, 0);

        MobileOrgApplication appInst = (MobileOrgApplication) getApplication();
        OrgDatabase db = appInst.getDB();
		this.node = new NodeWrapper(db.getNode(node_id));

        
        setTitle("MobileOrg Timeclock");
        TextView textView = (TextView) findViewById(R.id.timeclock_text);
        textView.setText(node.getName());
        node.close();
        
        Button button = (Button) findViewById(R.id.timeclock_cancel);
        button.setOnClickListener(cancelListener);
        button = (Button) findViewById(R.id.timeclock_edit);
        button.setOnClickListener(editListener);
        button = (Button) findViewById(R.id.timeclock_save);
        button.setOnClickListener(saveListener);
	}

	private void saveClock(int hour, int minute) {
		node.addLogbook(hour, minute);
	}
	
	private void endTimeclock() {
		sendBroadcast(new Intent(TimeclockService.TIMECLOCK_CANCEL));
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
			newFragment.show(ft, "TimeDialog");
		}
	};
	
	private class EditTimePickerFragment extends DialogFragment {
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new TimePickerDialog(getActivity(), timeEditListener, hour,
					minute, true);
		}

		private TimePickerDialog.OnTimeSetListener timeEditListener = new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hour, int minute) {
				saveClock(hour, minute);
				endTimeclock();
			}
		};
	}
}
