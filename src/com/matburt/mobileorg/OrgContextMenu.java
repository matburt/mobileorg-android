package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.widget.Button;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import android.view.View;
import android.view.View.OnClickListener;

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
        this.docEditButton = (Button)this.findViewById(R.id.documentModeEdit);
        this.docButton.setOnClickListener(this);
        this.docEditButton.setOnClickListener(this);
        this.poplateDisplay();
    }

    public void poplateDisplay() {
        Intent txtIntent = getIntent();
        this.npath = txtIntent.getIntegerArrayListExtra("nodePath");
    }

    public void onClick(View v) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        Node thisNode = appInst.rootNode;
        Intent textIntent = new Intent();
        String displayBuffer = new String();
        for (int idx = 0; idx < this.npath.size(); idx++) {
            thisNode = thisNode.subNodes.get(
                                             this.npath.get(idx));
        }
        displayBuffer = thisNode.nodeName + "\n"+ thisNode.nodePayload + "\n";
        for (int idx = 0; idx < thisNode.subNodes.size(); idx++) {
            displayBuffer += thisNode.subNodes.get(idx).nodeTitle +
                "\n" + thisNode.subNodes.get(idx).nodePayload + "\n\n";
        }

        if (v == this.docButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SimpleTextDisplay");
        }
        else if (v == this.docEditButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.Capture");
        }
        textIntent.putExtra("txtValue", displayBuffer);
        startActivity(textIntent);
    }
}