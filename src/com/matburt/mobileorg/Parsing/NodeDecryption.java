package com.matburt.mobileorg.Parsing;

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

public class NodeDecryption extends Activity
{
    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;
    private static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
    private static final int DECRYPT_MESSAGE = 0x21070001;
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";


    private String filename;
    private long file_id;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(isAvailable() == false)
			return;
		
		Intent intent = getIntent();
		
		this.filename = intent.getStringExtra("filename");
		this.file_id = intent.getLongExtra("file_id", -1);
		
		if(this.file_id == -1)
			return;
		
		String data = intent.getStringExtra("data");
		
		if (data == null)
			return;
		
		Intent APGintent = new Intent(DECRYPT_AND_RETURN);
		APGintent.setType("text/plain");
		APGintent.putExtra(NodeDecryption.EXTRA_TEXT, data);

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
		case NodeDecryption.DECRYPT_MESSAGE:
			if (resultCode != RESULT_OK || intent == null)
				return;

			String decryptedData = intent
					.getStringExtra(NodeDecryption.EXTRA_DECRYPTED_MESSAGE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(decryptedData.getBytes())));

			OrgFileParser parser = new OrgFileParser(
					((MobileOrgApplication) this.getApplication()).getDB());
			parser.parse(filename, reader, file_id, getApplicationContext());

			finish();
			break;
		}
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
}