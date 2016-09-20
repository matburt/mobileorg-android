package com.matburt.mobileorg.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

import com.matburt.mobileorg.orgdata.OrgProviderUtils;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.settings.synchronizers.SDCardSettingsActivity;
import com.matburt.mobileorg.settings.synchronizers.ScpSettingsActivity;
import com.matburt.mobileorg.settings.synchronizers.UbuntuOneSettingsActivity;
import com.matburt.mobileorg.settings.synchronizers.WebDAVSettingsActivity;
import com.matburt.mobileorg.util.OrgUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String KEY_SYNC_SOURCE = "syncSource";
	public static final int SYNCHRONIZER_PREFERENCES = 10;
	private static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";
	private String KEY_AUTO_SYNC_INTERVAL;
	private String KEY_VIEW_RECURSION_MAX;
	private String KEY_DEFAULT_TODO;
	private String KEY_CALENDAR_NAME;
	private String KEY_CALENDAR_REMINDER_INTERVAL;
	private String KEY_THEME;
	private String KEY_FONT_SIZE;
	private String KEY_QUICK_TODOS;
	private SharedPreferences appSettings;
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
									OrgProviderUtils.clearDB(getContentResolver());
								}

							}).setNegativeButton(R.string.no, null).show();
			return false;
		}
	};

	private static List<PackageItemInfo> getSynchronizerPlugins(Context context) {
		Intent discoverSynchro = new Intent(SYNCHRONIZER_PLUGIN_ACTION);
		List<ResolveInfo> packages = context.getPackageManager()
				.queryIntentActivities(discoverSynchro, 0);

		ArrayList<PackageItemInfo> out = new ArrayList<PackageItemInfo>();

		for (ResolveInfo info : packages)
			out.add(info.activityInfo);

		return out;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		OrgUtils.setTheme(this);
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		init();
//		populateSyncSources();
//		populateTodoKeywords();
        populateVersionName();
//		findPreference("clearDB").setOnPreferenceClickListener(onClearDBClick);
	}

	private void init() {
		this.appSettings = getPreferenceScreen().getSharedPreferences();

		this.KEY_AUTO_SYNC_INTERVAL = getString(R.string.key_autoSyncInterval);
		this.KEY_VIEW_RECURSION_MAX = getString(R.string.key_viewRecursionMax);
		this.KEY_DEFAULT_TODO = getString(R.string.key_defaultTodo);
		this.KEY_THEME = getString(R.string.key_theme);
		this.KEY_FONT_SIZE = getString(R.string.key_fontSize);
		this.KEY_QUICK_TODOS = getString(R.string.key_quick_todos);

		initSettings();
	}
	
	private void initSettings() {
		updatePreferenceSummary(KEY_SYNC_SOURCE);
		updatePreferenceSummary(KEY_AUTO_SYNC_INTERVAL);
		updatePreferenceSummary(KEY_VIEW_RECURSION_MAX);
		updatePreferenceSummary(KEY_DEFAULT_TODO);
		updatePreferenceSummary(KEY_CALENDAR_NAME);
		updatePreferenceSummary(KEY_CALENDAR_REMINDER_INTERVAL);
		updatePreferenceSummary(KEY_THEME);
		updatePreferenceSummary(KEY_FONT_SIZE);
		updatePreferenceSummary(KEY_QUICK_TODOS);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updatePreferenceSummary(key);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.appSettings.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		this.appSettings.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	private void updatePreferenceSummary(String key) {
		if (key == null)
			return;

		Preference pref = findPreference(key);
		if (pref == null)
			return;

		String summary = null;

		if (key.equals(KEY_SYNC_SOURCE)) {
			String value = appSettings.getString(key, "");
			summary = OrgUtils.lookUpValueFromArray(this, R.array.fileSources,
					R.array.fileSourcesVals, value);
		} else if (key.equals(KEY_AUTO_SYNC_INTERVAL)) {
			String value = appSettings.getString(key, "");
			summary = OrgUtils.lookUpValueFromArray(this,
					R.array.syncIntervals, R.array.syncIntervalsVals, value);
		} else if (key.equals(KEY_VIEW_RECURSION_MAX)) {
			String value = appSettings.getString(key, "");
			summary = OrgUtils.lookUpValueFromArray(this,
					R.array.viewRecursionLevels,
					R.array.viewRecursionLevelsVals, value);
		} else if (key.equals(KEY_DEFAULT_TODO)) {
			summary = appSettings.getString(key, "");
		} else if (key.equals(KEY_CALENDAR_NAME)) {
			summary = appSettings.getString(key, "");
		} else if (key.equals(KEY_CALENDAR_REMINDER_INTERVAL)) {
			summary = appSettings.getString(key, "");
		} else if (key.equals(KEY_THEME)) {
			summary = appSettings.getString(key, "");
		} else if (key.equals(KEY_FONT_SIZE)) {
			summary = appSettings.getString(key, "");
		} else if (key.equals(KEY_QUICK_TODOS)) {
			summary = appSettings.getString(key, "");
		}

		if (TextUtils.isEmpty(summary) == false)
			pref.setSummary(summary);
	}

    private void populateVersionName() {
        Preference version = findPreference(getResources().getString(R.string.key_version));
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
            version.setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }


	private void populateTodoKeywords() {
		ListPreference defaultTodo = (ListPreference) findPreference(KEY_DEFAULT_TODO);

		ArrayList<String> todoList = OrgProviderUtils.getTodos(getContentResolver());

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

	private void populateSyncSources() {
		List<PackageItemInfo> synchronizers = getSynchronizerPlugins(this);

		ListPreference syncSource = (ListPreference) findPreference(KEY_SYNC_SOURCE);

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
}
