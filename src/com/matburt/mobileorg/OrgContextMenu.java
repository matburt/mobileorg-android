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
    private Button docEditTitleButton;
    private Button docEditBodyButton;
    private Button docTodoButton;
    private Button docPriorityButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.longcontext);
        this.docButton = (Button)this.findViewById(R.id.documentMode);
        this.docEditTitleButton = (Button)this.findViewById(R.id.documentSetTitle);
        this.docEditBodyButton = (Button)this.findViewById(R.id.documentSetBody);
        //NOTE: Remove when editing the body works
        this.docEditBodyButton.setVisibility(View.GONE);
        this.docTodoButton = (Button)this.findViewById(R.id.documentSetTodo);
        this.docPriorityButton = (Button)this.findViewById(R.id.documentSetPriority);
        this.docButton.setOnClickListener(this);
        this.docEditTitleButton.setOnClickListener(this);
        this.docEditBodyButton.setOnClickListener(this);
        this.docTodoButton.setOnClickListener(this);
        this.docPriorityButton.setOnClickListener(this);
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
        if (thisNode.todo == null || thisNode.todo.length() < 1) {
            this.docTodoButton.setVisibility(View.GONE);
        }

        if (thisNode.priority == null || thisNode.priority.length() < 1) {
            this.docPriorityButton.setVisibility(View.GONE);
        }
        appInst.popSelection();
    }

    public void onClick(View v) {
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

        if (v == this.docButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SimpleTextDisplay");
            textIntent.putExtra("txtValue", thisNode.nodePayload);
        }
        else if (v == this.docEditTitleButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.Capture");
            if (thisNode.nodeId != null && thisNode.nodeId.length() > 0) {
                textIntent.putExtra("nodeId", thisNode.nodeId);
            }
            textIntent.putExtra("editType", "heading");
            textIntent.putExtra("txtValue", thisNode.nodeName);
        }
        else if (v == this.docEditBodyButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.Capture");
            if (thisNode.nodeId != null && thisNode.nodeId.length() > 0) {
                textIntent.putExtra("nodeId", thisNode.nodeId);
            }
            textIntent.putExtra("editType", "body");
            textIntent.putExtra("txtValue", thisNode.nodePayload);
        }

        else if (v == this.docTodoButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.EditDetailsActivity");
            textIntent.putExtra("nodePath", this.npath);
            textIntent.putExtra("editType", "todo");
        }
        else if (v == this.docPriorityButton) {
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.EditDetailsActivity");
            textIntent.putExtra("nodePath", this.npath);
            textIntent.putExtra("editType", "priority");
        }
        textIntent.putExtra("nodeTitle", thisNode.nodeName);
        startActivity(textIntent);
    }
}