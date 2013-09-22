package com.matburt.mobileorg.Gui.Widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;
import com.matburt.mobileorg.Gui.Capture.EditHost;
import com.matburt.mobileorg.Gui.Capture.LocationFragment;
import com.matburt.mobileorg.OrgData.OrgNode;

public class CaptureWidgetConfig extends SherlockFragmentActivity implements EditHost {

	private int mAppWidgetId;
	private LocationFragment locationFragment;
	private EditText titleView;
	private EditActivityControllerCaptureWidget controller;

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
        
        titleView = (EditText) findViewById(R.id.widget_title);

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
        String title = titleView.getText().toString();
        
		CaptureWidgetProvider.writeConfig(mAppWidgetId, this, locationOlpId, title);
	}
	
	private void cancelConfig() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
	}


	@Override
	public EditActivityController getController() {
		if (this.controller == null)
			this.controller = new EditActivityControllerCaptureWidget();
		return this.controller;
	}
	
	public class EditActivityControllerCaptureWidget extends EditActivityController {

		@Override
		public boolean isNodeEditable() {
			return true;
		}


		@Override
		public OrgNode getOrgNode() {
			return null;
		}

		@Override
		public String getActionMode() {
			return EditActivityController.ACTIONMODE_ADDCHILD;
		}

		@Override
		public boolean isNodeRefilable() {
			return true;
		}

		@Override
		public OrgNode getParentOrgNode() {
			return null;
		}


		@Override
		public void saveEdits(OrgNode newNode) {			
		}

		@Override
		public boolean hasEdits(OrgNode newNode) {
			return false;
		}
	}
}
