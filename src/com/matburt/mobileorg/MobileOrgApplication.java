package com.matburt.mobileorg;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.util.Log;
import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MobileOrgApplication extends Application {
    public Node rootNode = null;
    public ArrayList<Integer> nodeSelection;
    public ArrayList<EditNode> edits;
    public static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";
    protected Context mContext;

    public void setSelection(ArrayList<Integer> selection) {
        nodeSelection = selection;
    }

    public void setContext(Context context) {
        mContext = context;
    }
    public void pushSelection(int selectedNode)
    {
        if (nodeSelection == null) {
            nodeSelection = new ArrayList<Integer>();
        }
        nodeSelection.add(new Integer(selectedNode));
    }

    public void popSelection()
    {
        if (nodeSelection.size() > 0)
            this.nodeSelection.remove(nodeSelection.size()-1);
    }

    public Node getSelectedNode()
    {
    	return getNode(this.nodeSelection);
    }

    /**
     * Convenience function for retrieving a node based on a path from the root node.
     * @param path ArrayList of integers representing the indexes
     * @return node
     */
    public Node getNode(ArrayList<Integer> path) {
    	return getNode(path, path.size());
    }
    public Node getParent(ArrayList<Integer> path) {
    	return getNode(path, path.size() - 1);
	}
    
    public Node getNode(ArrayList<Integer> path, int count) {
    	Node thisNode = rootNode;
    	if (path != null) {
    		for (int idx = 0; idx < count; idx++) {
    			thisNode = thisNode.subNodes.get(path.get(idx));
    		}
    	}
    	return thisNode;
    }
    
    public ArrayList<EditNode> findEdits(String nodeId) {
        ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();
        if (this.edits == null)
            return thisEdits;
        for (int idx = 0 ; idx < this.edits.size(); idx++)
            {
                String compareS = "";
                if (nodeId.indexOf("olp:") == 0)
                    compareS = "olp:" + this.edits.get(idx).nodeId;
                else
                    compareS = this.edits.get(idx).nodeId;
                if (compareS.equals(nodeId)) {
                    thisEdits.add(this.edits.get(idx));
                }
            }
        return thisEdits;
    }

    static String getStorageFolder()
    {
        File root = Environment.getExternalStorageDirectory();   
        File morgDir = new File(root, "mobileorg");
        return morgDir.getAbsolutePath() + "/";
    }

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