package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgDatabase;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;

public class FileDecryptionActivity extends Activity
{
    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;
    private static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
    private static final int DECRYPT_MESSAGE = 0x21070001;
    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";

    private String filename;
    private String name;
    private String checksum;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(isAvailable() == false)
			return;
		
		Intent intent = getIntent();
		
		this.filename = intent.getStringExtra("filename");
		this.name = intent.getStringExtra("filenameAlias");
		this.checksum = intent.getStringExtra("checksum");
		byte[] data = intent.getByteArrayExtra("data");
		
		if (data == null)
			return;
		
		Intent APGintent = new Intent(DECRYPT_AND_RETURN);
		APGintent.setType("text/plain");
		APGintent.putExtra(FileDecryptionActivity.EXTRA_DATA, data);

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

			OrgDatabase db = new OrgDatabase(this);
			OrgFileParser parser = new OrgFileParser(db, getContentResolver());
			parser.parse(new OrgFile(filename, name, checksum), reader, this);
			db.close();
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
				Toast.makeText(this, R.string.error_apg_version_not_supported,
						Toast.LENGTH_SHORT).show();
			}
		} catch (NameNotFoundException e) {
			Toast.makeText(this, R.string.error_apg_not_found, Toast.LENGTH_SHORT)
					.show();
		}
		return false;
	}
}