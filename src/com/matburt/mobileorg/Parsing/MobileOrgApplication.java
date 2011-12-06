package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;


public class MobileOrgApplication extends Application {
    private Node rootNode = null;
    private ArrayList<Node> nodestack = new ArrayList<Node>();
    
    public ArrayList<EditNode> edits;
    private OrgDatabase appdb;
    private OrgFileParser parser;
    
    @Override
    public void onCreate() {
    	this.appdb = new OrgDatabase(this);
		this.rootNode = new Node("");
		clearNodestack();
		this.parser = new OrgFileParser(getBaseContext(), this);
    }
    
    
    public boolean init() {
		if (this.appdb.getOrgFiles().isEmpty())
			return false;
		
		this.rootNode = this.parser.prepareRootNode();
		clearNodestack();
		
		this.edits = this.parser.parseEdits();
		
		return true;
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
    

    public void makeSureNodeIsParsed(Node node) {	
		if (node.parsed == false) {
			if (node.encrypted == false) {
				this.parser.parseFile(node.name, rootNode);
			} else {
			//	decryptNode(node);
				return;
			}
		}
    }

	/**
	 * This function is called by the synchronizer or capture for each file that
	 * has changed. It will set the parsed flag of the file's node to false.
	 * Additionally it will try to update the node stack to point to the new
	 * nodes, which will cause the user display to be updated appropriately.
	 */
    public void addOrUpdateFile(String filename, String name, String checksum) {
    	appdb.addOrUpdateFile(filename, name, checksum);
    	
    	Node filenode = this.rootNode.getChild(filename);
    	
    	if(filenode == null) {
    		filenode = new Node(filename, this.rootNode);
    		rootNode.sortChildren();
    	}
    	
		filenode.parsed = false;
    	
    	refreshNodestack(filename);
    }

	private void refreshNodestack(String filename) {
		if (nodestack.size() >= 2 && nodestack.get(1).name.equals(filename)) {
			parser.parseFile(filename, this.rootNode);

			ArrayList<Node> newNodestack = new ArrayList<Node>();
			newNodestack.add(this.rootNode);

			this.nodestack.remove(0);

			Node newNode = this.rootNode;
			for (Node node : this.nodestack) {
				newNode = newNode.getChild(node.name);
				if (newNode != null) {
					newNodestack.add(newNode);
				}
				else
					break;
			}

			this.nodestack = newNodestack;
		}
	}
    
    public boolean removeFile(String filename) {
    	appdb.removeFile(filename);
    	this.rootNode.removeChild(filename);
    	OrgFile orgfile = new OrgFile(filename, this.getApplicationContext());
    	orgfile.delete();
    	return true;
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
}
