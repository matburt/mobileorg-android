package com.matburt.mobileorg;

import android.app.Application;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import android.util.Log;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;

public class MobileOrgApplication extends Application {
    public Node rootNode = null;
    public ArrayList<Integer> nodeSelection;
    public static final String SYNCHRONIZER_PLUGIN_ACTION = "com.matburt.mobileorg.SYNCHRONIZE";

    public void pushSelection(int selectedNode)
    {
        if (nodeSelection == null) {
            nodeSelection = new ArrayList<Integer>();
        }
        nodeSelection.add(new Integer(selectedNode));
    }

    public void popSelection()
    {
        nodeSelection.remove(nodeSelection.size()-1);        
    }

    public Node getSelectedNode()
    {
        Node thisNode = rootNode;
        if (nodeSelection != null) {
            for (int idx = 0; idx < nodeSelection.size(); idx++) {
                thisNode = thisNode.subNodes.get(nodeSelection.get(idx));
            }
        }
        return thisNode;
    }

    static String getStorageFolder()
    {
        File root = Environment.getExternalStorageDirectory();   
        File morgDir = new File(root, "mobileorg");
        return morgDir.getAbsolutePath() + "/";
    }

    static List<PackageItemInfo> discoverSynchronizerPlugins(Context context)
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