package com.matburt.mobileorg.Gui;

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
import android.database.Cursor;
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
import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtil;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Settings.WizardActivity;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.util.OrgUtils;

public class OutlineActivity extends SherlockActivity
{
	public final static String NODE_ID = "node_id";
	private ContentResolver resolver;

	private Long node_id;
	
	/**
	 * Keeps track of the last selected item chosen from the outline. When the
	 * outline resumes it will remember what node was selected. Purely cosmetic
	 * feature.
	 */
	private int lastSelection = 0;
	
	private ListView listView;
	private SynchServiceReceiver syncReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.outline);
		this.resolver = getContentResolver();
				
		Intent intent = getIntent();
		node_id = intent.getLongExtra(NODE_ID, -1);

		if (this.node_id == -1) {
			displayNewUserDialog();
			if (OrgUtils.isSyncConfigured(this) == false)
				showWizard();
		}

		listView = (ListView) this.findViewById(R.id.outline_list);
		listView.setOnItemClickListener(outlineClickListener);
		listView.setOnItemLongClickListener(outlineLongClickListener);
		listView.setEmptyView(findViewById(R.id.outline_list_empty));
		registerForContextMenu(listView);	
		
		this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
		
		refreshDisplay();
	}
	
	private void displayNewUserDialog() {
		if (!checkVersionCode()) {
			showUpgradePopup();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setTitle();
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
		final String outlineSort = node_id > 0 ? OrgData.DEFAULT_SORT 
											   : OrgData.NAME_SORT;
		
		Cursor cursor = getContentResolver().query(
				OrgData.buildChildrenUri(node_id.toString()),
				OrgData.DEFAULT_COLUMNS, null, null, outlineSort);

		if (node_id >= 0) {
			if(cursor.getCount() == 0) {
				finish();
				return;
			}
		}

		startManagingCursor(cursor);

		listView.setAdapter(new OutlineCursorAdapter(this, cursor, getContentResolver()));
		listView.setSelection(lastSelection);
     
        setTitle();
	}
	
    
    private String getChangesString() {
    	int changes = OrgProviderUtil.getChangesCount(getContentResolver());
    	if(changes > 0)
    		return "[" + changes + "]";
    	else
    		return "";
    }
	
	private void setTitle() {
		this.getSupportActionBar().setTitle("MobileOrg " + getChangesString());
		if(this.node_id > -1) {
			OrgNode node = new OrgNode(this.node_id, getContentResolver());
			final String subTitle = node.constructOlpId(getContentResolver()).substring("olp:".length());
			this.getSupportActionBar().setSubtitle(subTitle);
		}
	}

	private OnItemClickListener outlineClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			Long clicked_node_id = listView.getItemIdAtPosition(position);
			lastSelection = position;
			if (OrgNode.hasChildren(clicked_node_id, resolver))
				runExpandSelection(clicked_node_id);
			else 
				runEditNodeActivity(clicked_node_id);
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.outline_menu, menu);
	    
	    if(this.node_id == -1 || isNodeInFile(this.node_id, "agendas.org"))
			menu.findItem(R.id.menu_capturechild).setVisible(false);
	    
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
			runExpandSelection(-1);
			return true;

		case R.id.menu_capturechild:
			runEditCaptureNodeChildActivity(this.node_id);
			return true;
			
		case R.id.menu_capture:
			runEditNewNodeActivity(null);
			return true;

		case R.id.menu_search:
			return runSearch();

		case R.id.menu_help:
			runHelp(null);
			return true;
		}
		return false;
	}

	private OnItemLongClickListener outlineLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v, int position,
				long id) {
			long node_id = listView.getItemIdAtPosition(position);
			
	        SharedPreferences appSettings =
	                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        if(appSettings.getBoolean("viewDefaultEdit", false))
	        	runEditNodeActivity(node_id);
	        else
	        	runViewNodeActivity(node_id);
			return true;
		}
	};
	
	
	private boolean isNodeInFile(long node_id, String filename) {
		return new OrgNode(node_id, getContentResolver()).getFilename(getContentResolver()).equals(
				filename);
	}

    private void showWizard() {
        startActivityForResult(new Intent(this, WizardActivity.class), 0);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!this.checkVersionCode()) {
            this.showUpgradePopup();
        }
    }
    
    public void runHelp(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
    	startActivity(intent);
    }
    
    public void runSynchronize(View view) {
		startService(new Intent(this, SyncService.class));
    }
	
	public void runEditNewNodeActivity(View view) {
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra(EditActivity.ACTIONMODE, EditActivity.ACTIONMODE_CREATE);
		startActivity(intent);
	}
	
	private void runEditNodeActivity(long nodeId) {
		Intent intent = new Intent(this,
				EditActivity.class);
		intent.putExtra(EditActivity.ACTIONMODE, EditActivity.ACTIONMODE_EDIT);
		intent.putExtra(EditActivity.NODE_ID, nodeId);
		startActivity(intent);
	}

	private void runEditCaptureNodeChildActivity(long node_id) {
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra(EditActivity.ACTIONMODE, EditActivity.ACTIONMODE_ADDCHILD);
		intent.putExtra(EditActivity.NODE_ID, node_id);
		startActivity(intent);
	}

	private void runViewNodeActivity(long nodeId) {		
		Intent intent = new Intent(this, ViewActivity.class);
		intent.putExtra(ViewActivity.NODE_ID, nodeId);
		startActivity(intent);
	}

	private void runExpandSelection(long id) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putExtra(OutlineActivity.NODE_ID, id);
		startActivity(intent);
	}
	
	private void runDeleteFileNode(final long node_id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_delete_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								OrgFile file = new OrgFile(node_id, resolver);
								file.removeFile();
								refreshDisplay();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		builder.create().show();
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
