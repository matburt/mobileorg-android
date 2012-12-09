package com.matburt.mobileorg.Settings;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Wizard.WizardActivity;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.Services.CalendarSyncService;
import com.matburt.mobileorg.Services.CalendarWrapper;
import com.matburt.mobileorg.Settings.Synchronizers.SDCardSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.ScpSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.UbuntuOneSettingsActivity;
import com.matburt.mobileorg.Settings.Synchronizers.WebDAVSettingsActivity;
import com.matburt.mobileorg.util.OrgUtils;

public class SettingsActivity extends SherlockPreferenceActivity implements
SharedPreferences.OnSharedPreferenceChangeListener {
	
	public static final String KEY_SYNC_SOURCE = "syncSource";
	public static final String KEY_SYNC_PREF = "syncPref";
	public static final String KEY_AUTO_SYNC_INTERVAL = "autoSyncInterval";
	public static final String KEY_VIEW_RECURSION_MAX = "viewRecursionMax";
	public static final String KEY_DEFAULT_TODO = "defaultTodo";
	public static final String KEY_CALENDAR_NAME = "calendarName";
	public static final String KEY_CALENDAR_REMINDER_INTERVAL = "calendarReminderInterval";
	public static final String KEY_DO_AUTO_SYNC = "doAutoSync";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);

		Intent prefsIntent = getIntent();
		int resourceID = prefsIntent.getIntExtra("prefs", R.xml.preferences);
		addPreferencesFromResource(R.xml.preferences);

		populateSyncSources();
		populateTodoKeywords();
		try {
			populateCalendarNames();
		} catch (Exception e) {
			// Don't crash because of fault in calendar synchronizer!
		}

		findPreference("clearDB").setOnPreferenceClickListener(onClearDBClick);

		SharedPreferences appSettings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		appSettings.registerOnSharedPreferenceChangeListener(this);

		// SynchronizerPreferences sync = (SynchronizerPreferences) findPreference(KEY_SYNC_PREF);
		// sync.setParentActivity(this);

		// Manually invoke so that settings are pre-loaded and sync preference is enabled or disabled as appropriate 
		onSharedPreferenceChanged(appSettings, KEY_SYNC_SOURCE);
		setPreferenceSummary(appSettings, KEY_AUTO_SYNC_INTERVAL);
		setPreferenceSummary(appSettings, KEY_VIEW_RECURSION_MAX);
		setPreferenceSummary(appSettings, KEY_DEFAULT_TODO);
		setPreferenceSummary(appSettings, KEY_CALENDAR_NAME);
		setPreferenceSummary(appSettings, KEY_CALENDAR_REMINDER_INTERVAL);
	}

	@Override
	public void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// Set up the initial values following the Settings design guidelines for Ice Cream Sandwich
		// Settings should show their current value instead of a description
		setPreferenceSummary(sharedPreferences, key);
		if (key.equals(KEY_SYNC_SOURCE)) {
			if (sharedPreferences.getString(key, "").equals("null")) {
				// Disable synchronizer settings
				//findPreference(KEY_SYNC_PREF).setEnabled(false);
				findPreference(KEY_DO_AUTO_SYNC).setEnabled(false);
			} else {
				// Disable synchronizer settings
				//findPreference(KEY_SYNC_PREF).setEnabled(true);
				findPreference(KEY_DO_AUTO_SYNC).setEnabled(true);
			}
		}
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
									OrgProviderUtils
											.clearDB(getContentResolver());
									OrgUtils.announceSyncDone(getApplicationContext());
									
									Intent clearCalDBIntent = new Intent(getBaseContext(), CalendarSyncService.class);
									clearCalDBIntent.putExtra(CalendarSyncService.CLEARDB, true);
									startService(clearCalDBIntent);
								}

							}).setNegativeButton(R.string.no, null).show();
			return false;
		}
	};

	private boolean isCalendarEnabled() {
		return getPreferenceManager().getSharedPreferences().getBoolean(
				"calendarEnabled", false);
	}

	private void populateCalendarNames() {
		ListPreference calendarName = (ListPreference) findPreference("calendarName");

		CharSequence[] calendars = CalendarWrapper.getCalendars(
				getApplicationContext());

		calendarName.setEntries(calendars);
		calendarName.setEntryValues(calendars);
	}

	private void populateTodoKeywords() {
		ListPreference defaultTodo = (ListPreference) findPreference("defaultTodo");

		ArrayList<String> todoList = OrgProviderUtils.getTodos(getContentResolver());;

		CharSequence[] todos = new CharSequence[todoList.size() + 1];
		int i = 0;
		for (String todo : todoList) {
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
		Intent synchroIntent = new Intent(getApplicationContext(),
				WebDAVSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("webdav", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(),
				SDCardSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("sdcard", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(),
				ScpSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("scp", synchroIntent);

		synchroIntent = new Intent(getApplicationContext(),
				UbuntuOneSettingsActivity.class);
		SynchronizerPreferences.syncIntents.put("ubuntu", synchroIntent);

		// populate the sync source list with updated data
		syncSource.setEntries(entries);
		syncSource.setEntryValues(values);
	}

	private static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";
	public static final int SYNCHRONIZER_PREFERENCES = 10;
	
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

	protected void setPreferenceSummary(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);

		if (pref != null) {
			if (key.equals(KEY_SYNC_SOURCE)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(lookUpValue(R.array.fileSources, R.array.fileSourcesVals, value));
			}
			if (key.equals(KEY_AUTO_SYNC_INTERVAL)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(lookUpValue(R.array.syncIntervals, R.array.syncIntervalsVals, value));
			}
			if (key.equals(KEY_VIEW_RECURSION_MAX)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(lookUpValue(R.array.viewRecursionLevels, R.array.viewRecursionLevelsVals, value));
			}
			if (key.equals(KEY_DEFAULT_TODO)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(value);
			}
			if (key.equals(KEY_CALENDAR_NAME)) {
				String value = sharedPreferences.getString(key, "");
				pref.setSummary(value);
			}
		}

	}
	/**
	 * Convenience method for
	 * @param keyID the ID of the StringArray that contains the labels
	 * @param valID the ID of the StringArray that contains the values
	 * @param value the value to search for
	 * @return
	 */
	private String lookUpValue(int keyID, int valID, String value) {
		String[] keys = getResources().getStringArray(keyID);
		String[] values = getResources().getStringArray(valID);
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(value)) {
				return keys[i];
			}
		}
		return null;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SYNCHRONIZER_PREFERENCES) {
        	((SynchronizerPreferences) findPreference(KEY_SYNC_PREF)).setPreferenceSummary();
        }
	}
}
