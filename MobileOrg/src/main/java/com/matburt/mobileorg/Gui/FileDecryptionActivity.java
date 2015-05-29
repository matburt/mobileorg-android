package com.matburt.mobileorg.Gui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgDatabase;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgFileParser;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;

public class FileDecryptionActivity extends Activity
{
    private static final String mOpenPgpPackageName = "org.sufficientlysecure.keychain";
    // TODO Required version?
    private static final int mMinRequiredVersion = 0;
    private static final String logTag = "MobileOrg Decrypt";
    private static final int DECRYPT_CODE = 42;

    private String filename;
    private String name;
    private String checksum;
    private byte[] data;

    private OpenPgpServiceConnection mServiceConnection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(isAvailable() == false)
			return;

                this.mServiceConnection = new OpenPgpServiceConnection(
                            FileDecryptionActivity.this.getApplicationContext(),
                            mOpenPgpPackageName,
                            new OpenPgpServiceConnection.OnBound() {
                                @Override
                                public void onBound(IOpenPgpService service) {
                                    Log.d(OpenPgpApi.TAG, "OpenPgpApi bound");
				    Intent intent = new Intent();
				    decryptAndVerify(intent);
                                }
                                @Override
                                public void onError(Exception e) {
                                    Log.e(OpenPgpApi.TAG, "Exception when binding OpenPgpApi!", e);
                                }
                            }
								       );
                this.mServiceConnection.bindToService();

		Intent intent = getIntent();
		this.filename = intent.getStringExtra("filename");
		this.name = intent.getStringExtra("filenameAlias");
		this.checksum = intent.getStringExtra("checksum");
		this.data = intent.getByteArrayExtra("data");
	}

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (this.mServiceConnection != null) {
                this.mServiceConnection.unbindFromService();
            }
        }

        public void decryptAndVerify(Intent intent) {
	    intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
            InputStream is = new ByteArrayInputStream(data);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
	    IOpenPgpService service = this.mServiceConnection.getService();
	    if (service == null) {
		Log.e(logTag, "service is null");
		return;
	    }

            OpenPgpApi api = new OpenPgpApi(this, service);
            Intent result = api.executeApi(intent, is, os);

	    switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
		Log.d(logTag, "decryption success for: " + this.filename);
                BufferedReader reader = new BufferedReader(
                       new InputStreamReader(
                                 new ByteArrayInputStream(os.toByteArray())));
                OrgDatabase db = new OrgDatabase(this);
                OrgFileParser parser = new OrgFileParser(db, getContentResolver());
                parser.parse(new OrgFile(filename, name, checksum), reader, this);
                db.close();
		break;
            }
	    case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
		Log.i(logTag, "user interaction required");
		PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
		try {
		    startIntentSenderForResult(pi.getIntentSender(), DECRYPT_CODE, null, 0, 0, 0);
		} catch (IntentSender.SendIntentException e) {
		    Log.e(logTag, "SendIntentException", e);
		}
		break;
	    }
	    case OpenPgpApi.RESULT_CODE_ERROR: {
		OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
		Log.e(logTag, "decryption error for: " + this.filename);
		Log.e(logTag, "getErrorId: " + error.getErrorId());
		Log.e(logTag, "getMessage: " + error.getMessage());
		break;
	    }
	    }
	}

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    Log.d(logTag, "onActivityResult resultCode: " + resultCode);
	    Log.d(logTag, "onActivityResult requestCode: " + requestCode);
	    if (resultCode == RESULT_OK) {
                decryptAndVerify(data);
            }
	    else {
		Log.e(logTag, "no succes for: " + this.filename);
	    }
        }

	private boolean isAvailable() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					mOpenPgpPackageName, 0);
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
