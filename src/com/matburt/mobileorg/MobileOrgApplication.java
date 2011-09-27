package com.matburt.mobileorg;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class MobileOrgApplication extends Application {
    public Node rootNode = null;
    private ArrayList<Node> nodestack = new ArrayList<Node>();
    
    public ArrayList<EditNode> edits;
    private OrgDatabase appdb;
    
    @Override
    public void onCreate() {
    	this.appdb = new OrgDatabase(this);
    }
    
    public void pushNodestack(Node node) {
        nodestack.add(node);
    }

    public void popNodestack() {
        if (this.nodestack.size() > 0)
            this.nodestack.remove(nodestack.size()-1);
    }
       
	public void clearNodestack() {
		this.nodestack.clear();
		this.nodestack.add(this.rootNode);
	}
    
    public Node nodestackTop() {
        if (this.nodestack.size() > 0)
            return this.nodestack.get(nodestack.size()-1);
    	return null;
    }
    
    public int nodestackSize() {
    	return this.nodestack.size();
    }


	public void refreshNodestack() {

	}
    
    public boolean isSynchConfigured() {
    	SharedPreferences appSettings = PreferenceManager
		.getDefaultSharedPreferences(getBaseContext());
    	
		if (appSettings.getString("syncSource", "").equals("")
				|| (appSettings.getString("syncSource", "").equals(
						"webdav") && appSettings.getString("webUrl", "")
						.equals(""))
				|| (appSettings.getString("syncSource", "").equals(
						"sdcard") && appSettings.getString(
						"indexFilePath", "").equals("")))
			return false;
		else
			return true;
	}

    //TODO Should do something else
	public static String nodeSelectionStr(ArrayList<Integer> nodes) {
		if (nodes != null) {
			String tmp = "";
	
			for (Integer i : nodes) {
				if (tmp.length() > 0)
					tmp += ",";
				tmp += i;
			}
			return tmp;
		}
		return "null";
	}

	static String getStorageFolder()
    {
        File root = Environment.getExternalStorageDirectory();   
        File morgDir = new File(root, "mobileorg");
        return morgDir.getAbsolutePath() + "/";
    }

    public static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";
	
    public static List<PackageItemInfo> discoverSynchronizerPlugins(Context context)
    {
        Intent discoverSynchro = new Intent(SYNCHRONIZER_PLUGIN_ACTION);
        List<ResolveInfo> packages = context.getPackageManager().queryIntentActivities(discoverSynchro,0);
        Log.d("MobileOrg","Found " + packages.size() + " total synchronizer plugins");

        ArrayList<PackageItemInfo> out = new ArrayList<PackageItemInfo>();

        for (ResolveInfo info : packages)
        {
            out.add(info.activityInfo);
            Log.d("MobileOrg","Found synchronizer plugin: "+info.activityInfo.packageName);            
        }
        return out;
    }
    
    public HashMap<String, String> getOrgFiles() {
    	return appdb.getOrgFiles();
    }
    
    public ArrayList<String> getPriorities() {
    	return appdb.getPriorities();
    }
    
    public ArrayList<HashMap<String, Integer>> getGroupedTodods() {
    	return appdb.getGroupedTodods();
    }
    
    public ArrayList<String> getTodods() {
    	return appdb.getTodods();
    }
    
    public void addOrUpdateFile(String filename, String name, String checksum) {
    	appdb.addOrUpdateFile(filename, name, checksum);
    }
}