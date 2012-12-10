package com.matburt.mobileorg.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class OrgUtils {
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
	
	public static String getDefaultTodo(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("defaultTodo", "");
	}
    
	public static boolean useAdvancedCapturing(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("captureAdvanced", true);
	}
	
	public static int getLevelOfRecursion(Context context) {
		return Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						"viewRecursionMax", "0"));
	}
	
    public static boolean isSyncConfigured(Context context) {
    	String syncSource = PreferenceManager.getDefaultSharedPreferences(context)
		.getString("syncSource", "");
    	
    	if(TextUtils.isEmpty(syncSource))
    		return false;
    	else
    		return true;
    }
    
	public static boolean getCombineBlockAgendas(Context context) {
		try {
			return PreferenceManager.getDefaultSharedPreferences(context)
					.getBoolean("combineBlockAgendas", false);
		} catch (UnsupportedOperationException e) { return false; }
	}
    
    public static void setupSpinnerWithEmpty(Spinner spinner, ArrayList<String> data,
			String selection) {
		data.add("");
		setupSpinner(spinner, data, selection);
    }
    
	public static void setupSpinner(Spinner spinner, ArrayList<String> data,
			String selection) {		
		if(!TextUtils.isEmpty(selection) && !data.contains(selection))
			data.add(selection);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(spinner.getContext(),
				android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(R.layout.edit_spinner_layout);
		spinner.setAdapter(adapter);
		int pos = data.indexOf(selection);
		if (pos < 0) {
			pos = 0;
		}
		spinner.setSelection(pos, true);
	}
	
	public static OrgNode getCaptureIntentContents(Intent intent) {
		String subject = intent
				.getStringExtra("android.intent.extra.SUBJECT");
		String text = intent.getStringExtra("android.intent.extra.TEXT");

		if(text != null && subject != null) {
			subject = "[[" + text + "][" + subject + "]]";
			text = "";
		}
		
		if(subject == null)
			subject = "";
		if(text == null)
			text = "";

		OrgNode node = new OrgNode();
		node.name = subject;
		node.setPayload(text);
		return node;
	}
	

	public static long getNodeFromPath(String path, ContentResolver resolver) throws OrgFileNotFoundException {
		String filename = path.substring("file://".length(), path.length());
		
		// TODO Handle links to headings instead of simply stripping it out
		if(filename.indexOf(":") > -1)
			filename = filename.substring(0, filename.indexOf(":"));
				
		OrgFile file = new OrgFile(filename, resolver);
		return file.nodeId;
	}
	
	public static void announceSyncDone(Context context) {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		context.sendBroadcast(intent);
	}
	
	public static void announceSyncStart(Context context) {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_START, true);
		context.sendBroadcast(intent);
	}
	
	public static void announceSyncUpdateProgress(int progress, Context context) {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_PROGRESS_UPDATE, progress);
		context.sendBroadcast(intent);
	}

    public static String getStringFromResource(int resource, Context context) {
        InputStream is = context.getResources().openRawResource(resource);
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
    

    public static boolean isUpgradedVersion(Context context) {
        SharedPreferences appSettings =
            PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = appSettings.edit();
        int versionCode = appSettings.getInt("appVersion", -1);
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int newVersion = pInfo.versionCode;
            if (versionCode != -1 && versionCode != newVersion) {
                editor.putInt("appVersion", newVersion);
                editor.commit();
                return true;
            }
        } catch (Exception e) { };
        return false;
    }
    
	
    public static class SortIgnoreCase implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }
    
    public static String getThemeName(Context context) {
        SharedPreferences appSettings =
                PreferenceManager.getDefaultSharedPreferences(context);
        return appSettings.getString("theme", "Dark");
    }
    
    public static void setTheme(Activity activity) {
    	String themeName = getThemeName(activity);
    	
    	if(themeName.equals("Dark"))
    		activity.setTheme(R.style.Theme_MobileOrg_Dark);
    	else 
    		activity.setTheme(R.style.Theme_MobileOrg_Light);
    }
    
	public static byte[] serializeObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.close();

			byte[] buf = bos.toByteArray();

			return buf;
		} catch (IOException ioe) {
			Log.e("serializeObject", "error", ioe);

			return null;
		}
	}

	public static Object deserializeObject(byte[] b) {
		try {
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(b));
			Object object = in.readObject();
			in.close();

			return object;
		} catch (ClassNotFoundException cnfe) {
			Log.e("deserializeObject", "class not found error", cnfe);

			return null;
		} catch (IOException ioe) {
			Log.e("deserializeObject", "io error", ioe);

			return null;
		}
	}
}
