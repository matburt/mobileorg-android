package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgFileParser;

@SuppressLint("NewApi")
public class FileDecryptionActivity extends Activity
{
    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;
    private static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
    private static final int DECRYPT_MESSAGE = 0x21070001;
    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";

    private String filename;
    private String filenameAlias;
    private String checksum;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(isAvailable() == false)
			return;
		
		Intent intent = getIntent();
		
		this.filename = intent.getStringExtra("filename");
		this.filenameAlias = intent.getStringExtra("filenameAlias");
		this.checksum = intent.getStringExtra("checksum");
		byte[] data = intent.getByteArrayExtra("data");
		
		if (data == null)
			return;
		
		Intent APGintent = new Intent(DECRYPT_AND_RETURN);
		APGintent.setType("text/plain");
		APGintent.putExtra(FileDecryptionActivity.EXTRA_DATA, data);
		// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("animateTransitions", true)) {
			overridePendingTransition(0, 0);
		}	

		try {
			startActivityForResult(APGintent, DECRYPT_MESSAGE);
		} catch (ActivityNotFoundException e) {
			Log.e("MobileOrg", "Error: " + e.getMessage()
					+ " while launching APG intent");
		}
	}
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case FileDecryptionActivity.DECRYPT_MESSAGE:
			if (resultCode != RESULT_OK || intent == null)
				return;

			String decryptedData = intent
					.getStringExtra(FileDecryptionActivity.EXTRA_DECRYPTED_MESSAGE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(decryptedData.getBytes())));

			OrgFileParser parser = new OrgFileParser(
					((MobileOrgApplication) this.getApplication()).getDB());
			parser.parse(filename, filenameAlias, checksum, reader,
					getApplicationContext());

			break;
		}
		finish();
	}
    
	private boolean isAvailable() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					mApgPackageName, 0);
			if (pi.versionCode >= mMinRequiredVersion) {
				return true;
			} else {
				Toast.makeText(this, R.string.apg_version_not_supported,
						Toast.LENGTH_SHORT).show();
			}
		} catch (NameNotFoundException e) {
			Toast.makeText(this, R.string.apg_not_found, Toast.LENGTH_SHORT)
					.show();
		}
		return false;
	}
	
	 @Override
	 public void finish() {
		 super.finish();
		 // Disable transitions if configured
		 if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("animateTransitions", true)) {
			 overridePendingTransition(0, 0);
		 }	
	 }
}