package com.matburt.mobileorg;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import android.content.ActivityNotFoundException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;


class Encryption
{
    public static class Intent
    {
        public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
    }
    public static final int DECRYPT_MESSAGE = 0x21070001;

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";

    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;


    public static boolean decrypt(Activity activity, byte[] data)
    {
        android.content.Intent intent = new android.content.Intent(Intent.DECRYPT_AND_RETURN);
        intent.setType("text/plain");
        if (data == null)
        {
            return false;
        }
        try
        {
            intent.putExtra(Encryption.EXTRA_DATA, data);
            activity.startActivityForResult(intent, DECRYPT_MESSAGE);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            Log.e("MobileOrg", "Error: " + e.getMessage() + " while launching APG intent");
            return false;
        }
    }


    
    public static boolean isAvailable(Context context)
    {
        try
        {
            PackageInfo pi = context.getPackageManager().getPackageInfo(mApgPackageName, 0);
            if (pi.versionCode >= mMinRequiredVersion)
            {
                return true;
            }
            else
            {
                Toast.makeText(context,
                               R.string.apg_version_not_supported, 
                               Toast.LENGTH_SHORT).show();
            }
        }
        catch (NameNotFoundException e)
        {
            Toast.makeText(context,
                           R.string.apg_not_found, 
                           Toast.LENGTH_SHORT).show();
        }

        return false;
    }
}