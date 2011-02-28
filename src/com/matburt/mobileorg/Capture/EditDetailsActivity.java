package com.matburt.mobileorg;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.View.OnClickListener;
import android.preference.Preference;
import android.preference.ListPreference;
import android.widget.Toast;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageItemInfo;
import android.util.Log;
import android.graphics.PorterDuff;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.HashMap;

public class EditDetailsActivity extends Activity implements OnClickListener
{
    public static final String LT = "MobileOrg";
    private ScrollView scrollableLayout = null;
    private TableLayout mainLayout = null;
    private MobileOrgDatabase appdb;
    private MobileOrgApplication appinst;
    private ArrayList<Integer> npath;
    private ArrayList<Button> buttonList = null;
    private Node activeNode = null;
    private CreateEditNote noteEditor = null;
    private String editType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent txtIntent = getIntent();
        this.npath = txtIntent.getIntegerArrayListExtra("nodePath");
        this.editType = txtIntent.getStringExtra("editType");

        this.noteEditor = new CreateEditNote(this);
        this.appdb = new MobileOrgDatabase((Context)this);

        if (this.editType.indexOf("todo") != -1) {
            this.editTodo();
        }
        else if (this.editType.indexOf("priority") != -1) {
            this.editPriority();
        }
    }

    private void editPriority() {
        ArrayList<ArrayList<String>> allPriorities = this.appdb.getPriorities();
        this.buttonList = new ArrayList<Button>();
        mainLayout = new TableLayout(this);
        mainLayout.setLayoutParams(
                     new TableLayout.LayoutParams(
                          LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mainLayout.setColumnStretchable(0, true);
        for (int idx = 0; idx < allPriorities.size(); idx++) {
            for (int jdx = 0; jdx < allPriorities.get(idx).size(); jdx++) {
                TableRow aTr = new TableRow(this);
                aTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
                Button aButton = new Button(this);
                aButton.setText(allPriorities.get(idx).get(jdx));
                aButton.setOnClickListener(this);
                aTr.addView(aButton);
                mainLayout.addView(aTr);
                this.buttonList.add(aButton);
            }
            TableRow nTr = new TableRow(this);
            nTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
            TextView spacer = new TextView(this);
            spacer.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
            nTr.addView(spacer);
            mainLayout.addView(nTr);
        }
        setContentView(mainLayout);
        this.populateInfo();
    }

    private void editTodo() {
        ArrayList<HashMap<String, Integer>> allTodos = this.appdb.getTodos();
        this.buttonList = new ArrayList<Button>();
        scrollableLayout = new ScrollView(this);
        mainLayout = new TableLayout(this);
        mainLayout.setLayoutParams(
                     new TableLayout.LayoutParams(
                          LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mainLayout.setColumnStretchable(0, true);
        for (HashMap<String, Integer> group : allTodos) {
            for (String key : group.keySet()) {
                TableRow aTr = new TableRow(this);
                aTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
                Button aButton = new Button(this);
                aButton.setText(key);
                aButton.setOnClickListener(this);
                if (group.get(key) > 0)
                    aButton.getBackground().setColorFilter(0xFF00FF00,
                                                           PorterDuff.Mode.MULTIPLY);
                else
                    aButton.getBackground().setColorFilter(0xFFFF0000,
                                                           PorterDuff.Mode.MULTIPLY);
                aButton.setTextColor(Color.WHITE);
                aTr.addView(aButton);
                mainLayout.addView(aTr);
                this.buttonList.add(aButton);
            }
            TableRow nTr = new TableRow(this);
            nTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
            TextView spacer = new TextView(this);
            spacer.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.FILL_PARENT));
            nTr.addView(spacer);
            mainLayout.addView(nTr);
        }
        scrollableLayout.addView(mainLayout);
        setContentView(scrollableLayout);
        this.populateInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.noteEditor != null) {
            this.noteEditor.close();
        }

        if (this.appdb != null) {
            this.appdb.close();
        }
    }

    public void saveEditPriority(String newPriority) {
        this.noteEditor.editNote("priority",
                                 this.activeNode.nodeId,
                                 this.activeNode.nodeName,
                                 this.activeNode.priority,
                                 newPriority);
    }

    public void saveEditTodo(String newTodo) {
        this.noteEditor.editNote("todo",
                                 this.activeNode.nodeId,
                                 this.activeNode.nodeName,
                                 this.activeNode.todo,
                                 newTodo);
    }

    public void populateInfo() {
        this.appinst = (MobileOrgApplication)this.getApplication();
        Node thisNode = this.appinst.rootNode;
        Intent textIntent = new Intent();
        String displayBuffer = new String();
        for (int idx = 0; idx < this.npath.size(); idx++) {
            thisNode = thisNode.subNodes.get(this.npath.get(idx));
        }
        this.activeNode = thisNode;
    }

    public void onClick(View v) {
        for (int idx = 0; idx < this.buttonList.size(); idx++) {
            if (v == this.buttonList.get(idx)) {
                if (this.editType.indexOf("todo") != -1) {
                    this.saveEditTodo(this.buttonList.get(idx).getText().toString());
                }
                else if (this.editType.indexOf("priority") != -1) {
                    this.saveEditPriority(this.buttonList.get(idx).getText().toString());
                }
                
                //triggers a refresh of the main display
                //doesn't seem to work
                this.appinst.rootNode = null;
                this.finish();
            }
        }
    }
}