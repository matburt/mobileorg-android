package com.matburt.mobileorg.Gui.Capture;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

public class EditActivity extends SherlockFragmentActivity implements EditHost,
		PayloadFragment.OnPayloadModifiedListener,
		DatesFragment.OnDatesModifiedListener {

	private EditActivityController controller;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit);
		getSupportActionBar().setTitle(R.string.menu_capture);
				
		SyncService.stopAlarm(this); // Don't run background sync while editing node
		
		controller = EditActivityController.getController(getIntent(),
				getContentResolver(), PreferenceUtils.getDefaultTodo());
		invalidateOptionsMenu();
	}
	
	public EditActivityController getController() {
		return this.controller;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		SyncService.startAlarm(this);
	}
	
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.edit, menu);

		if(controller != null && controller.isNodeEditable() == false)
			menu.findItem(R.id.nodeedit_save).setVisible(false);
	    
    	return super.onCreateOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			doCancel();
			return true;
			
		case R.id.nodeedit_save:
			saveEdits();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
		}
		return false;
	}

	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(hasEdits() == false) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.prompt_node_edit)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}
	
	public boolean hasEdits() {
		OrgNode newNode = getEditedNode();
		return controller.hasEdits(newNode);
	}

	
	public void saveEdits() {
		OrgNode newNode = getEditedNode();
		controller.saveEdits(newNode);
		
		OrgUtils.announceSyncDone(this);
	}
	
	public OrgNode getEditedNode() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		
		HeadingFragment headingFragment = (HeadingFragment) fragmentManager
				.findFragmentByTag("headingFragment");
		OrgNode newNode = headingFragment.getEditedOrgNode();

		TagsFragment tagsFragment = (TagsFragment) fragmentManager
				.findFragmentByTag("tagsFragment");
		newNode.tags = tagsFragment.getTags();

		LocationFragment locationFragment = (LocationFragment) fragmentManager
				.findFragmentByTag("locationFragment");
		OrgNode newParent = locationFragment.getLocationSelection();
		newNode.parentId = newParent.id;
		newNode.fileId = newParent.fileId;

		PayloadFragment payloadFragment = (PayloadFragment) fragmentManager
				.findFragmentByTag("payloadFragment");
		newNode.setPayload(payloadFragment.getPayload());

		return newNode;
	}
	

	@Override
	public void onDatesModified() {
		PayloadFragment payloadFragment = (PayloadFragment) getSupportFragmentManager()
				.findFragmentByTag("payloadFragment");
		payloadFragment.switchToView();
	}
	
	@Override
	public void onPayloadModified() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setupDates();
	}

	@Override
	public void onPayloadStartedEdit() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setModifable(false);
	}

	@Override
	public void onPayloadEndedEdit() {
		DatesFragment datesFragment = (DatesFragment) getSupportFragmentManager()
				.findFragmentByTag("datesFragment");
		datesFragment.setModifable(true);
	}
}
