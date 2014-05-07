package com.matburt.mobileorg.Gui.Outline;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Agenda.AgendasActivity;
import com.matburt.mobileorg.Gui.Wizard.WizardActivity;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

public class OutlineActivity extends SherlockActivity {

	public final static String NODE_ID = "node_id";
	private final static String OUTLINE_NODES = "nodes";
	private final static String OUTLINE_CHECKED_POS = "selection";
	private final static String OUTLINE_SCROLL_POS = "scrollPosition";

    public final static String SYNC_FAILED = "com.matburt.mobileorg.SYNC_FAILED";

	private Long node_id;
		
	private OutlineListView listView;

	private SynchServiceReceiver syncReceiver;
	private MenuItem synchronizerMenuItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.outline);
				
		Intent intent = getIntent();
		node_id = intent.getLongExtra(NODE_ID, -1);

		if (this.node_id == -1)
			displayNewUserDialogs();
		setupList();

		this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
		
		refreshDisplay();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLongArray(OUTLINE_NODES, listView.getState());
		outState.putInt(OUTLINE_CHECKED_POS, listView.getCheckedItemPosition());
		outState.putInt(OUTLINE_SCROLL_POS, listView.getFirstVisiblePosition());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		long[] state = savedInstanceState.getLongArray(OUTLINE_NODES);
		if(state != null)
			listView.setState(state);
		
		int checkedPos= savedInstanceState.getInt(OUTLINE_CHECKED_POS, 0);
		listView.setItemChecked(checkedPos, true);
		
		int scrollPos = savedInstanceState.getInt(OUTLINE_SCROLL_POS, 0);
		listView.setSelection(scrollPos);

	}

	private void setupList() {
		listView = (OutlineListView) findViewById(R.id.outline_list);
		listView.setActivity(this);
		listView.setEmptyView(findViewById(R.id.outline_list_empty));
	}
	
	private void displayNewUserDialogs() {
		if (PreferenceUtils.isSyncConfigured() == false)
			runShowWizard(null);

		if (PreferenceUtils.isUpgradedVersion())
			showUpgradePopup();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshTitle();
	}

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(SYNC_FAILED)) {
            Bundle extrasBundle = intent.getExtras();
            String errorMsg = extrasBundle.getString("ERROR_MESSAGE");
            showSyncFailPopup(errorMsg);
        }
    }

	@Override
	protected void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		super.onDestroy();
	}
		
	public void refreshDisplay() {
		this.listView.refresh();
		refreshTitle();
	}
	
	
	private void refreshTitle() {
		this.getSupportActionBar().setTitle("MobileOrg " + getChangesString());
	}
    
    private String getChangesString() {
    	int changes = OrgProviderUtils.getChangesCount(getContentResolver());
    	if(changes > 0)
    		return "[" + changes + "]";
    	else
    		return "";
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.outline_menu, menu);
	    
	    synchronizerMenuItem = menu.findItem(R.id.menu_sync);
	    
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			listView.collapseCurrent();
			return true;
			
		case R.id.menu_sync:
			runSynchronize(null);
			return true;

		case R.id.menu_settings:
			runShowSettings(null);
			return true;

		case R.id.menu_outline:
			runExpandableOutline(-1);
			return true;

		case R.id.menu_agenda:
			runAgenda();
			return true;
			
		case R.id.menu_capturechild:
			OutlineActionMode.runCaptureActivity(listView.getCheckedNodeId(), this);
			return true;

		case R.id.menu_search:
			return runSearch();

		case R.id.menu_help:
			runHelp(null);
			return true;
		}
		return false;
	}

	public void runHelp(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
    	startActivity(intent);
    }
    
    public void runSynchronize(View view) {
		startService(new Intent(this, SyncService.class));
    }

	public void runShowSettings(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
    public void runShowWizard(View view) {
        startActivity(new Intent(this, WizardActivity.class));
    }
    
    
    private void runExpandableOutline(long id) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putExtra(OutlineActivity.NODE_ID, id);
		startActivity(intent);
    }

    private void runAgenda() {
        startActivity(new Intent(this, AgendasActivity.class));
    }

	private boolean runSearch() {
		return onSearchRequested();
	}

	private void showUpgradePopup() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(OrgUtils.getStringFromResource(R.raw.upgrade, this));
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}

    private void showSyncFailPopup(String errorMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(errorMsg);
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}

	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean syncStart = intent.getBooleanExtra(Synchronizer.SYNC_START, false);
			boolean syncDone = intent.getBooleanExtra(Synchronizer.SYNC_DONE, false);
			boolean showToast = intent.getBooleanExtra(Synchronizer.SYNC_SHOW_TOAST, false);
			int progress = intent.getIntExtra(Synchronizer.SYNC_PROGRESS_UPDATE, -1);
			
			if(syncStart) {
				synchronizerMenuItem.setVisible(false);
				setSupportProgress(Window.PROGRESS_START);
				setSupportProgressBarIndeterminate(true);
				setSupportProgressBarIndeterminateVisibility(true);
			} else if (syncDone) {
				setSupportProgressBarVisibility(false);
				setSupportProgressBarIndeterminateVisibility(false);
				refreshDisplay();
				synchronizerMenuItem.setVisible(true);

				if (showToast)
					Toast.makeText(context,
							R.string.sync_successful,
							Toast.LENGTH_SHORT).show();
			} else if (progress >= 0 && progress <= 100) {
				if(progress == 100)
					setSupportProgressBarIndeterminateVisibility(false);
				
				setSupportProgressBarIndeterminate(false);
				int normalizedProgress = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * progress;
				setSupportProgress(normalizedProgress);
				refreshDisplay();
			}
		}
	}
}
