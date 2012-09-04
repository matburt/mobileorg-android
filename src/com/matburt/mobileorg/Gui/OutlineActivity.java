package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Services.SyncService;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Settings.WizardActivity;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.provider.OrgContract.OrgData;

public class OutlineActivity extends SherlockActivity
{
    private MobileOrgApplication appInst;

	private Long node_id;
	
	/**
	 * Keeps track of the last selected item chosen from the outline. When the
	 * outline resumes it will remember what node was selected. Purely cosmetic
	 * feature.
	 */
	private int lastSelection = 0;
	
	private ListView listView;
	private OutlineCursorAdapter outlineAdapter;
	private SynchServiceReceiver syncReceiver;

    private boolean emptylist = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.outline);		
		
		this.appInst = (MobileOrgApplication) this.getApplication();
		
		Intent intent = getIntent();
		node_id = intent.getLongExtra("node_id", -1);

		if (this.node_id == -1) {
			if (this.appInst.isSyncConfigured() == false) {
				this.showWizard();
			} else {
				if (!this.checkVersionCode()) {
					this.showUpgradePopup();
				}
			}
		} else {
			if (!this.checkVersionCode()) {
				this.showUpgradePopup();
			}
		}

		listView = (ListView) this.findViewById(R.id.outline_list);
		listView.setOnItemClickListener(outlineClickListener);
		listView.setOnItemLongClickListener(outlineLongClickListener);
		registerForContextMenu(listView);	
		
		this.syncReceiver = new SynchServiceReceiver();
		registerReceiver(this.syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
		
		refreshDisplay();
	}
	
	@Override
	protected void onResume() {
		setTitle();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(this.syncReceiver);
		this.appInst = null;
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
			if(cursor.getCount() == 0)
				finish();
		}

        if (cursor == null || cursor.getCount() < 1) {
            emptylist = true;
            LinkedHashMap<String, String> lhm = new LinkedHashMap<String, String>();
            lhm.put("Synchronize", "Fetch your org data");
            lhm.put("Settings", "Configure MobileOrg");
            lhm.put("Capture", "Capture a new note");
            lhm.put("Website", "Visit the MobileOrg Wiki");
            listView.setAdapter(new HashMapAdapter(lhm, this));
        }
        else {
            emptylist = false;
            startManagingCursor(cursor);
				
            this.outlineAdapter = new OutlineCursorAdapter(this, cursor, appInst.getDB());
            listView.setAdapter(outlineAdapter);
		
            listView.setSelection(lastSelection);
        }
        setTitle();
	}
	
	private void setTitle() {
		this.getSupportActionBar().setTitle("MobileOrg " + appInst.getChangesString());
		if(this.node_id > -1) {
			NodeWrapper node = new NodeWrapper(this.node_id, appInst.getDB());
			final String subTitle = node.constructOlpId().substring("olp:".length());
			this.getSupportActionBar().setSubtitle(subTitle);
		}
	}

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
			runSync();
			return true;

		case R.id.menu_settings:
			return runShowSettings();

		case R.id.menu_outline:
			runExpandSelection(-1);
			return true;

		case R.id.menu_capturechild:
			runEditCaptureNodeChildActivity(this.node_id);
			return true;
			
		case R.id.menu_capture:
			return runEditNewNodeActivity();

		case R.id.menu_search:
			return runSearch();

		case R.id.menu_help:
			runHelp();
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
		return new NodeWrapper(node_id, appInst.getDB()).getFileName().equals(
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
    
    private void runHelp() {
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
    	startActivity(intent);
    }
    
    private void runSync() {
		startService(new Intent(this, SyncService.class));
    }
	
	private boolean runEditNewNodeActivity() {
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_CREATE);
		startActivity(intent);
		return true;
	}
	
	private void runEditNodeActivity(long nodeId) {
		Intent intent = new Intent(this,
				EditActivity.class);
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_EDIT);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private void runEditCaptureNodeChildActivity(long node_id) {
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra("actionMode", EditActivity.ACTIONMODE_ADDCHILD);
		intent.putExtra("node_id", node_id);
		startActivity(intent);
	}

	private void runViewNodeActivity(long nodeId) {		
		Intent intent = new Intent(this, NodeViewActivity.class);
		intent.putExtra("node_id", nodeId);
		startActivity(intent);
	}

	private void runExpandSelection(long id) {
		Intent intent = new Intent(this, OutlineActivity.class);
		intent.putExtra("node_id", id);
		startActivity(intent);
	}
	
	// TODO Enable the deletion of files
//	private void runDeleteFileNode(final long node_id) {	
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setMessage(R.string.outline_delete_prompt)
//				.setCancelable(false)
//				.setPositiveButton(R.string.yes,
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int id) {
//								appInst.getDB().removeFile(node_id);
//								refreshDisplay();
//							}
//						})
//				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int id) {
//						dialog.cancel();
//					}
//				});
//		builder.create().show();
//	}

	private boolean runSearch() {
		return onSearchRequested();
	}
	
	private boolean runShowSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
		return true;
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
	

	private OnItemClickListener outlineClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if (emptylist) {
				if (position == SYNC_OPTION) {
					runSync();
				} else if (position == SETTINGS_OPTION) {
					runShowSettings();
				} else if (position == CAPTURE_OPTION) {
					runEditNewNodeActivity();
				} else if (position == WEBSITE_OPTION) {
					String url = "https://github.com/matburt/mobileorg-android/wiki";
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
				return;
			}

			Long clicked_node_id = listView.getItemIdAtPosition(position);
			lastSelection = position;
			if (appInst.getDB().hasNodeChildren(clicked_node_id) || node_id == -1)
				runExpandSelection(clicked_node_id);
			else 
				runEditNodeActivity(clicked_node_id);
		}
	};
	
    private static final int SYNC_OPTION = 0;
    private static final int SETTINGS_OPTION = 1;
    private static final int CAPTURE_OPTION = 2;
    private static final int WEBSITE_OPTION = 3;

    private class HashMapAdapter extends BaseAdapter {
        private LinkedHashMap<String, String> mData = new LinkedHashMap<String, String>();
        private String[] mKeys;
        private LayoutInflater mInflater;

        public HashMapAdapter(LinkedHashMap<String, String> data, OutlineActivity act){
            mData  = data;
            mKeys = mData.keySet().toArray(new String[data.size()]);
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
            public int getCount() {
            return mData.size();
        }

        @Override
            public Object getItem(int position) {
            return mData.get(mKeys[position]);
        }

        @Override
            public long getItemId(int arg0) {
            return arg0;
        }

        @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
            String key = mKeys[pos];
            String value = getItem(pos).toString();

            View row;
 
            row = mInflater.inflate(R.layout.simple_list_item, null);
 
            TextView slitem = (TextView) row.findViewById(R.id.sl_item);
            slitem.setText(key);
            TextView slinfo = (TextView) row.findViewById(R.id.sl_info);
            slinfo.setText(value);
            return row;
        }
    }
}
