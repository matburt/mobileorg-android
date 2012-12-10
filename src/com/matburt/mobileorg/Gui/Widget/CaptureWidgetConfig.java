package com.matburt.mobileorg.Gui.Widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.LocationFragment;
import com.matburt.mobileorg.OrgData.OrgNode;

public class CaptureWidgetConfig extends SherlockFragmentActivity {

	private int mAppWidgetId;
	private LocationFragment locationFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.capture_widget_config);
		
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        ((Button) findViewById(R.id.widget_save)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveConfig();
			}
		});
        
        ((Button) findViewById(R.id.widget_cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelConfig();
			}
		});
        
		locationFragment = (LocationFragment) getSupportFragmentManager()
				.findFragmentByTag("captureLocationFragment");
	}

	
	private void saveConfig() {
		save();
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		CaptureWidgetProvider.updateWidget(mAppWidgetId, appWidgetManager, this);

		Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
	}
	
	private void save() {
        OrgNode node = locationFragment.getLocationSelection();
        String locationOlpId = node.getOlpId(getContentResolver());
        
		CaptureWidgetProvider.writeConfig(mAppWidgetId, locationOlpId,
				getApplicationContext());
	}
	
	private void cancelConfig() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
	}
}
