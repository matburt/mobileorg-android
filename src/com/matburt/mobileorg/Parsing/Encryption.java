package com.matburt.mobileorg.Parsing;

import com.matburt.mobileorg.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.widget.Toast;


public class Encryption
{
    public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
    public static final int DECRYPT_MESSAGE = 0x21070001;

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";

    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;


	public static boolean decrypt(Activity activity, byte[] data) {
		if (data == null)
			return false;
		
		Intent intent = new Intent(DECRYPT_AND_RETURN);
		intent.setType("text/plain");
		intent.putExtra(Encryption.EXTRA_DATA, data);

		try {
			activity.startActivityForResult(intent, DECRYPT_MESSAGE);
			return true;
		} catch (ActivityNotFoundException e) {
			Log.e("MobileOrg", "Error: " + e.getMessage()
					+ " while launching APG intent");
			return false;
		}
	}
    
	public static boolean isAvailable(Context context) {
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(
					mApgPackageName, 0);
			if (pi.versionCode >= mMinRequiredVersion) {
				return true;
			} else {
				Toast.makeText(context, R.string.apg_version_not_supported,
						Toast.LENGTH_SHORT).show();
			}
		} catch (NameNotFoundException e) {
			Toast.makeText(context, R.string.apg_not_found, Toast.LENGTH_SHORT)
					.show();
		}

		return false;
	}
}