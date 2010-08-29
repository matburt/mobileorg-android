package com.matburt.mobileorg;

import android.app.Application;
import java.util.ArrayList;
import java.io.File;
import android.os.Environment;

public class MobileOrgApplication extends Application {
    public Node rootNode = null;
    public ArrayList<Integer> nodeSelection;

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

    
}