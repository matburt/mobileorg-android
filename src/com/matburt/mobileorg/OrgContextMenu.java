package com.matburt.mobileorg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.matburt.mobileorg.Capture.ViewNodeDetailsActivity;
import com.matburt.mobileorg.Parsing.Node;
import java.util.ArrayList;

public class OrgContextMenu extends Activity implements OnClickListener
{
    public static final String LT = "MobileOrg";
    ArrayList<Integer> npath;
    private Button docButton;
    private Button docEditButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.longcontext);
        this.docButton = (Button)this.findViewById(R.id.documentMode);
        this.docEditButton = (Button)this.findViewById(R.id.documentEdit);
        this.docButton.setOnClickListener(this);
        this.docEditButton.setOnClickListener(this);
        this.poplateDisplay();
    }

    public void poplateDisplay() {
        Intent txtIntent = getIntent();
        this.npath = txtIntent.getIntegerArrayListExtra("nodePath");
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        Node thisNode = appInst.rootNode;
        Intent textIntent = new Intent();
        String displayBuffer = new String();
        if (thisNode == null)
            return;

        for (int idx = 0; idx < this.npath.size(); idx++) {
            thisNode = thisNode.subNodes.get(
                                             this.npath.get(idx));
        }
        //If we need to do something special with the context menu based on the Node we'll do it here
        appInst.popSelection();
    }

    public void onClick(View v) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        Node thisNode = appInst.getNode(this.npath);
        if (thisNode == null) {
        	return;
        }
        Intent textIntent = new Intent();
        String displayBuffer = new String();
        if (v == this.docButton) {
            textIntent.setClass(this,
                    SimpleTextDisplay.class);
            textIntent.putExtra("txtValue", thisNode.nodePayload);
            textIntent.putExtra("nodeTitle", thisNode.nodeName);
        }
        else if (v == this.docEditButton) {
            textIntent.setClass(this, ViewNodeDetailsActivity.class);
            textIntent.putExtra("actionMode", "edit");
            textIntent.putIntegerArrayListExtra("nodePath", this.npath);
        }
        startActivity(textIntent);
    }
}
