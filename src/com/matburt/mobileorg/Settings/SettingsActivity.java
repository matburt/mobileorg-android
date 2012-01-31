package com.matburt.mobileorg.Settings;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class SettingsActivity extends PreferenceActivity {

	OrgDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent prefsIntent = getIntent();
		int resourceID = prefsIntent.getIntExtra("prefs", R.xml.preferences);
		addPreferencesFromResource(resourceID);

		this.db = ((MobileOrgApplication) this.getApplication()).getDB();

		populateSyncSources();
		populateTodoKeywords();

		findPreference("clearDB").setOnPreferenceClickListener(onClearDBClick);
	}

	private Preference.OnPreferenceClickListener onClearDBClick = new Preference.OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			new AlertDialog.Builder(SettingsActivity.this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.preference_clear_db_dialog_title)
					.setMessage(R.string.preference_clear_db_dialog_message)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									db.clearDB();
								}

							}).setNegativeButton(R.string.no, null).show();
			return false;
		}
	};
	
	private void populateTodoKeywords() {
		ListPreference defaultTodo = (ListPreference) findPreference("defaultTodo");
		
		ArrayList<String> todoList = db.getTodos();
		
		CharSequence[] todos = new CharSequence[todoList.size() + 1];
		int i = 0;
		for(String todo: todoList) {
			todos[i] = todo;
			i++;
		}
		
		todos[i] = "";
		
		defaultTodo.setEntries(todos);
		defaultTodo.setEntryValues(todos);
	}

	protected void populateSyncSources() {
		List<PackageItemInfo> synchronizers = discoverSynchronizerPlugins(this);

		ListPreference syncSource = (ListPreference) findPreference("syncSource");

		// save the items for built-in synchronizer originally
		// retrieved from xml resources
		CharSequence[] entries = new CharSequence[synchronizers.size()
				+ syncSource.getEntries().length];
		CharSequence[] values = new CharSequence[synchronizers.size()
				+ syncSource.getEntryValues().length];
		System.arraycopy(syncSource.getEntries(), 0, entries, 0,
				syncSource.getEntries().length);
		System.arraycopy(syncSource.getEntryValues(), 0, values, 0,
				syncSource.getEntryValues().length);

		// populate the sync source list and prepare Intents for
		// discovered synchronizers
		int offset = syncSource.getEntries().length;
		for (PackageItemInfo info : synchronizers) {
			entries[offset] = info.nonLocalizedLabel;
			values[offset] = info.packageName;
			Intent syncIntent = new Intent(this, SettingsActivity.class);
			SynchronizerPreferences.syncIntents.put(info.packageName,
					syncIntent);
			offset++;
		}

		// fill in the Intents for built-in synchronizers
		Intent synchroIntent = new Intent(getApplicationContext(), WebDAVSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("webdav", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(), SDCardSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("sdcard", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(), DropboxSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("dropbox", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(), ScpSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("scp", synchroIntent);

		// populate the sync source list with updated data
		syncSource.setEntries(entries);
		syncSource.setEntryValues(values);
	}

	private static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";

	private static List<PackageItemInfo> discoverSynchronizerPlugins(
			Context context) {
		Intent discoverSynchro = new Intent(SYNCHRONIZER_PLUGIN_ACTION);
		List<ResolveInfo> packages = context.getPackageManager()
				.queryIntentActivities(discoverSynchro, 0);

		ArrayList<PackageItemInfo> out = new ArrayList<PackageItemInfo>();

		for (ResolveInfo info : packages)
			out.add(info.activityInfo);

		return out;
	}
}