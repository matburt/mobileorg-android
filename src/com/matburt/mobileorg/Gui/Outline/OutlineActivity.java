package com.matburt.mobileorg.Gui.Outline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Settings.WizardActivity;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.util.OrgUtils;

public class OutlineActivity extends SherlockActivity {

	public final static String NODE_ID = "node_id";
	private ContentResolver resolver;

	private Long node_id;
	
	private Context context;
	
	private ListView listView;
	private OutlineActionMode actionMode;
	private OutlineAdapter adapter;
	private SynchServiceReceiver syncReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		
		setContentView(R.layout.outline);
		this.resolver = getContentResolver();
				
		Intent intent = getIntent();
		node_id = intent.getLongExtra(NODE_ID, -1);

		if (this.node_id == -1) {
			displayNewUserDialog();
			if (OrgUtils.isSyncConfigured(this) == false)
				showWizard();
		}

		setupList();

		this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
		
		refreshDisplay();
	}
	
	private void setupList() {
		listView = (ListView) this.findViewById(R.id.outline_list);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnItemClickListener(outlineClickListener);
		listView.setOnItemLongClickListener(outlineLongClickListener);
		listView.setEmptyView(findViewById(R.id.outline_list_empty));
		
		this.actionMode = new OutlineActionMode(this);
		this.adapter = new OutlineAdapter(this);
		listView.setAdapter(adapter);	
	}
	
	private void displayNewUserDialog() {
		if (!checkVersionCode()) {
			showUpgradePopup();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshTitle();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		super.onDestroy();
	}
		
	/**
	 * Refreshes the outline display. Should be called when the underlying 
	 * data has been updated.
	 */
	private void refreshDisplay() {
		adapter.init();
		refreshTitle();
	}
	
    
    private String getChangesString() {
    	int changes = OrgProviderUtils.getChangesCount(getContentResolver());
    	if(changes > 0)
    		return "[" + changes + "]";
    	else
    		return "";
    }
	
	private void refreshTitle() {
		this.getSupportActionBar().setTitle("MobileOrg " + getChangesString());
	}

	private OnItemClickListener outlineClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			OrgNode node = adapter.getItem(position);
			if(node.hasChildren(resolver))
				adapter.collapseExpand(position);
			else
				OutlineActionMode.runEditNodeActivity(context, node.id);
		}
	};
	

	private OnItemLongClickListener outlineLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v, int position,
				long id) {
			actionMode.initActionMode(listView, position);
			startActionMode(actionMode);
			return true;
		}
	};
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.outline_menu, menu);
	    
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if(this.node_id != -1)
				finish();
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

		case R.id.menu_capturechild:
			runCaptureActivity();
			return true;

		case R.id.menu_search:
			return runSearch();

		case R.id.menu_help:
			runHelp(null);
			return true;
		}
		return false;
	}

    private void showWizard() {
        startActivityForResult(new Intent(this, WizardActivity.class), 0);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!this.checkVersionCode()) {
            this.showUpgradePopup();
        }
    }
    
    private void runExpandableOutline(long id) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putExtra(OutlineActivity.NODE_ID, id);
		startActivity(intent);
    }
    
    public void runHelp(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
    	startActivity(intent);
    }
    
    public void runSynchronize(View view) {
		startService(new Intent(this, SyncService.class));
    }


	private void runCaptureActivity() {
		Intent intent = new Intent(this, EditActivity.class);
		
		String captureMode = EditActivity.ACTIONMODE_CREATE;
		if (OrgUtils.useAdvancedCapturing(this)) {
			captureMode = EditActivity.ACTIONMODE_ADDCHILD;
		}
		
		intent.putExtra(EditActivity.ACTIONMODE, captureMode);
		intent.putExtra(EditActivity.NODE_ID, getCheckedNodeId());
		startActivity(intent);
	}

	private long getCheckedNodeId() {
		if(listView.getCheckedItemPosition() == ListView.INVALID_POSITION)
			return -1;
		else {
			int position = listView.getCheckedItemPosition();
			return adapter.getNodeId(position);
		}
	}

	private boolean runSearch() {
		return onSearchRequested();
	}
	
	public void runShowSettings(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(Synchronizer.SYNC_DONE, false)) {
				if (intent.getBooleanExtra("showToast", false))
					Toast.makeText(context,
							R.string.outline_synchronization_successful,
							Toast.LENGTH_SHORT).show();
				refreshDisplay();
			}
		}
	}
	

    private void showUpgradePopup() {
        Log.i("MobileOrg", "Showing upgrade");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(this.getRawContents(R.raw.upgrade));
        builder.setCancelable(false);
		builder.setPositiveButton(R.string.ok,
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int id) {
                                          dialog.dismiss();
                                      }
                                  });
		builder.create().show();
    }

    private String getRawContents(int resource) {
        InputStream is = this.getResources().openRawResource(resource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine = null;
        String contents = "";

        try {
            // While the BufferedReader readLine is not null 
            while ((readLine = br.readLine()) != null) {
                contents += readLine + "\n";
            }

            // Close the InputStream and BufferedReader
            is.close();
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    private boolean checkVersionCode() {
        SharedPreferences appSettings =
            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = appSettings.edit();
        int versionCode = appSettings.getInt("appVersion", 0);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int newVersion = pInfo.versionCode;
            if (versionCode != newVersion) {
                editor.putInt("appVersion", newVersion);
                editor.commit();
                return false;
            }
        } catch (Exception e) { };
        return true;
    }
}
